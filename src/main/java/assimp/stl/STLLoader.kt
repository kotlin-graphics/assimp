package java.assimp.stl

import main.vec._4.Vec4d
import java.assimp.*
import java.io.File
import java.io.RandomAccessFile
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
        protected var clrColorDefault: Vec4d

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
        fun isBinarySTL(buffer: ByteBuffer): Boolean {
            if (buffer.capacity() < 84) {
                return false
            }

            val faceCount = buffer.getInt(80)
            val expectedBinaryFileSize = faceCount * 50 + 84

            return expectedBinaryFileSize == fileSize
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
    override fun internReadFile(pFile: String,
                                pScene: AiScene) {

        val file = File(pFile)

        // Check whether we can read from the file
        if (!file.canRead()) throw FileSystemException("Failed to open STL file $pFile.")

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

        if (isBinarySTL(mBuffer)) {
            bMatClr = LoadBinaryFile()
        } else if (IsAsciiSTL(mBuffer, fileSize)) {
            LoadASCIIFile()
        } else {
            throw DeadlyImportError("Failed to determine STL storage representation for $pFile.")
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Read a binary STL file
    fun loadBinaryFile(): Boolean {

    }
}