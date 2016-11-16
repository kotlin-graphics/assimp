package java.assimp.stl

import main.d
import main.i
import main.vec._4.Vec4d
import java.assimp.*
import java.io.File
import java.io.RandomAccessFile
import java.lang.Error
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileSystemException
import java.nio.file.Files

/**
 * Created by elect on 13/11/2016.
 */

class STLLoader(

        /** Buffer to hold the loaded file */
        protected var mBuffer: ByteBuffer,

        /** Size of the file, in bytes */
        protected var fileSize: Int,

        /** Output scene */
        protected var pScene: AiScene,

        /** Default vertex color */
        protected var clrColorDefault: AiColor4D

) : BaseImporter() {

    companion object {

        val desc = AiImporterDesc(
                "Stereolithography (STL) Importer",
                "",
                "",
                "",
                AiImporterFlags.SupportTextFlavour or AiImporterFlags.SupportBinaryFlavour,
                0,
                0,
                0,
                0,
                "stl")


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
        // Note: The "solid NAME" check is necessary, but not sufficient, to determine
        // if the buffer is ASCII; a binary header could also begin with "solid NAME".
        fun IsAsciiSTL(buffer: ByteBuffer, fileSize: Int): Boolean {
            if (isBinarySTL(buffer, fileSize)) return false

            return true
        }
    }

    override fun canRead(pFile: String, checkSig: Boolean): Boolean {

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
    override fun internReadFile(pFile: String, pScene: AiScene) {

        val file = File(pFile)

        // Check whether we can read from the file
        if (!file.canRead()) throw FileSystemException("Failed to open STL file $pFile.")

        fileSize = file.length().i

        // allocate storage and copy the contents of the file to a memory buffer
        val fileChannel = RandomAccessFile(file, "r").channel
        val mBuffer2 = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())

        this.pScene = pScene
        this.mBuffer = mBuffer2

        // the default vertex color is light gray.
        clrColorDefault.to(0.6f)

        // allocate a single node
        pScene.mRootNode = AiNode()

        var bMatClr = false

//        if (isBinarySTL(mBuffer, fileSize)) {
//            bMatClr = LoadBinaryFile()
//        } else if (IsAsciiSTL(mBuffer, fileSize)) {
//            LoadASCIIFile()
//        } else {
//            throw DeadlyImportError("Failed to determine STL storage representation for $pFile.")
//        }
    }

    // ------------------------------------------------------------------------------------------------
    // Read a binary STL file
    fun loadBinaryFile(): Boolean {

        // allocate one mesh
        pScene.mNumMeshes = 1
        pScene.mMeshes = mutableListOf(AiMesh())
        val pMesh = pScene.mMeshes!![0]
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
                clrColorDefault.r = (mBuffer.getFloat(sz2++) * invByte).d
                clrColorDefault.g = (mBuffer.getFloat(sz2++) * invByte).d
                clrColorDefault.b = (mBuffer.getFloat(sz2++) * invByte).d
                clrColorDefault.a = (mBuffer.getFloat(sz2++) * invByte).d
                break
            }
        }
        var sz = 80

        // now read the number of facets
        pScene.mRootNode!!.mName = "<STL_BINARY>"

        pMesh.mNumFaces = mBuffer.getInt(sz)
        sz += 4

        if (fileSize < 84 + pMesh.mNumFaces * 50) throw Error("STL: file is too small to hold all facets")

        if (pMesh.mNumFaces == 0) throw Error("STL: file is empty. There are no facets defined")

        pMesh.mNumVertices = pMesh.mNumFaces * 3

        pMesh.mVertices = arrayOfNulls<AiVector3D?>(pMesh.mNumVertices)
        pMesh.mNormals = arrayOfNulls<AiVector3D>(pMesh.mNumVertices)
        var vp = 0
        var vn = 0

        for (i in 0..pMesh.mNumFaces) {

            // NOTE: Blender sometimes writes empty normals ... this is not
            // our fault ... the RemoveInvalidData helper step should fix that
            pMesh.mNormals!![vn] = AiVector3D(mBuffer, sz)
        }

        return true
    }
}