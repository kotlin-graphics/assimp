package format.collada

import main.BaseImporter
import java.net.URI
import main.*

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
    val mLights = ArrayList<AiLightSourceType>()

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
    }

    /** Recursively constructs a scene node for the given parser node and returns it.   */
    internal fun buildHierarchy(pParser: ColladaParser, pNode: Node): AiNode {

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
//        BuildMeshesForNode(pParser, pNode, node);
//
//        // construct cameras
//        BuildCamerasForNode(pParser, pNode, node);
//
//        // construct lights
//        BuildLightsForNode(pParser, pNode, node);
        return node;
    }

    /** Builds meshes for the given node and references them    */
    internal fun buildMeshesForNode(pParser: ColladaParser, pNode: Node, pTarget: AiNode) {

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
            val vertexStart = 0
            val faceStart = 0
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
//                val dstMesh = createMesh(pParser, srcMesh, submesh, srcController, vertexStart, faceStart)
//
//                // store the mesh, and store its new index in the node
//                newMeshRefs.push_back(mMeshes.size());
//                mMeshIndexByID[index] = mMeshes.size();
//                mMeshes.push_back(dstMesh);
//                vertexStart += dstMesh->mNumVertices; faceStart += submesh.mNumFaces;
//
//                // assign the material index
//                dstMesh->mMaterialIndex = matIdx;
//                if (dstMesh->mName.length == 0)
//                {
//                    dstMesh ->
//                    mName = mid.mMeshOrController;
//                }
            }
        }

        // now place all mesh references we gathered in the target node
//        pTarget->mNumMeshes = static_cast<unsigned int>(newMeshRefs.size());
//        if (newMeshRefs.size()) {
//            struct UIntTypeConverter
//                    {
//                        unsigned int operator()(const size_t & v) const
//                                {
//                                    return static_cast < unsigned int >(v);
//                                }
//                    };
//
//            pTarget->mMeshes = new unsigned int[pTarget->mNumMeshes];
//            std::transform(newMeshRefs.begin(), newMeshRefs.end(), pTarget->mMeshes, UIntTypeConverter());
//        }
    }

    /** Find mesh from either meshes or morph target meshes */
    internal fun findMesh(meshid: String) = mMeshes.firstOrNull { it.mName == meshid } ?: mTargetMeshes.firstOrNull { it.mName == meshid }

//    typealias IndexPairVector = ArrayList<Pair<Int, Int>>

    /** Creates a mesh for the given ColladaMesh face subset and returns the newly created mesh */
    internal fun createMesh(pParser: ColladaParser, pSrcMesh: Mesh, pSubMesh: SubMesh, pSrcController: Controller?, pStartVertex: Int, pStartFace: Int): AiMesh {

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
            val dstBones = MutableList(numBones, {AiVertexWeight()})

            // build a temporary array of pointers to the start of each vertex's weights
            val weightStartPerVertex = MutableList(pSrcController.mWeightCounts.size, {pSrcController.mWeights.last().toList().toMutableList()})
//            wei
//
//            IndexPairVector::const_iterator pit = pSrcController->mWeights.begin();
//            for (size_t a = 0; a < pSrcController->mWeightCounts.size(); ++a)
//            {
//                weightStartPerVertex[a] = pit;
//                pit += pSrcController->mWeightCounts[a];
//            }

            // now for each vertex put the corresponding vertex weights into each bone's weight collection
//            for( size_t a = pStartVertex; a < pStartVertex + numVertices; ++a)
//            {
//                // which position index was responsible for this vertex? that's also the index by which
//                // the controller assigns the vertex weights
//                size_t orgIndex = pSrcMesh->mFacePosIndices[a];
//                // find the vertex weights for this vertex
//                IndexPairVector::const_iterator iit = weightStartPerVertex[orgIndex];
//                size_t pairCount = pSrcController->mWeightCounts[orgIndex];
//
//                for( size_t b = 0; b < pairCount; ++b, ++iit)
//                {
//                    size_t jointIndex = iit->first;
//                    size_t vertexIndex = iit->second;
//
//                    ai_real weight = ReadFloat( weightsAcc, weights, vertexIndex, 0);
//
//                    // one day I gonna kill that XSI Collada exporter
//                    if( weight > 0.0f)
//                    {
//                        aiVertexWeight w;
//                        w.mVertexId = static_cast<unsigned int>(a - pStartVertex);
//                        w.mWeight = weight;
//                        dstBones[jointIndex].push_back( w);
//                    }
//                }
//            }
//
//            // count the number of bones which influence vertices of the current submesh
//            size_t numRemainingBones = 0;
//            for( std::vector<std::vector<aiVertexWeight> >::const_iterator it = dstBones.begin(); it != dstBones.end(); ++it)
//            if( it->size() > 0)
//            numRemainingBones++;
//
//            // create bone array and copy bone weights one by one
//            dstMesh->mNumBones = static_cast<unsigned int>(numRemainingBones);
//            dstMesh->mBones = new aiBone*[numRemainingBones];
//            size_t boneCount = 0;
//            for( size_t a = 0; a < numBones; ++a)
//            {
//                // omit bones without weights
//                if( dstBones[a].size() == 0)
//                    continue;
//
//                // create bone with its weights
//                aiBone* bone = new aiBone;
//                bone->mName = ReadString( jointNamesAcc, jointNames, a);
//                bone->mOffsetMatrix.a1 = ReadFloat( jointMatrixAcc, jointMatrices, a, 0);
//                bone->mOffsetMatrix.a2 = ReadFloat( jointMatrixAcc, jointMatrices, a, 1);
//                bone->mOffsetMatrix.a3 = ReadFloat( jointMatrixAcc, jointMatrices, a, 2);
//                bone->mOffsetMatrix.a4 = ReadFloat( jointMatrixAcc, jointMatrices, a, 3);
//                bone->mOffsetMatrix.b1 = ReadFloat( jointMatrixAcc, jointMatrices, a, 4);
//                bone->mOffsetMatrix.b2 = ReadFloat( jointMatrixAcc, jointMatrices, a, 5);
//                bone->mOffsetMatrix.b3 = ReadFloat( jointMatrixAcc, jointMatrices, a, 6);
//                bone->mOffsetMatrix.b4 = ReadFloat( jointMatrixAcc, jointMatrices, a, 7);
//                bone->mOffsetMatrix.c1 = ReadFloat( jointMatrixAcc, jointMatrices, a, 8);
//                bone->mOffsetMatrix.c2 = ReadFloat( jointMatrixAcc, jointMatrices, a, 9);
//                bone->mOffsetMatrix.c3 = ReadFloat( jointMatrixAcc, jointMatrices, a, 10);
//                bone->mOffsetMatrix.c4 = ReadFloat( jointMatrixAcc, jointMatrices, a, 11);
//                bone->mNumWeights = static_cast<unsigned int>(dstBones[a].size());
//                bone->mWeights = new aiVertexWeight[bone->mNumWeights];
//                std::copy( dstBones[a].begin(), dstBones[a].end(), bone->mWeights);
//
//                // apply bind shape matrix to offset matrix
//                aiMatrix4x4 bindShapeMatrix;
//                bindShapeMatrix.a1 = pSrcController->mBindShapeMatrix[0];
//                bindShapeMatrix.a2 = pSrcController->mBindShapeMatrix[1];
//                bindShapeMatrix.a3 = pSrcController->mBindShapeMatrix[2];
//                bindShapeMatrix.a4 = pSrcController->mBindShapeMatrix[3];
//                bindShapeMatrix.b1 = pSrcController->mBindShapeMatrix[4];
//                bindShapeMatrix.b2 = pSrcController->mBindShapeMatrix[5];
//                bindShapeMatrix.b3 = pSrcController->mBindShapeMatrix[6];
//                bindShapeMatrix.b4 = pSrcController->mBindShapeMatrix[7];
//                bindShapeMatrix.c1 = pSrcController->mBindShapeMatrix[8];
//                bindShapeMatrix.c2 = pSrcController->mBindShapeMatrix[9];
//                bindShapeMatrix.c3 = pSrcController->mBindShapeMatrix[10];
//                bindShapeMatrix.c4 = pSrcController->mBindShapeMatrix[11];
//                bindShapeMatrix.d1 = pSrcController->mBindShapeMatrix[12];
//                bindShapeMatrix.d2 = pSrcController->mBindShapeMatrix[13];
//                bindShapeMatrix.d3 = pSrcController->mBindShapeMatrix[14];
//                bindShapeMatrix.d4 = pSrcController->mBindShapeMatrix[15];
//                bone->mOffsetMatrix *= bindShapeMatrix;
//
//                // HACK: (thom) Some exporters address the bone nodes by SID, others address them by ID or even name.
//                // Therefore I added a little name replacement here: I search for the bone's node by either name, ID or SID,
//                // and replace the bone's name by the node's name so that the user can use the standard
//                // find-by-name method to associate nodes with bones.
//                const Collada::Node* bnode = FindNode( pParser.mRootNode, bone->mName.data);
//                if( !bnode)
//                    bnode = FindNodeBySID( pParser.mRootNode, bone->mName.data);
//
//                // assign the name that we would have assigned for the source node
//                if( bnode)
//                    bone->mName.Set( FindNameForNode( bnode));
//                else
//                DefaultLogger::get()->warn( format() << "ColladaLoader::CreateMesh(): could not find corresponding node for joint \"" << bone->mName.data << "\"." );
//
//                // and insert bone
//                dstMesh->mBones[boneCount++] = bone;
//            }
        }

        return dstMesh
    }

    /** Resolve node instances  */
    internal fun resolveNodeInstances(pParser: ColladaParser, pNode: Node, resolved: ArrayList<Node>) =
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
    internal fun applyVertexToEffectSemanticMapping(sampler: Sampler, table: SemanticMappingTable) = table.mMap[sampler.mUVChannel]?.let {
        if (it.mType != InputType.Texcoord)
            System.err.println("Collada: Unexpected effect input mapping")

        sampler.mUVId = it.mSet
    }

    /** Constructs materials from the collada material definitions  */
    internal fun buildMaterials(pParser: ColladaParser) = pParser.mMaterialLibrary.forEach { id, material ->

        // a material is only a reference to an effect
        pParser.mEffectLibrary[material.mEffect]?.let { effect ->

            // create material
            val mat = AiMaterial(name = if (material.mName.isEmpty()) id else material.mName)

            // store the material
            mMaterialIndexByName[id] = newMats.size
            newMats.add(Pair(effect, mat))
        }
    }


    /** Finds a node in the collada scene by the given name */
    internal fun findNode(pNode: Node, pName: String): Node? {
        if (pNode.mName == pName || pNode.mID == pName)
            return pNode

        pNode.mChildren.forEach { findNode(it, pName)?.let { node -> return node } }

        return null
    }

    /** Finds a proper name for a node derived from the collada-node's properties   */
    internal fun findNameForNode(pNode: Node) =
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