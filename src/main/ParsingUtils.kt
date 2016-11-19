package main

import main.b
import java.nio.ByteBuffer

/**
 * Created by elect on 15/11/2016.
 */

fun ByteBuffer.skipSpaces(): Boolean {
    var value = get()
    while (value == ' '.b || value == '\t'.b) value = get()
    return !value.isLineEnd()
}

fun Byte.isLineEnd() = this == '\r'.b || this == '\n'.b || this == 0.b /* '\0'.b */ || this == 12.b /* '\f'.b */

fun Byte.isSpaceOrNewLine() = this.isSpace() || this.isLineEnd()
fun Char.isNewLine() = this == '\n'

fun Byte.isSpace() = this == ' '.b || this == '\t'.b

fun ByteBuffer.cmp(string: String) = string.all { get() == it.b }

fun main(args: Array<String>) {
    println(' '.b)
    println('\t'.b)
    println('\r'.b)
    println('\n'.b)
}