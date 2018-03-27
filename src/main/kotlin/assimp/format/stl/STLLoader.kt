package assimp.format.stl

import assimp.*
import glm_.*
import unsigned.ushr
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*

/**
 * Created by elect on 13/11/2016.
 */

class StlImporter : BaseImporter() {

    override val info = AiImporterDesc(
            name = "Stereolithography (STL) Importer",
            flags = AiImporterFlags.SupportTextFlavour or AiImporterFlags.SupportBinaryFlavour,
            fileExtensions = listOf("stl"))

    companion object {

        // A valid binary STL buffer should consist of the following elements, in order:
        // 1) 80 byte header
        // 2) 4 byte face count
        // 3) 50 bytes per face
        fun isBinarySTL(buffer: ByteBuffer, fileSize: Int): Boolean {
            if (fileSize < 84) {
                return false
            }

            val faceCount = buffer.getInt(80)
            val expectedBinaryFileSize = faceCount * 50 + 84

            return expectedBinaryFileSize == fileSize
        }

        // An ascii STL buffer will begin with "solid NAME", where NAME is optional.
        // Note: The "solid NAME" check is necessary, but not sufficient, to determine if the buffer is ASCII;
        // a binary header could also begin with "solid NAME".
        fun isAsciiSTL(buffer: ByteBuffer, fileSize: Int): Boolean {

            if (isBinarySTL(buffer, fileSize)) return false

            if (!buffer.skipSpaces()) return false

            if (buffer.position() + 5 >= fileSize) return false

            var isASCII = buffer startsWith "solid"
            if (isASCII) {
                // A lot of importers are write solid even if the file is binary. So we have to check for ASCII-characters.
                if (fileSize >= 500) {
                    isASCII = true
                    for (i in 0 until 500)
                        if (buffer.get() > 127)
                            return false
                }
            }
            return isASCII
        }
    }

    /** Buffer to hold the loaded file */
    protected lateinit var mBuffer: ByteBuffer

    /** Size of the file, in bytes */
    protected var fileSize = 0

    /** Output scene */
    protected lateinit var pScene: AiScene

    /** Default vertex color */
    protected var clrColorDefault = AiColor4D()

    // ------------------------------------------------------------------------------------------------
    // Returns whether the class can handle the format of the given file.
    override fun canRead(pFile: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {

        val extension = pFile.substring(pFile.lastIndexOf('.') + 1)

        if (extension == "stl") {
            return true
        }
//      TODO
//        else if (!extension.isEmpty() || checkSig) {
//            if (!pIOHandler) {
//                return true;
//            }
//            const char * tokens [] = { "STL", "solid" };
//            return SearchFileHeaderForToken(pIOHandler, pFile, tokens, 2);
//        }

        return false
    }

    // ------------------------------------------------------------------------------------------------
    // Imports the given file into the given scene structure.
    override fun internReadFile(pFile: String, ioSystem: IOSystem, scene: AiScene) {

        val file = File(pFile)

        // Check whether we can read from the file
        if (!file.canRead()) throw IOException("Failed to open STL file $pFile.")

        fileSize = file.length().i

        // allocate storage and copy the contents of the file to a memory buffer
        val fileChannel = RandomAccessFile(file, "r").channel
        val mBuffer2 = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        this.pScene = scene
        this.mBuffer = mBuffer2

        // the default vertex color is light gray.
        clrColorDefault put 0.6f

        // allocate a single node
        scene.rootNode = AiNode()

        var bMatClr = false

        if (isBinarySTL(mBuffer, fileSize))
            bMatClr = loadBinaryFile()
        else if (isAsciiSTL(mBuffer, fileSize))
            loadASCIIFile()
        else throw Error("Failed to determine STL storage representation for $pFile.")

        // add all created meshes to the single node
        scene.rootNode.numMeshes = scene.numMeshes
        scene.rootNode.meshes = IntArray(scene.numMeshes)
        for (i in 0 until scene.numMeshes)
            scene.rootNode.meshes[i] = i

        /*  create a single default material, using a light white diffuse color for consistency with other geometric
            types (e.g., PLY).  */
        val pcMat = AiMaterial()
        pcMat.name = AI_DEFAULT_MATERIAL_NAME

        val clrDiffuse = AiColor4D(1f)

        if (bMatClr) clrDiffuse put clrColorDefault

        pcMat.color = AiMaterial.Color(
                diffuse = AiColor3D(clrDiffuse),
                specular = AiColor3D(clrDiffuse),
                ambient = AiColor3D(1f)
        )

        scene.numMaterials = 1
        scene.materials.add(pcMat)
    }

    // ------------------------------------------------------------------------------------------------
    // Read a binary STL file
    fun loadBinaryFile(): Boolean {

        // allocate one mesh
        pScene.numMeshes = 1
        pScene.meshes.add(AiMesh())
        val pMesh = pScene.meshes[0]
        pMesh.materialIndex = 0

        // skip the first 80 bytes
        if (fileSize < 84) throw Error("STL: file is too small for the header")

        var bIsMaterialise = false

        // search for an occurrence of "COLOR=" in the header
        var sz2 = 0
        while (sz2 < 80) {
            if (mBuffer.getChar(sz2++) == 'C' && mBuffer.getChar(sz2++) == 'O' && mBuffer.getChar(sz2++) == 'L' &&
                    mBuffer.getChar(sz2++) == 'O' && mBuffer.getChar(sz2++) == 'R' && mBuffer.getChar(sz2++) == '=') {
                // read the default vertex color for facets
                bIsMaterialise = true
                logger.info { "STL: Taking code path for Materialise files" }
                val invByte = 1f / 255f
                clrColorDefault.r = (mBuffer.getFloat(sz2++) * invByte).f
                clrColorDefault.g = (mBuffer.getFloat(sz2++) * invByte).f
                clrColorDefault.b = (mBuffer.getFloat(sz2++) * invByte).f
                clrColorDefault.a = (mBuffer.getFloat(sz2) * invByte).f
                break
            }
        }
        var sz = 80

        // now read the number of facets
        pScene.rootNode.name = "<STL_BINARY>"

        pMesh.numFaces = mBuffer.getInt(sz)
        sz += 4

        if (fileSize < 84 + pMesh.numFaces * 50) throw Error("STL: file is too small to hold all facets")

        if (pMesh.numFaces == 0) throw Error("STL: file is empty. There are no facets defined")

        pMesh.numVertices = pMesh.numFaces * 3

        pMesh.vertices = ArrayList<AiVector3D>()
        pMesh.normals = ArrayList<AiVector3D>()

        for (i in 0 until pMesh.numFaces) {

            // NOTE: Blender sometimes writes empty normals ... this is not our fault ... the RemoveInvalidData helper
            // step should fix that
            val vn = AiVector3D(mBuffer, sz)
            sz += AiVector3D.size
            repeat(3) {
                pMesh.normals.add(AiVector3D(vn))
                pMesh.vertices.add(AiVector3D(mBuffer, sz))
                sz += AiVector3D.size
            }

            val color = mBuffer.getShort(sz)
            sz += Short.BYTES

            if ((color and (1 shl 15)) != 0.s) {

                // seems we need to take the color
                if (pMesh.colors[0] == null) {

                    pMesh.colors[0] = Array(pMesh.numVertices, { AiColor4D(clrColorDefault) }).toMutableList()

                    logger.info { "STL: Mesh has vertex colors" }
                }
                val clr = pMesh.colors[0]!![i * 3]
                clr.a = 1f
                val invVal = 1f / 31
                if (bIsMaterialise) {    // this is reversed

                    clr.r = (color and 0x31) * invVal
                    clr.g = ((color and (0x31 shl 5)) ushr 5) * invVal
                    clr.b = ((color and (0x31 shl 10)) ushr 10) * invVal
                } else {
                    clr.b = (color and 0x31) * invVal
                    clr.g = ((color and (0x31 shl 5)) ushr 5) * invVal
                    clr.r = ((color and (0x31 shl 10)) ushr 10) * invVal
                }
                // assign the color to all vertices of the face
                val a = pMesh.colors[i + 1]
                pMesh.colors[i + 1].forEach { it put clr }
                pMesh.colors[i + 2].forEach { it put clr }
            }
        }
        // now copy faces
        addFacesToMesh(pMesh);

        // use the color as diffuse material color
        return bIsMaterialise && pMesh.colors[0] == null
    }

    // ------------------------------------------------------------------------------------------------
    // Read an ASCII STL file
    fun loadASCIIFile() {

        val meshes = ArrayList<AiMesh>()
        val positionBuffer = ArrayList<AiVector3D>()
        val normalBuffer = ArrayList<AiVector3D>()

        val bytes = ByteArray(mBuffer.position(0).remaining())
        mBuffer.get(bytes)
        var buffer = String(bytes)

        val pMesh = AiMesh()
        pMesh.materialIndex = 0
        meshes.add(pMesh)

        buffer = buffer.removePrefix("solid")    // skip the "solid"
        buffer = buffer.trim()

        val words = buffer.words

        // setup the name of the node
        if (!buffer[0].isNewLine) {
            if (words[0].length >= MAXLEN) throw Error("STL: Node name too long")
            pScene.rootNode.name = words[0]
        } else pScene.rootNode.name = "<STL_ASCII>"

        var faceVertexCounter = 3
        var i = 0

        while (true) {
            val word = words[i]
            // seems we're finished although there was no end marker
            if (i == word.length - 1 && word != "endsolid") {
                logger.warn { "STL: unexpected EOF. \'endsolid\' keyword was expected" }
                break
            }

            if (word == "facet") {

                if (faceVertexCounter != 3) logger.warn { "STL: A new facet begins but the old is not yet complete" }

                faceVertexCounter = 0
                val vn = AiVector3D()
                normalBuffer.add(vn)

                if (words[i + 1] != "normal") logger.warn { "STL: a facet normal vector was expected but not found" }
                else {
                    try {
                        i++
                        vn.x = words[++i].f
                        vn.y = words[++i].f
                        vn.z = words[++i].f
                        normalBuffer.add(AiVector3D(vn))
                        normalBuffer.add(AiVector3D(vn))
                    } catch (exc: NumberFormatException) {
                        throw Error("STL: unexpected EOF while parsing facet")
                    }
                }
            } else if (word == "vertex") {

                if (faceVertexCounter >= 3) {
                    logger.error { "STL: a facet with more than 3 vertices has been found" }
                    i++
                } else {
                    try {
                        val vn = AiVector3D()
                        vn.x = words[++i].f
                        vn.y = words[++i].f
                        vn.z = words[++i].f
                        positionBuffer.add(vn)
                        faceVertexCounter++
                    } catch (exc: NumberFormatException) {
                        throw Error("STL: unexpected EOF while parsing facet")
                    }
                }
            }
            // finished!
            else if (word == "endsolid") break

            i++
        }

        if (positionBuffer.isEmpty()) {
            pMesh.numFaces = 0
            throw Error("STL: ASCII file is empty or invalid; no data loaded")
        }
        if (positionBuffer.size % 3 != 0) {
            pMesh.numFaces = 0
            throw Error("STL: Invalid number of vertices")
        }
        if (normalBuffer.size != positionBuffer.size) {
            pMesh.numFaces = 0
            throw Error("Normal buffer size does not match position buffer size")
        }
        pMesh.numFaces = positionBuffer.size / 3
        pMesh.numVertices = positionBuffer.size
        pMesh.vertices.addAll(positionBuffer)
        positionBuffer.clear()
        pMesh.normals = normalBuffer.toMutableList()
        normalBuffer.clear()

        pScene.rootNode.name = words[0]

        // now copy faces
        addFacesToMesh(pMesh)

        // now add the loaded meshes
        pScene.numMeshes = meshes.size
        pScene.meshes.addAll(meshes)
    }

    fun addFacesToMesh(pMesh: AiMesh) = repeat(pMesh.numFaces) {
        pMesh.faces.add(mutableListOf(it * 3, it * 3 + 1, it * 3 + 2))
    }
}