package assimp.format.assbin

import assimp.*
import assimp.format.X.reserve
import gli_.has
import gli_.hasnt
import glm_.*
import java.io.InputStream

class AssbinLoader : BaseImporter() {

    override val info = AiImporterDesc(
                name = ".assbin Importer",
                comments = "Gargaj / Conspiracy",
                flags = AiImporterFlags.SupportBinaryFlavour or AiImporterFlags.SupportCompressedFlavour,
                fileExtensions = listOf("assbin"))

    var shortened = false
    var compressed = false
    private val be = false // big endian TODO glm global?

    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean) =
            ioSystem.open(file).read().use { i -> "ASSIMP.binary-dump.".all { it.i == i.read() } }

    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        ioSystem.open(file).read().use {

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
//            ReadBinaryScene(& io, scene)
//
//            delete[] uncompressedData
//            delete[] compressedData

            } else it.readScene(scene)

        }
    }


    fun InputStream.readScene(scene: AiScene) {

        assert(int(be) == ASSBIN_CHUNK_AISCENE)
        int(be)    // size

        scene.flags = int(be)
        scene.numMeshes = int(be)
        scene.numMaterials = int(be)
        scene.numAnimations = int(be)
        scene.numTextures = int(be)
        scene.numLights = int(be)
        scene.numCameras = int(be)

        // Read node graph
        scene.rootNode = AiNode()
        readNode(scene.rootNode)

        // Read all meshes
        for (i in 0 until scene.numMeshes)
            scene.meshes.add(AiMesh().also { readMesh(it) })

        // Read materials
        for (i in 0 until scene.numMaterials)
            scene.materials.add(AiMaterial().also { readMaterial(it) })

        // Read all animations
        for (i in 0 until scene.numAnimations)
            scene.animations.add(AiAnimation().also { readAnimation(it) })

        // Read all textures
        for (i in 0 until scene.numTextures)
            readTexture()
//            scene.textures["$i"] = gli.Texture(AiAnimation().also { readAnimation(it) })
//        if (scene.numTextures > 0) {
//            scene.textures = new aiTexture *[scene->numTextures]
//            for (unsigned int i = 0; i < scene->numTextures;++i) {
//                scene.textures[i] = new aiTexture ()
//                ReadBinaryTexture(stream, scene->textures[i])
//            }
//        }

        // Read lights
        for (i in 0 until scene.numLights)
            scene.lights = Array(scene.numLights, { AiLight().also { readLight(it) } }).toCollection(ArrayList())

        // Read cameras
        for (i in 0 until scene.numCameras)
            scene.cameras = Array(scene.numCameras, { AiCamera().also { readCamera(it) } }).toCollection(ArrayList())
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

        mesh.primitiveTypes = int(be)
        mesh.numVertices = int(be)
        mesh.numFaces = int(be)
        mesh.numBones = int(be)
        mesh.materialIndex = int(be)

        // first of all, write bits for all existent vertex components
        val c = int(be)

        if (c has ASSBIN_MESH_HAS_POSITIONS)
            if (shortened)
                TODO()//ReadBounds(stream, mesh->vertices, mesh->numVertices)
            else    // else write as usual
                mesh.vertices = MutableList(mesh.numVertices, { AiVector3D(this, be) })

        if (c has ASSBIN_MESH_HAS_NORMALS)
            if (shortened)
                TODO()//ReadBounds(stream, mesh->normals, mesh->numVertices)
            else    // else write as usual
                mesh.normals = MutableList(mesh.numVertices, { AiVector3D(this, be) })

        if (c has ASSBIN_MESH_HAS_TANGENTS_AND_BITANGENTS) {
            if (shortened) {
                TODO()//ReadBounds(stream, mesh->tangents, mesh->numVertices)
                //ReadBounds(stream, mesh->bitangents, mesh->numVertices)
            } else {   // else write as usual
                mesh.tangents = MutableList(mesh.numVertices, { AiVector3D(this, be) })
                mesh.bitangents = MutableList(mesh.numVertices, { AiVector3D(this, be) })
            }
        }
        for (n in 0 until AI_MAX_NUMBER_OF_COLOR_SETS) {
            if (c hasnt ASSBIN_MESH_HAS_COLOR(n))
                break

            if (shortened)
                TODO()//ReadBounds(stream, mesh->colors[n], mesh->numVertices)
            else    // else write as usual
                mesh.colors.add(MutableList(mesh.numVertices, { AiColor4D(this) }))
        }
        for (n in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
            if (c hasnt ASSBIN_MESH_HAS_TEXCOORD(n))
                break

            // write number of UV components
            val mNumUVComponents = int(be)

            if (shortened)
                TODO()//ReadBounds(stream, mesh->textureCoords[n], mesh->numVertices)
            else    // else write as usual
                mesh.textureCoords.add(MutableList(mesh.numVertices, {
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
            mesh.faces = MutableList(mesh.numFaces, {
                assert(AI_MAX_FACE_INDICES <= 0xffff, { "AI_MAX_FACE_INDICES <= 0xffff" })
                val mNumIndices = short(be)
                MutableList(mNumIndices.i, { if (mesh.numVertices < (1 shl 16)) short(be) else int(be) })
            })

        // write bones
        for (i in 0 until mesh.numBones)
            mesh.bones.add(AiBone().also { readBone(it) })
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
            b.weights = MutableList(b.numWeights, { vertexWeight() })
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
        tex.width = int(be)
        tex.width = int(be)
        tex.achFormatHint = string(4)

        if (!shortened)
            tex.pcData =
                    if (tex.height == 0)
                        ByteArray(tex.width, { byte() })
                    else
                        ByteArray(tex.width * tex.height * 4, { byte() })

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
        nd.preState = AiAnimBehaviour.of(int(be))
        nd.postState = AiAnimBehaviour.of(int(be))

        if (nd.numPositionKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->positionKeys, nd->numPositionKeys)
            else    // else write as usual
                nd.positionKeys.apply {
                    clear()
                    reserve(nd.numPositionKeys) { vectorKey() }
                }

        if (nd.numRotationKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->rotationKeys, nd->numRotationKeys)
            else    // else write as usual
                nd.rotationKeys.apply {
                    clear()
                    reserve(nd.numRotationKeys) { quatKey() }
                }

        if (nd.numScalingKeys > 0)
            if (shortened)
                TODO()//ReadBounds(stream, nd->scalingKeys, nd->numScalingKeys)
            else    // else write as usual
                nd.scalingKeys.apply {
                    clear()
                    reserve(nd.numScalingKeys) { vectorKey() }
                }
    }

    private fun InputStream.readLight(l: AiLight) {

        assert(int(be) == ASSBIN_CHUNK_AILIGHT)
        int(be)   // size

        l.name = string()
        l.type = AiLightSourceType.of(int(be))

        if (l.type != AiLightSourceType.DIRECTIONAL) {
            l.attenuationConstant = float(be)
            l.attenuationLinear = float(be)
            l.attenuationQuadratic = float(be)
        }

        l.colorDiffuse = AiColor3D(this)
        l.colorSpecular = AiColor3D(this)
        l.colorAmbient = AiColor3D(this)

        if (l.type == AiLightSourceType.SPOT) {
            l.angleInnerCone = float(be)
            l.angleOuterCone = float(be)
        }
    }

    private fun InputStream.readCamera(cam: AiCamera) {

        assert(int(be) == ASSBIN_CHUNK_AICAMERA)
        int(be)   // size

        cam.name = string()
        cam.position = AiVector3D(this, be)
        cam.lookAt = AiVector3D(this, be)
        cam.up = AiVector3D(this, be)
        cam.horizontalFOV = float(be)
        cam.clipPlaneNear = float(be)
        cam.clipPlaneFar = float(be)
        cam.aspect = float(be)
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