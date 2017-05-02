package format.collada

import glm.*
import main.BaseImporter
import java.net.URI
import main.*
import unsigned.Uint
import unsigned.ui
import glm.mat4x4.Mat4

/**
 * Created by elect on 23/01/2017.
 */

class ColladaLoader : BaseImporter() {

    companion object {

        val desc = AiImporterDesc(
                mName = "Collada Importer",
                mComments = "http://collada.org",
                mFlags = main.AiImporterFlags.SupportTextFlavour.i,
                mMinMajor = 1, mMinMinor = 3,
                mMaxMajor = 1, mMaxMinor = 5,
                mFileExtensions = "dae"
        )
    }

    /** Filename, for a verbose error message */
    lateinit var mFileName: URI

    /** Which mesh-material compound was stored under which mesh ID */
    val mMeshIndexByID = mutableMapOf<ColladaMeshIndex, Int>()

    /** Which material was stored under which index in the scene */
    val mMaterialIndexByName = mutableMapOf<String, Int>()

    /** Accumulated meshes for the target scene */
    val mMeshes = ArrayList<AiMesh>()

    /** Accumulated morph target meshes */
    val mTargetMeshes = ArrayList<AiMesh>()

    /** Temporary material list */
    val newMats = ArrayList<Pair<Effect, AiMaterial>>()

    /** Temporary camera list */
    val mCameras = ArrayList<AiCamera>()

    /** Temporary light list */
    val mLights = ArrayList<AiLight>()

    /** Temporary texture list */
    val mTextures = ArrayList<AiTexture>()

    /** Accumulated animations for the target scene */
    val mAnims = ArrayList<AiAnimation>()

    val noSkeletonMesh = false
    val ignoreUpDirection = false

    /** Used by FindNameForNode() to generate unique node names */
    var mNodeNameCounter = 0

    // ------------------------------------------------------------------------------------------------
    // Returns whether the class can handle the format of the given file.
    override fun canRead(pFile: URI, checkSig: Boolean): Boolean {

        // check file extension
        val extension = pFile.s.substring(pFile.s.lastIndexOf('.') + 1)

        if (extension == "dae")
            return true

        // XML - too generic, we need to open the file and search for typical keywords
        if (extension == "xml" || extension.isEmpty() || checkSig)
            TODO()
        return false
    }

    // ------------------------------------------------------------------------------------------------
    // Imports the given file into the given scene structure.
    override fun internReadFile(pFile: URI, pScene: AiScene) {

        mFileName = pFile

        // parse the input file
        val parser = ColladaParser(pFile)

        if (parser.mRootNode == null)
            throw Error("Collada: File came out empty. Something is wrong here.")

        // create the materials first, for the meshes to find
        buildMaterials(parser)

        // build the node hierarchy from it
        pScene.mRootNode = buildHierarchy(parser, parser.mRootNode!!)

        // ... then fill the materials with the now adjusted settings
        fillMaterials(parser)
    }

    /** Recursively constructs a scene node for the given parser node and returns it.   */
    fun buildHierarchy(pParser: ColladaParser, pNode: Node): AiNode {

        // create a node for it and find a name for the new node. It's more complicated than you might think
        val node = AiNode(mName = findNameForNode(pNode))

        // calculate the transformation matrix for it
        node.mTransformation = pParser.calculateResultTransform(pNode.mTransforms)

        // now resolve node instances
        val instances = ArrayList<Node>()
        resolveNodeInstances(pParser, pNode, instances)

        // add children. first the *real* ones
        node.mNumChildren = pNode.mChildren.size + instances.size
        node.mChildren = MutableList(node.mNumChildren, { AiNode() })

        for (a in 0 until pNode.mChildren.size) {
            node.mChildren[a] = buildHierarchy(pParser, pNode.mChildren[a])
            node.mChildren[a].mParent = node
        }

        // ... and finally the resolved node instances
        for (a in 0 until pNode.mChildren.size) {
            node.mChildren[pNode.mChildren.size + a] = buildHierarchy(pParser, instances[a])
            node.mChildren[pNode.mChildren.size + a].mParent = node
        }

        // construct meshes
        buildMeshesForNode(pParser, pNode, node)

        // construct cameras
        buildCamerasForNode(pParser, pNode, node)

        // construct lights
        buildLightsForNode(pParser, pNode, node)

        return node
    }

    /** Builds lights for the given node and references them    */
    fun buildLightsForNode(pParser: ColladaParser, pNode: Node, pTarget: AiNode) = pNode.mLights.forEach {

        // find the referred light
        val srcLight = pParser.mLightLibrary[it.mLight] ?: run {
            println("Collada: Unable to find light for ID \"${it.mLight}\". Skipping.")
            return
        }

        // now fill our ai data structure
        val out = AiLight(mName = pTarget.mName, mType = srcLight.mType)

        // collada lights point in -Z by default, rest is specified in node transform
        out.mDirection.put(0f, 0f, -1f)

        out.mAttenuationConstant = srcLight.mAttConstant
        out.mAttenuationLinear = srcLight.mAttLinear
        out.mAttenuationQuadratic = srcLight.mAttQuadratic

        val intensity = srcLight.mColor * srcLight.mIntensity
        out.mColorDiffuse = intensity
        out.mColorSpecular = intensity
        out.mColorAmbient = intensity
        if (out.mType == AiLightSourceType.AMBIENT) {
            out.mColorDiffuse = AiColor3D(0)
            out.mColorSpecular = AiColor3D(0)
            out.mColorAmbient = srcLight.mColor * srcLight.mIntensity
        } else {
            // collada doesn't differentiate between these color types
            out.mColorDiffuse = intensity
            out.mColorSpecular = intensity
            out.mColorAmbient = AiColor3D(0)
        }

        // convert falloff angle and falloff exponent in our representation, if given
        if (out.mType == AiLightSourceType.SPOT) {

            out.mAngleInnerCone = srcLight.mFalloffAngle.rad

            // ... some extension magic.
            if (srcLight.mOuterAngle >= ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET * (1 - 1e-6f)) {
                // ... some deprecation magic.
                if (srcLight.mPenumbraAngle >= ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET * (1 - 1e-6f))
                // Need to rely on falloff_exponent. I don't know how to interpret it, so I need to guess .... epsilon chosen to be 0.1
                    out.mAngleOuterCone = glm.acos(glm.pow(.1f, 1f / srcLight.mFalloffExponent)) + out.mAngleInnerCone
                else {
                    out.mAngleOuterCone = out.mAngleInnerCone + srcLight.mPenumbraAngle.rad
                    if (out.mAngleOuterCone < out.mAngleInnerCone) {
                        val temp = out.mAngleInnerCone
                        out.mAngleInnerCone = out.mAngleOuterCone
                        out.mAngleOuterCone = temp
                    }
                }
            } else
                out.mAngleOuterCone = srcLight.mOuterAngle.rad
        }

        // add to light list
        mLights.add(out)
    }

    /** Builds cameras for the given node and references them   */
    fun buildCamerasForNode(pParser: ColladaParser, pNode: Node, pTarget: AiNode) = pNode.mCameras.forEach {

        // find the referred light
        val srcCamera = pParser.mCameraLibrary[it.mCamera] ?: run {
            println("Collada: Unable to find camera for ID \"${it.mCamera}\". Skipping.")
            return
        }

        // orthographic cameras not yet supported in Assimp
        if (srcCamera.mOrtho)
            println("Collada: Orthographic cameras are not supported.")

        // now fill our ai data structure
        val out = AiCamera(mName = pTarget.mName)

        // collada cameras point in -Z by default, rest is specified in node transform
        out.mLookAt.put(0f, 0f, -1f)

        // near/far z is already ok
        out.mClipPlaneFar = srcCamera.mZFar
        out.mClipPlaneNear = srcCamera.mZNear

        // ... but for the rest some values are optional and we need to compute the others in any combination.
        if (srcCamera.mAspect != 10e10f)
            out.mAspect = srcCamera.mAspect

        with(glm) {
            if (srcCamera.mHorFov != 10e10f) {
                out.mHorizontalFOV = srcCamera.mHorFov

                if (srcCamera.mVerFov != 10e10f && srcCamera.mAspect == 10e10f)
                    out.mAspect = tan(srcCamera.mHorFov.rad) / tan(srcCamera.mVerFov.rad)
            } else if (srcCamera.mAspect != 10e10f && srcCamera.mVerFov != 10e10f) {
                out.mHorizontalFOV = 2f * atan(srcCamera.mAspect * tan(srcCamera.mVerFov.rad * .5f)).deg
            }
        }

        // Collada uses degrees, we use radians
        out.mHorizontalFOV = out.mHorizontalFOV.rad

        // add to camera list
        mCameras.add(out)
    }

    /** Builds meshes for the given node and references them    */
    fun buildMeshesForNode(pParser: ColladaParser, pNode: Node, pTarget: AiNode) {

        var srcController: Controller? = null

        // accumulated mesh references by this node
        val newMeshRefs = ArrayList<Int>()

        // add a mesh for each subgroup in each collada mesh
        pNode.mMeshes.forEach { mid ->

            // find the referred mesh
            var srcMesh = pParser.mMeshLibrary[mid.mMeshOrController]
            if (srcMesh == null) {

                // if not found in the mesh-library, it might also be a controller referring to a mesh
                pParser.mControllerLibrary[mid.mMeshOrController]?.let {
                    srcController = it
                    srcMesh = pParser.mMeshLibrary[srcController!!.mMeshId]
                }

                if (srcMesh == null) {
                    System.out.println("Collada: Unable to find geometry for ID \"${mid.mMeshOrController}\". Skipping.")
                    return
                }
            }
            // else ID found in the mesh library -> direct reference to an unskinned mesh

            // build a mesh for each of its subgroups
            var vertexStart = 0
            var faceStart = 0
            for (sm in 0 until srcMesh!!.mSubMeshes.size) {

                val submesh = srcMesh!!.mSubMeshes[sm]

                if (submesh.mNumFaces == 0)
                    return

                // find material assigned to this submesh
                var meshMaterial = ""

                val table = mid.mMaterials[submesh.mMaterial]
                if (table != null)
                    meshMaterial = table.mMatName
                else {
                    System.out.println("Collada: No material specified for subgroup <${submesh.mMaterial}> in geometry <${mid.mMeshOrController}>.")
                    if (mid.mMaterials.isNotEmpty())
                        meshMaterial = mid.mMaterials.values.first().mMatName
                }

                // OK ... here the *real* fun starts ... we have the vertex-input-to-effect-semantic-table given.
                // The only mapping stuff which we do actually support is the UV channel.
                val matIdx = mMaterialIndexByName[meshMaterial] ?: 0

                if (table != null && table.mMap.isNotEmpty()) {

                    val mat = newMats[matIdx]

                    // Iterate through all texture channels assigned to the effect and
                    // check whether we have mapping information for it.
                    applyVertexToEffectSemanticMapping(mat.first.mTexDiffuse, table)
                    applyVertexToEffectSemanticMapping(mat.first.mTexAmbient, table)
                    applyVertexToEffectSemanticMapping(mat.first.mTexSpecular, table)
                    applyVertexToEffectSemanticMapping(mat.first.mTexEmissive, table)
                    applyVertexToEffectSemanticMapping(mat.first.mTexTransparent, table)
                    applyVertexToEffectSemanticMapping(mat.first.mTexBump, table)
                }

                // built lookup index of the Mesh-Submesh-Material combination
                val index = ColladaMeshIndex(mid.mMeshOrController, sm, meshMaterial)

                // if we already have the mesh at the library, just add its index to the node's array
                mMeshIndexByID[index]?.let {
                    newMeshRefs.add(it)
                    return
                }

                // else we have to add the mesh to the collection and store its newly assigned index at the node
                val dstMesh = createMesh(pParser, srcMesh!!, submesh, srcController, vertexStart, faceStart)

                // store the mesh, and store its new index in the node
                newMeshRefs.add(mMeshes.size)
                mMeshIndexByID[index] = mMeshes.size
                mMeshes.add(dstMesh)
                vertexStart += dstMesh.mNumVertices; faceStart += submesh.mNumFaces

                // assign the material index
                dstMesh.mMaterialIndex = matIdx
                if (dstMesh.mName.isEmpty())
                    dstMesh.mName = mid.mMeshOrController
            }
        }

        // now place all mesh references we gathered in the target node
        pTarget.mNumMeshes = newMeshRefs.size
        if (newMeshRefs.isNotEmpty())
            pTarget.mMeshes = newMeshRefs.toIntArray()
    }

    /** Find mesh from either meshes or morph target meshes */
    fun findMesh(meshid: String) = mMeshes.firstOrNull { it.mName == meshid } ?: mTargetMeshes.firstOrNull { it.mName == meshid }

    /** Creates a mesh for the given ColladaMesh face subset and returns the newly created mesh */
    fun createMesh(pParser: ColladaParser, pSrcMesh: Mesh, pSubMesh: SubMesh, pSrcController: Controller?, pStartVertex: Int, pStartFace: Int): AiMesh {

        val dstMesh = AiMesh(mName = pSrcMesh.mName)

        // count the vertices addressed by its faces
        val numVertices = pSrcMesh.mFaceSize.filterIndexed { i, value -> i in pStartFace until (pStartFace + pSubMesh.mNumFaces) }.sum()

        // copy positions
        dstMesh.mNumVertices = numVertices
        dstMesh.mVertices = pSrcMesh.mPositions.filterIndexed { i, vec3 -> i in pStartVertex until (pStartFace + numVertices) }.toMutableList()

        // normals, if given. HACK: (thom) Due to the glorious Collada spec we never know if we have the same number of normals as there are positions. So we
        // also ignore any vertex attribute if it has a different count
        if (pSrcMesh.mNormals.size >= pStartVertex + numVertices)
            dstMesh.mNormals = pSrcMesh.mNormals.filterIndexed { i, vec3 -> i in pStartVertex until (pStartFace + numVertices) }.toMutableList()

        // tangents, if given.
        if (pSrcMesh.mTangents.size >= pStartVertex + numVertices)
            dstMesh.mTangents = pSrcMesh.mTangents.filterIndexed { i, vec3 -> i in pStartVertex until (pStartFace + numVertices) }.toMutableList()

        // bitangents, if given.
        if (pSrcMesh.mBitangents.size >= pStartVertex + numVertices)
            dstMesh.mBitangents = pSrcMesh.mBitangents.filterIndexed { i, vec3 -> i in pStartVertex until (pStartFace + numVertices) }.toMutableList()

        // same for texturecoords, as many as we have empty slots are not allowed, need to pack and adjust UV indexes accordingly
        for (texNumber in 0 until pSrcMesh.mTexCoords.size) {
            dstMesh.mTextureCoords.add(mutableListOf())
            if (pSrcMesh.mTexCoords[texNumber].size >= pStartVertex + numVertices) {
                dstMesh.mTextureCoords[texNumber] = mutableListOf()
                for (v in 0 until numVertices)
                    dstMesh.mTextureCoords[texNumber].add(pSrcMesh.mTexCoords[texNumber][pStartVertex + v])
            }
        }

        // same for vertex colors, as many as we have. again the same packing to avoid empty slots
        for (colorNumber in 0 until pSrcMesh.mColors.size) {
            dstMesh.mColors.add(mutableListOf())
            if (pSrcMesh.mColors.size >= pStartVertex + numVertices)
                for (v in 0 until numVertices)
                    dstMesh.mColors[colorNumber].add(pSrcMesh.mColors[colorNumber][pStartVertex + v])
        }

        // create faces. Due to the fact that each face uses unique vertices, we can simply count up on each vertex
        var vertex = 0
        dstMesh.mNumFaces = pSubMesh.mNumFaces
        for (a in 0 until dstMesh.mNumFaces) {
            val s = pSrcMesh.mFaceSize[pStartFace + a]
            dstMesh.mFaces.add(
                    MutableList(s, { vertex++ })
            )
        }

        // create morph target meshes if any
        val targetMeshes = ArrayList<AiMesh>()
        val targetWeights = ArrayList<Float>()
        var method = MorphMethod.Normalized

        pParser.mControllerLibrary.forEach { _, c ->

            val baseMesh = pParser.mMeshLibrary[c.mMeshId]!!

            if (c.mType == ControllerType.Morph && baseMesh.mName == pSrcMesh.mName) {

                val targetAccessor = pParser.mAccessorLibrary[c.mMorphTarget]!!
                val weightAccessor = pParser.mAccessorLibrary[c.mMorphWeight]!!
                val targetData = pParser.mDataLibrary[targetAccessor.mSource]!!
                val weightData = pParser.mDataLibrary[weightAccessor.mSource]!!

                // take method
                method = c.mMethod

                if (!targetData.mIsStringArray)
                    throw Error("target data must contain id. ")
                if (weightData.mIsStringArray)
                    throw Error("target weight data must not be textual ")

                targetData.mStrings.forEach {

                    val targetMesh = pParser.mMeshLibrary[it]!!

                    targetMeshes.add(findMesh(targetMesh.mName) ?: run {
                        if (targetMesh.mSubMeshes.size > 1)
                            throw Error("Morhing target mesh must be a single")
                        createMesh(pParser, targetMesh, targetMesh.mSubMeshes[0], null, 0, 0)
                    })
                }
                targetWeights.addAll(weightData.mValues)
            }
        }
        if (targetMeshes.size > 0 && targetWeights.size == targetMeshes.size) {

            val animMeshes = ArrayList<AiAnimMesh>()
            targetMeshes.forEachIndexed { i, it ->

                val animMesh = aiCreateAnimMesh(it)
                animMesh.mWeight = targetWeights[i]
                animMeshes.add(animMesh)
            }
            dstMesh.mMethod = if (method == MorphMethod.Relative) AiMorphingMethod.MORPH_RELATIVE.i else AiMorphingMethod.MORPH_NORMALIZED.i
            dstMesh.mNumAnimMeshes = animMeshes.size
            dstMesh.mAnimMeshes.addAll(animMeshes)
        }

        // create bones if given
        if (pSrcController != null && pSrcController.mType == ControllerType.Skin) {

            // refuse if the vertex count does not match
//      if( pSrcController->mWeightCounts.size() != dstMesh->mNumVertices)
//          throw DeadlyImportError( "Joint Controller vertex count does not match mesh vertex count");

            // resolve references - joint names
            val jointNamesAcc = pParser.mAccessorLibrary[pSrcController.mJointNameSource]!!
            val jointNames = pParser.mDataLibrary[jointNamesAcc.mSource]!!
            // joint offset matrices
            val jointMatrixAcc = pParser.mAccessorLibrary[pSrcController.mJointOffsetMatrixSource]!!
            val jointMatrices = pParser.mDataLibrary[jointMatrixAcc.mSource]!!
            // joint vertex_weight name list - should refer to the same list as the joint names above. If not, report and reconsider
            val weightNamesAcc = pParser.mAccessorLibrary[pSrcController.mWeightInputJoints.mAccessor]!!
            if (weightNamesAcc !== jointNamesAcc)
                throw Error("Temporary implementational laziness. If you read this, please report to the author.")
            // vertex weights
            val weightsAcc = pParser.mAccessorLibrary[pSrcController.mWeightInputWeights.mAccessor]!!
            val weights = pParser.mDataLibrary[weightsAcc.mSource]!!

            if (!jointNames.mIsStringArray || jointMatrices.mIsStringArray || weights.mIsStringArray)
                throw Error("Data type mismatch while resolving mesh joints")
            // sanity check: we rely on the vertex weights always coming as pairs of BoneIndex-WeightIndex
            if (pSrcController.mWeightInputJoints.mOffset != 0 || pSrcController.mWeightInputWeights.mOffset != 1)
                throw Error("Unsupported vertex_weight addressing scheme. ")

            // create containers to collect the weights for each bone
            val numBones = jointNames.mStrings.size
            val dstBones = MutableList(numBones, { mutableListOf<AiVertexWeight>() })

            // build a temporary array of pointers to the start of each vertex's weights
            val weightStartPerVertex = ArrayList<Int>()

            var pit = 0
            pSrcController.mWeightCounts.forEachIndexed { i, a ->
                weightStartPerVertex[a] = pit
                pit += pSrcController.mWeightCounts[a]
            }

            // now for each vertex put the corresponding vertex weights into each bone's weight collection
            for (a in pStartVertex until pStartVertex + numVertices) {

                // which position index was responsible for this vertex? that's also the index by which the controller assigns the vertex weights
                val orgIndex = pSrcMesh.mFacePosIndices[a]
                // find the vertex weights for this vertex
                val iit = weightStartPerVertex[orgIndex]
                val pairCount = pSrcController.mWeightCounts[orgIndex]

                for (b in 0 until pairCount) {

                    val jointIndex = pSrcController.mWeights[iit].first
                    val vertexIndex = pSrcController.mWeights[iit].second

                    val weight = readFloat(weightsAcc, weights, vertexIndex, 0)

                    // one day I gonna kill that XSI Collada exporter
                    if (weight > 0f)
                        dstBones[jointIndex].add(AiVertexWeight(mVertexId = (a - pStartVertex).ui.v, mWeight = weight))
                }
            }

            // count the number of bones which influence vertices of the current submesh
            val numRemainingBones = dstBones.filter { it.isNotEmpty() }.size

            // create bone array and copy bone weights one by one
            dstMesh.mNumBones = numRemainingBones
            for (a in 0 until numBones) {

                // omit bones without weights
                if (dstBones[a].isEmpty())
                    continue

                // create bone with its weights
                val bone = AiBone(mName = readString(jointNamesAcc, jointNames, a))
                bone.mOffsetMatrix.a0 = readFloat(jointMatrixAcc, jointMatrices, a, 0)
                bone.mOffsetMatrix.a1 = readFloat(jointMatrixAcc, jointMatrices, a, 1)
                bone.mOffsetMatrix.a2 = readFloat(jointMatrixAcc, jointMatrices, a, 2)
                bone.mOffsetMatrix.a3 = readFloat(jointMatrixAcc, jointMatrices, a, 3)
                bone.mOffsetMatrix.b0 = readFloat(jointMatrixAcc, jointMatrices, a, 4)
                bone.mOffsetMatrix.b1 = readFloat(jointMatrixAcc, jointMatrices, a, 5)
                bone.mOffsetMatrix.b2 = readFloat(jointMatrixAcc, jointMatrices, a, 6)
                bone.mOffsetMatrix.b3 = readFloat(jointMatrixAcc, jointMatrices, a, 7)
                bone.mOffsetMatrix.c0 = readFloat(jointMatrixAcc, jointMatrices, a, 8)
                bone.mOffsetMatrix.c1 = readFloat(jointMatrixAcc, jointMatrices, a, 9)
                bone.mOffsetMatrix.c2 = readFloat(jointMatrixAcc, jointMatrices, a, 10)
                bone.mOffsetMatrix.c3 = readFloat(jointMatrixAcc, jointMatrices, a, 11)
                bone.mNumWeights = dstBones[a].size
                bone.mWeights = dstBones[a].toList()

                // apply bind shape matrix to offset matrix
                val bindShapeMatrix = Mat4()
                bindShapeMatrix.a0 = pSrcController.mBindShapeMatrix[0]
                bindShapeMatrix.a1 = pSrcController.mBindShapeMatrix[1]
                bindShapeMatrix.a2 = pSrcController.mBindShapeMatrix[2]
                bindShapeMatrix.a3 = pSrcController.mBindShapeMatrix[3]
                bindShapeMatrix.b0 = pSrcController.mBindShapeMatrix[4]
                bindShapeMatrix.b1 = pSrcController.mBindShapeMatrix[5]
                bindShapeMatrix.b2 = pSrcController.mBindShapeMatrix[6]
                bindShapeMatrix.b3 = pSrcController.mBindShapeMatrix[7]
                bindShapeMatrix.c0 = pSrcController.mBindShapeMatrix[8]
                bindShapeMatrix.c1 = pSrcController.mBindShapeMatrix[9]
                bindShapeMatrix.c2 = pSrcController.mBindShapeMatrix[10]
                bindShapeMatrix.c3 = pSrcController.mBindShapeMatrix[11]
                bindShapeMatrix.d0 = pSrcController.mBindShapeMatrix[12]
                bindShapeMatrix.d1 = pSrcController.mBindShapeMatrix[13]
                bindShapeMatrix.d2 = pSrcController.mBindShapeMatrix[14]
                bindShapeMatrix.d3 = pSrcController.mBindShapeMatrix[15]
                bone.mOffsetMatrix *= bindShapeMatrix

                // HACK: (thom) Some exporters address the bone nodes by SID, others address them by ID or even name.
                // Therefore I added a little name replacement here: I search for the bone's node by either name, ID or SID, and replace the bone's name by the
                // node's name so that the user can use the standard find-by-name method to associate nodes with bones.
                val bnode = findNode(pParser.mRootNode!!, bone.mName) ?: findNodeBySID(pParser.mRootNode!!, bone.mName)

                // assign the name that we would have assigned for the source node
                if (bnode != null)
                    bone.mName = findNameForNode(bnode)
                else
                    println("ColladaLoader::CreateMesh(): could not find corresponding node for joint \"${bone.mName}\".")

                // and insert bone
                dstMesh.mBones.add(bone)
            }
        }

        return dstMesh
    }

    /** Resolve node instances  */
    fun resolveNodeInstances(pParser: ColladaParser, pNode: Node, resolved: ArrayList<Node>) =
            // iterate through all nodes to be instanced as children of pNode
            pNode.mNodeInstances.forEach {

                // find the corresponding node in the library
                var nd = pParser.mNodeLibrary[it.mNode]

                // FIX for http://sourceforge.net/tracker/?func=detail&aid=3054873&group_id=226462&atid=1067632
                // need to check for both name and ID to catch all. To avoid breaking valid files,
                // the workaround is only enabled when the first attempt to resolve the node has failed.
                if (nd == null)
                    nd = findNode(pParser.mRootNode!!, it.mNode)

                if (nd == null)
                    System.err.println("Collada: Unable to resolve reference to instanced node ${it.mNode}")
                else
                    resolved.add(nd)    //  attach this node to the list of children
            }

    /** Resolve UV channels */
    fun applyVertexToEffectSemanticMapping(sampler: Sampler, table: SemanticMappingTable) = table.mMap[sampler.mUVChannel]?.let {
        if (it.mType != InputType.Texcoord)
            System.err.println("Collada: Unexpected effect input mapping")

        sampler.mUVId = it.mSet
    }

    /** Add a texture to a material structure   */
    fun addTexture(mat: AiMaterial, pParser: ColladaParser, effect: Effect, sampler: Sampler, type: AiTexture.Type, idx: Int = mat.textures.lastIndex) {

        // first of all, basic file name
        val tex = AiMaterial.Texture(type = type, file = findFilenameForEffectTexture(pParser, effect, sampler.mName))

        // mapping mode
        tex.mapModeU =
                if (sampler.mWrapU)
                    if (sampler.mMirrorU) AiTexture.MapMode.mirror
                    else AiTexture.MapMode.wrap
                else AiTexture.MapMode.clamp

        tex.mapModeV =
                if (sampler.mWrapV)
                    if (sampler.mMirrorU) AiTexture.MapMode.mirror
                    else AiTexture.MapMode.wrap
                else AiTexture.MapMode.clamp

        // UV transformation
        tex.uvTrafo = sampler.mTransform

        // Blend mode
        tex.op = sampler.mOp

        // Blend factor
        tex.blend = sampler.mWeighting

        // UV source index ... if we didn't resolve the mapping, it is actually just a guess but it works in most cases. We search for the frst occurrence of a
        // number in the channel name. We assume it is the zero-based index into the UV channel array of all corresponding meshes. It could also be one-based
        // for some exporters, but we won't care of it unless someone complains about.
        tex.uvwsrc =
                if (sampler.mUVId != Uint.MAX_VALUE.i) // TODO MAX_VALUE to Int
                    sampler.mUVId
                else
                    sampler.mUVChannel.firstOrNull(Char::isNumeric)?.let {
                        println("Collada: unable to determine UV channel for texture")
                        0
                    }

        mat.textures.add(idx, tex)
    }

    /** Fills materials from the collada material definitions   */
    fun fillMaterials(pParser: ColladaParser) = newMats.forEach {

        val mat = it.second
        val effect = it.first

        // resolve shading mode
        mat.shadingModel =
                if (effect.mFaceted) /* fixme */
                    AiShadingMode.flat
                else
                    when (effect.mShadeType) {
                        ShadeType.Constant -> AiShadingMode.noShading
                        ShadeType.Lambert -> AiShadingMode.gouraud
                        ShadeType.Blinn -> AiShadingMode.blinn
                        ShadeType.Phong -> AiShadingMode.phong
                        else -> {
                            println("Collada: Unrecognized shading mode, using gouraud shading")
                            AiShadingMode.gouraud
                        }
                    }

        // double-sided?
        mat.twoSided = effect.mDoubleSided

        // wireframe?
        mat.wireframe = effect.mWireframe

        // add material colors
        mat.color = AiMaterial.Color(
                ambient = AiColor3D(effect.mAmbient),
                diffuse = AiColor3D(effect.mDiffuse),
                specular = AiColor3D(effect.mSpecular),
                emissive = AiColor3D(effect.mEmissive),
                reflective = AiColor3D(effect.mReflective))

        // scalar properties
        mat.shininess = effect.mShininess
        mat.reflectivity = effect.mReflectivity
        mat.refracti = effect.mRefractIndex

        /* transparency, a very hard one. seemingly not all files are following the specification here
         (1.0 transparency => completly opaque)... therefore, we let the opportunity for the user to manually invert
         the transparency if necessary and we add preliminary support for RGB_ZERO mode */
        if (effect.mTransparency in 0f..1f) {
            // handle RGB transparency completely, cf Collada specs 1.5.0 pages 249 and 304
            if (effect.mRGBTransparency) {
                // use luminance as defined by ISO/CIE color standards (see ITU-R Recommendation BT.709-4)
                effect.mTransparency *= (.212671f * effect.mTransparent.r + .715160f * effect.mTransparent.g + .072169f * effect.mTransparent.b)

                effect.mTransparent.a = 1.f

                mat.color!!.transparent = AiColor3D(effect.mTransparent)
            } else
                effect.mTransparency *= effect.mTransparent.a

            if (effect.mInvertTransparency)
                effect.mTransparency = 1f - effect.mTransparency

            // Is the material finally transparent ?
            if (effect.mHasTransparency || effect.mTransparency < 1f)
                mat.opacity = effect.mTransparency
        }

        // add textures, if given
        if (effect.mTexAmbient.mName.isNotEmpty())
        /* It is merely a lightmap */
            addTexture(mat, pParser, effect, effect.mTexAmbient, AiTexture.Type.lightmap)

        if (effect.mTexEmissive.mName.isNotEmpty())
            addTexture(mat, pParser, effect, effect.mTexEmissive, AiTexture.Type.emissive);

        if (effect.mTexSpecular.mName.isNotEmpty())
            addTexture(mat, pParser, effect, effect.mTexSpecular, AiTexture.Type.specular);

        if (effect.mTexDiffuse.mName.isNotEmpty())
            addTexture(mat, pParser, effect, effect.mTexDiffuse, AiTexture.Type.diffuse);

//        if (!effect.mTexBump.mName.empty())
//            AddTexture(mat, pParser, effect, effect.mTexBump, aiTextureType_NORMALS);
//
//        if (!effect.mTexTransparent.mName.empty())
//            AddTexture(mat, pParser, effect, effect.mTexTransparent, aiTextureType_OPACITY);
//
//        if (!effect.mTexReflective.mName.empty())
//            AddTexture(mat, pParser, effect, effect.mTexReflective, aiTextureType_REFLECTION);
    }

    /** Constructs materials from the collada material definitions  */
    fun buildMaterials(pParser: ColladaParser) = pParser.mMaterialLibrary.forEach { id, material ->

        // a material is only a reference to an effect
        pParser.mEffectLibrary[material.mEffect]?.let { effect ->

            // create material
            val mat = AiMaterial(name = if (material.mName.isEmpty()) id else material.mName)

            // store the material
            mMaterialIndexByName[id] = newMats.size
            newMats.add(Pair(effect, mat))
        }
    }

    /** Resolves the texture name for the given effect texture entry    */
    fun findFilenameForEffectTexture(pParser: ColladaParser, pEffect: Effect, pName: String): String {

        // recurse through the param references until we end up at an image
        var name = pName
        while (true) {
            // the given string is a param entry. Find it, if not found, we're at the end of the recursion. The resulting string should be the image ID
            if (!pEffect.mParams.contains(name))
                break

            // else recurse on
            name = pEffect.mParams[name]!!.mReference
        }

        // find the image referred by this name in the image library of the scene
        val image = pParser.mImageLibrary[name] ?: throw Error("Collada: Unable to resolve effect texture entry \"$pName\", ended up at ID \"$name\".")

        var result = ""

        // if this is an embedded texture image setup an aiTexture for it
        if (image.mFileName.isEmpty()) {
            if (image.mImageData.isEmpty())
                throw Error("Collada: Invalid texture, no data or file reference given")

            val tex = AiTexture()

            // setup format hint
            if (image.mEmbeddedFormat.length > 3)
                println("Collada: texture format hint is too long, truncating to 3 characters")

            tex.achFormatHint = image.mEmbeddedFormat.substring(0, 4)

            // and copy texture data
            tex.mWidth = image.mImageData.size
            tex.pcData = image.mImageData.clone()

            // setup texture reference string
            result = "*"

            // and add this texture to the list
            mTextures.add(tex)
        } else
            result = image.mFileName

        return result
    }

    /** Reads a float value from an accessor and its data array.    */
    fun readFloat(pAccessor: Accessor, pData: Data, pIndex: Int, pOffset: Int): Float {
        // FIXME: (thom) Test for data type here in every access? For the moment, I leave this to the caller
        val pos = pAccessor.mStride * pIndex + pAccessor.mOffset + pOffset
        assert(pos < pData.mValues.size)
        return pData.mValues[pos]
    }

    /** Reads a string value from an accessor and its data array.   */
    fun readString(pAccessor: Accessor, pData: Data, pIndex: Int): String {
        val pos = pAccessor.mStride * pIndex + pAccessor.mOffset
        assert(pos < pData.mStrings.size)
        return pData.mStrings[pos]
    }

    /** Finds a node in the collada scene by the given name */
    fun findNode(pNode: Node, pName: String): Node? {
        if (pNode.mName == pName || pNode.mID == pName)
            return pNode

        pNode.mChildren.forEach { findNode(it, pName)?.let { node -> return node } }

        return null
    }

    /** Finds a node in the collada scene by the given SID  */
    fun findNodeBySID(pNode: Node, pSID: String): Node? {
        if (pNode.mSID == pSID)
            return pNode

        pNode.mChildren.forEach {
            val node = findNodeBySID(it, pSID)
            if (node != null)
                return node
        }

        return null
    }

    /** Finds a proper name for a node derived from the collada-node's properties   */
    fun findNameForNode(pNode: Node) =
            // now setup the name of the node. We take the name if not empty, otherwise the collada ID
            // FIX: Workaround for XSI calling the instanced visual scene 'untitled' by default.
            if (pNode.mName.isNotEmpty() && pNode.mName != "untitled")
                pNode.mName
            else if (pNode.mID.isNotEmpty())
                pNode.mID
            else if (pNode.mSID.isNotEmpty())
                pNode.mSID
            // No need to worry. Unnamed nodes are no problem at all, except if cameras or lights need to be assigned to them.
            else
                "\$ColladaAutoName\$_${mNodeNameCounter++}"

    //
// ColladaLoader.h -----------------------------------------------------------------------------------------------------
//
    class ColladaMeshIndex(

            var mMeshID: String = "",
            var mSubMesh: Int = 0,
            var mMaterial: String = "") {

        operator fun compareTo(p: ColladaMeshIndex): Int =
                if (mMeshID == p.mMeshID)
                    if (mSubMesh == p.mSubMesh)
                        mMaterial.compareTo(p.mMaterial)
                    else
                        mSubMesh.compareTo(p.mSubMesh)
                else
                    mMeshID.compareTo(p.mMeshID)
    }
}