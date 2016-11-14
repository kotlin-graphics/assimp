package java.assimp.stl

import glm.vec._4.d.Vec4d
import java.assimp.AiScene
import java.assimp.BaseImporter
import java.nio.ByteBuffer

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

        return
    }
}