package assimp

import java.io.BufferedReader
import java.io.InputStream
import java.io.Reader

interface IOStream {
    val path : String

    val filename: String

    fun read() : InputStream

    fun reader() : BufferedReader

    fun parentPath() : String
}