package assimp

import java.io.BufferedReader
import java.io.InputStream
import java.nio.file.Path

interface IOStream {

    val path : Path

    val filename: String

    fun read() : InputStream

    fun reader() : BufferedReader

    fun parentPath() : String
}