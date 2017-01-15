package stl

import AI_DEFAULT_MATERIAL_NAME
import MAXLEN
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileSystemException
import java.util.*
import ushr
import and
import AiScene
import AiColor3D
import AiColor4D
import AiVector3D
import i
import AiMesh
import AiNode
import AiMaterial
import BaseImporter
import AiImporterDesc
import BYTES
import f
import isNewLine
import s
import skipSpaces
import startsWith
import vec._4.Vec4
import java.net.URI
import java.nio.ByteOrder

/**
 * Created by elect on 13/11/2016.
 */

class STLImporter : BaseImporter() {

    companion object {

        val desc = AiImporterDesc(
                mName = "Stereolithography (STL) Importer",
                mFlags = AiImporterFlags.SupportTextFlavour or AiImporterFlags.SupportBinaryFlavour,
                mFileExtensions = "stl"
        )


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
    override fun canRead(pFile: URI, checkSig: Boolean): Boolean {

        val extension = pFile.s.substring(pFile.s.lastIndexOf('.') + 1)

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
    override fun internReadFile(pFile: URI, pScene: AiScene) {

        val file = File(pFile)

        // Check whether we can read from the file
        if (!file.canRead()) throw FileSystemException("Failed to open STL file $pFile.")

        fileSize = file.length().i

        // allocate storage and copy the contents of the file to a memory buffer
        val fileChannel = RandomAccessFile(file, "r").channel
        val mBuffer2 = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        this.pScene = pScene
        this.mBuffer = mBuffer2

        // the default vertex color is light gray.
        clrColorDefault Set 0.6f

        // allocate a single node
        pScene.mRootNode = AiNode()

        var bMatClr = false

        if (isBinarySTL(mBuffer, fileSize))
            bMatClr = loadBinaryFile()
        else if (isAsciiSTL(mBuffer, fileSize))
            loadASCIIFile()
        else throw Error("Failed to determine STL storage representation for $pFile.")

        // add all created meshes to the single node
        pScene.mRootNode.mNumMeshes = pScene.mNumMeshes
        pScene.mRootNode.mMeshes = IntArray(pScene.mNumMeshes)
        for (i in 0 until pScene.mNumMeshes)
            pScene.mRootNode.mMeshes!![i] = i

        // create a single default material, using a light gray diffuse color for consistency with other geometric types
        // (e.g., PLY).
        val pcMat = AiMaterial()
        pcMat.name = AI_DEFAULT_MATERIAL_NAME

        val clrDiffuse = AiColor4D(0.6, 0.6, 0.6, 1)

        if (bMatClr) clrDiffuse Set clrColorDefault

        pcMat.color = AiMaterial.Color(
                diffuse = AiColor3D(clrDiffuse),
                specular = AiColor3D(clrDiffuse),
                ambient = AiColor3D(0.05, 0.05, 0.05)
        )

        pScene.mNumMaterials = 1
        pScene.mMaterials = mutableListOf(pcMat)
    }

    // ------------------------------------------------------------------------------------------------
    // Read a binary STL file
    fun loadBinaryFile(): Boolean {

        // allocate one mesh
        pScene.mNumMeshes = 1
        pScene.mMeshes = mutableListOf(AiMesh())
        val pMesh = pScene.mMeshes[0]
        pMesh.mMaterialIndex = 0

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
                val invByte = 1f / 255f
                clrColorDefault.r = (mBuffer.getFloat(sz2++) * invByte).f
                clrColorDefault.g = (mBuffer.getFloat(sz2++) * invByte).f
                clrColorDefault.b = (mBuffer.getFloat(sz2++) * invByte).f
                clrColorDefault.a = (mBuffer.getFloat(sz2++) * invByte).f
                break
            }
        }
        var sz = 80

        // now read the number of facets
        pScene.mRootNode.mName = "<STL_BINARY>"

        pMesh.mNumFaces = mBuffer.getInt(sz)
        sz += 4

        if (fileSize < 84 + pMesh.mNumFaces * 50) throw Error("STL: file is too small to hold all facets")

        if (pMesh.mNumFaces == 0) throw Error("STL: file is empty. There are no facets defined")

        pMesh.mNumVertices = pMesh.mNumFaces * 3

        pMesh.mVertices = ArrayList<AiVector3D>()
        pMesh.mNormals = ArrayList<AiVector3D>()

        for (i in 0 until pMesh.mNumFaces) {

            // NOTE: Blender sometimes writes empty normals ... this is not our fault ... the RemoveInvalidData helper
            // step should fix that
            val vn = AiVector3D(mBuffer, sz)
            sz += AiVector3D.SIZE
            repeat(3) {
                pMesh.mNormals.add(vn.copy())
                pMesh.mVertices.add(AiVector3D(mBuffer, sz))
                sz += AiVector3D.SIZE
            }

            val color = mBuffer.getShort(sz)
            sz += Short.BYTES

            if ((color and (1 shl 15)) != 0.s) {

                // seems we need to take the color
                if (pMesh.mColors[0] == null) {

                    pMesh.mColors[0] = Array(pMesh.mNumVertices, { AiColor4D(clrColorDefault) }).toMutableList()

                    println("STL: Mesh has vertex colors")
                }
                val clr = pMesh.mColors[0]!![i * 3]
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
                val a = pMesh.mColors[i + 1]
                pMesh.mColors[i + 1].forEach { it.Set(clr) }
                pMesh.mColors[i + 2].forEach { it.Set(clr) }
            }
        }
        // now copy faces
        addFacesToMesh(pMesh);

        // use the color as diffuse material color
        return bIsMaterialise && pMesh.mColors[0] == null
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
        pMesh.mMaterialIndex = 0
        meshes.add(pMesh)

        buffer = buffer.removePrefix("solid")    // skip the "solid"
        buffer = buffer.trim()

        val words = buffer.split("\\s+".toRegex()).toMutableList()

        // setup the name of the node
        if (!buffer[0].isNewLine()) {
            if (words[0].length >= MAXLEN) throw Error("STL: Node name too long")
            pScene.mRootNode.mName = words[0]
        } else pScene.mRootNode.mName = "<STL_ASCII>"

        var faceVertexCounter = 3
        var i = 0

        while (true) {

            val word = words[i]

            if (i == word.length - 1 && word != "endsolid") {
                System.err.println("STL: unexpected EOF. \'endsolid\' keyword was expected")
                break
            }

            if (word == "facet") {

                if (faceVertexCounter != 3) System.err.println("STL: A new facet begins but the old is not yet complete")

                faceVertexCounter = 0
                val vn = AiVector3D()
                normalBuffer.add(vn)

                if (words[i + 1] != "normal") System.err.println("STL: a facet normal vector was expected but not found")
                else {
                    try {
                        i++
                        vn.x = words[++i].toFloat()
                        vn.y = words[++i].toFloat()
                        vn.z = words[++i].toFloat()
                        normalBuffer.add(vn.copy())
                        normalBuffer.add(vn.copy())
                    } catch (exc: NumberFormatException) {
                        throw Error("STL: unexpected EOF while parsing facet")
                    }
                }
            } else if (word == "vertex") {

                if (faceVertexCounter >= 3) {
                    System.err.println("STL: a facet with more than 3 vertices has been found")
                    i++
                } else {
                    try {
                        val vn = AiVector3D()
                        vn.x = words[++i].toFloat()
                        vn.y = words[++i].toFloat()
                        vn.z = words[++i].toFloat()
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
            pMesh.mNumFaces = 0
            throw Error("STL: ASCII file is empty or invalid; no data loaded")
        }
        if (positionBuffer.size % 3 != 0) {
            pMesh.mNumFaces = 0
            throw Error("STL: Invalid number of vertices")
        }
        if (normalBuffer.size != positionBuffer.size) {
            pMesh.mNumFaces = 0
            throw Error("Normal buffer size does not match position buffer size")
        }
        pMesh.mNumFaces = positionBuffer.size / 3
        pMesh.mNumVertices = positionBuffer.size
        pMesh.mVertices = positionBuffer.toMutableList()
        positionBuffer.clear()
        pMesh.mNormals = normalBuffer.toMutableList()
        normalBuffer.clear()

        pScene.mRootNode.mName = words[0]

        // now copy faces
        addFacesToMesh(pMesh)

        // now add the loaded meshes
        pScene.mNumMeshes = meshes.size
        pScene.mMeshes = meshes.toMutableList()
    }

    fun addFacesToMesh(pMesh: AiMesh) {
        var p = 0
        pMesh.mFaces = (0 until pMesh.mNumFaces).map { mutableListOf(p++, p++, p++) }
    }
}