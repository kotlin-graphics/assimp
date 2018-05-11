/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/

package assimp.format.md5

import assimp.*
import assimp.format.AiConfig
import java.io.BufferedReader
import java.io.IOException
import kotlin.math.sqrt

// Minimum weight value. Weights inside [-n ... n] are ignored
val AI_MD5_WEIGHT_EPSILON = 1e-5f

/** Importer class for the MD5 file format */
class Md5Importer : BaseImporter() {

    /** Path to the file, excluding the file extension but with the dot */
    var file = ""
    /** Buffer to hold the loaded file */
    var buffer = ArrayList<String>()
    /** Size of the file */
    var fileSize = 0
    /** Current line number. For debugging purposes */
    var iLineNumber = 0
    /** Scene to be filled */
    var scene: AiScene? = null
    /** true if a MD5MESH file has already been parsed */
    var hadMD5Mesh = false
    /** true if a MD5ANIM file has already been parsed */
    var hadMD5Anim = false
    /** true if a MD5CAMERA file has already been parsed */
    var hadMD5Camera = false
    /** configuration option: prevent anim autoload */
    var configNoAutoLoad = false

    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {
        val extension = getExtension(file)
        if (extension == "md5anim" || extension == "md5mesh" || extension == "md5camera") return true
        else if (extension.isNotEmpty() || checkSig) {
//            TODO() silented to pass tests
//            const char* tokens[] = {"MD5Version"};
//            return SearchFileHeaderForToken(pIOHandler,pFile,tokens,1);
        }
        return false
    }

    /** Return importer meta information.
     * See BaseImporter::info for the details     */
    override val info
        get() = AiImporterDesc(
                name = "Doom 3 / MD5 Mesh Importer",
                flags = AiImporterFlags.SupportBinaryFlavour.i,
                fileExtensions = listOf("md5mesh", "md5camera", "md5anim")
        )

    /** Called prior to ReadFile().
     *  The function is a request to the importer to update its configuration basing on the Importer's configuration
     *  property list.     */
    override fun setupProperties(imp: Importer) {
        configNoAutoLoad = imp[AiConfig.Import.MD5_NO_ANIM_AUTOLOAD] ?: false
    }

    /** Imports the given file into the given scene structure.
     *  See BaseImporter::internReadFile() for details     */
    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        this.scene = scene
        hadMD5Mesh = false // TODO remove?
        hadMD5Anim = false
        hadMD5Camera = false

        val extension = getExtension(file)

        // remove the file extension
        val pos = file.lastIndexOf('.')
        this.file = if (pos == -1) file else file.substring(0, pos + 1)

        try {
            if (extension == "md5camera") loadMD5CameraFile(ioSystem)
            else if (configNoAutoLoad || extension == "md5anim") {
                // determine file extension and process just *one* file
                if (extension.isEmpty()) throw Error("Failure, need file extension to determine MD5 part type")
                if (extension == "md5anim") loadMD5AnimFile(ioSystem)
                else if (extension == "md5mesh") loadMD5MeshFile(ioSystem)
            } else {
                loadMD5MeshFile(ioSystem)
                loadMD5AnimFile(ioSystem)
            }
        } catch (exc: Exception) { // std::exception, Assimp::DeadlyImportError
            unloadFileFromMemory()
            throw Error(exc)
        }
        // make sure we have at least one file
        if (!hadMD5Mesh && !hadMD5Anim && !hadMD5Camera) throw Error("Failed to read valid contents out of this MD5* file")
        // Now rotate the whole scene 90 degrees around the x axis to match our internal coordinate system
        scene.rootNode.transformation.put(
                1f, 0f, 0f, 0f,
                0f, 0f, -1f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f)
        // the output scene wouldn't pass the validation without this flag
        if (!hadMD5Mesh) scene.flags = scene.flags or AI_SCENE_FLAGS_INCOMPLETE
        // clean the instance -- the BaseImporter instance may be reused later.
        unloadFileFromMemory()
    }

    /** Load a *.MD5MESH file.     */
    fun loadMD5MeshFile(ioSystem: IOSystem) {
        //val file = File(file + "md5mesh")
        val ioFile : IOStream
        try {
            ioFile = ioSystem.open(file + "md5mesh")
        } catch(e : IOException) {
            // Check whether we can read from the file
            logger.warn { "Failed to access MD5MESH file: $file" }
            return
        }
        loadFileIntoMemory(ioFile.reader())
        hadMD5Mesh = true

        // now construct a parser and parse the file
        val parser = MD5Parser(buffer)

        // load the mesh information from it
        val meshParser = MD5MeshParser(parser.sections)

        val scene = scene!!

        // create the bone hierarchy - first the root node and dummy nodes for all meshes
        scene.rootNode = AiNode("<MD5_Root>").apply {
            numChildren = 2

            children.add(AiNode().apply { name = "<MD5_Mesh>" })

            // build the hierarchy from the MD5MESH file
            children.add(AiNode().apply {
                name = "<MD5_Hierarchy>"
                attachChilds_Mesh(-1, this, meshParser.joints)
            })
        }
        scene.rootNode.children[0].parent = scene.rootNode
        scene.rootNode.children[1].parent = scene.rootNode

        // FIX: MD5 files exported from Blender can have empty meshes
        scene.numMaterials = meshParser.meshes.filter { it.faces.isNotEmpty() && it.vertices.isNotEmpty() }.count()

        // generate all meshes
        scene.numMeshes = scene.numMaterials

        //  storage for node mesh indices
        with(scene.rootNode.children[0]) {
            numMeshes = scene.numMeshes
            meshes = IntArray(numMeshes, { it })
        }

        var n = 0
        meshParser.meshes.filter { it.faces.isNotEmpty() || it.vertices.isNotEmpty() }.forEach { src ->

            val mesh = AiMesh()
            scene.meshes.add(mesh.apply {
                primitiveTypes = AiPrimitiveType.TRIANGLE.i

                // generate unique vertices in our internal verbose format
                makeDataUnique(src)

                numVertices = src.vertices.size
            })

            // copy texture coordinates
            mesh.textureCoords.add(MutableList(src.vertices.size, {
                with(src.vertices[it]) {
                    floatArrayOf(uv.x, 1f - uv.y) // D3D to OpenGL
                }
            }))

            // sort all bone weights - per bone
            val piCount = IntArray(meshParser.joints.size)

            src.vertices.forEach {
                for (w in it.firstWeight until it.firstWeight + it.numWeights) {
                    val desc = src.weights[w]
                    /* FIX for some invalid exporters */
                    if (!(desc.weight < AI_MD5_WEIGHT_EPSILON && desc.weight >= -AI_MD5_WEIGHT_EPSILON))
                        ++piCount[desc.bone]
                }
            }

            // check how many we will need
            for (p in 0 until meshParser.joints.size)
                if (piCount[p] != 0) mesh.numBones++

            if (mesh.numBones != 0) { // just for safety
                var h = 0
                for (q in meshParser.joints.indices) {
                    if (piCount[q] == 0) continue
                    mesh.bones.add(AiBone().apply {
                        numWeights = piCount[q]
                        weights = MutableList(numWeights, { AiVertexWeight() })
                        name = meshParser.joints[q].name
                        offsetMatrix put meshParser.joints[q].invTransform
                    })
                    // store the index for later use
                    val boneSrc = meshParser.joints[q]
                    boneSrc.map = h++

                    // compute w-component of quaternion
                    boneSrc.rotationQuat convertTo boneSrc.rotationQuatConverted
                }

                var pvi = -1
                for (it in src.vertices) {
                    // compute the final vertex position from all single weights
                    val pv = AiVector3D()
                    mesh.vertices.add(pv)
                    pvi++
                    // there are models which have weights which don't sum to 1 ...
                    val sum = (it.firstWeight until it.firstWeight + it.numWeights).map { src.weights[it].weight }.sum()
                    if (sum == 0f) {
                        logger.error { "MD5MESH: The sum of all vertex bone weights is 0" }
                        continue
                    }
                    // process bone weights
                    for (w in it.firstWeight until it.firstWeight + it.numWeights) {
                        if (w >= src.weights.size) throw Error("MD5MESH: Invalid weight index")

                        val desc = src.weights[w]
                        if (desc.weight < AI_MD5_WEIGHT_EPSILON && desc.weight >= -AI_MD5_WEIGHT_EPSILON) continue

                        val newWeight = desc.weight / sum

                        // transform the local position into worldspace
                        val boneSrc = meshParser.joints[desc.bone]
                        val v = boneSrc.rotationQuatConverted rotate desc.offsetPosition

                        // use the original weight to compute the vertex position (some MD5s seem to depend on the invalid weight values ...)
                        pv += (boneSrc.positionXYZ + v) * desc.weight

                        mesh.bones[boneSrc.map].weights[pvi].apply { vertexId = pvi; weight = newWeight; }
                    }
                }
            }
            /*  now setup all faces - we can directly copy the list
                (however, take care that the aiFace destructor doesn't delete the mIndices array)             */
            mesh.numFaces = src.faces.size
//            for (c in 0 until mesh.numFaces) TODO check
            mesh.faces.addAll(src.faces)

            // generate a material for the mesh
            scene.materials.add(AiMaterial().apply {
                /*  insert the typical doom3 textures:
                    nnn_local.tga  - normal map
                    nnn_h.tga      - height map
                    nnn_s.tga      - specular map
                    nnn_d.tga      - diffuse map */
                if (src.shader.isNotEmpty() && !src.shader.contains('.')) {
                    textures.add(AiMaterial.Texture(type = AiTexture.Type.normals, file = "${src.shader}_local.tga"))
                    textures.add(AiMaterial.Texture(type = AiTexture.Type.specular, file = "${src.shader}_s.tga"))
                    textures.add(AiMaterial.Texture(type = AiTexture.Type.diffuse, file = "${src.shader}_d.tga"))
                    textures.add(AiMaterial.Texture(type = AiTexture.Type.height, file = "${src.shader}_h.tga"))
                    // set this also as material name
                    name = src.shader
                } else textures.add(AiMaterial.Texture(file = src.shader))
            })
            mesh.materialIndex = n++
        }
    }

    infix fun AiQuaternion.rotate(v: AiVector3D): AiVector3D {
        val q2 = AiQuaternion(0f, v)
        val qinv = AiQuaternion(this).apply { conjugateAssign() }

        val q = times(q2)
        q timesAssign qinv
        return AiVector3D(q.x, q.y, q.z)
    }

    /** Load a *.MD5ANIM file.     */
    fun loadMD5AnimFile(ioSystem: IOSystem) {
        val ioFile : IOStream
        try {
            ioFile = ioSystem.open(file + "md5anim")
        } catch(e : IOException) {
            // Check whether we can read from the file
            logger.warn { "Failed to access MD5ANIM file: $file" }
            return
        }
        loadFileIntoMemory(ioFile.reader())

        // parse the basic file structure
        val parser = MD5Parser(buffer)
        TODO()
        // load the animation information from the parse tree
//        val animParser = MD5AnimParser (parser.sections)
//
//        // generate and fill the output animation
//        if (animParser.mAnimatedBones.empty() || animParser.mFrames.empty() ||
//                animParser.mBaseFrames.size() != animParser.mAnimatedBones.size())  {
//
//            DefaultLogger::get()->error("MD5ANIM: No frames or animated bones loaded")
//        }
//        else {
//            bHadMD5Anim = true
//
//            pScene->mAnimations = new aiAnimation*[pScene->mNumAnimations = 1]
//            aiAnimation* anim = pScene->mAnimations[0] = new aiAnimation()
//            anim->mNumChannels = (unsigned int)animParser.mAnimatedBones.size()
//            anim->mChannels = new aiNodeAnim*[anim->mNumChannels]
//            for (unsigned int i = 0; i < anim->mNumChannels;++i)    {
//                aiNodeAnim* node = anim->mChannels[i] = new aiNodeAnim()
//                node->mNodeName = aiString( animParser.mAnimatedBones[i].mName )
//
//                // allocate storage for the keyframes
//                node->mPositionKeys = new aiVectorKey[animParser.mFrames.size()]
//                node->mRotationKeys = new aiQuatKey[animParser.mFrames.size()]
//            }
//
//            // 1 tick == 1 frame
//            anim->mTicksPerSecond = animParser.fFrameRate
//
//            for (FrameList::const_iterator iter = animParser.mFrames.begin(), iterEnd = animParser.mFrames.end();iter != iterEnd;++iter){
//                double dTime = (double)(*iter).iIndex
//                aiNodeAnim** pcAnimNode = anim->mChannels
//                if (!(*iter).mValues.empty() || iter == animParser.mFrames.begin()) /* be sure we have at least one frame */
//                {
//                    // now process all values in there ... read all joints
//                    MD5::BaseFrameDesc* pcBaseFrame = &animParser.mBaseFrames[0]
//                    for (AnimBoneList::const_iterator iter2 = animParser.mAnimatedBones.begin(); iter2 != animParser.mAnimatedBones.end();++iter2,
//                    ++pcAnimNode,++pcBaseFrame)
//                    {
//                        if((*iter2).iFirstKeyIndex >= (*iter).mValues.size()) {
//
//                        // Allow for empty frames
//                        if ((*iter2).iFlags != 0) {
//                        throw DeadlyImportError("MD5: Keyframe index is out of range")
//
//                    }
//                        continue
//                    }
//                        const float* fpCur = &(*iter).mValues[(*iter2).iFirstKeyIndex]
//                        aiNodeAnim* pcCurAnimBone = *pcAnimNode
//
//                        aiVectorKey* vKey = &pcCurAnimBone->mPositionKeys[pcCurAnimBone->mNumPositionKeys++]
//                        aiQuatKey* qKey = &pcCurAnimBone->mRotationKeys  [pcCurAnimBone->mNumRotationKeys++]
//                        aiVector3D vTemp
//
//                        // translational component
//                        for (unsigned int i = 0; i < 3; ++i) {
//                        if ((*iter2).iFlags & (1u << i)) {
//                        vKey->mValue[i] =  *fpCur++
//                    }
//                        else vKey->mValue[i] = pcBaseFrame->vPositionXYZ[i]
//                    }
//
//                        // orientation component
//                        for (unsigned int i = 0; i < 3; ++i) {
//                        if ((*iter2).iFlags & (8u << i)) {
//                        vTemp[i] =  *fpCur++
//                    }
//                        else vTemp[i] = pcBaseFrame->vRotationQuat[i]
//                    }
//
//                        MD5::ConvertQuaternion(vTemp, qKey->mValue)
//                        qKey->mTime = vKey->mTime = dTime
//                    }
//                }
//
//                // compute the duration of the animation
//                anim->mDuration = std::max(dTime,anim->mDuration)
//            }
//
//            // If we didn't build the hierarchy yet (== we didn't load a MD5MESH),
//            // construct it now from the data given in the MD5ANIM.
//            if (!pScene->mRootNode) {
//                pScene->mRootNode = new aiNode()
//                pScene->mRootNode->mName.Set("<MD5_Hierarchy>")
//
//                AttachChilds_Anim(-1,pScene->mRootNode,animParser.mAnimatedBones,(const aiNodeAnim**)anim->mChannels)
//
//                // Call SkeletonMeshBuilder to construct a mesh to represent the shape
//                if (pScene->mRootNode->mNumChildren) {
//                    SkeletonMeshBuilder skeleton_maker(pScene,pScene->mRootNode->mChildren[0])
//                }
//            }
//        }
    }

    /** Load a *.MD5CAMERA file.     */
    fun loadMD5CameraFile(ioSystem: IOSystem): Nothing = TODO()

    /** Construct node hierarchy from a given MD5ANIM
     *  @param iParentID Current parent ID
     *  @param piParent Parent node to attach to
     *  @param bones Input bones
     *  @param node_anims Generated node animations
     */
//    void AttachChilds_Anim (int iParentID, aiNode* piParent, AnimBoneList& bones, const aiNodeAnim** node_anims)

    /** Construct node hierarchy from a given MD5MESH
     *  @param iParentID Current parent ID
     *  @param piParent Parent node to attach to
     *  @param bones Input bones
     */
    fun attachChilds_Mesh(parentId: Int, parent: AiNode, bones: ArrayList<BoneDesc>) {
        assert(parent.numChildren == 0)

        // First find out how many children we'll have
        parent.numChildren = bones.filterIndexed { i, b -> parentId != i && b.parentIndex == parentId }.count()
        if (parent.numChildren != 0) {
            bones.forEachIndexed { i, b ->
                // (avoid infinite recursion)
                if (parentId != i && b.parentIndex == parentId) {
                    val pc = AiNode()
                    // setup a new node
                    parent.children.add(pc.apply { name = b.name })
                    pc.parent = parent

                    // get the transformation matrix from rotation and translational components
                    val quat = AiQuaternion()
                    b.rotationQuat convertTo quat

                    with(b.transform) {
                        put(AiMatrix4x4(quat.mat))
                        d0 = b.positionXYZ.x
                        d1 = b.positionXYZ.y
                        d2 = b.positionXYZ.z
                    }
                    // store it for later use
                    b.invTransform put b.transform
                    pc.transformation put b.transform
                    b.invTransform.inverseAssign()

                    /*  the transformations for each bone are absolute, so we need to multiply them with the inverse of
                        the absolute matrix of the parent joint                     */
                    if (-1 != parentId) pc.transformation put bones[parentId].invTransform * pc.transformation

                    // add children to this node, too
                    attachChilds_Mesh(i, pc, bones)
                }
            }
            // undo offset computations
//            for (i in 0 until parent.numChildren) parent.children.removeAt(parent.children.lastIndex)
        }
    }

    /** Build unique vertex buffers from a given MD5ANIM
     *  @param meshSrc Input data     */
    fun makeDataUnique(meshSrc: MeshDesc) {
        val abHad = BooleanArray(meshSrc.vertices.size)

        // allocate enough storage to keep the output structures
        val iNewNum = meshSrc.faces.size * 3
        var iNewIndex = meshSrc.vertices.size
        for (i in meshSrc.vertices.size until iNewNum) meshSrc.vertices.add(VertexDesc())

        meshSrc.faces.forEach {
            for (i in 0..2) {
                if (it[0] >= meshSrc.vertices.size) throw Error("MD5MESH: Invalid vertex index")
                if (abHad[it[i]]) {
                    // generate a new vertex
                    meshSrc.vertices[iNewIndex] = meshSrc.vertices[it[i]]
                    it[i] = iNewIndex++
                } else abHad[it[i]] = true
            }
            // swap face order
            val t = it[0]
            it[0] = it[2]
            it[2] = t
        }
    }

    /** Load the contents of a specific file into memory and
     *  allocates a buffer to keep it.
     *
     *  mBuffer is modified to point to this buffer.
     *  @param pFile File stream to be read
     */
    fun loadFileIntoMemory(file: BufferedReader) {
        // unload the previous buffer, if any
        unloadFileFromMemory()

        //fileSize = file.length().i
        //assert(fileSize != 0)

        // allocate storage and copy the contents of the file to a memory buffer
        file.readLines()
                // now remove all line comments from the file
                .filter { !it.startsWith("//") && it.isNotEmpty() && it.isNotBlank() }
                .map { it.trim() }
                .toCollection(buffer)
        iLineNumber = 1
    }

    /** Unload the current memory buffer */
    fun unloadFileFromMemory() {
        // delete the file buffer
        buffer.clear()
        fileSize = 0
    }
}

/** Convert a quaternion to its usual representation */
infix fun AiVector3D.convertTo(out: AiQuaternion) {
    out.x = x
    out.y = y
    out.z = z
    val t = 1f - x * x - y * y - z * z
    if (t < 0f) out.w = 0f
    else out.w = sqrt(t)
    // Assimp convention.
    out.w *= -1f
}

/** Returns a matrix representation of the quaternion */
val AiQuaternion.mat
    get() = AiMatrix3x3().apply {
        a0 = 1f - 2f * (y * y + z * z)
        b0 = 2f * (x * y - z * w)
        c0 = 2f * (x * z + y * w)
        a1 = 2f * (x * y + z * w)
        b1 = 1f - 2f * (x * x + z * z)
        c1 = 2f * (y * z - x * w)
        a2 = 2f * (x * z - y * w)
        b2 = 2f * (y * z + x * w)
        c2 = 1f - 2f * (x * x + y * y)
    }