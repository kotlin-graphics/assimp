package assimp

import glm_.*
import java.nio.ByteBuffer

/** signed variant of strtoul10 */
fun ByteBuffer.strtol10(begin: Int, end: Int): Int {
    val bytes = ByteArray(end - begin) { get(begin + it) }
    return String(bytes).i
}

/** Convert a string in decimal format to a number */
fun ByteBuffer.strtoul10(beginOut: IntArray): Int {
    var value = 0
    var begin = beginOut[0]
    while (true) {
        val c = get(begin).c
        if (c < '0' || c > '9') break
        value = value * 10 + (get(begin).c - '0')
        ++begin
    }
    if (beginOut[1] != 0) beginOut[1] = begin
    return value
}

/** Special version of the function, providing higher accuracy and safety
 *  It is mainly used by fast_atof to prevent ugly and unwanted integer overflows. */
fun ByteBuffer.strtoul10_64(beginOutMax: IntArray): Long {
    var cur = 0
    var value = 0L
    var begin = beginOutMax[0]
    var c = get(begin).c
    if (c < '0' || c > '9') throw Error("The string starting with \"$c\" cannot be converted into a value.")
    while (true) {
        if (c < '0' || c > '9') break
        val newValue = value * 10 + (c - '0')
        // numeric overflow, we rely on you
        if (newValue < value) logger.warn { "Converting the string starting with \"$c\" into a value resulted in overflow." }
        //throw std::overflow_error();
        value = newValue
        c = get(++begin).c
        ++cur
        if (beginOutMax[2] != 0 && beginOutMax[2] == cur) {
            if (beginOutMax[1] != 0) { /* skip to end */
                while (c in '0'..'9')
                    c = get(++begin).c
                beginOutMax[1] = begin
            }
            return value
        }
    }
    if (beginOutMax[1] != 0) beginOutMax[1] = begin
    if (beginOutMax[2] != 0) beginOutMax[2] = cur
    return value
}

/** signed variant of strtoul10_64 */
fun ByteBuffer.strtol10_64(begin: Int, end: Int): Long {
    val bytes = ByteArray(end - begin) { get(begin + it) }
    return String(bytes).L
}

fun ByteBuffer.fast_atof(begin: Int, end: Int): Float {
    val bytes = ByteArray(end - begin) { get(begin + it) }
    return String(bytes).f
}

fun ByteBuffer.strncmp(string: String, ptr: Int = pos, length: Int = string.length): Boolean {
    for (i in 0 until length)
        if (get(ptr + i).c != string[i])
            return false
    return true
}


fun strtoul10(input: String, ptr: Int): uint {

    var value = 0
    var i = ptr

    while (true) {
        if (input[i] < '0' || input[i] > '9') break
        value = value * 10 + (input[i] - '0')
        ++i
    }
    return value
}