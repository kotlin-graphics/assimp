package assimp

import glm_.b
import glm_.c
import glm_.mat4x4.Mat4
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Created by elect on 15/11/2016.
 */

/**
 * Legend:
 * '\0' -> 0
 * '\f' -> 12
 */

fun ByteBuffer.skipSpaces(): Boolean {
    var value = this[position()]
    while (value == ' '.b || value == '\t'.b) {
        get()
        value = this[position()]
    }
    return !value.isLineEnd
}

fun ByteBuffer.skipLine(): Boolean {
    var value = this[position()]
    while (value != '\r'.b && value != '\n'.b && value != 0.b) {
        get()
        value = this[position()]
    }
    // files are opened in binary mode. Ergo there are both NL and CR
    while (value == '\r'.b || value == '\n'.b) {
        get()
        value = this[position()]
    }
    return value != 0.b
}

fun ByteBuffer.skipSpacesAndLineEnd(): Boolean {
    var value = this[position()]
    while (value == ' '.b || value == '\t'.b || value == '\r'.b || value == '\n'.b) {
        get()
        // check if we are at the end of file, e.g: ply
        if (remaining() > 0) value = this[position()]
        else return true
    }
    return value != 0.b
}

fun ByteBuffer.nextWord(): String {
    skipSpaces()
    val bytes = ArrayList<Byte>()
    while (!this[position()].isSpaceOrNewLine) bytes.add(get())
    return String(bytes.toByteArray())
}

fun ByteBuffer.restOfLine(): String {
    val bytes = ArrayList<Byte>()
    while (!this[position()].isLineEnd) bytes.add(get())
    return String(bytes.toByteArray())
}

val Byte.isLineEnd get() = this == '\r'.b || this == '\n'.b || this == 0.b /* '\0' */ || this == 12.b /* '\f' */
val Char.isLineEnd get () = this == '\r' || this == '\n' || this == '\u0000' /* '\0' */ || this == 12.c /* '\f' */

val Byte.isSpaceOrNewLine get() = isSpace || isLineEnd
val Char.isSpaceOrNewLine get() = this == ' ' || isLineEnd
val Char.isNewLine get() = this == '\n'

val Byte.isSpace get() = this == ' '.b || this == '\t'.b

infix fun ByteBuffer.startsWith(string: String) = string.all { get() == it.b }

val Char.isNumeric get() = if (isDigit()) true else (this == '-' || this == '+')

fun main(args: Array<String>) {
    println(' '.b)
    println('\t'.b)
    println('\r'.b)
    println('\n'.b)
}