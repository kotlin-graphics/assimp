package assimp

import java.io.File

/** Interface to the file system. */
interface IOSystem {
    fun exists(pFile: String): Boolean

    fun open(pFile : String): IOStream

    fun close(ioStream: IOStream) = Unit //unused ?

    fun getOsSeperator() = File.separator
}