package assimp.format.ply

import glm_.*
import glm_.vec3.Vec3
import assimp.*
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Created by elect on 10/12/2016.
 */

class PlyLoader : BaseImporter() {

    override val info = AiImporterDesc(
                name = "Stanford Polygon Library (PLY) Importer",
                flags = AiImporterFlags.SupportBinaryFlavour or AiImporterFlags.SupportTextFlavour,
                fileExtensions = listOf("ply"))

    /** Document object model representation extracted from the file */
    lateinit var pcDom: DOM

    // ------------------------------------------------------------------------------------------------
    // Returns whether the class can handle the format of the given file.
    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {

        val extension = file.substring(file.lastIndexOf('.') + 1)

        if (extension == "ply")
            return true

        return false
    }

    // ------------------------------------------------------------------------------------------------
    // Imports the given file into the given scene structure.
    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        val file = File(file)

        // Check whether we can read from the file
        if (!file.canRead()) throw IOException("Failed to open PLY file $file.")

        // allocate storage and copy the contents of the file to a memory buffer
        val fileChannel = RandomAccessFile(file, "r").channel
        val mBuffer2 = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        // the beginning of the file must be PLY - magic, magic
        if (mBuffer2.nextWord().toLowerCase() != "ply") throw Error("Invalid .ply file: Magic number 'ply' is no there")

        mBuffer2.skipSpacesAndLineEnd()

        // determine the format of the file data
        val sPlyDom = DOM()
        if (mBuffer2.nextWord() == "format") {

            val format = mBuffer2.nextWord()
            if (format == "ascii") {
                mBuffer2.skipLine()
                if (!DOM.parseInstance(mBuffer2, sPlyDom))
                    throw Error("Invalid .ply file: Unable to build DOM (#1)")
            } else {
                // revert ascii
                mBuffer2.position(mBuffer2.position() - format.length)

                if (mBuffer2.startsWith("binary_")) {

                    val bIsBE = mBuffer2.get(mBuffer2.position()) == 'b'.b || mBuffer2.get(mBuffer2.position()) == 'B'.b
                    mBuffer2.order(if (bIsBE) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

                    // skip the line, parse the rest of the header and build the DOM
                    mBuffer2.skipLine()
                    if (!DOM.parseInstanceBinary(mBuffer2, sPlyDom))
                        throw Error("Invalid .ply file: Unable to build DOM (#2)")

                } else throw Error("Invalid .ply file: Unknown file format")
            }
        } else throw Error("Invalid .ply file: Missing format specification")

        pcDom = sPlyDom

        // now load a list of vertices. This must be successfully in order to procedure
        val avPositions = ArrayList<AiVector3D>()
        loadVertices(avPositions, false)

        // now load a list of normals.
        val avNormals = ArrayList<AiVector3D>()
        loadVertices(avNormals, true)

        // load the face list
        val avFaces = ArrayList<Face>()
        loadFaces(avFaces)

        // if no face list is existing we assume that the vertex list is containing a list of triangles
        if (avFaces.isEmpty()) {

            if (avPositions.size < 3)
                throw Error("Invalid .ply file: Not enough vertices to build a proper face list.")

            val iNum = avPositions.size / 3
            repeat(iNum) {
                val sFace = Face()
                sFace.mIndices[0] = it * 3
                sFace.mIndices[1] = it * 3 + 1
                sFace.mIndices[2] = it * 3 + 2
                avFaces.add(sFace)
            }
        }

        // now load a list of all materials
        val avMaterials = ArrayList<AiMaterial>()
        loadMaterial(avMaterials)

        // now load a list of all vertex color channels
        val avColors = ArrayList<AiColor4D>()
        loadVertexColor(avColors)

        // now try to load texture coordinates
        val avTexCoords = ArrayList<AiVector2D>()
        loadTextureCoordinates(avTexCoords)

        // now replace the default material in all faces and validate all material indices
        replaceDefaultMaterial(avFaces, avMaterials)

        // now convert this to a list of aiMesh instances
        val avMeshes = ArrayList<AiMesh>()
        convertMeshes(avFaces, avPositions, avNormals, avColors, avTexCoords, avMaterials, avMeshes)

        if (avMeshes.isEmpty()) throw Error("Invalid .ply file: Unable to extract mesh data ")

        // now generate the output scene object. Fill the material list
        scene.numMaterials = avMaterials.size
        scene.materials.addAll(avMaterials)

        // fill the mesh list
        scene.numMeshes = avMeshes.size
        scene.meshes.addAll(avMeshes)

        // generate a simple node structure
        scene.rootNode = AiNode()
        scene.rootNode.numMeshes = scene.numMeshes
        scene.rootNode.meshes = IntArray(scene.rootNode.numMeshes, { it })
    }

    /** Try to extract vertices from the PLY DOM.     */
    private fun loadVertices(pvOut: ArrayList<AiVector3D>, p_bNormals: Boolean) {

        val aiPositions = intArrayOf(0xFFFFFFFF.i, 0xFFFFFFFF.i, 0xFFFFFFFF.i)
        val aiTypes = arrayListOf(EDataType.Char, EDataType.Char, EDataType.Char)
        var pcList: ElementInstanceList? = null
        var cnt = 0

        // search in the DOM for a vertex entry
        for (element in pcDom.alElements) {

            val i = pcDom.alElements.indexOf(element)

            if (element.eSemantic == EElementSemantic.Vertex) {

                pcList = pcDom.alElementData[i]

                // load normal vectors?
                if (p_bNormals)
                // now check whether which normal components are available
                    for (property in element.alProperties) {

                        if (property.bIsList) continue

                        val a = element.alProperties.indexOf(property)

                        if (property.semantic == ESemantic.XNormal) {
                            cnt++
                            aiPositions[0] = a
                            aiTypes[0] = property.eType
                        }
                        if (property.semantic == ESemantic.YNormal) {
                            cnt++
                            aiPositions[1] = a
                            aiTypes[1] = property.eType
                        }
                        if (property.semantic == ESemantic.ZNormal) {
                            cnt++
                            aiPositions[2] = a
                            aiTypes[2] = property.eType
                        }
                    }
                // load vertex coordinates
                else
                // now check whether which coordinate sets are available
                    for (property in element.alProperties) {

                        if (property.bIsList) continue

                        val a = element.alProperties.indexOf(property)

                        if (property.semantic == ESemantic.XCoord) {
                            cnt++
                            aiPositions[0] = a
                            aiTypes[0] = property.eType
                        }
                        if (property.semantic == ESemantic.YCoord) {
                            cnt++
                            aiPositions[1] = a
                            aiTypes[1] = property.eType
                        }
                        if (property.semantic == ESemantic.ZCoord) {
                            cnt++
                            aiPositions[2] = a
                            aiTypes[2] = property.eType
                        }
                        if (cnt == 3) break
                    }

                break
            }
        }
        // check whether we have a valid source for the vertex data
        if (pcList != null && cnt != 0)

            for (instance in pcList.alInstances) {

                // convert the vertices to sp floats
                val vOut = AiVector3D()

                if (aiPositions[0] != 0xFFFFFFFF.i)
                    vOut.x = instance.alProperties[aiPositions[0]].avList[0].f

                if (aiPositions[1] != 0xFFFFFFFF.i)
                    vOut.y = instance.alProperties[aiPositions[1]].avList[0].f

                if (aiPositions[2] != 0xFFFFFFFF.i)
                    vOut.z = instance.alProperties[aiPositions[2]].avList[0].f

                // and add them to our nice list
                pvOut.add(vOut)
            }
    }

    /** Try to extract proper faces from the PLY DOM.     */
    fun loadFaces(pvOut: ArrayList<Face>) {

        var pcList: ElementInstanceList? = null
        var bOne = false

        // index of the vertex index list
        var iProperty = 0xFFFFFFFF.i
        var eType = EDataType.Char
        var bIsTriStrip = false

        // index of the material index property
        var iMaterialIndex = 0xFFFFFFFF.i
        var eType2 = EDataType.Char

        // search in the DOM for a face entry
        for (element in pcDom.alElements) {

            val i = pcDom.alElements.indexOf(element)
            // face = unique number of vertex indices
            if (element.eSemantic == EElementSemantic.Face) {

                pcList = pcDom.alElementData[i]
                for (property in element.alProperties) {

                    val a = element.alProperties.indexOf(property)

                    if (property.semantic == ESemantic.VertexIndex) {
                        // must be a dynamic list!
                        if (!property.bIsList) continue
                        iProperty = a
                        bOne = true
                        eType = property.eType

                    } else if (property.semantic == ESemantic.MaterialIndex) {
                        if (property.bIsList) continue
                        iMaterialIndex = a
                        bOne = true
                        eType = property.eType
                    }
                }
                break
            }
            // triangle strip
            // TODO: triangle strip and material index support???
            else if (element.eSemantic == EElementSemantic.TriStrip) {
                // find a list property in this ...
                pcList = pcDom.alElementData[i]
                for (property in element.alProperties) {

                    val a = element.alProperties.indexOf(property)
                    // must be a dynamic list!
                    if (!property.bIsList) continue
                    iProperty = a
                    bOne = true
                    bIsTriStrip = true
                    eType = property.eType
                    break
                }
                break
            }
        }
        // check whether we have at least one per-face information set
        if (pcList != null && bOne)

            if (!bIsTriStrip) {

                for (instance in pcList.alInstances) {

                    val sFace = Face()

                    // parse the list of vertex indices
                    if (iProperty != 0xFFFFFFFF.i) {

                        val iNum = instance.alProperties[iProperty].avList.size
                        sFace.mIndices = IntArray(iNum, { 0 })

                        val p = instance.alProperties[iProperty].avList

                        repeat(iNum) {
                            sFace.mIndices[it] = p[it].ui.v
                        }
                    }
                    // parse the material index
                    if (iMaterialIndex != 0xFFFFFFFF.i)
                        sFace.iMaterialIndex = instance.alProperties[iMaterialIndex].avList[0].ui.v

                    pvOut.add(sFace)
                }
            }
            // triangle strips
            else {
                // normally we have only one triangle strip instance where a value of -1 indicates a restart of the strip
                var flip = false
                for (instance in pcList.alInstances) {
                    val quak = instance.alProperties[iProperty].avList

                    val aiTable = intArrayOf(-1, -1)
                    for (number in quak) {
                        val p = number.i

                        if (p == -1) {
                            // restart the strip ...
                            aiTable[0] = -1
                            aiTable[1] = -1
                            flip = false
                            continue
                        }
                        if (aiTable[0] == -1) {
                            aiTable[0] = p
                            continue
                        }
                        if (aiTable[1] == -1) {
                            aiTable[1] = p
                            continue
                        }
                        pvOut.add(Face())
                        val sFace = pvOut.last()
                        sFace.mIndices[0] = aiTable[0]
                        sFace.mIndices[1] = aiTable[1]
                        sFace.mIndices[2] = p
                        flip = !flip
                        if (flip) {
                            val t = sFace.mIndices[0]
                            sFace.mIndices[0] = sFace.mIndices[1]
                            sFace.mIndices[1] = t
                        }
                        aiTable[0] = aiTable[1]
                        aiTable[1] = p
                    }
                }
            }
    }

    /** Extract a material from the PLY DOM */
    fun loadMaterial(pvOut: ArrayList<AiMaterial>) {

        // diffuse[4], specular[4], ambient[4]
        // rgba order
        val aaiPositions = Array(3, { IntArray(4, { 0xFFFFFFFF.i }) })
        val aaiTypes = Array(3, { Array(4, { EDataType.Char }) })
        var pcList: ElementInstanceList? = null

        var iPhong = 0xFFFFFFFF.i
        var ePhong = EDataType.Char

        var iOpacity = 0xFFFFFFFF.i
        var eOpacity = EDataType.Char

        // search in the DOM for a vertex entry
        for (element in pcDom.alElements) {

            val i = pcDom.alElements.indexOf(element)

            if (element.eSemantic == EElementSemantic.Material) {

                pcList = pcDom.alElementData[i]

                // now check whether which coordinate sets are available
                for (property in element.alProperties) {

                    if (property.bIsList) continue

                    val a = element.alProperties.indexOf(property)

                    when (property.semantic) {

                    // pohng specularity      -----------------------------------
                        ESemantic.PhongPower -> {
                            iPhong = a
                            ePhong = property.eType
                        }

                    // general opacity        -----------------------------------
                        ESemantic.Opacity -> {
                            iOpacity = a
                            eOpacity = property.eType
                        }

                    // diffuse color channels -----------------------------------
                        ESemantic.DiffuseRed -> {
                            aaiPositions[0][0] = a
                            aaiTypes[0][0] = property.eType
                        }
                        ESemantic.DiffuseGreen -> {
                            aaiPositions[0][1] = a
                            aaiTypes[0][1] = property.eType
                        }
                        ESemantic.DiffuseBlue -> {
                            aaiPositions[0][2] = a
                            aaiTypes[0][2] = property.eType
                        }
                        ESemantic.DiffuseAlpha -> {
                            aaiPositions[0][3] = a
                            aaiTypes[0][3] = property.eType
                        }
                    // specular color channels -----------------------------------
                        ESemantic.SpecularRed -> {
                            aaiPositions[1][0] = a
                            aaiTypes[1][0] = property.eType
                        }
                        ESemantic.SpecularGreen -> {
                            aaiPositions[1][1] = a
                            aaiTypes[1][1] = property.eType
                        }
                        ESemantic.SpecularBlue -> {
                            aaiPositions[1][2] = a
                            aaiTypes[1][2] = property.eType
                        }
                        ESemantic.SpecularAlpha -> {
                            aaiPositions[1][3] = a
                            aaiTypes[1][3] = property.eType
                        }
                    // ambient color channels -----------------------------------
                        ESemantic.AmbientRed -> {
                            aaiPositions[2][0] = a
                            aaiTypes[2][0] = property.eType
                        }
                        ESemantic.AmbientGreen -> {
                            aaiPositions[2][1] = a
                            aaiTypes[2][1] = property.eType
                        }
                        ESemantic.AmbientBlue -> {
                            aaiPositions[2][2] = a
                            aaiTypes[2][2] = property.eType
                        }
                        ESemantic.AmbientAlpha -> {
                            aaiPositions[2][3] = a
                            aaiTypes[2][3] = property.eType
                        }
                    }
                    break
                }
            }
        }

        // check whether we have a valid source for the material data
        if (pcList != null)

            for (elementInstance in pcList.alInstances) {

                val clrOut = AiColor4D()
                val material = AiMaterial(color = AiMaterial.Color())

                // build the diffuse material color
                getMaterialColor(elementInstance.alProperties, aaiPositions[0], aaiTypes[0], clrOut)
                material.color!!.diffuse = Vec3(clrOut)

                // build the specular material color
                getMaterialColor(elementInstance.alProperties, aaiPositions[1], aaiTypes[1], clrOut)
                material.color!!.specular = Vec3(clrOut)

                // build the ambient material color
                getMaterialColor(elementInstance.alProperties, aaiPositions[1], aaiTypes[1], clrOut)
                material.color!!.ambient = Vec3(clrOut)

                // handle phong power and shading mode
                var iMode = AiShadingMode.gouraud
                if (iPhong != 0xFFFFFFFF.i) {
                    var fSpec = elementInstance.alProperties[iPhong].avList[0].f

                    // if shininess is 0 (and the pow() calculation would therefore always become 1, not depending on the angle), use gouraud lighting
                    if (fSpec != 0f) {
                        // scale this with 15 ... hopefully this is correct
                        fSpec *= 15
                        material.shininess = fSpec

                        iMode = AiShadingMode.phong
                    }
                }
                material.shadingModel = iMode

                // handle opacity
                if (iOpacity != 0xFFFFFFFF.i) {
                    val fOpacity = elementInstance.alProperties[iPhong].avList[0].f
                    material.opacity = fOpacity
                }

                // The face order is absolutely undefined for PLY, so we have to use two-sided rendering to be sure it's ok.
                material.twoSided = true

                // add the newly created material instance to the list
                pvOut.add(material)
            }
    }

    /** Get a RGBA color in [0...1] range   */
    fun getMaterialColor(avList: ArrayList<PropertyInstance>, aiPosition: IntArray, aiTypes: Array<EDataType>, clrOut: AiColor4D) {

        clrOut.r = if (aiPosition[0] == 0xFFFFFFFF.i) 0f
        else normalizeColorValue(avList[aiPosition[0]].avList[0], aiTypes[0])

        clrOut.g = if (aiPosition[1] == 0xFFFFFFFF.i) 0f
        else normalizeColorValue(avList[aiPosition[1]].avList[0], aiTypes[1])

        clrOut.b = if (aiPosition[2] == 0xFFFFFFFF.i) 0f
        else normalizeColorValue(avList[aiPosition[2]].avList[0], aiTypes[2])
        // assume 1.0 for the alpha channel ifit is not set
        clrOut.a = if (aiPosition[3] == 0xFFFFFFFF.i) 1f
        else normalizeColorValue(avList[aiPosition[3]].avList[0], aiTypes[3])
    }

    /** Convert a color component to [0...1]    */
    fun normalizeColorValue(value: Number, eType: EDataType) = when (eType) {

        EDataType.Float, EDataType.Double -> value.f
        EDataType.UChar -> value.ub.f
        EDataType.Char -> value.f
        EDataType.UShort -> value.us.f
        EDataType.Short -> value.f
        EDataType.UInt -> value.ui.f
        EDataType.Int -> value.f
        else -> 0f
    }

    /** Try to extract proper vertex colors from the PLY DOM    */
    fun loadVertexColor(pvOut: ArrayList<AiColor4D>) {

        val aiPositions = IntArray(4, { 0xFFFFFFFF.i })
        val aiTypes = Array(4, { EDataType.Char })
        var cnt = 0
        var pcList: ElementInstanceList? = null

        // search in the DOM for a vertex entry
        for (element in pcDom.alElements) {

            val i = pcDom.alElements.indexOf(element)

            if (element.eSemantic == EElementSemantic.Vertex) {

                pcList = pcDom.alElementData[i]

                // now check whether which coordinate sets are available
                for (property in element.alProperties) {

                    if (property.bIsList) continue

                    val a = element.alProperties.indexOf(property)

                    if (property.semantic == ESemantic.Red) {
                        cnt++
                        aiPositions[0] = a
                        aiTypes[0] = property.eType
                    }
                    if (property.semantic == ESemantic.Green) {
                        cnt++
                        aiPositions[1] = a
                        aiTypes[1] = property.eType
                    }
                    if (property.semantic == ESemantic.Blue) {
                        cnt++
                        aiPositions[2] = a
                        aiTypes[2] = property.eType
                    }
                    if (property.semantic == ESemantic.Alpha) {
                        cnt++
                        aiPositions[3] = a
                        aiTypes[3] = property.eType
                    }
                    if (cnt == 4) break
                }
                break
            }
        }
        // check whether we have a valid source for the vertex data
        if (pcList != null && cnt != 0)

            for (elementInstance in pcList.alInstances) {

                // convert the vertices to sp floats
                val vOut = AiColor4D()

                if (aiPositions[0] != 0xFFFFFFFF.i)
                    vOut.r = normalizeColorValue(elementInstance.alProperties[aiPositions[0]].avList[0], aiTypes[0])

                if (aiPositions[1] != 0xFFFFFFFF.i)
                    vOut.g = normalizeColorValue(elementInstance.alProperties[aiPositions[1]].avList[0], aiTypes[1])

                if (aiPositions[2] != 0xFFFFFFFF.i)
                    vOut.b = normalizeColorValue(elementInstance.alProperties[aiPositions[2]].avList[0], aiTypes[2])

                if (aiPositions[3] != 0xFFFFFFFF.i)
                    vOut.a = 1f
                else
                    vOut.a = normalizeColorValue(elementInstance.alProperties[aiPositions[3]].avList[0], aiTypes[3])

                // and add them to our nice list
                pvOut.add(vOut)
            }
    }

    fun loadTextureCoordinates(pvOut: ArrayList<AiVector2D>) {

        val aiPosition = IntArray(2, { 0xFFFFFFFF.i })
        val aiTypes = Array(2, { EDataType.Char })
        var pcList: ElementInstanceList? = null
        var cnt = 0

        // search in the DOM for a vertex entry
        for (element in pcDom.alElements) {

            val i = pcDom.alElements.indexOf(element)

            if (element.eSemantic == EElementSemantic.Vertex) {

                pcList = pcDom.alElementData[i]

                // now check whether which normal components are available
                for (property in element.alProperties) {

                    if (property.bIsList) continue

                    val a = element.alProperties.indexOf(property)

                    if (property.semantic == ESemantic.UTextureCoord) {
                        cnt++
                        aiPosition[0] = a
                        aiTypes[0] = property.eType
                    } else if (property.semantic == ESemantic.VTextureCoord) {
                        cnt++
                        aiPosition[1] = a
                        aiTypes[1] = property.eType
                    }
                }
            }
        }
        // check whether we have a valid source for the texture coordinates data
        if (pcList != null && cnt != 0)

            for (elementInstance in pcList.alInstances) {

                // convert the vertices to sp floats
                val vOut = AiVector2D()

                if (aiPosition[0] != 0xFFFFFFFF.i)
                    vOut.x = elementInstance.alProperties[aiPosition[0]].avList[0].f

                if (aiPosition[1] != 0xFFFFFFFF.i)
                    vOut.y = elementInstance.alProperties[aiPosition[1]].avList[0].f

                // and add them to our nice list
                pvOut.add(vOut)
            }
    }

    /** Generate a default material if none was specified and apply it to all vanilla faces */
    fun replaceDefaultMaterial(avFaces: ArrayList<Face>, avMaterials: ArrayList<AiMaterial>) {

        var bNeedDefaultMat = false

        avFaces.forEach {
            if (it.iMaterialIndex == 0xFFFFFFFF.i) {
                bNeedDefaultMat = true
                it.iMaterialIndex = avMaterials.size

            } else if (it.iMaterialIndex >= avMaterials.size)
            // clamp the index
                it.iMaterialIndex = avMaterials.size - 1
        }

        if (bNeedDefaultMat)
        // generate a default material
            avMaterials.add(AiMaterial(
                    // fill in a default material
                    shadingModel = AiShadingMode.gouraud,
                    color = AiMaterial.Color(
                            diffuse = AiColor3D(.6f),
                            specular = AiColor3D(.6f),
                            ambient = AiColor3D(.05f)),
                    // The face order is absolutely undefined for PLY, so we have to use two-sided rendering to be sure it's ok.
                    twoSided = true))
    }

    /** Split meshes by material IDs    */
    fun convertMeshes(avFaces: ArrayList<Face>, avPositions: ArrayList<AiVector3D>, avNormals: ArrayList<AiVector3D>, avColors: ArrayList<AiColor4D>,
                      avTexCoords: ArrayList<AiVector2D>, avMaterials: ArrayList<AiMaterial>, avOut: ArrayList<AiMesh>) {

        // split by materials
        val aiSplit = Array(avMaterials.size, { ArrayList<Int>() })

        var iNum = 0
        avFaces.forEach {
            aiSplit[it.iMaterialIndex].add(iNum)
            iNum++
        }

        // now generate sub-meshes
        for (p in 0 until avMaterials.size)

            if (aiSplit[p].isNotEmpty()) {

                // allocate the mesh object
                val p_pcOut = AiMesh()
                p_pcOut.materialIndex = p

                p_pcOut.numFaces = aiSplit[p].size
                p_pcOut.faces = MutableList(aiSplit[p].size, { ArrayList<Int>() })

                // at first we need to determine the size of the output vector array
                iNum = (0 until aiSplit[p].size).sumBy { avFaces[aiSplit[p][it]].mIndices.size }

                p_pcOut.numVertices = iNum
                if (iNum == 0)   // nothing to do
                    return  // cleanup

                p_pcOut.vertices = MutableList(iNum, { AiVector3D() })

                if (avColors.isNotEmpty())
                    p_pcOut.colors[0] = ArrayList()
                if (avTexCoords.isNotEmpty())
                    p_pcOut.textureCoords = mutableListOf(MutableList(iNum, { floatArrayOf(0f, 0f) }))
                if (avNormals.isNotEmpty())
                    p_pcOut.normals = MutableList(iNum, { AiVector3D() })

                // add all faces
                iNum = 0
                var iVertex = 0
                for (i in 0 until aiSplit[p].size) {

                    p_pcOut.faces[iNum] = MutableList(avFaces[i].mIndices.size, { 0 })

                    // build an unique set of vertices/colors for this face
                    for (q in 0 until p_pcOut.faces[iNum].size) {

                        p_pcOut.faces[iNum][q] = iVertex
                        val idx = avFaces[i].mIndices[q]
                        if (idx >= avPositions.size)
                        // out of border
                            continue

                        p_pcOut.vertices[iVertex] put avPositions[idx]

                        if (avColors.isNotEmpty())
                            p_pcOut.colors[0][iVertex] put avColors[idx]

                        if (avTexCoords.isNotEmpty()) {
                            val vec = avTexCoords[idx]
                            p_pcOut.textureCoords[0][iVertex] = floatArrayOf(vec.x, vec.y)
                        }

                        if (avNormals.isNotEmpty())
                            p_pcOut.normals[iVertex] put avNormals[idx]

                        iVertex++
                    }
                    iNum++
                }
                // add the mesh to the output list
                avOut.add(p_pcOut)
            }
    }
}