package assimp

/** Interface to the file system. */
interface IOSystem {
    fun Exists(pFile: String): Boolean

    fun Open(pFile : String): IOStream

    fun Close(ioStream: IOStream) = Unit //unused ?

    fun getOsSeperator() = "/"
}