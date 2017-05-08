package assimp.format.obj

import assimp.*
import glm.f
import java.io.InputStream
import java.util.stream.Stream

/**
 * Created by elect on 21/11/2016.
 */

val DEFAULT_MATERIAL = AI_DEFAULT_MATERIAL_NAME
val DefaultObjName = "defaultobject"

class ObjFileParser(stream: InputStream) {

    //! Pointer to model instance
    val m_pModel = Model()

    init {
        // Create the model instance to store all the data
        m_pModel.m_ModelName = _filename.substringAfterLast('/')

        // create default material and store it
        m_pModel.m_pDefaultMaterial = Material(DEFAULT_MATERIAL)
        m_pModel.m_MaterialLib.add(DEFAULT_MATERIAL)
        m_pModel.m_MaterialMap.put(DEFAULT_MATERIAL, m_pModel.m_pDefaultMaterial!!)

        // Start parsing the file

        parseFile(stream.bufferedReader().lines())
    }

    // -------------------------------------------------------------------
    //  File parsing method.
    fun parseFile(streamBuffer: Stream<String>) {

        for (line in streamBuffer) {

            if (line.isEmpty()) continue

            val words = line.words

            when (line[0]) {
            // Parse a vertex texture coordinate
                'v' -> when (line[1]) {

                    ' ', '\t' -> when (words.size - 1) {
                    // read in vertex definition
                        3 -> m_pModel.m_Vertices.add(AiVector3D((1..3).map { words[it].f }))
                    // read in vertex definition (homogeneous coords)
                        4 -> {
                            val w = words[4].f
                            assert(w != 0f)
                            m_pModel.m_Vertices.add(AiVector3D((1..3).map { words[it].f / w }))
                        }
                    // read vertex and vertex-color
                        6 -> {
                            m_pModel.m_Vertices.add(AiVector3D((1..3).map { words[it].f }))
                            m_pModel.m_VertexColors.add(AiVector3D((4..6).map { words[it].f }))
                        }
                    }
                // read in texture coordinate ( 2D or 3D )
                    't' -> m_pModel.m_TextureCoord.add(mutableListOf(words[1].f, words[2].f, if (words.size == 3) 0f else words[3].f))
                // Read in normal vector definition
                    'n' -> m_pModel.m_Normals.add(AiVector3D((1..3).map { words[it].f }))
                }
            // Parse a face, line or point statement
                'p' -> getFace(AiPrimitiveType.POINT, line)
                'l' -> getFace(AiPrimitiveType.LINE, line)
                'f' -> getFace(AiPrimitiveType.POLYGON, line)
            // Parse a material desc. setter
                'u' -> getMaterialDesc(line)
            // Parse a material library or merging group ('mg')
                'm' -> {
                    when (words[0]) {
                        "mg" -> getGroupNumberAndResolution()
                        "mtllib" -> getMaterialLib(words)
                    }
                }
            // Parse group name
                'g' -> getGroupName(line)
            // Parse group number
                's' -> getGroupNumber()
            // Parse object name
                'o' -> getObjectName(line)
            }
        }
    }

    // -------------------------------------------------------------------
    //  Get values for a new face instance
    fun getFace(type: AiPrimitiveType, line: String) {

        val vertices = line.substring(1).trim().split("\\s+".toRegex())

        val face = Face(type)
        var hasNormal = false

        val vSize = m_pModel.m_Vertices.size
        val vtSize = m_pModel.m_TextureCoord.size
        val vnSize = m_pModel.m_Normals.size

        for (vertex in vertices) {

            if (vertex[0] == '/' && type == AiPrimitiveType.POINT) throw Error("Obj: Separator unexpected in point statement")

            val component = vertex.split("/")

            for (i in component.indices)

                if (component[i].isNotEmpty()) {

                    val iVal = component[i].toInt()

                    if (iVal > 0)
                    // Store parsed index
                        when (i) {
                            0 -> face.m_vertices.add(iVal - 1)
                            1 -> face.m_texturCoords.add(iVal - 1)
                            2 -> {
                                face.m_normals.add(iVal - 1)
                                hasNormal = true
                            }
                            else -> throw Error("OBJ: Not supported token in face description detected")
                        }
                    else if (iVal < 0)
                    // Store relatively index
                        when (i) {
                            0 -> face.m_vertices.add(vSize + iVal)
                            1 -> face.m_texturCoords.add(vtSize + iVal)
                            2 -> {
                                face.m_normals.add(vnSize + iVal)
                                hasNormal = true
                            }
                            else -> throw Error("OBJ: Not supported token in face description detected")
                        }
                }
        }

        if (face.m_vertices.isEmpty()) throw Error("Obj: Ignoring empty face")

        // Set active material, if one set
        face.m_pMaterial = m_pModel.m_pCurrentMaterial ?: m_pModel.m_pDefaultMaterial

        // Create a default object, if nothing is there
        if (m_pModel.m_pCurrent == null)
            createObject(DefaultObjName)

        // Assign face to mesh
        if (m_pModel.m_pCurrentMesh == null)
            createMesh(DefaultObjName)

        // Store the face
        with(m_pModel.m_pCurrentMesh!!) {
            m_Faces.add(face)
            m_uiNumIndices += face.m_vertices.size
            m_uiUVCoordinates[0] += face.m_texturCoords.size
            if (!m_hasNormals && hasNormal)
                m_hasNormals = true
        }
    }

    // -------------------------------------------------------------------
    //  Creates a new object instance
    fun createObject(objName: String) {

        m_pModel.m_pCurrent = Object()
        m_pModel.m_pCurrent!!.m_strObjName = objName
        m_pModel.m_Objects.add(m_pModel.m_pCurrent!!)

        createMesh(objName)

        m_pModel.m_pCurrentMaterial?.let {
            m_pModel.m_pCurrentMesh!!.m_uiMaterialIndex =
                    m_pModel.m_MaterialLib.indexOfFirst { it == m_pModel.m_pCurrentMaterial!!.materialName }
            m_pModel.m_pCurrentMesh!!.m_pMaterial = m_pModel.m_pCurrentMaterial
        }
    }

    // -------------------------------------------------------------------
    //  Creates a new mesh
    fun createMesh(meshName: String) {

        m_pModel.m_pCurrentMesh = Mesh(meshName)
        m_pModel.m_Meshes.add(m_pModel.m_pCurrentMesh!!)
        val meshId = m_pModel.m_Meshes.size - 1
        if (m_pModel.m_pCurrent != null)
            m_pModel.m_pCurrent!!.m_Meshes.add(meshId)
        else
            throw Error("OBJ: No object detected to attach a new mesh instance.")
    }

    // -------------------------------------------------------------------
    //  Get values for a new material description
    fun getMaterialDesc(line: String) {

        // Get name
        var strName = line.split("\\s+".toRegex())[1].trim()

        // If the current mesh has the same material, we simply ignore that 'usemtl' command
        // There is no need to create another object or even mesh here
        if (m_pModel.m_pCurrentMaterial == null || m_pModel.m_pCurrentMaterial!!.materialName != strName) {
            // Search for material
            m_pModel.m_pCurrentMaterial = m_pModel.m_MaterialMap.getOrElse(strName, {
                System.err.println("OBJ: failed to locate material $strName, skipping")
                strName = m_pModel.m_pDefaultMaterial!!.materialName
                m_pModel.m_pDefaultMaterial!!
            })
        }

        if (needsNewMesh(strName))
            createMesh(strName)

        m_pModel.m_pCurrentMesh!!.m_uiMaterialIndex = m_pModel.m_MaterialLib.indexOfFirst { it == strName }
    }

    // -------------------------------------------------------------------
    //  Returns true, if a new mesh must be created.
    fun needsNewMesh(materialName: String): Boolean {

        // If no mesh data yet
        if (m_pModel.m_pCurrentMesh == null) return true

        var newMat = false
        val matIdx = m_pModel.m_MaterialLib.indexOfFirst { it == materialName }
        val curMatIdx = m_pModel.m_pCurrentMesh!!.m_uiMaterialIndex
        if (curMatIdx != Mesh.NoMaterial && curMatIdx != matIdx
                // no need create a new mesh if no faces in current lets say 'usemtl' goes straight after 'g'
                && m_pModel.m_pCurrentMesh!!.m_Faces.size > 0)
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

        if (words.size < 2) throw Error("File name of the material is absent.")

        val filename = words[1]

        val parent = _filename.substringBeforeLast('/')
        val stream = _assets.open(parent + filename)

        if (stream == null) {
            System.err.println("OBJ: Unable to locate material file $filename")
            val strMatFallbackName = filename.substring(0, filename.length - 3) + "mtl"
            println("OBJ: Opening fallback material file $strMatFallbackName")
            if (_assets.open(strMatFallbackName) == null) {
                System.err.println("OBJ: Unable to locate fallback material file $strMatFallbackName")
                return
            }
        }

        // Import material library data from file.
        // Some exporters (e.g. Silo) will happily write out empty material files if the model doesn't use any materials, so we allow that.
        val buffer = stream.bufferedReader().lines().filter(String::isNotBlank)

        ObjFileMtlImporter(buffer, m_pModel)
    }

    // -------------------------------------------------------------------
    //  Getter for a group name.
    fun getGroupName(line: String) {

        val groupName = line.split("\\s+".toRegex())[1]
        // Change active group, if necessary
        if (m_pModel.m_strActiveGroup != groupName) {

            // We are mapping groups into the object structure
            createObject(groupName)
            // Search for already existing entry
            if (!m_pModel.m_Groups.containsKey(groupName))
                m_pModel.m_Groups[groupName] = mutableListOf()
            else
                m_pModel.m_pGroupFaceIDs = m_pModel.m_Groups[groupName]!!

            m_pModel.m_strActiveGroup = groupName
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
            m_pModel.m_pCurrent = m_pModel.m_Objects.find { it.m_strObjName == objectName }

            if (m_pModel.m_pCurrent == null)
                createObject(objectName)
        }
    }
}