package assimp

import java.io.File

/** Interface to the file system. */
interface IOSystem {

    fun exists(file: String): Boolean

    fun open(file : String): IOStream

    fun close(ioStream: IOStream) = Unit // TODO unused ?

    val osSeparator: String get() = File.separator
}