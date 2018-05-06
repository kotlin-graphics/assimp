package assimp.format.collada

import assimp.*
import assimp.format.AiConfig
import glm_.*
import glm_.func.common.max
import glm_.func.deg
import glm_.func.rad
import glm_.mat4x4.Mat4
import unsigned.Uint
import unsigned.Ulong
import unsigned.ui
import java.net.URI

/**
 * Created by elect on 23/01/2017.
 */

class ColladaLoader : BaseImporter() {

    companion object {

        val desc = AiImporterDesc(
                name = "Collada Importer",
                comments = "http://collada.org",
                flags = AiImporterFlags.SupportTextFlavour.i,
                minMajor = 1, minMinor = 3,
                maxMajor = 1, maxMinor = 5,
                fileExtensions = listOf("dae")
        )
    }

    /** Filename, for a verbose error message */
    lateinit var mFileName: String

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

    var noSkeletonMesh = false
    var ignoreUpDirection = false

    /** Used by FindNameForNode() to generate unique node names */
    var mNodeNameCounter = 0

    // ------------------------------------------------------------------------------------------------
    // Returns whether the class can handle the format of the given file.
    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {

        // check file extension
        val extension = file.substring(file.lastIndexOf('.') + 1).toLowerCase()

        if (extension == "dae")
            return true

        // XML - too generic, we need to open the file and search for typical keywords
        if (extension == "xml" || extension.isEmpty() || checkSig)
            TODO()
        return false
    }

    // ------------------------------------------------------------------------------------------------
    // Imports the given file into the given scene structure.
    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {
        mFileName = file
        // parse the input file
        val parser = ColladaParser(ioSystem.open(file))
        if (parser.mRootNode == null)
            throw Error("Collada: File came out empty. Something is wrong here.")
        // create the materials first, for the meshes to find
        buildMaterials(parser)
        // build the node hierarchy from it
        scene.rootNode = buildHierarchy(parser, parser.mRootNode!!)
        // ... then fill the materials with the now adjusted settings
        fillMaterials(parser)
        // Apply unitsize scale calculation  // TODO glm avoid mat4 instance?
        scene.rootNode.transformation timesAssign AiMatrix4x4(parser.mUnitSize, 0, 0, 0,
                0, parser.mUnitSize, 0, 0,
                0, 0, parser.mUnitSize, 0,
                0, 0, 0, 1)
        if (!ignoreUpDirection)
        // Convert to Y_UP, if different orientation
            if (parser.mUpDirection == ColladaParser.UpDirection.X)
                scene.rootNode.transformation timesAssign AiMatrix4x4(
                        0, 1, 0, 0,
                        -1, 0, 0, 0,
                        0, 0, 1, 0,
                        0, 0, 0, 1)
            else if (parser.mUpDirection == ColladaParser.UpDirection.Z)
                scene.rootNode.transformation timesAssign AiMatrix4x4(
                        1, 0, 0, 0,
                        0, 0, -1, 0,
                        0, +1, 0, 0,
                        0, 0, 0, 1)

        // store all meshes
        storeSceneMeshes(scene)
        // store all materials
        storeSceneMaterials(scene)
        // store all lights
        storeSceneLights(scene)
        // store all cameras
        storeSceneCameras(scene)
        // store all animations
        storeAnimations(scene, parser)
        // If no meshes have been loaded, it's probably just an animated skeleton.
        if (scene.numMeshes == 0) {
            if (!noSkeletonMesh) SkeletonMeshBuilder(scene)
            scene.flags = scene.flags or AI_SCENE_FLAGS_INCOMPLETE
        }
    }

    /** Recursively constructs a scene node for the given parser node and returns it.   */
    fun buildHierarchy(pParser: ColladaParser, pNode: Node): AiNode {

        // create a node for it and find a name for the new node. It's more complicated than you might think
        val node = AiNode(name = findNameForNode(pNode))

        // calculate the transformation matrix for it
        node.transformation = pParser.calculateResultTransform(pNode.mTransforms)

        // now resolve node instances
        val instances = ArrayList<Node>()
        resolveNodeInstances(pParser, pNode, instances)

        // add children. first the *real* ones
        node.numChildren = pNode.mChildren.size + instances.size
        node.children = ArrayList<AiNode>()

        for (a in 0 until pNode.mChildren.size)
            with(buildHierarchy(pParser, pNode.mChildren[a])) {
                node.children.add(this)
                parent = node
            }

        // ... and finally the resolved node instances
        for (a in 0 until instances.size) {
            node.children[pNode.mChildren.size + a] = buildHierarchy(pParser, instances[a])
            node.children[pNode.mChildren.size + a].parent = node
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
            logger.warn { "Collada: Unable to find light for ID \"${it.mLight}\". Skipping." }
            return
        }

        // now fill our ai data structure
        val out = AiLight(name = pTarget.name, type = srcLight.mType)

        // collada lights point in -Z by default, rest is specified in node transform
        out.direction.put(0f, 0f, -1f)

        out.attenuationConstant = srcLight.mAttConstant
        out.attenuationLinear = srcLight.mAttLinear
        out.attenuationQuadratic = srcLight.mAttQuadratic

        val intensity = srcLight.mColor * srcLight.mIntensity
        out.colorDiffuse = intensity
        out.colorSpecular = intensity
        out.colorAmbient = intensity
        if (out.type == AiLightSourceType.AMBIENT) {
            out.colorDiffuse = AiColor3D(0)
            out.colorSpecular = AiColor3D(0)
            out.colorAmbient = srcLight.mColor * srcLight.mIntensity
        } else {
            // collada doesn't differentiate between these color types
            out.colorDiffuse = intensity
            out.colorSpecular = intensity
            out.colorAmbient = AiColor3D(0)
        }

        // convert falloff angle and falloff exponent in our representation, if given
        if (out.type == AiLightSourceType.SPOT) {

            out.angleInnerCone = srcLight.mFalloffAngle.rad

            // ... some extension magic.
            if (srcLight.mOuterAngle >= ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET * (1 - 1e-6f)) {
                // ... some deprecation magic.
                if (srcLight.mPenumbraAngle >= ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET * (1 - 1e-6f))
                // Need to rely on falloff_exponent. I don't know how to interpret it, so I need to guess .... epsilon chosen to be 0.1
                    out.angleOuterCone = glm.acos(glm.pow(.1f, 1f / srcLight.mFalloffExponent)) + out.angleInnerCone
                else {
                    out.angleOuterCone = out.angleInnerCone + srcLight.mPenumbraAngle.rad
                    if (out.angleOuterCone < out.angleInnerCone) {
                        val temp = out.angleInnerCone
                        out.angleInnerCone = out.angleOuterCone
                        out.angleOuterCone = temp
                    }
                }
            } else
                out.angleOuterCone = srcLight.mOuterAngle.rad
        }

        // add to light list
        mLights.add(out)
    }

    /** Builds cameras for the given node and references them   */
    fun buildCamerasForNode(pParser: ColladaParser, pNode: Node, pTarget: AiNode) = pNode.mCameras.forEach {

        // find the referred light
        val srcCamera = pParser.mCameraLibrary[it.mCamera] ?: run {
            logger.warn { "Collada: Unable to find camera for ID \"${it.mCamera}\". Skipping." }
            return
        }

        // orthographic cameras not yet supported in Assimp
        if (srcCamera.mOrtho) logger.warn { "Collada: Orthographic cameras are not supported." }

        // now fill our ai data structure
        val out = AiCamera(name = pTarget.name)

        // collada cameras point in -Z by default, rest is specified in node transform
        out.lookAt.put(0f, 0f, -1f)

        // near/far z is already ok
        out.clipPlaneFar = srcCamera.mZFar
        out.clipPlaneNear = srcCamera.mZNear

        // ... but for the rest some values are optional and we need to compute the others in any combination.
        if (srcCamera.mAspect != 10e10f)
            out.aspect = srcCamera.mAspect

        with(glm) {
            if (srcCamera.mHorFov != 10e10f) {
                out.horizontalFOV = srcCamera.mHorFov

                if (srcCamera.mVerFov != 10e10f && srcCamera.mAspect == 10e10f)
                    out.aspect = tan(srcCamera.mHorFov.rad) / tan(srcCamera.mVerFov.rad)
            } else if (srcCamera.mAspect != 10e10f && srcCamera.mVerFov != 10e10f) {
                out.horizontalFOV = 2f * atan(srcCamera.mAspect * tan(srcCamera.mVerFov.rad * .5f)).deg
            }
        }

        // Collada uses degrees, we use radians
        out.horizontalFOV = out.horizontalFOV.rad

        // add to camera list
        mCameras.add(out)
    }

    /** Builds meshes for the given node and references them    */
    fun buildMeshesForNode(pParser: ColladaParser, pNode: Node, pTarget: AiNode) {

        var srcController: Controller? = null

        // accumulated mesh references by this node
        val newMeshRefs = ArrayList<Int>(pNode.mMeshes.size)

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
                    logger.warn { "Collada: Unable to find geometry for ID \"${mid.mMeshOrController}\". Skipping." }
                    return@forEach
                }
            }
            // else ID found in the mesh library -> direct reference to an unskinned mesh

            // build a mesh for each of its subgroups
            var vertexStart = 0
            var faceStart = 0
            for (sm in 0 until srcMesh!!.mSubMeshes.size) {

                val submesh = srcMesh!!.mSubMeshes[sm]

                if (submesh.mNumFaces == 0)
                    return@forEach

                // find material assigned to this submesh
                var meshMaterial = ""

                val table = mid.mMaterials[submesh.mMaterial]
                if (table != null)
                    meshMaterial = table.mMatName
                else {
                    logger.warn { "Collada: No material specified for subgroup <${submesh.mMaterial}> in geometry <${mid.mMeshOrController}>." }
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
                    newMeshRefs += it
                    return@forEach
                }

                // else we have to add the mesh to the collection and store its newly assigned index at the node
                val dstMesh = createMesh(pParser, srcMesh!!, submesh, srcController, vertexStart, faceStart)

                // store the mesh, and store its new index in the node
                newMeshRefs.add(mMeshes.size)
                mMeshIndexByID[index] = mMeshes.size
                mMeshes.add(dstMesh)
                vertexStart += dstMesh.numVertices; faceStart += submesh.mNumFaces

                // assign the material index
                dstMesh.materialIndex = matIdx
                if (dstMesh.name.isEmpty())
                    dstMesh.name = mid.mMeshOrController
            }
        }

        // now place all mesh references we gathered in the target node
        pTarget.numMeshes = newMeshRefs.size
        if (newMeshRefs.isNotEmpty())
            pTarget.meshes = newMeshRefs.toIntArray()
    }

    /** Find mesh from either meshes or morph target meshes */
    fun findMesh(meshid: String) = mMeshes.firstOrNull { it.name == meshid } ?: mTargetMeshes.firstOrNull { it.name == meshid }

    /** Creates a mesh for the given ColladaMesh face subset and returns the newly created mesh */
    fun createMesh(pParser: ColladaParser, pSrcMesh: Mesh, pSubMesh: SubMesh, pSrcController: Controller?, pStartVertex: Int, pStartFace: Int): AiMesh {

        val dstMesh = AiMesh(name = pSrcMesh.mName)

        // count the vertices addressed by its faces
        val numVertices = pSrcMesh.mFaceSize.slice(pStartFace until (pStartFace + pSubMesh.mNumFaces)).sum()

        // copy positions
        dstMesh.numVertices = numVertices
        dstMesh.vertices.addAll(pSrcMesh.mPositions.slice(pStartVertex until (pStartVertex + numVertices)))

        // normals, if given. HACK: (thom) Due to the glorious Collada spec we never know if we have the same number of normals as there are positions. So we
        // also ignore any vertex attribute if it has a different count
        if (pSrcMesh.mNormals.size >= pStartVertex + numVertices)
            dstMesh.normals.addAll(pSrcMesh.mNormals.slice(pStartVertex until (pStartVertex + numVertices)))

        // tangents, if given.
        if (pSrcMesh.mTangents.size >= pStartVertex + numVertices)
            dstMesh.tangents.addAll(pSrcMesh.mTangents.slice(pStartVertex until (pStartVertex + numVertices)))

        // bitangents, if given.
        if (pSrcMesh.mBitangents.size >= pStartVertex + numVertices)
            dstMesh.bitangents.addAll(pSrcMesh.mBitangents.slice(pStartVertex until (pStartVertex + numVertices)))

        // same for texturecoords, as many as we have empty slots are not allowed, need to pack and adjust UV indexes accordingly
        var real = 0
        for (a in pSrcMesh.mTexCoords.indices) {
            if (pSrcMesh.mTexCoords[a].size >= pStartVertex + numVertices) {
                if (real >= dstMesh.textureCoords.size)
                    dstMesh.textureCoords.add(mutableListOf())
                dstMesh.textureCoords[real] = mutableListOf()
                for (v in 0 until numVertices)
                    dstMesh.textureCoords[real].add(pSrcMesh.mTexCoords[a][pStartVertex + v])

                ++real
            }
        }

        // same for vertex colors, as many as we have. again the same packing to avoid empty slots
        for (colorNumber in 0 until pSrcMesh.mColors.size) {
            dstMesh.colors.add(mutableListOf())
            if (pSrcMesh.mColors.size >= pStartVertex + numVertices)
                for (v in 0 until numVertices)
                    dstMesh.colors[colorNumber].add(pSrcMesh.mColors[colorNumber][pStartVertex + v])
        }

        // create faces. Due to the fact that each face uses unique vertices, we can simply count up on each vertex
        var vertex = 0
        dstMesh.numFaces = pSubMesh.mNumFaces
        for (a in 0 until dstMesh.numFaces) {
            val s = pSrcMesh.mFaceSize[pStartFace + a]
            dstMesh.faces.add(MutableList(s, { vertex++ }))
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
                val targetData = pParser.mDataLibrary[targetAccessor.source]!!
                val weightData = pParser.mDataLibrary[weightAccessor.source]!!

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
                targetWeights.addAll(weightData.values)
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
//      if( pSrcController->weightCounts.size() != dstMesh->numVertices)
//          throw DeadlyImportError( "Joint Controller vertex count does not match mesh vertex count");

            // resolve references - joint names
            val jointNamesAcc = pParser.mAccessorLibrary[pSrcController.mJointNameSource]!!
            val jointNames = pParser.mDataLibrary[jointNamesAcc.source]!!
            // joint offset matrices
            val jointMatrixAcc = pParser.mAccessorLibrary[pSrcController.mJointOffsetMatrixSource]!!
            val jointMatrices = pParser.mDataLibrary[jointMatrixAcc.source]!!
            // joint vertex_weight name list - should refer to the same list as the joint names above. If not, report and reconsider
            val weightNamesAcc = pParser.mAccessorLibrary[pSrcController.mWeightInputJoints.mAccessor]!!
            if (weightNamesAcc !== jointNamesAcc)
                throw Error("Temporary implementational laziness. If you read this, please report to the author.")
            // vertex weights
            val weightsAcc = pParser.mAccessorLibrary[pSrcController.mWeightInputWeights.mAccessor]!!
            val weights = pParser.mDataLibrary[weightsAcc.source]!!

            if (!jointNames.mIsStringArray || jointMatrices.mIsStringArray || weights.mIsStringArray)
                throw Error("Data type mismatch while resolving mesh joints")
            // sanity check: we rely on the vertex weights always coming as pairs of BoneIndex-WeightIndex
            if (pSrcController.mWeightInputJoints.mOffset != 0 || pSrcController.mWeightInputWeights.mOffset != 1)
                throw Error("Unsupported vertex_weight addressing scheme. ")

            // create containers to collect the weights for each bone
            val numBones = jointNames.mStrings.size
            val dstBones = MutableList(numBones, { mutableListOf<AiVertexWeight>() })

            // build a temporary array of pointers to the start of each vertex's weights
            val weightStartPerVertex = LongArray(pSrcController.weightCounts.size)
            var pit = 0L
            pSrcController.weightCounts.forEachIndexed { i, a ->
                weightStartPerVertex[i] = pit
                pit += a
            }

            // now for each vertex put the corresponding vertex weights into each bone's weight collection
            for (a in pStartVertex until pStartVertex + numVertices) {

                // which position index was responsible for this vertex? that's also the index by which the controller assigns the vertex weights
                val orgIndex = pSrcMesh.mFacePosIndices[a]
                // find the vertex weights for this vertex
                val iit = weightStartPerVertex[orgIndex]
                val pairCount = pSrcController.weightCounts[orgIndex]

                for (b in 0 until pairCount) {

                    val jointIndex = pSrcController.weights[iit.i].first
                    val vertexIndex = pSrcController.weights[iit.i].second

                    val weight = readFloat(weightsAcc, weights, vertexIndex, 0)

                    // one day I gonna kill that XSI Collada exporter
                    if (weight > 0f)
                        dstBones[jointIndex.i].add(AiVertexWeight(vertexId = (a - pStartVertex).ui.v, weight = weight))
                }
            }

            // count the number of bones which influence vertices of the current submesh
            val numRemainingBones = dstBones.filter { it.isNotEmpty() }.size

            // create bone array and copy bone weights one by one
            dstMesh.numBones = numRemainingBones
            for (a in 0 until numBones) {

                // omit bones without weights
                if (dstBones[a].isEmpty())
                    continue

                // create bone with its weights
                val bone = AiBone(name = readString(jointNamesAcc, jointNames, a.L))
                with(bone) {
                    for (i in 0..11)
                        offsetMatrix[i % 4, i / 4] = readFloat(jointMatrixAcc, jointMatrices, a.L, i.L)
                    numWeights = dstBones[a].size
                    this.weights = dstBones[a].toMutableList()
                    // apply bind shape matrix to offset matrix
                    offsetMatrix timesAssign Mat4(pSrcController.mBindShapeMatrix, true)
                }

                // HACK: (thom) Some exporters address the bone nodes by SID, others address them by ID or even name.
                // Therefore I added a little name replacement here: I search for the bone's node by either name, ID or SID, and replace the bone's name by the
                // node's name so that the user can use the standard find-by-name method to associate nodes with bones.
                val bnode = findNode(pParser.mRootNode!!, bone.name) ?: findNodeBySID(pParser.mRootNode!!, bone.name)

                // assign the name that we would have assigned for the source node
                if (bnode != null) bone.name = findNameForNode(bnode)
                else logger.warn { "ColladaLoader::CreateMesh(): could not find corresponding node for joint \"${bone.name}\"." }

                // and insert bone
                dstMesh.bones.add(bone)
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
                    logger.error { "Collada: Unable to resolve reference to instanced node ${it.mNode}" }
                else
                    resolved.add(nd)    //  attach this node to the list of children
            }

    /** Resolve UV channels */
    fun applyVertexToEffectSemanticMapping(sampler: Sampler, table: SemanticMappingTable) = table.mMap[sampler.mUVChannel]?.let {
        if (it.mType != InputType.Texcoord)
            logger.error { "Collada: Unexpected effect input mapping" }
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
                        logger.warn { "Collada: unable to determine UV channel for texture" }
                        0
                    }

        if(idx != -1)
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
                            logger.warn { "Collada: Unrecognized shading mode, using gouraud shading" }
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
            addTexture(mat, pParser, effect, effect.mTexEmissive, AiTexture.Type.emissive)

        if (effect.mTexSpecular.mName.isNotEmpty())
            addTexture(mat, pParser, effect, effect.mTexSpecular, AiTexture.Type.specular)

        if (effect.mTexDiffuse.mName.isNotEmpty())
            addTexture(mat, pParser, effect, effect.mTexDiffuse, AiTexture.Type.diffuse)

//        if (!effect.mTexBump.name.empty())
//            AddTexture(mat, pParser, effect, effect.mTexBump, aiTextureType_NORMALS);
//
//        if (!effect.mTexTransparent.name.empty())
//            AddTexture(mat, pParser, effect, effect.mTexTransparent, aiTextureType_OPACITY);
//
//        if (!effect.mTexReflective.name.empty())
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

        var result: String

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
        val image = pParser.mImageLibrary[name] ?: run {
            // missing texture should not stop the conversion
//            logger.error { "Collada: Unable to resolve effect texture entry \"$pName\", ended up at ID \"$name\"." }
            logger.warn { "Collada: Unable to resolve effect texture entry \"$pName\", ended up at ID \"$name\"." }
            //set default texture file name
            return "$name.jpg" // result
        }

        // if this is an embedded texture image setup an aiTexture for it
        if (image.mFileName.isEmpty()) {
            if (image.mImageData.isEmpty())
                throw Error("Collada: Invalid texture, no data or file reference given")

            val tex = AiTexture()

            // setup format hint
            if (image.mEmbeddedFormat.length > 3)
                logger.warn { "Collada: texture format hint is too long, truncating to 3 characters" }

            tex.achFormatHint = image.mEmbeddedFormat.substring(0, 4)

            // and copy texture data
            tex.width = image.mImageData.size
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
    fun readFloat(pAccessor: Accessor, pData: Data, pIndex: Long, pOffset: Long): Float {
        // FIXME: (thom) Test for data type here in every access? For the moment, I leave this to the caller
        val pos = pAccessor.stride * pIndex + pAccessor.offset + pOffset
        assert(pos < pData.values.size)
        return pData.values[pos.i]
    }

    /** Reads a string value from an accessor and its data array.   */
    fun readString(pAccessor: Accessor, pData: Data, pIndex: Long): String {
        val pos = pAccessor.stride * pIndex + pAccessor.offset
        assert(pos < pData.mStrings.size)
        return pData.mStrings[pos.i]
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

    /** Finds a proper unique name for a node derived from the collada-node's properties.
     *  The name must be unique for proper node-bone association.   */
    fun findNameForNode(pNode: Node) = when {
    /*  Now setup the name of the assimp node. The collada name might not be unique, so we use the collada ID.  */
        pNode.mID.isNotEmpty() -> pNode.mID
        pNode.mSID.isNotEmpty() -> pNode.mSID
    // No need to worry. Unnamed nodes are no problem at all, except if cameras or lights need to be assigned to them.
        else -> "\$ColladaAutoName\$_${mNodeNameCounter++}"
    }

    /** Stores all meshes in the given scene    */
    fun storeSceneMeshes(pScene: AiScene) {
        pScene.numMeshes = mMeshes.size
        if (mMeshes.isNotEmpty()) {
            pScene.meshes.addAll(mMeshes)
            mMeshes.clear()
        }
    }

    /** Stores all materials in the given scene */
    fun storeSceneMaterials(pScene: AiScene) {
        pScene.numMaterials = newMats.size
        if (newMats.isNotEmpty()) {
            pScene.materials.clear()
            for (i in 0 until newMats.size)
                pScene.materials.add(newMats[i].second)
            newMats.clear()
        }
    }

    /** Stores all lights in the given scene    */
    fun storeSceneLights(pScene: AiScene) {
        pScene.numLights = mLights.size
        if (mLights.isNotEmpty()) {
            pScene.lights.addAll(mLights)
            mLights.clear()
        }
    }

    /** Stores all cameras in the given scene   */
    fun storeSceneCameras(pScene: AiScene) {
        pScene.numCameras = mCameras.size
        if (mCameras.isNotEmpty()) {
            pScene.cameras.addAll(mCameras)
            mCameras.clear()
        }
    }

    /** Stores all animations   */
    fun storeAnimations(scene: AiScene, parser: ColladaParser) {
        // recursivly collect all animations from the collada scene
        storeAnimations(scene, parser, parser.mAnims, "")

        // catch special case: many animations with the same length, each affecting only a single node.
        // we need to unite all those single-node-anims to a proper combined animation
        for (a in 0 until mAnims.size) {
            val templateAnim = mAnims[a]
            if (templateAnim.numChannels == 1) {
                // search for other single-channel-anims with the same duration
                val collectedAnimIndices = ArrayList<Int>()
                for (b in a + 1 until mAnims.size) {
                    val other = mAnims[b]
                    if (other.numChannels == 1 && other.duration == templateAnim.duration && other.ticksPerSecond == templateAnim.ticksPerSecond)
                        collectedAnimIndices.add(b)
                }

                // if there are other animations which fit the template anim, combine all channels into a single anim
                if (collectedAnimIndices.isNotEmpty()) {
                    val combinedAnim = AiAnimation().apply {
                        name = "combinedAnim_$a"
                        duration = templateAnim.duration
                        ticksPerSecond = templateAnim.ticksPerSecond
                        numChannels = collectedAnimIndices.size + 1
                        channels = templateAnim.channels    // add the template anim as first channel by moving its aiNodeAnim to the combined animation
                    }
                    // combined animation replaces template animation in the anim array
                    mAnims[a] = combinedAnim

                    // move the memory of all other anims to the combined anim and erase them from the source anims
                    for (b in 0 until collectedAnimIndices.size) {
                        val srcAnimation = mAnims[collectedAnimIndices[b]]
                        combinedAnim.channels[1 + b] = srcAnimation.channels[0]
                        srcAnimation.channels[0] = null
                    }

                    // in a second go, delete all the single-channel-anims that we've stripped from their channels
                    // back to front to preserve indices - you know, removing an element from a vector moves all elements behind the removed one
                    while (collectedAnimIndices.isNotEmpty()) {
                        mAnims.removeAt(collectedAnimIndices.last())
                        collectedAnimIndices.remove(collectedAnimIndices.lastIndex)
                    }
                }
            }
        }

        // now store all anims in the scene
        if (mAnims.isNotEmpty()) {
            scene.numAnimations = mAnims.size
            scene.animations.addAll(mAnims)
        }
        mAnims.clear()
    }

    /** Constructs the animations for the given source anim */
    fun storeAnimations(scene: AiScene, parser: ColladaParser, srcAnim: Animation, prefix: String) {

        val animName = if (prefix.isEmpty()) srcAnim.mName else "${prefix}_${srcAnim.mName}"
        // create nested animations, if given
        for (it in srcAnim.mSubAnims)
            storeAnimations(scene, parser, it, animName)
        // create animation channels, if any
        if (srcAnim.mChannels.isNotEmpty())
            createAnimation(scene, parser, srcAnim, animName)
    }

    /** Constructs the animation for the given source anim  */
    fun createAnimation(pScene: AiScene, pParser: ColladaParser, pSrcAnim: Animation, pName: String) {
        // collect a list of animatable nodes
        val nodes = ArrayList<AiNode>()
        collectNodes(pScene.rootNode, nodes)

        val anims = ArrayList<AiNodeAnim>()
        val morphAnims = ArrayList<AiMeshMorphAnim>()

        for (node in nodes) {
            // find all the collada anim channels which refer to the current node
            val entries = ArrayList<ChannelEntry>()
            val nodeName = node.name

            // find the collada node corresponding to the aiNode
            val srcNode = findNode(pParser.mRootNode!!, nodeName) ?: continue
            //      ai_assert( srcNode != NULL);

            // now check all channels if they affect the current node
            for (srcChannel in pSrcAnim.mChannels) {

                val entry = ChannelEntry()

                /*  we expect the animation target to be of type "nodeName/transformID.subElement". Ignore all others
                    find the slash that separates the node name - there should be only one  */
                val slashPos = srcChannel.mTarget.indexOf('/')
                if (slashPos == -1) {
                    val targetPos = srcChannel.mTarget.indexOf(srcNode.mID)
                    if (targetPos == -1) continue

                    // not node transform, but something else. store as unknown animation channel for now
                    entry.mChannel = srcChannel
                    entry.targetId = srcChannel.mTarget.substring(targetPos + pSrcAnim.mName.length)
                    if (entry.targetId[0] == '-')
                        entry.targetId = entry.targetId.substring(1)
                    entries.add(entry)
                    continue
                }
                if (srcChannel.mTarget.indexOf('/', slashPos + 1) != -1) continue
                val targetID = srcChannel.mTarget.substring(0, slashPos)
                if (targetID != srcNode.mID) continue

                // find the dot that separates the transformID - there should be only one or zero
                val dotPos = srcChannel.mTarget.indexOf('.')
                if (dotPos != -1) {
                    if (srcChannel.mTarget.indexOf('.', dotPos + 1) != -1) continue

                    entry.mTransformId = srcChannel.mTarget.substring(slashPos + 1, dotPos)

                    val subElement = srcChannel.mTarget.substring(dotPos + 1)
                    entry.mSubElement = when (subElement) {
                        "ANGLE" -> 3 // last number in an Axis-Angle-Transform is the angle
                        "X" -> 0
                        "Y" -> 1
                        "Z" -> 2
                        else -> logger.warn({ "Unknown anim subelement <\"$subElement\">. Ignoring" }).run { 0L }
                    }
                } else  // no subelement following, transformId is remaining string
                    entry.mTransformId = srcChannel.mTarget.substring(slashPos + 1)

                val bracketPos = srcChannel.mTarget.indexOf('(')
                if (bracketPos != -1) {
                    entry.mTransformId = srcChannel.mTarget.substring(slashPos + 1, bracketPos - 1)
                    val subElement = srcChannel.mTarget.substring(bracketPos)
                    entry.mSubElement = when (subElement) {
                        "(0)(0)" -> 0
                        "(1)(0)" -> 1
                        "(2)(0)" -> 2
                        "(3)(0)" -> 3
                        "(0)(1)" -> 4
                        "(1)(1)" -> 5
                        "(2)(1)" -> 6
                        "(3)(1)" -> 7
                        "(0)(2)" -> 8
                        "(1)(2)" -> 9
                        "(2)(2)" -> 10
                        "(3)(2)" -> 11
                        "(0)(3)" -> 12
                        "(1)(3)" -> 13
                        "(2)(3)" -> 14
                        "(3)(3)" -> 15
                        else -> 0L
                    }
                }
                // determine which transform step is affected by this channel
                entry.mTransformIndex = Ulong.MAX_VALUE.L
                for (a in srcNode.mTransforms.indices)
                    if (srcNode.mTransforms[a].mID == entry.mTransformId)
                        entry.mTransformIndex = a.L

                if (entry.mTransformIndex == Ulong.MAX_VALUE.L)
                    if (entry.mTransformId.indexOf("morph-weights") != -1) {
                        entry.targetId = entry.mTransformId
                        entry.mTransformId = ""
                    } else continue

                entry.mChannel = srcChannel
                entries.add(entry)
            }

            // if there's no channel affecting the current node, we skip it
            if (entries.isEmpty()) continue

            // resolve the data pointers for all anim channels. Find the minimum time while we're at it
            var startTime = 1e20f
            var endTime = -1e20f
            for (e in entries) {

                e.mTimeAccessor = pParser.resolveLibraryReference(pParser.mAccessorLibrary, e.mChannel.mSourceTimes)
                e.timeData = pParser.resolveLibraryReference(pParser.mDataLibrary, e.mTimeAccessor.source)
                e.mValueAccessor = pParser.resolveLibraryReference(pParser.mAccessorLibrary, e.mChannel.mSourceValues)
                e.valueData = pParser.resolveLibraryReference(pParser.mDataLibrary, e.mValueAccessor.source)

                // time count and value count must match
                if (e.mTimeAccessor.count != e.mValueAccessor.count)
                    throw Error("Time count / value count mismatch in animation channel \"${e.mChannel.mTarget}\".")

                if (e.mTimeAccessor.count > 0) {
                    // find bounding times
                    startTime = glm.min(startTime, readFloat(e.mTimeAccessor, e.timeData, 0, 0))
                    endTime = glm.max(endTime, readFloat(e.mTimeAccessor, e.timeData, e.mTimeAccessor.count - 1, 0))
                }
            }

            val resultTrafos = ArrayList<AiMatrix4x4>()
            if (entries.isNotEmpty() && entries.first().mTimeAccessor.count > 0) {
                // create a local transformation chain of the node's transforms
                val transforms = srcNode.mTransforms

                /*  now for every unique point in time, find or interpolate the key values for that time and apply them
                    to the transform chain. Then the node's present transformation can be calculated.   */
                var time = startTime
                while (true) {
                    for (e in entries) {

                        // find the keyframe behind the current point in time
                        var pos = 0L
                        var postTime = 0f
                        while (true) {
                            if (pos >= e.mTimeAccessor.count) break
                            postTime = readFloat(e.mTimeAccessor, e.timeData, pos, 0)
                            if (postTime >= time) break
                            ++pos
                        }

                        pos = glm.min(pos, e.mTimeAccessor.count - 1)

                        // read values from there
                        val temp = FloatArray(16)
                        for (c in 0 until e.mValueAccessor.size)
                            temp[c.i] = readFloat(e.mValueAccessor, e.valueData, pos, c)

                        // if not exactly at the key time, interpolate with previous value set
                        if (postTime > time && pos > 0) {
                            val preTime = readFloat(e.mTimeAccessor, e.timeData, pos - 1, 0)
                            val factor = (time - postTime) / (preTime - postTime)

                            for (c in 0 until e.mValueAccessor.size) {
                                val v = readFloat(e.mValueAccessor, e.valueData, pos - 1, c)
                                temp[c.i] += (v - temp[c.i]) * factor
                            }
                        }
                        // Apply values to current transformation
                        System.arraycopy(temp, 0, transforms[e.mTransformIndex.i].f, e.mSubElement.i, e.mValueAccessor.size.i)
                    }
                    // Calculate resulting transformation
                    val mat = pParser.calculateResultTransform(transforms)

                    // out of laziness: we store the time in matrix.d4
                    mat.d3 = time
                    resultTrafos.add(mat)

                    // find next point in time to evaluate. That's the closest frame larger than the current in any channel
                    var nextTime = 1e20f
                    for (channelElement in entries) {
                        // find the next time value larger than the current
                        var pos = 0L
                        while (pos < channelElement.mTimeAccessor.count) {
                            val t = readFloat(channelElement.mTimeAccessor, channelElement.timeData, pos, 0)
                            if (t > time) {
                                nextTime = glm.min(nextTime, t)
                                break
                            }
                            ++pos
                        }

                        /*  https://github.com/assimp/assimp/issues/458.    Sub-sample axis-angle channels if the delta
                            between two consecutive key-frame angles is >= 180 degrees.                         */
                        if (transforms[channelElement.mTransformIndex.i].mType == TransformType.ROTATE && channelElement.mSubElement.i == 3
                                && pos > 0 && pos < channelElement.mTimeAccessor.count) {
                            val curKeyAngle = readFloat(channelElement.mValueAccessor, channelElement.valueData, pos, 0)
                            val lastKeyAngle = readFloat(channelElement.mValueAccessor, channelElement.valueData, pos - 1, 0)
                            val curKeyTime = readFloat(channelElement.mTimeAccessor, channelElement.timeData, pos, 0)
                            val lastKeyTime = readFloat(channelElement.mTimeAccessor, channelElement.timeData, pos - 1, 0)
                            val lastEvalAngle = lastKeyAngle + (curKeyAngle - lastKeyAngle) * (time - lastKeyTime) / (curKeyTime - lastKeyTime)
                            val delta = glm.abs(curKeyAngle - lastEvalAngle)
                            if (delta >= 180) {
                                val subSampleCount = glm.floor(delta / 90).i
                                if (curKeyTime != time) {
                                    val nextSampleTime = time + (curKeyTime - time) / subSampleCount
                                    nextTime = glm.min(nextTime, nextSampleTime)
                                }
                            }
                        }
                    }
                    // no more keys on any channel after the current time -> we're done
                    if (nextTime > 1e19) break
                    // else construct next keyframe at this following time point
                    time = nextTime
                }
            }
            // there should be some keyframes, but we aren't that fixated on valid input data
//      ai_assert( resultTrafos.size() > 0);

            // build an animation channel for the given node out of these trafo keys
            if (resultTrafos.isNotEmpty()) {
                val dstAnim = AiNodeAnim(
                        nodeName = nodeName,
                        numPositionKeys = resultTrafos.size,
                        numRotationKeys = resultTrafos.size,
                        numScalingKeys = resultTrafos.size,
                        positionKeys = Array(resultTrafos.size){ AiVectorKey() }.toCollection(ArrayList()),
                        rotationKeys = Array(resultTrafos.size) { AiQuatKey() }.toCollection(ArrayList()),
                        scalingKeys = Array(resultTrafos.size) { AiVectorKey() }.toCollection(ArrayList()))

                for (a in 0 until resultTrafos.size) {
                    val mat = resultTrafos[a]
                    val time = mat.d3.d // remember? time is stored in mat.d4
                    mat.d3 = 1f

                    dstAnim.positionKeys[a].time = time
                    dstAnim.rotationKeys[a].time = time
                    dstAnim.scalingKeys[a].time = time
                    mat.decompose(dstAnim.scalingKeys[a].value, dstAnim.rotationKeys[a].value, dstAnim.positionKeys[a].value)
                }
                anims.add(dstAnim)
            } else logger.warn("Collada loader: found empty animation channel, ignored. Please check your exporter.")

            if (entries.isNotEmpty() && entries.first().mTimeAccessor.count > 0L) {
                val morphChannels = ArrayList<ChannelEntry>()
                for (e in entries) {
                    // skip non-transform types
                    if (e.targetId.isEmpty()) continue
                    if (e.targetId.contains("morph-weights")) morphChannels.add(e)
                }
                if (morphChannels.isNotEmpty()) {
                    // either 1) morph weight animation count should contain morph target count channels
                    // or     2) one channel with morph target count arrays
                    // assume first
                    val morphAnim = AiMeshMorphAnim().apply { name = nodeName }
                    val morphTimeValues = ArrayList<MorphTimeValues>()
                    var morphAnimChannelIndex = 0
                    for (e in morphChannels) {
                        val aPos = e.targetId.indexOf('(')
                        val bPos = e.targetId.indexOf(')')
                        if (aPos == -1 || bPos == -1) continue // unknown way to specify weight -> ignore this animation
                        // weight target can be in format Weight_M_N, Weight_N, WeightN, or some other way
                        // we ignore the name and just assume the channels are in the right order
                        for (i in 0 until e.timeData.values.size)
                            insertMorphTimeValue(morphTimeValues, e.timeData.values[i], e.valueData.values[i], morphAnimChannelIndex)
                        ++morphAnimChannelIndex
                    }
                    morphAnim.numKeys = morphTimeValues.size
                    morphAnim.keys = Array<AiMeshMorphKey>(morphAnim.numKeys, { key ->
                        AiMeshMorphKey(
                                numValuesAndWeights = morphChannels.size,
                                values = IntArray(morphChannels.size, { it }),
                                weights = DoubleArray(morphChannels.size, { getWeightAtKey(morphTimeValues[key], it).d }),
                                time = morphTimeValues[key].time.d)
                    })
                    morphAnims.add(morphAnim)
                }
            }
        }

        if (anims.isNotEmpty() || morphAnims.isNotEmpty())
            mAnims.add(AiAnimation(
                    name = pName,
                    numChannels = anims.size,
                    numMorphMeshChannels = morphAnims.size,
                    duration = 0.0,
                    ticksPerSecond = 1.0).apply {
                if (numChannels > 0) channels = anims.filterNotNullTo(ArrayList())
                if (numMorphMeshChannels > 0) morphMeshChannels = morphAnims
                anims.forEach{
                    duration = duration max it.positionKeys[it.numPositionKeys - 1].time
                    duration = duration max it.rotationKeys[it.numPositionKeys - 1].time
                    duration = duration max it.scalingKeys[it.numPositionKeys - 1].time
                }
                morphAnims.forEach { duration = duration max it.keys[it.numKeys - 1].time }
            })
    }

    /** Collects all nodes into the given array */
    fun collectNodes(pNode: AiNode, poNodes: ArrayList<AiNode>) {
        poNodes += pNode
        pNode.children.forEach { collectNodes(it, poNodes) }
    }

    class MorphTimeValues(var time: Float = 0f, key: Key) {
        val keys = arrayListOf(key)

        class Key(var weight: Float = 0f, var value: Int = 0)
    }

    fun insertMorphTimeValue(values: ArrayList<MorphTimeValues>, time: Float, weight: Float, value: Int) {
        val k = MorphTimeValues.Key(weight, value)
        if (values.isEmpty() || time < values[0].time) {
            values.add(0, MorphTimeValues(time, k))
            return
        }
        if (time > values.last().time) {
            values.add(MorphTimeValues(time, k))
            return
        }
        values.forEachIndexed { i, it ->
            if (glm.abs(time - it.time) < 1e-6f) {
                it.keys.add(k)
                return
            } else if (time > it.time && time < values[i + 1].time) {
                values.add(0 + i, MorphTimeValues(time, k))
                return
            }
        }
        throw Error()   // should not get here
    }

    /** no value at key found, try to interpolate if present at other keys. if not, return zero
     *  TODO: interpolation */
    fun getWeightAtKey(_val: MorphTimeValues, value: Int) = _val.keys.firstOrNull { it.value == value }?.weight ?: 0f

    /** Return importer meta information.
     *  See BaseImporter.info for the details */
    override val info = desc

    override fun setupProperties(imp:Importer)    {
        noSkeletonMesh = imp[AiConfig.Import.NO_SKELETON_MESHES] ?: false
        ignoreUpDirection = imp[AiConfig.Import.COLLADA_IGNORE_UP_DIRECTION] ?: false
    }

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