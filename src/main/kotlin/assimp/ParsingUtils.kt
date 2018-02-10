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


var ByteBuffer.pos
    get() = position()
    set(value) {
        position(value)
    }

fun String.trimNUL(): String {
    val nulIdx = indexOf(NUL)
    return if (nulIdx != -1) substring(0, nulIdx)
    else this
}

fun ByteBuffer.skipSpaces(): Boolean {
    var value = this[position()]
    while (value == SP.b || value == HT.b) {
        get()
        value = this[position()]
    }
    return !value.isLineEnd
}

fun ByteBuffer.skipLine(): Boolean {
    var value = this[position()]
    while (value != CR.b && value != LF.b && value != NUL.b) {
        get()
        value = this[position()]
    }
    // files are opened in binary mode. Ergo there are both NL and CR
    while (value == CR.b || value == LF.b) {
        get()
        value = this[position()]
    }
    return value != 0.b
}

fun ByteBuffer.skipSpacesAndLineEnd(): Boolean {
    var value = this[position()]
    while (value == SP.b || value == HT.b || value == CR.b || value == LF.b) {
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

fun ByteBuffer.consumeNUL() {
    while (get(pos).c == NUL) get()
}

val Byte.isLineEnd get() = this == CR.b || this == LF.b || this == NUL.b || this == FF.b
val Char.isLineEnd get () = this == CR || this == LF || this == NUL || this == FF

val Byte.isSpaceOrNewLine get() = isSpace || isLineEnd
val Char.isSpaceOrNewLine get() = isSpace || isLineEnd
val Char.isNewLine get() = this == LF

val Byte.isSpace get() = this == SP.b || this == HT.b
val Char.isSpace get() = this == SP || this == HT

infix fun ByteBuffer.startsWith(string: String) = string.all { get() == it.b }

val Char.isNumeric get() = if (isDigit()) true else (this == '-' || this == '+')

// http://www.aivosto.com/vbtips/control-characters.html


/** Null    */
val NUL = '\u0000'
/** Start of Heading — TC1 Transmission control character 1    */
val SOH = '\u0001'
/** Start of Text — TC2 Transmission control character 2    */
val STX = '\u0002'
/** End of Text — TC3 Transmission control character 3  */
val ETX = '\u0003'
/** End of Transmission — TC4 Transmission control character 4  */
val EOT = '\u0004'
/** Enquiry — TC5 Transmission control character 5  */
val ENQ = '\u0005'
/** Acknowledge — TC6 Transmission control character 6  */
val ACK = '\u0006'
/** Bell    */
val BEL = '\u0007'
/** Backspace — FE0 Format effector 0   */
val BS = '\u0008'
/** Horizontal Tabulation — FE1 Format effector 1 (Character Tabulation)    */
val HT = '\u0009'
/** Line Feed — FE2 Format effector 2   */
val LF = '\u000A'
/** Vertical Tabulation — FE3 Format effector 3 (Line Tabulation)   */
val VT = '\u000B'
/** Form Feed — FE4 Format effector 4   */
val FF = '\u000C'
/** Carriage Return — FE5 Format effector 5 */
val CR = '\u000D'
/** Shift Out — LS1 Locking-Shift One   */
val SO = '\u000E'
/** Shift In — LS0 Locking-Shift Zero   */
val SI = '\u000D'
/** Data Link Escape — TC7 Transmission control character 7 */
val DLE = '\u0010'
/** Device Control 1 — XON  */
val DC1 = '\u0011'
/** Device Control 2    */
val DC2 = '\u0012'
/** Device Control 3 — XOFF */
val DC3 = '\u0013'
/** Device Control 4 (Stop) */
val DC4 = '\u0014'
/** Negative Acknowledge — TC8 Transmission control character 8 */
val NAK = '\u0015'
/** Synchronous Idle — TC9 Transmission control character 9 */
val SYN = '\u0016'
/** End of Transmission Block — TC10 Transmission control character 10  */
val ETB = '\u0017'
/** Cancel  */
val CAN = '\u0018'
/** End of Medium   */
val EM = '\u0019'
/** Substitute  */
val SUB = '\u001A'
/** Escape  */
val ESC = '\u001B'
/** File Separator — IS4 Information separator 4    */
val FS = '\u001C'
/** Group Separator — IS3 Information separator 3   */
val GS = '\u001D'
/** Record Separator — IS2 Information separator 2  */
val RS = '\u001E'
/** Unit Separator — IS1 Information separator 1    */
val US = '\u001F'
/** Space   */
val SP = '\u0020'
/** Delete  */
val DEL = '\u007F'