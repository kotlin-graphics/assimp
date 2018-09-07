package assimp

import java.io.BufferedReader
import java.io.InputStream
import java.nio.*

interface IOStream {

    val path : String

    val filename: String

    fun read() : InputStream

    fun reader() : BufferedReader

    val parentPath : String

    /**
     * length of the IOStream in bytes
     */
    val length: Long

    /**
     * reads the ioStream into a byte buffer.
     * The byte order of the buffer is be [ByteOrder.nativeOrder].
     */
    fun readBytes(): ByteBuffer

	val osSystem: IOSystem
}