package ply

import BaseImporter
import AiImporterDesc
import AiScene
import b
import nextWord
import s
import skipLine
import skipSpacesAndLineEnd
import startsWith
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.FileSystemException

/**
 * Created by elect on 10/12/2016.
 */

class PlyLoader : BaseImporter() {

    companion object {

        val desc = AiImporterDesc(
                mName = "Stanford Polygon Library (PLY) Importer",
                mFlags = AiImporterFlags.SupportTextFlavour or AiImporterFlags.SupportTextFlavour,
                mFileExtensions = "ply"
        )
    }

    // ------------------------------------------------------------------------------------------------
    // Returns whether the class can handle the format of the given file.
    override fun canRead(pFile: URI, checkSig: Boolean): Boolean {

        val extension = pFile.s.substring(pFile.s.lastIndexOf('.') + 1)

        if (extension == "ply")
            return true

        return false
    }

    // ------------------------------------------------------------------------------------------------
    // Imports the given file into the given scene structure.
    override fun internReadFile(pFile: URI, pScene: AiScene) {

        val file = File(pFile)

        // Check whether we can read from the file
        if (!file.canRead()) throw FileSystemException("Failed to open PLY file $pFile.")

        // allocate storage and copy the contents of the file to a memory buffer
        val fileChannel = RandomAccessFile(file, "r").channel
        val mBuffer2 = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        // the beginning of the file must be PLY - magic, magic
        if (mBuffer2.nextWord().toLowerCase() != "ply") throw Error("Invalid .ply file: Magic number 'ply' is no there")

        mBuffer2.skipSpacesAndLineEnd()

        // determine the format of the file data
        val sPlyDom = DOM()
        if(mBuffer2.nextWord() == "format") {
            if(mBuffer2.nextWord() == "ascii") {
                mBuffer2.skipLine()
                if(!DOM.parseInstance(mBuffer2, sPlyDom))
                    throw Error("Invalid .ply file: Unable to build DOM (#1)")
            } else {
                // revert ascii
                mBuffer2.position(mBuffer2.position() - "ascii".length)
                if (mBuffer2.startsWith("binary_")) {

                    val bIsBE = mBuffer2.get(mBuffer2.position()) == 'b'.b || mBuffer2.get(mBuffer2.position()) == 'B'.b
                    mBuffer2.order(if(bIsBE) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
                    // skip the line, parse the rest of the header and build the DOM
                    mBuffer2.skipLine()
                    if(!DOM.parseInstanceBinary(mBuffer2, sPlyDom))
                        throw Error("Invalid .ply file: Unable to build DOM (#2)")
                }
            }
        }
    }
}