package assimp.format.assbin

import assimp.*
import gli_.has
import gli_.hasnt
import glm_.*
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
    private val be = false // big endian TODO glm global?

    override fun canRead(pFile: URI, checkSig: Boolean) =
            File(pFile).inputStream().use { i -> "ASSIMP.binary-dump.".all { it.i == i.read() } }

    override fun internReadFile(pFile: URI, pScene: AiScene) {

        pFile.toURL().openStream().use {

            it.skip(44) // signature

            val a = it.int(be)   // unsigned int versionMajor
            val b = it.int(be)   // unsigned int versionMinor
            val c = it.int(be)   // unsigned int versionRevision
            val d = it.int(be)   // unsigned int compileFlags

            shortened = it.short(be).bool
            compressed = it.short(be).bool

            if (shortened) throw Error("Shortened binaries are not supported!")

            it.skip(256)    // original filename
            it.skip(128)    // options
            it.skip(64)     // padding

            if (compressed) {

                TODO()
//            val uncompressedSize = stream.readint(be)
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

        assert(int(be) == ASSBIN_CHUNK_AISCENE)
        int(be)    // size

        scene.flags = int(be)
        scene.numMeshes = int(be)
        scene.mNumMaterials = int(be)
        scene.mNumAnimations = int(be)
        scene.mNumTextures = int(be)
        scene.mNumLights = int(be)
        scene.mNumCameras = int(be)

        // Read node graph
        scene.rootNode = AiNode()
        readNode(scene.rootNode)

        // Read all meshes
        for (i in 0 until scene.numMeshes)
            scene.meshes.add(AiMesh().also { readMesh(it) })

        // Read materials
        for (i in 0 until scene.mNumMaterials)
            scene.mMaterials.add(AiMaterial().also { readMaterial(it) })

        // Read all animations
        for (i in 0 until scene.mNumAnimations)
            scene.mAnimations.add(AiAnimation().also { readAnimation(it) })

        // Read all textures
        for (i in 0 until scene.mNumTextures)
            readTexture()
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
            scene.mLights = Array(scene.mNumLights, { AiLight().also { readLight(it) } }).toCollection(ArrayList())

        // Read cameras
        for (i in 0 until scene.mNumCameras)
            scene.mCameras = Array(scene.mNumCameras, { AiCamera().also { readCamera(it) } }).toCollection(ArrayList())
    }

    private fun InputStream.readNode(node: AiNode, parent: AiNode? = null) {

        assert(int(be) == ASSBIN_CHUNK_AINODE)
        int(be)    // size

        node.name = string()
        node.transformation = mat4()
        node.numChildren = int(be)
        node.numMeshes = int(be)
        parent?.let { node.parent = parent }

        if (node.numMeshes > 0)
            node.meshes = IntArray(node.numMeshes, { int(be) })

        for (i in 0 until node.numChildren)
            node.children.add(AiNode().also { readNode(it, node) })
    }

    private fun InputStream.readMesh(mesh: AiMesh) {

        assert(int(be) == ASSBIN_CHUNK_AIMESH)
        int(be)    // size

        mesh.mPrimitiveTypes = int(be)
        mesh.mNumVertices = int(be)
        mesh.mNumFaces = int(be)
        mesh.mNumBones = int(be)
        mesh.mMaterialIndex = int(be)

        // first of all, write bits for all existent vertex components
        val c = int(be)

        if (c has ASSBIN_MESH_HAS_POSITIONS)
            if (shortened)
                TODO()//ReadBounds(stream, mesh->mVertices, mesh->mNumVertices)
            else    // else write as usual
                mesh.mVertices = MutableList(mesh.mNumVertices, { AiVector3D(this, be) })

        if (c has ASSBIN_MESH_HAS_NORMALS)
            if (shortened)
                TODO()//ReadBounds(stream, mesh->mNormals, mesh->mNumVertices)
            else    // else write as usual
                mesh.mNormals = MutableList(mesh.mNumVertices, { AiVector3D(this, be) })

        if (c has ASSBIN_MESH_HAS_TANGENTS_AND_BITANGENTS) {
            if (shortened) {
                TODO()//ReadBounds(stream, mesh->mTangents, mesh->mNumVertices)
                //ReadBounds(stream, mesh->mBitangents, mesh->mNumVertices)
            } else {   // else write as usual
                mesh.mTangents = MutableList(mesh.mNumVertices, { AiVector3D(this, be) })
                mesh.mBitangents = MutableList(mesh.mNumVertices, { AiVector3D(this, be) })
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
            val mNumUVComponents = int(be)

            if (shortened)
                TODO()//ReadBounds(stream, mesh->mTextureCoords[n], mesh->mNumVertices)
            else    // else write as usual
                mesh.mTextureCoords.add(MutableList(mesh.mNumVertices, {
                    val uv = AiVector3D(this, be)
                    (0 until mNumUVComponents).map { uv[it] }.toFloatArray()
                }))
        }

        /*  write faces. There are no floating-point calculations involved in these, so we can write a simple hash over
            the face data to the dump file. We generate a single 32 Bit hash for 512 faces using Assimp's standard
            hashing function.   */
        if (shortened)
            int(be)
        else  // else write as usual
        // if there are less than 2^16 vertices, we can simply use 16 bit integers ...
            mesh.mFaces = MutableList(mesh.mNumFaces, {
                assert(AI_MAX_FACE_INDICES <= 0xffff, { "AI_MAX_FACE_INDICES <= 0xffff" })
                val mNumIndices = short(be)
                MutableList(mNumIndices.i, { if (mesh.mNumVertices < (1 shl 16)) short(be) else int(be) })
            })

        // write bones
        for (i in 0 until mesh.mNumBones)
            mesh.mBones.add(AiBone().also { readBone(it) })
    }

    private fun InputStream.readBone(b: AiBone) {

        assert(int(be) == ASSBIN_CHUNK_AIBONE)
        int(be)   // size

        b.name = string()
        b.numWeights = int(be)
        b.offsetMatrix = mat4()

        // for the moment we write dumb min/max values for the bones, too.
        // maybe I'll add a better, hash-like solution later
        if (shortened)
            TODO()  //ReadBounds(stream, b->weights, b->mNumWeights)
        else    // else write as usual
            b.weights = Array(b.numWeights, { vertexWeight() })
    }

    private fun InputStream.readMaterial(mat: AiMaterial) {

        assert(int(be) == ASSBIN_CHUNK_AIMATERIAL)
        int(be)   // size

        val mNumProperties = int(be)
        if (mNumProperties > 0) {
            // TODO reset?
//            if (mat->mProperties)
//                delete[] mat->mProperties
            for (i in 0 until mNumProperties)
                readMaterialProperty(mat)
        }
    }

    private fun InputStream.readMaterialProperty(mat: AiMaterial) {

        assert(int(be) == ASSBIN_CHUNK_AIMATERIALPROPERTY)
        int(be)   // size

        val mKey = string()
        val mSemantic = int(be)
        val mIndex = int(be)

        val mDataLength = int(be)
        val mType = int(be)
        when (mKey) {
            "?mat.name" -> mat.name = string(mDataLength).replace(Regex("[^A-Za-z0-9 ]"), "")
            "\$mat.twosided" -> mat.twoSided = short(be).bool
            "\$mat.shadingm" -> mat.shadingModel = AiShadingMode.of(int(be))
            "\$mat.wireframe" -> mat.wireframe = short(be).bool
            "\$mat.blend" -> mat.blendFunc = AiBlendMode.of(int(be))
            "\$mat.opacity" -> mat.opacity = float(be)
            "\$mat.bumpscaling" -> mat.bumpScaling = float(be)
            "\$mat.shininess" -> mat.shininess = float(be)
            "\$mat.reflectivity" -> mat.reflectivity = float(be)
            "\$mat.shinpercent" -> mat.shininessStrength = float(be)
            "\$mat.refracti" -> mat.refracti = float(be)
            "\$clr.diffuse" -> {
                val diffuse = AiColor3D(this, be)
                if (mDataLength == AiColor4D.size) float() // read another float
                if (mat.color != null)
                    mat.color!!.diffuse = diffuse
                mat.color = AiMaterial.Color(diffuse = diffuse)
            }
            "\$clr.ambient" -> {
                val ambient = AiColor3D(this, be)
                if (mDataLength == AiColor4D.size) float() // read another float
                if (mat.color != null)
                    mat.color!!.ambient = ambient
                mat.color = AiMaterial.Color(ambient = ambient)
            }
            "\$clr.specular" -> {
                val specular = AiColor3D(this, be)
                if (mDataLength == AiColor4D.size) float() // read another float
                if (mat.color != null)
                    mat.color!!.specular = specular
                mat.color = AiMaterial.Color(specular = specular)
            }
            "\$clr.emissive" -> {
                val emissive = AiColor3D(this, be)
                if (mDataLength == AiColor4D.size) float() // read another float
                if (mat.color != null)
                    mat.color!!.emissive = emissive
                mat.color = AiMaterial.Color(emissive = emissive)
            }
            "\$clr.transparent" -> {
                val transparent = AiColor3D(this, be)
                if (mDataLength == AiColor4D.size) float() // read another float
                if (mat.color != null)
                    mat.color!!.transparent = transparent
                mat.color = AiMaterial.Color(transparent = transparent)
            }
            "\$clr.reflective" -> {
                val reflective = AiColor3D(this, be)
                if (mDataLength == AiColor4D.size) float() // read another float
                if (mat.color != null)
                    mat.color!!.reflective = reflective
                mat.color = AiMaterial.Color(reflective = reflective)
            }
            "?bg.global" -> TODO()

            "\$tex.file" -> {
                if (mIndex >= mat.textures.size)
                    mat.textures.add(AiMaterial.Texture())
                mat.textures[mIndex].file = string(mDataLength).replace(Regex("[^A-Za-z0-9 ]"), "")
            }
            "\$tex.uvwsrc" -> mat.textures[mIndex].uvwsrc = int(be)
            "\$tex.op" -> mat.textures[mIndex].op = AiTexture.Op.of(int(be))
            "\$tex.mapping" -> mat.textures[mIndex].mapping = AiTexture.Mapping.of(int(be))
            "\$tex.blend" -> mat.textures[mIndex].blend = float(be)
            "\$tex.mapmodeu" -> mat.textures[mIndex].mapModeU = AiTexture.MapMode.of(int(be))
            "\$tex.mapmodev" -> mat.textures[mIndex].mapModeV = AiTexture.MapMode.of(int(be))
            "\$tex.mapaxis" -> mat.textures[mIndex].mapAxis = AiVector3D(this, be)
            "\$tex.uvtrafo" -> TODO("vec2(this)")//mat.textures[mIndex].uvTrafo = AiUVTransform()
            "\$tex.flags" -> mat.textures[mIndex].flags = int(be)
        }
    }

    private fun InputStream.readTexture() {

        assert(int(be) == ASSBIN_CHUNK_AITEXTURE)
        int()   // size

        val tex = AiTexture()
        tex.mWidth = int(be)
        tex.mWidth = int(be)
        tex.achFormatHint = string(4)

        if (!shortened)
            tex.pcData =
                    if (tex.mHeight == 0)
                        ByteArray(tex.mWidth, { byte() })
                    else
                        ByteArray(tex.mWidth * tex.mHeight * 4, { byte() })

        // TODO
    }

    private fun InputStream.readAnimation(anim: AiAnimation) {

        assert(int(be) == ASSBIN_CHUNK_AIANIMATION)
        int(be)   // size

        anim.name = string()
        anim.duration = double(be)
        anim.ticksPerSecond = double(be)
        anim.numChannels = int(be)

        anim.channels = Array(anim.numChannels, { AiNodeAnim().also { readNodeAnim(it) } }).toCollection(ArrayList())
    }

    private fun InputStream.readNodeAnim(nd: AiNodeAnim) {

        assert(int(be) == ASSBIN_CHUNK_AINODEANIM)
        int(be)   // size

        nd.nodeName = string()
        nd.numPositionKeys = int(be)
        nd.numRotationKeys = int(be)
        nd.numScalingKeys = int(be)
        nd.mPreState = AiAnimBehaviour.of(int(be))
        nd.mPostState = AiAnimBehaviour.of(int(be))

        if (nd.numPositionKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->positionKeys, nd->numPositionKeys)
            else    // else write as usual
                nd.positionKeys = List(nd.numPositionKeys, { vectorKey() })

        if (nd.numRotationKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->rotationKeys, nd->numRotationKeys)
            else    // else write as usual
                nd.rotationKeys = List(nd.numRotationKeys, { quatKey() })

        if (nd.numScalingKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->scalingKeys, nd->numScalingKeys)
            else    // else write as usual
                nd.scalingKeys = List(nd.numScalingKeys, { vectorKey() })
    }

    private fun InputStream.readLight(l: AiLight) {

        assert(int(be) == ASSBIN_CHUNK_AILIGHT)
        int(be)   // size

        l.mName = string()
        l.mType = AiLightSourceType.of(int(be))

        if (l.mType != AiLightSourceType.DIRECTIONAL) {
            l.mAttenuationConstant = float(be)
            l.mAttenuationLinear = float(be)
            l.mAttenuationQuadratic = float(be)
        }

        l.mColorDiffuse = AiColor3D(this)
        l.mColorSpecular = AiColor3D(this)
        l.mColorAmbient = AiColor3D(this)

        if (l.mType == AiLightSourceType.SPOT) {
            l.mAngleInnerCone = float(be)
            l.mAngleOuterCone = float(be)
        }
    }

    private fun InputStream.readCamera(cam: AiCamera) {

        assert(int(be) == ASSBIN_CHUNK_AICAMERA)
        int(be)   // size

        cam.mName = string()
        cam.mPosition = AiVector3D(this, be)
        cam.mLookAt = AiVector3D(this, be)
        cam.mUp = AiVector3D(this, be)
        cam.mHorizontalFOV = float(be)
        cam.mClipPlaneNear = float(be)
        cam.mClipPlaneFar = float(be)
        cam.mAspect = float(be)
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


    private fun InputStream.string(length: Int = int(be)) = String(ByteArray(length, { byte() }))
    private fun InputStream.vertexWeight() = AiVertexWeight(int(be), float(be))
    private fun InputStream.vectorKey() = AiVectorKey(double(be), AiVector3D(this, be))
    private fun InputStream.quatKey() = AiQuatKey(double(be), AiQuaternion(this, be))
}