package assimp.format.obj

import assimp.*
import glm_.f
import glm_.i
import glm_.max
import java.io.BufferedReader
import assimp.AiPrimitiveType as Pt

/**
 * Created by elect on 21/11/2016.
 */

val DEFAULT_MATERIAL = AI_DEFAULT_MATERIAL_NAME
val DefaultObjName = "defaultobject"

class ObjFileParser(private val file: IOStream, val ioSystem: IOSystem) {

    //! Pointer to model instance
    val model = Model()

    init {
        // Create the model instance to store all the data
        model.m_ModelName = file.filename

        // create default material and store it
        model.defaultMaterial = Material(DEFAULT_MATERIAL)
        model.materialLib.add(DEFAULT_MATERIAL)
        model.materialMap[DEFAULT_MATERIAL] = model.defaultMaterial!!

        // Start parsing the file

        parseFile(file.reader())
    }

    // -------------------------------------------------------------------
    //  File parsing method.
//    fun parseFile(streamBuffer: List<String>) {
    fun parseFile(streamBuffer: BufferedReader) {


        var line: String?
        do {
            //Support for continuationToken
            line = ObjTools.getNextDataLine(streamBuffer, "\\")

            //End of stream
            if (line == null)
                return

            if (line.isEmpty()) continue

            val words = line.words

            when (line[0]) {
                // Parse a vertex texture coordinate
                'v' -> when (line[1]) {

                    ' ', '\t' -> when (words.size - 1) {
                        // read in vertex definition
                        3 -> model.m_Vertices.add(AiVector3D((1..3).map { words[it].f }))
                        // read in vertex definition (homogeneous coords)
                        4 -> {
                            val w = words[4].f
                            assert(w != 0f)
                            model.m_Vertices.add(AiVector3D((1..3).map { words[it].f / w }))
                        }
                        // read vertex and vertex-color
                        6 -> {
                            model.m_Vertices.add(AiVector3D((1..3).map { words[it].f }))
                            model.m_VertexColors.add(AiVector3D((4..6).map { words[it].f }))
                        }
                    }
                    // read in texture coordinate ( 2D or 3D )
                    't' -> {
                        val dim = getTexCoordVector(words)
                        model.textureCoordDim = model.textureCoordDim max dim
                    }
                    // Read in normal vector definition
                    'n' -> model.m_Normals.add(AiVector3D((1..3).map { words[it].f }))
                }
                // Parse a face, line or point statement
                'p', 'l', 'f' -> getFace(if (line[0] == 'f') Pt.POLYGON else if (line[0] == 'l') Pt.LINE else Pt.POINT, line)
                // Parse a material desc. setter
                'u' -> if (words[0] == "usemtl") getMaterialDesc(line)
                // Parse a material library or merging group ('mg')
                'm' -> when (words[0]) {
                    "mg" -> getGroupNumberAndResolution()
                    "mtllib" -> getMaterialLib(words)
                }
                // Parse group name
                'g' -> getGroupName(line)
                // Parse group number
                's' -> getGroupNumber()
                // Parse object name
                'o' -> getObjectName(line)
            }
        } while (line != null)
    }

    /** input cant be:
     *  vt 0f 0f
     *  or
     *  vt 0f 0f 0f
     */
    fun getTexCoordVector(input: List<String>): Int {
        val numComponents = input.size
        val thirdDim = when (numComponents) {
            1 + 2 -> 0f
            1 + 3 -> input[3].toFloat
            else -> throw Error("OBJ: Invalid number of components")
        }
        model.m_TextureCoord.add(mutableListOf(input[1].toFloat, input[2].toFloat, thirdDim))
        return numComponents
    }

    /** Coerce nan and inf to 0 as is the OBJ default value */
    val String.toFloat: Float
        get() = try {
            f
        } catch (e: NumberFormatException) {
            when {
                equals("nan", ignoreCase = true) || equals("inf", ignoreCase = true) -> 0f
                else -> throw e // Invalid string
            }
        }

    // -------------------------------------------------------------------
    //  Get values for a new face instance
    fun getFace(type: Pt, line: String) {

        val vertices = line.substring(1).trim().split(Regex("\\s+"))

        val face = Face(type)
        var hasNormal = false

        val vSize = model.m_Vertices.size
        val vtSize = model.m_TextureCoord.size
        val vnSize = model.m_Normals.size

        for (vertex in vertices) {

            if (vertex[0] == '/' && type == Pt.POINT) logger.error { "Obj: Separator unexpected in point statement" }

            val component = vertex.split("/")

            var skip = false
            var i = 0
            while (i in 0 until component.size && !skip) {
                if (component[i].isNotEmpty()) {
                    val iVal = component[i].i
                    if (iVal > 0) when (i) {    // Store parsed index
                        0 -> face.m_vertices.add(iVal - 1)
                        1 -> face.m_texturCoords.add(iVal - 1)
                        2 -> {
                            face.m_normals.add(iVal - 1)
                            hasNormal = true
                        }
                        else -> {
                            logger.error { "OBJ: Not supported token in face description detected --> " + line }
                            skip = true
                        }
                    }
                    else if (iVal < 0) when (i) {   // Store relatively index
                        0 -> face.m_vertices.add(vSize + iVal)
                        1 -> face.m_texturCoords.add(vtSize + iVal)
                        2 -> {
                            face.m_normals.add(vnSize + iVal)
                            hasNormal = true
                        }
                        else -> {
                            logger.error { "OBJ: Not supported token in face description detected -- >" + line }
                            skip = true
                        }
                    }
                }
                i++
            }
        }
        if (face.m_vertices.isEmpty()) throw Exception("Obj: Ignoring empty face")
        // Set active material, if one set
        face.m_pMaterial = model.currentMaterial ?: model.defaultMaterial
        // Create a default object, if nothing is there
        if (model.m_pCurrent == null) createObject(DefaultObjName)
        // Assign face to mesh
        if (model.currentMesh == null) createMesh(DefaultObjName)
        // Store the face
        with(model.currentMesh!!) {
            m_Faces.add(face)
            m_uiNumIndices += face.m_vertices.size
            m_uiUVCoordinates[0] += face.m_texturCoords.size
            if (!m_hasNormals && hasNormal) m_hasNormals = true
        }
    }

    // -------------------------------------------------------------------
    //  Creates a new object instance
    fun createObject(objName: String) {

        model.m_pCurrent = Object()
        model.m_pCurrent!!.m_strObjName = objName
        model.m_Objects.add(model.m_pCurrent!!)

        createMesh(objName)

        model.currentMaterial?.let {
            model.currentMesh!!.m_uiMaterialIndex = getMaterialIndex(it.materialName)
            model.currentMesh!!.m_pMaterial = it
        }
    }

    // -------------------------------------------------------------------
    //  Creates a new mesh
    fun createMesh(meshName: String) {

        model.currentMesh = Mesh(meshName)
        model.m_Meshes.add(model.currentMesh!!)
        val meshId = model.m_Meshes.size - 1
        if (model.m_pCurrent != null)
            model.m_pCurrent!!.m_Meshes.add(meshId)
        else
            throw Exception("OBJ: No object detected to attach a new mesh instance.")
    }

    // -------------------------------------------------------------------
    //  Get values for a new material description
    fun getMaterialDesc(line: String) {
        // Get name (support for spaces)
        val strName = ObjTools.getNameWithSpace(line)

        // If the current mesh has the same material, we simply ignore that 'usemtl' command
        // There is no need to create another object or even mesh here
        if (model.currentMaterial == null || model.currentMaterial!!.materialName != strName) {
            // Search for material
            model.currentMaterial = model.materialMap[strName] ?: Material(materialName = strName).also {
                /*  Not found, so we don't know anything about the material except for its name.
                    This may be the case if the material library is missing. We don't want to lose all materials if that
                    happens, so create a new named material instead of discarding it completely.    */
                logger.error("OBJ: failed to locate material $strName, creating new material")
                model.materialLib.add(strName)
                model.materialMap[strName] = it
            }
        }

        if (needsNewMesh(strName))
            createMesh(strName)

        model.currentMesh!!.m_uiMaterialIndex = getMaterialIndex(strName)
    }

    // -------------------------------------------------------------------
    //  Returns true, if a new mesh must be created.
    fun needsNewMesh(materialName: String): Boolean {

        // If no mesh data yet
        if (model.currentMesh == null) return true

        var newMat = false
        val matIdx = getMaterialIndex(materialName)
        val curMatIdx = model.currentMesh!!.m_uiMaterialIndex
        if (curMatIdx != Mesh.NoMaterial && curMatIdx != matIdx
                // no need create a new mesh if no faces in current lets say 'usemtl' goes straight after 'g'
                && model.currentMesh!!.m_Faces.size > 0)
        // New material -> only one material per mesh, so we need to create a new material
            newMat = true

        return newMat
    }

    // -------------------------------------------------------------------
    //  Not supported
    fun getGroupNumberAndResolution() {
        // Not used
    }

    // -------------------------------------------------------------------
    //  Get material library from file.
    fun getMaterialLib(words: List<String>) {

        if (words.size < 2) throw Exception("File name of the material is absent.")

        // get the name of the mat file with spaces
        val filename = ObjTools.getNameWithSpace(words, 1)

        val pFile = "${file.parentPath}${ioSystem.osSeparator}$filename"
//        println(pFile)

        if (!ioSystem.exists(pFile)) {
            logger.error { "OBJ: Unable to locate material file $filename" }

            // TODO ?? what happens here?
            val strMatFallbackName = filename.substring(0, filename.length - 3) + "mtl"
            println("OBJ: Opening fallback material file $strMatFallbackName")
            if (!ioSystem.exists(strMatFallbackName)) {
                System.err.println("OBJ: Unable to locate fallback material file $strMatFallbackName")
                return
            }
        }

        // Import material library data from file.
        // Some exporters (e.g. Silo) will happily write out empty material files if the model doesn't use any materials, so we allow that.
        val buffer = ioSystem.open(pFile).reader().readLines().filterTo(ArrayList(), String::isNotBlank)

        val bom = buffer[0].byteOrderMarkLength
        if(bom != 0)
            buffer[0] = buffer[0].drop(bom)

        ObjFileMtlImporter(buffer, model)
    }

    /** Returns the index of the material. Is -1 if not material was found. */
    fun getMaterialIndex(materialName: String): Int = when {
        materialName.isEmpty() -> -1
        else -> model.materialLib.indexOf(materialName)
    }

    // -------------------------------------------------------------------
    //  Getter for a group name.
    fun getGroupName(line: String) {

        val groupName = line.split("\\s+".toRegex()).getOrElse(1) { "" }
        // Change active group, if necessary
        if (model.m_strActiveGroup != groupName) {

            // We are mapping groups into the object structure
            createObject(groupName)
            // Search for already existing entry
            if (!model.m_Groups.containsKey(groupName))
                model.m_Groups[groupName] = mutableListOf()
            else
                model.m_pGroupFaceIDs = model.m_Groups[groupName]!!

            model.m_strActiveGroup = groupName
        }
    }

    // -------------------------------------------------------------------
    //  Not supported
    fun getGroupNumber() {
        // Not used
    }

    // -------------------------------------------------------------------
    //  Stores values for a new object instance, name will be used to identify it.
    fun getObjectName(line: String) {

        val objectName = line.split("\\s+".toRegex())[1]

        if (objectName.isNotEmpty()) {

            // Search for actual object
            model.m_pCurrent = model.m_Objects.find { it.m_strObjName == objectName }

            if (model.m_pCurrent == null)
                createObject(objectName)
        }
    }
}