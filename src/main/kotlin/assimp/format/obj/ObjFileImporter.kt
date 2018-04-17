package assimp.format.obj

import assimp.*
import gli_.gli
import java.io.IOException

/**
 * Created by elect on 21/11/2016.
 */

const val ObjMinSize = 16

class ObjFileImporter : BaseImporter() {

    override val info = AiImporterDesc(
            name = "Wavefront Object Importer",
            comments = "surfaces not supported",
            flags = AiImporterFlags.SupportTextFlavour.i,
            fileExtensions = listOf("obj"))

    /**  Returns true, if file is an obj file.  */
    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {

        if (!checkSig)   //Check File Extension
            return file.substring(file.lastIndexOf('.') + 1) == "obj"
        else //Check file Header
            return false
    }

    //  reference to load textures later
    private lateinit var file: String

    /** Obj-file import implementation  */
    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        // Read file into memory
        this.file = file//File(file)
        if (!ioSystem.exists(file)) throw IOException("Failed to open file $file.")

        // Get the file-size and validate it, throwing an exception when fails
        //val fileSize = this.file.length()

        //if (fileSize < ObjMinSize) throw Error("OBJ-file is too small.")

        // parse the file into a temporary representation
        val parser = ObjFileParser(ioSystem.open(file), ioSystem)

        // And create the proper return structures out of it
        createDataFromImport(parser.m_pModel, scene, ioSystem)
    }

    /**  Create the data from parsed obj-file   */
    fun createDataFromImport(pModel: Model, pScene: AiScene, ioSystem: IOSystem) {

        // Create the root node of the scene
        pScene.rootNode = AiNode()
        if (pModel.m_ModelName.isNotEmpty())
        // Set the name of the scene
            pScene.rootNode.name = pModel.m_ModelName
        // This is a fatal error, so break down the application
        else throw Error("pModel.m_ModelName is empty")

        // Create nodes for the whole scene
        val meshArray = ArrayList<AiMesh>()
        for (index in 0 until pModel.m_Objects.size)
            createNodes(pModel, pModel.m_Objects[index], pScene.rootNode, pScene, meshArray)

        // Create mesh pointer buffer for this scene
        if (pScene.numMeshes > 0)
            pScene.meshes.addAll(meshArray)

        // Create all materials
        createMaterials(pModel, pScene)

        if (ASSIMP_LOAD_TEXTURES)
            loadTextures(pScene, ioSystem)
    }

    /**  Creates all nodes of the model */
    fun createNodes(pModel: Model, pObject: Object, pParent: AiNode, pScene: AiScene, meshArray: MutableList<AiMesh>): AiNode {

        // Store older mesh size to be able to computes mesh offsets for new mesh instances
        val oldMeshSize = meshArray.size
        val pNode = AiNode()

        pNode.name = pObject.m_strObjName

        // store the parent
        appendChildToParentNode(pParent, pNode)

        for (meshId in pObject.m_Meshes) {
            val pMesh = createTopology(pModel, pObject, meshId)
            if (pMesh != null && pMesh.numFaces > 0)
                meshArray.add(pMesh)
        }

        // Create all nodes from the sub-objects stored in the current object
        if (pObject.m_SubObjects.isNotEmpty()) {

            val numChilds = pObject.m_SubObjects.size
            pNode.numChildren = numChilds
            pNode.children = arrayListOf()
            pNode.numMeshes = 1
            pNode.meshes = IntArray(1)
        }

        // Set mesh instances into scene- and node-instances
        val meshSizeDiff = meshArray.size - oldMeshSize
        if (meshSizeDiff > 0) {
            pNode.meshes = IntArray(meshSizeDiff)
            pNode.numMeshes = meshSizeDiff
            var index = 0
            for (i in oldMeshSize until meshArray.size) {
                pNode.meshes[index] = pScene.numMeshes
                pScene.numMeshes++
                index++
            }
        }

        return pNode
    }

    /**  Appends this node to the parent node   */
    fun appendChildToParentNode(pParent: AiNode, pChild: AiNode) {

        // Assign parent to child
        pChild.parent = pParent

        pParent.numChildren++
        pParent.children.add(pChild)
    }

    /**  Create topology data   */
    fun createTopology(pModel: Model, pData: Object, meshIndex: Int): AiMesh? {

        // Create faces
        val pObjMesh = pModel.m_Meshes[meshIndex]

        if (pObjMesh.m_Faces.isEmpty()) return null

        val pMesh = AiMesh()
        if (pObjMesh.m_name.isNotEmpty()) pMesh.name = pObjMesh.m_name

        pObjMesh.m_Faces.forEach {
            when (it.m_PrimitiveType) {
                AiPrimitiveType.LINE -> {
                    pMesh.numFaces += it.m_vertices.size - 1
                    pMesh.primitiveTypes = pMesh.primitiveTypes or AiPrimitiveType.LINE
                }
                AiPrimitiveType.POINT -> {
                    pMesh.numFaces += it.m_vertices.size
                    pMesh.primitiveTypes = pMesh.primitiveTypes or AiPrimitiveType.POINT
                }
                else -> {
                    pMesh.numFaces++
                    pMesh.primitiveTypes = pMesh.primitiveTypes or
                            if (it.m_vertices.size > 3) AiPrimitiveType.POLYGON
                            else AiPrimitiveType.TRIANGLE
                }
            }
        }

        var uiIdxCount = 0
        if (pMesh.numFaces > 0) {

            val faces = ArrayList<AiFace>()

            if (pObjMesh.m_uiMaterialIndex != Mesh.NoMaterial)
                pMesh.materialIndex = pObjMesh.m_uiMaterialIndex

            var outIndex = 0

            // Copy all data from all stored meshes
            pObjMesh.m_Faces.forEach {
                val face = ArrayList<Int>()
                when (it.m_PrimitiveType) {
                    AiPrimitiveType.LINE -> for (i in 0 until it.m_vertices.size - 1) {
                        val mNumIndices = 2
                        uiIdxCount += mNumIndices
                        repeat(mNumIndices, { face.add(0) })
                    }
                    AiPrimitiveType.POINT -> for (i in 0 until it.m_vertices.size) {
                        val mNumIndices = 1
                        uiIdxCount += mNumIndices
                        repeat(mNumIndices, { face.add(0) })
                    }
                    else -> {
                        val uiNumIndices = it.m_vertices.size
                        uiIdxCount += uiNumIndices
                        repeat(uiNumIndices, { face.add(0) })
                    }
                }
                faces.add(face)
            }
            pMesh.faces = faces
        }

        // Create mesh vertices
        createVertexArray(pModel, pData, meshIndex, pMesh, uiIdxCount)

        return pMesh
    }

    /**  Creates a vertex array */
    fun createVertexArray(pModel: Model, pCurrentObject: Object, uiMeshIndex: Int, pMesh: AiMesh, numIndices: Int) {

        // Break, if no faces are stored in object
        if (pCurrentObject.m_Meshes.isEmpty()) return

        // Get current mesh
        val pObjMesh = pModel.m_Meshes[uiMeshIndex]
        if (pObjMesh.m_uiNumIndices < 1) return

        // Copy vertices of this mesh instance
        pMesh.numVertices = numIndices
        if (pMesh.numVertices == 0)
            throw Error("OBJ:" + pModel.m_ModelName + " | " + pMesh.name + " --> no vertices")
        else if (pMesh.numVertices > AI_MAX_ALLOC(AiVector3D.size))
            throw Error("OBJ:" + pModel.m_ModelName + " | " + pMesh.name + " --> Too many vertices, would run out of memory")

        pMesh.vertices = MutableList(pMesh.numVertices, { AiVector3D() })

        // Allocate buffer for normal vectors
        if (pModel.m_Normals.isNotEmpty() && pObjMesh.m_hasNormals)
            pMesh.normals = Array(pMesh.numVertices, { AiVector3D() }).toMutableList()

        // Allocate buffer for vertex-color vectors
        if (pModel.m_VertexColors.isNotEmpty())
            pMesh.colors.add(Array(pMesh.numVertices, { AiColor4D() }).toMutableList())
        //pMesh.colors[0] = Array(pMesh.numVertices, { AiColor4D() }).toMutableList()

        // Allocate buffer for texture coordinates
        if (pModel.m_TextureCoord.isNotEmpty() && pObjMesh.m_uiUVCoordinates[0] != 0)
            pMesh.textureCoords.add(MutableList(pMesh.numVertices, { floatArrayOf(0f, 0f) }))

        // Copy vertices, normals and textures into aiMesh instance
        var newIndex = 0
        var outIndex = 0

        pObjMesh.m_Faces.forEach { pSourceFace ->

            // Copy all index arrays
            var outVertexIndex = 0
            for (vertexIndex in 0 until pSourceFace.m_vertices.size) {

                val vertex = pSourceFace.m_vertices[vertexIndex]

                if (vertex >= pModel.m_Vertices.size) throw Error("OBJ:" + pModel.m_ModelName + " | " + pMesh.name + " --> vertex index out of range")

                pMesh.vertices[newIndex] put pModel.m_Vertices[vertex]

                // Copy all normals
                if (pModel.m_Normals.isNotEmpty() && vertexIndex in pSourceFace.m_normals.indices) {
                    val normal = pSourceFace.m_normals[vertexIndex]
                    if (normal >= pModel.m_Normals.size)
                        throw Error("OBJ:" + pModel.m_ModelName + " | " + pMesh.name + " --> vertex normal index out of range")
                    pMesh.normals[newIndex] put pModel.m_Normals[normal]
                }

                // Copy all vertex colors
                if (pModel.m_VertexColors.isNotEmpty())
                    pMesh.colors[0][newIndex] put pModel.m_VertexColors[vertex]

                // Copy all texture coordinates
                if (pModel.m_TextureCoord.isNotEmpty() && vertexIndex < pSourceFace.m_texturCoords.size) {

                    val tex = pSourceFace.m_texturCoords[vertexIndex]
                    assert(tex < pModel.m_TextureCoord.size)

                    if (tex >= pModel.m_TextureCoord.size) throw Error("OBJ:" + pModel.m_ModelName + " | " + pMesh.name + " --> texture coordinate index out of range")

                    val coord3d = pModel.m_TextureCoord[tex]
                    pMesh.textureCoords[0][newIndex] = floatArrayOf(coord3d[0], coord3d[1])
                }

                if (pMesh.numVertices <= newIndex)
                    throw Error("OBJ:" + pModel.m_ModelName + " | " + pMesh.name + " --> bad vertex index")

                // Get destination face
                val faceIndex = outIndex;
                val pDestFace = pMesh.faces.getOrElse(faceIndex, { mutableListOf() })

                val last = vertexIndex == pSourceFace.m_vertices.lastIndex

                if (pSourceFace.m_PrimitiveType != AiPrimitiveType.LINE || !last) {
                    for (i in pDestFace.size..outVertexIndex)   // TODO check
                        pDestFace += 0
                    pDestFace[outVertexIndex] = newIndex
                    outVertexIndex++
                }

                when (pSourceFace.m_PrimitiveType) {

                    AiPrimitiveType.POINT -> {
                        outIndex++
                        outVertexIndex = 0
                    }
                    AiPrimitiveType.LINE -> {
                        outVertexIndex = 0

                        if (!last) outIndex++

                        if (vertexIndex != 0) {
                            if (!last) {
                                pMesh.vertices[newIndex + 1] = pMesh.vertices[newIndex]
                                if (pSourceFace.m_normals.isNotEmpty() && pModel.m_Normals.isNotEmpty())
                                    pMesh.normals[newIndex + 1] = pMesh.normals[newIndex]

                                if (pModel.m_TextureCoord.isNotEmpty())
                                    for (i in 0 until pMesh.getNumUVChannels())
                                        pMesh.textureCoords[i][newIndex + 1] = pMesh.textureCoords[i][newIndex]
                                ++newIndex
                            }
                            pMesh.faces[faceIndex - 1][1] = newIndex
                        }
                    }
                    else -> if (last) outIndex++
                }
                ++newIndex
            }
        }
    }

    /**  Creates the material   */
    fun createMaterials(pModel: Model, pScene: AiScene) {

        val numMaterials = pModel.m_MaterialLib.size
        pScene.numMaterials = 0
        if (pModel.m_MaterialLib.isEmpty()) {
            logger.debug { "OBJ: no materials specified" }
            return
        }

        for (matIndex in 0 until numMaterials) {

            // Store material name
            val pCurrentMaterial = pModel.m_MaterialMap[pModel.m_MaterialLib[matIndex]]

            // No material found, use the default material
            pCurrentMaterial ?: continue

            val mat = AiMaterial()
            mat.name = pCurrentMaterial.materialName

            // convert illumination model
            mat.shadingModel = when (pCurrentMaterial.illumination_model) {
                0 -> AiShadingMode.noShading
                1 -> AiShadingMode.gouraud
                2 -> AiShadingMode.phong
                else -> {
                    logger.error { "OBJ: unexpected illumination model (0-2 recognized)" }
                    AiShadingMode.gouraud
                }
            }

            // Adding material colors
            mat.color = AiMaterial.Color()
            mat.color?.ambient = pCurrentMaterial.ambient
            mat.color?.diffuse = pCurrentMaterial.diffuse
            mat.color?.specular = pCurrentMaterial.specular
            mat.color?.emissive = pCurrentMaterial.emissive
            mat.shininess = pCurrentMaterial.shineness
            mat.opacity = pCurrentMaterial.alpha
            mat.color?.transparent = pCurrentMaterial.transparent

            // Adding refraction index
            mat.refracti = pCurrentMaterial.ior

            // Adding textures
            val uvwIndex = 0

            val map = mapOf(
                    Material.Texture.Type.diffuse to AiTexture.Type.diffuse,
                    Material.Texture.Type.ambient to AiTexture.Type.ambient,
                    Material.Texture.Type.emissive to AiTexture.Type.emissive,
                    Material.Texture.Type.specular to AiTexture.Type.specular,
                    Material.Texture.Type.bump to AiTexture.Type.height,
                    Material.Texture.Type.normal to AiTexture.Type.normals,
                    Material.Texture.Type.reflectionCubeBack to AiTexture.Type.reflection,
                    Material.Texture.Type.reflectionCubeBottom to AiTexture.Type.reflection,
                    Material.Texture.Type.reflectionCubeFront to AiTexture.Type.reflection,
                    Material.Texture.Type.reflectionCubeLeft to AiTexture.Type.reflection,
                    Material.Texture.Type.reflectionCubeRight to AiTexture.Type.reflection,
                    Material.Texture.Type.reflectionCubeTop to AiTexture.Type.reflection,
                    Material.Texture.Type.reflectionSphere to AiTexture.Type.reflection,
                    Material.Texture.Type.disp to AiTexture.Type.displacement,
                    Material.Texture.Type.opacity to AiTexture.Type.opacity,
                    Material.Texture.Type.specularity to AiTexture.Type.shininess)

            pCurrentMaterial.textures.forEach {
                mat.textures.add(
                        if (it.clamp)
                            AiMaterial.Texture(type = map[it.type], file = it.name, uvwsrc = uvwIndex,
                                    mapModeU = AiTexture.MapMode.clamp, mapModeV = AiTexture.MapMode.clamp)
                        else
                            AiMaterial.Texture(type = map[it.type], file = it.name, uvwsrc = uvwIndex))
            }

            // Store material property info in material array in scene
            pScene.materials.add(mat)
            pScene.numMaterials++
        }

        // Test number of created materials.
        assert(pScene.numMaterials == numMaterials)
    }

    /**  Load textures   */
    fun loadTextures(scene: AiScene, ioSystem: IOSystem = this.ioSystem) {

        scene.materials.forEach { mtl ->

            mtl.textures.forEach { tex ->

                val name = tex.file!!

                if (!scene.textures.containsKey(name)) {

                    var i = 0
                    while (!name[i].isLetter()) i++
                    val cleaned = name.substring(i) //  e.g: .\wal67ar_small.jpg -> wal67ar_small.jpg

                    if(ioSystem is DefaultIOSystem) {

                        //If the default io system is in place, we can use the java.io.File api and list directories
                        //to match files even where case is mangled

                        val actualFile = (ioSystem.open(file) as DefaultIOSystem.FileIOStream).path.toFile()

                        when {
                            actualFile.parentFile.listFiles().any { it.name == cleaned } -> {

                                val texFile = actualFile.parentFile.listFiles().first { it.name == cleaned }!!
                                scene.textures[name] = gli.load(texFile.toPath())

                            }
                            actualFile.parentFile.listFiles().any { it.name.toUpperCase() == cleaned.toUpperCase() } -> {
                                // try case insensitive
                                val texFile = actualFile.parentFile.listFiles().first { it.name.toUpperCase() == cleaned.toUpperCase() }!!
                                scene.textures[name] = gli.load(texFile.toPath())

                            }
                            else -> logger.warn { "OBJ/MTL: Texture image not found --> $cleaned" }
                        }
                    } else {
                        //no such luck with custom io systems i'm afraid
                        //TODO gli load from bytebuf ?
                    }

                } else {
                    logger.warn { "OBJ/MTL: Scene contains  --> $name already" }
                }
            }
        }
    }
}
































