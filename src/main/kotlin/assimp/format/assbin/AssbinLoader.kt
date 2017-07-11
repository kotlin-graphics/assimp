package assimp.format.assbin

import assimp.*
import gli.has
import gli.hasnt
import glm_.*
import glm_.mat4x4.Mat4
import java.io.File
import java.io.InputStream
import java.net.URI

class AssbinLoader : BaseImporter() {

    companion object {

        val desc = AiImporterDesc(
                mName = ".assbin Importer",
                mComments = "Gargaj / Conspiracy",
                mFlags = AiImporterFlags.SupportBinaryFlavour or AiImporterFlags.SupportCompressedFlavour,
                mFileExtensions = "assbin"
        )
    }

    var shortened = false
    var compressed = false

    override fun canRead(pFile: URI, checkSig: Boolean) =
            File(pFile).inputStream().use { i -> "ASSIMP.binary-dump.".all { it.i == i.read() } }

    override fun internReadFile(pFile: URI, pScene: AiScene) {

        pFile.toURL().openStream().use {

            it.skip(44) // signature

            val a = it.int()   // unsigned int versionMajor
            val b = it.int()   // unsigned int versionMinor
            val c = it.int()   // unsigned int versionRevision
            val d = it.int()   // unsigned int compileFlags

            shortened = it.short().bool
            compressed = it.short().bool

            if (shortened) throw Error("Shortened binaries are not supported!")

            it.skip(256)    // original filename
            it.skip(128)    // options
            it.skip(64)     // padding

            if (compressed) {

                TODO()
//            val uncompressedSize = stream.readInt()
//            val compressedSize = static_cast < uLongf >(stream->FileSize()-stream->Tell())
//
//            unsigned char * compressedData = new unsigned char [compressedSize]
//            stream->Read(compressedData, 1, compressedSize)
//
//            unsigned char * uncompressedData = new unsigned char [uncompressedSize]
//
//            uncompress(uncompressedData, & uncompressedSize, compressedData, compressedSize)
//
//            MemoryIOStream io (uncompressedData, uncompressedSize)
//
//            ReadBinaryScene(& io, pScene)
//
//            delete[] uncompressedData
//            delete[] compressedData

            } else it.readScene(pScene)

        }
    }


    fun InputStream.readScene(scene: AiScene) {

        assert(int() == ASSBIN_CHUNK_AISCENE)
        int()    // size

        scene.mFlags = int()
        scene.mNumMeshes = int()
        scene.mNumMaterials = int()
        scene.mNumAnimations = int()
        scene.mNumTextures = int()
        scene.mNumLights = int()
        scene.mNumCameras = int()

        // Read node graph
        scene.mRootNode = AiNode()
        readNode(scene.mRootNode)

        // Read all meshes
        for (i in 0 until scene.mNumMeshes)
            scene.mMeshes.add(AiMesh().also { readMesh(it) })

        // Read materials
        for (i in 0 until scene.mNumMaterials)
            scene.mMaterials.add(AiMaterial().also { readMaterial(it) })

        // Read all animations
        for (i in 0 until scene.mNumAnimations)
            scene.mAnimations.add(AiAnimation().also { readAnimation(it) })

        // Read all textures
//        for (i in 0 until scene.mNumTextures)
//            scene.mTextures["$i"] = gli.Texture(AiAnimation().also { readAnimation(it) })
//        if (scene.mNumTextures > 0) {
//            scene.mTextures = new aiTexture *[scene->mNumTextures]
//            for (unsigned int i = 0; i < scene->mNumTextures;++i) {
//                scene.mTextures[i] = new aiTexture ()
//                ReadBinaryTexture(stream, scene->mTextures[i])
//            }
//        }

        // Read lights
        for (i in 0 until scene.mNumLights)
            scene.mLights = List(scene.mNumLights, { AiLight().also { readLight(it) } })

        // Read cameras
        for (i in 0 until scene.mNumCameras)
            scene.mCameras = List(scene.mNumCameras, { AiCamera().also { readCamera(it) } })
    }

    private fun InputStream.readNode(node: AiNode, parent: AiNode? = null) {

        assert(int() == ASSBIN_CHUNK_AINODE)
        int()    // size

        node.mName = readString()
        node.mTransformation = mat4()
        node.mNumChildren = int()
        node.mNumMeshes = int()
        parent?.let { node.mParent = parent }

        if (node.mNumMeshes > 0)
            node.mMeshes = IntArray(node.mNumMeshes, { int() })

        for (i in 0 until node.mNumChildren)
            node.mChildren.add(AiNode().also { readNode(it, node) })
    }

    private fun InputStream.readMesh(mesh: AiMesh) {

        assert(int() == ASSBIN_CHUNK_AIMESH)
        int()    // size

        mesh.mPrimitiveTypes = int()
        mesh.mNumVertices = int()
        mesh.mNumFaces = int()
        mesh.mNumBones = int()
        mesh.mMaterialIndex = int()

        // first of all, write bits for all existent vertex components
        val c = int()

        if (c has ASSBIN_MESH_HAS_POSITIONS)
            if (shortened)
                TODO()//ReadBounds(stream, mesh->mVertices, mesh->mNumVertices)
            else    // else write as usual
                mesh.mVertices = MutableList(mesh.mNumVertices, { AiVector3D(this) })

        if (c has ASSBIN_MESH_HAS_NORMALS)
            if (shortened)
                TODO()//ReadBounds(stream, mesh->mNormals, mesh->mNumVertices)
            else    // else write as usual
                mesh.mNormals = MutableList(mesh.mNumVertices, { AiVector3D(this) })

        if (c has ASSBIN_MESH_HAS_TANGENTS_AND_BITANGENTS) {
            if (shortened) {
                TODO()//ReadBounds(stream, mesh->mTangents, mesh->mNumVertices)
                //ReadBounds(stream, mesh->mBitangents, mesh->mNumVertices)
            } else {   // else write as usual
                mesh.mTangents = MutableList(mesh.mNumVertices, { AiVector3D(this) })
                mesh.mBitangents = MutableList(mesh.mNumVertices, { AiVector3D(this) })
            }
        }
        for (n in 0 until AI_MAX_NUMBER_OF_COLOR_SETS) {
            if (c hasnt ASSBIN_MESH_HAS_COLOR(n))
                break

            if (shortened)
                TODO()//ReadBounds(stream, mesh->mColors[n], mesh->mNumVertices)
            else    // else write as usual
                mesh.mColors.add(MutableList(mesh.mNumVertices, { AiColor4D(this) }))
        }
        for (n in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
            if (c hasnt ASSBIN_MESH_HAS_TEXCOORD(n))
                break

            // write number of UV components
            val mNumUVComponents = int()

            if (shortened)
                TODO()//ReadBounds(stream, mesh->mTextureCoords[n], mesh->mNumVertices)
            else    // else write as usual
                mesh.mTextureCoords.add(MutableList(mesh.mNumVertices, {
                    FloatArray(mNumUVComponents, { float() })
                }))
        }

        /*  write faces. There are no floating-point calculations involved in these, so we can write a simple hash over
            the face data to the dump file. We generate a single 32 Bit hash for 512 faces using Assimp's standard
            hashing function.   */
        if (shortened)
            int()
        else  // else write as usual
        // if there are less than 2^16 vertices, we can simply use 16 bit integers ...
            mesh.mFaces = MutableList(mesh.mNumFaces, {
                assert(AI_MAX_FACE_INDICES <= 0xffff, { "AI_MAX_FACE_INDICES <= 0xffff" })
                val mNumIndices = short()
                MutableList(mNumIndices.i, { if (mesh.mNumVertices < (1 shl 16)) short() else int() })
            })

        // write bones
        for (i in 0 until mesh.mNumBones)
            mesh.mBones.add(AiBone().also { readBone(it) })
    }

    private fun InputStream.readBone(b: AiBone) {

        assert(int() == ASSBIN_CHUNK_AIBONE)
        int()   // size

        b.mName = readString()
        b.mNumWeights = int()
        b.mOffsetMatrix = mat4()

        // for the moment we write dumb min/max values for the bones, too.
        // maybe I'll add a better, hash-like solution later
        if (shortened)
            TODO()  //ReadBounds(stream, b->mWeights, b->mNumWeights)
        else    // else write as usual
            b.mWeights = List(b.mNumWeights, { readVertexWeight() })
    }

    private fun InputStream.readMaterial(mat: AiMaterial) {

        assert(int() == ASSBIN_CHUNK_AIMATERIAL)
        int()   // size

        val mNumProperties = int()
        if (mNumProperties > 0) {
            readMaterialProperty(mat)
//            if (mat->mProperties)
//                delete[] mat->mProperties
//            mat.mProperties = new aiMaterialProperty*[mat->mNumProperties]
//            for (unsigned int i = 0; i < mat->mNumProperties;++i) {
//                mat.mProperties[i] = new aiMaterialProperty ()
//                ReadBinaryMaterialProperty(stream, mat->mProperties[i])
//            }
        }
    }

    private fun InputStream.readMaterialProperty(mat: AiMaterial) {

        assert(int() == ASSBIN_CHUNK_AIMATERIALPROPERTY)
        int()   // size

        val mKey = readString()
//        prop->mSemantic = Read<unsigned int>(stream)
//        prop->mIndex = Read<unsigned int>(stream)
//
//        prop->mDataLength = Read<unsigned int>(stream)
//        prop->mType = (aiPropertyTypeInfo)Read<unsigned int>(stream)
//        prop->mData = new char [ prop->mDataLength ]
//        stream->Read(prop->mData,1,prop->mDataLength)
    }

    private fun InputStream.readAnimation(anim: AiAnimation) {

        assert(int() == ASSBIN_CHUNK_AIANIMATION)
        int()   // size

        anim.mName = readString()
        anim.mDuration = double()
        anim.mTicksPerSecond = double()
        anim.mNumChannels = int()

        anim.mChannels = MutableList(anim.mNumChannels, { AiNodeAnim().also { readNodeAnim(it) } })
    }

    private fun InputStream.readNodeAnim(nd: AiNodeAnim) {

        assert(int() == ASSBIN_CHUNK_AINODEANIM)
        int()   // size

        nd.mNodeName = readString()
        nd.mNumPositionKeys = int()
        nd.mNumRotationKeys = int()
        nd.mNumScalingKeys = int()
        nd.mPreState = AiAnimBehaviour.of(int())
        nd.mPostState = AiAnimBehaviour.of(int())

        if (nd.mNumPositionKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->mPositionKeys, nd->mNumPositionKeys)
            else    // else write as usual
                nd.mPositionKeys = List(nd.mNumPositionKeys, { readVectorKey() })

        if (nd.mNumRotationKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->mRotationKeys, nd->mNumRotationKeys)
            else    // else write as usual
                nd.mRotationKeys = List(nd.mNumRotationKeys, { readQuatKey() })

        if (nd.mNumScalingKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->mScalingKeys, nd->mNumScalingKeys)
            else    // else write as usual
                nd.mScalingKeys = List(nd.mNumScalingKeys, { readQuatKey() })
    }

    private fun InputStream.readLight(l: AiLight) {

        assert(int() == ASSBIN_CHUNK_AILIGHT)
        int()   // size

        l.mName = readString()
        l.mType = AiLightSourceType.of(int())

        if (l.mType != AiLightSourceType.DIRECTIONAL) {
            l.mAttenuationConstant = float()
            l.mAttenuationLinear = float()
            l.mAttenuationQuadratic = float()
        }

        l.mColorDiffuse = AiColor3D(this)
        l.mColorSpecular = AiColor3D(this)
        l.mColorAmbient = AiColor3D(this)

        if (l.mType == AiLightSourceType.SPOT) {
            l.mAngleInnerCone = float()
            l.mAngleOuterCone = float()
        }
    }

    private fun InputStream.readCamera(cam: AiCamera) {

        assert(int() == ASSBIN_CHUNK_AICAMERA)
        int()   // size

        cam.mName = readString()
        cam.mPosition = AiVector3D(this)
        cam.mLookAt = AiVector3D(this)
        cam.mUp = AiVector3D(this)
        cam.mHorizontalFOV = float()
        cam.mClipPlaneNear = float()
        cam.mClipPlaneFar = float()
        cam.mAspect = float()
    }

    private val ASSBIN_HEADER_LENGTH = 512

    // these are the magic chunk identifiers for the binary ASS file format
    private val ASSBIN_CHUNK_AICAMERA = 0x1234
    private val ASSBIN_CHUNK_AILIGHT = 0x1235
    private val ASSBIN_CHUNK_AITEXTURE = 0x1236
    private val ASSBIN_CHUNK_AIMESH = 0x1237
    private val ASSBIN_CHUNK_AINODEANIM = 0x1238
    private val ASSBIN_CHUNK_AISCENE = 0x1239
    private val ASSBIN_CHUNK_AIBONE = 0x123a
    private val ASSBIN_CHUNK_AIANIMATION = 0x123b
    private val ASSBIN_CHUNK_AINODE = 0x123c
    private val ASSBIN_CHUNK_AIMATERIAL = 0x123d
    private val ASSBIN_CHUNK_AIMATERIALPROPERTY = 0x123e

    private val ASSBIN_MESH_HAS_POSITIONS = 0x1
    private val ASSBIN_MESH_HAS_NORMALS = 0x2
    private val ASSBIN_MESH_HAS_TANGENTS_AND_BITANGENTS = 0x4
    private val ASSBIN_MESH_HAS_TEXCOORD_BASE = 0x100
    private val ASSBIN_MESH_HAS_COLOR_BASE = 0x10000

    private fun ASSBIN_MESH_HAS_TEXCOORD(n: Int) = ASSBIN_MESH_HAS_TEXCOORD_BASE shl n
    private fun ASSBIN_MESH_HAS_COLOR(n: Int) = ASSBIN_MESH_HAS_COLOR_BASE shl n

    fun InputStream.int(): Int {
        val a = read()
        val b = read()
        val c = read()
        val d = read()
        return (d shl 24) + (c shl 16) + (b shl 8) + a
    }

    fun InputStream.short(): Int {
        val a = read()
        val b = read()
        return (b shl 8) + a
    }

    fun InputStream.byte() = read().b

    fun InputStream.float() = Float.intBitsToFloat(int())
    fun InputStream.double() = Double.longBitsToDouble(long())

    fun InputStream.long(): Long {
        val a = int()
        val b = int()
        return (b.L shl 32) + a
    }

    fun InputStream.mat4() = Mat4(
            float(), float(), float(), float(),
            float(), float(), float(), float(),
            float(), float(), float(), float(),
            float(), float(), float(), float())

    private fun InputStream.readString() = String(ByteArray(int(), { byte() }))
    private fun InputStream.readVertexWeight() = AiVertexWeight(int(), float())
    private fun InputStream.readVectorKey() = AiVectorKey(double(), AiVector3D(this))
    private fun InputStream.readQuatKey() = AiQuatKey(double(), AiQuaternion(this))
}