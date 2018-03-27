package assimp

import java.io.BufferedReader
import java.io.Reader

interface IOStream {
    val path : String

    val filename: String

    fun read() : BufferedReader

    fun parentPath() : String
}