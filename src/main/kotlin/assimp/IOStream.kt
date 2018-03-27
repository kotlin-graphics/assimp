package assimp

import java.io.BufferedReader
import java.io.Reader

interface IOStream {
    val name : String

    fun read() : BufferedReader

    fun parentPath() : String
}