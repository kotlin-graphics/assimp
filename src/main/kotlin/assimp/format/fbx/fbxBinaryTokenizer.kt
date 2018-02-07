import assimp.format.fbx.Token
import assimp.format.fbx.TokenType
import assimp.format.fbx.Util
import assimp.pos
import glm_.*
import java.nio.ByteBuffer
import kotlin.reflect.KMutableProperty0

/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/

/** @file  FBXBinaryTokenizer.cpp
 *  @brief Implementation of a fake lexer for binary fbx files -
 *    we emit tokens so the parser needs almost no special handling
 *    for binary files.
 */

//enum Flag
//{
//   e_unknown_0 = 1 << 0,
//   e_unknown_1 = 1 << 1,
//   e_unknown_2 = 1 << 2,
//   e_unknown_3 = 1 << 3,
//   e_unknown_4 = 1 << 4,
//   e_unknown_5 = 1 << 5,
//   e_unknown_6 = 1 << 6,
//   e_unknown_7 = 1 << 7,
//   e_unknown_8 = 1 << 8,
//   e_unknown_9 = 1 << 9,
//   e_unknown_10 = 1 << 10,
//   e_unknown_11 = 1 << 11,
//   e_unknown_12 = 1 << 12,
//   e_unknown_13 = 1 << 13,
//   e_unknown_14 = 1 << 14,
//   e_unknown_15 = 1 << 15,
//   e_unknown_16 = 1 << 16,
//   e_unknown_17 = 1 << 17,
//   e_unknown_18 = 1 << 18,
//   e_unknown_19 = 1 << 19,
//   e_unknown_20 = 1 << 20,
//   e_unknown_21 = 1 << 21,
//   e_unknown_22 = 1 << 22,
//   e_unknown_23 = 1 << 23,
//   e_flag_field_size_64_bit = 1 << 24, // Not sure what is
//   e_unknown_25 = 1 << 25,
//   e_unknown_26 = 1 << 26,
//   e_unknown_27 = 1 << 27,
//   e_unknown_28 = 1 << 28,
//   e_unknown_29 = 1 << 29,
//   e_unknown_30 = 1 << 30,
//   e_unknown_31 = 1 << 31
//};
//
//bool check_flag(uint32_t flags, Flag to_check)
//{
//	return (flags & to_check) != 0;
//}

// signal tokenization error, this is always unrecoverable. Throws DeadlyImportError.
fun tokenizeError(message: String, offset: Int): Nothing = throw Error(Util.addOffset("FBX-Tokenize", message, offset))

fun tokenizeError(message: String, input: ByteBuffer): Nothing = tokenizeError(message, input.pos)

fun ByteBuffer.readString(beginOut: KMutableProperty0<Int>? = null, endOut: KMutableProperty0<Int>? = null, longLength: Boolean = false, allowNull: Boolean = false): Int {
    val lenLen = if (longLength) 4 else 1
    if (remaining() < lenLen) tokenizeError("cannot ReadString, out of bounds reading length", this)

    val length = if (longLength) int else get().i

    if (remaining() < length) tokenizeError("cannot ReadString, length is out of bounds", this)

    val b = pos
    beginOut?.set(pos)
    pos += length

    endOut?.set(pos)

    if (!allowNull)
        for (i in 0 until length)
            if (get(b + i) == 0.b)
                tokenizeError("failed ReadString, unexpected NUL character in string", this)

    return length
}

fun ByteBuffer.readData(beginOut: KMutableProperty0<Int>, endOut: KMutableProperty0<Int>) {
    if (remaining() < 1) tokenizeError("cannot ReadData, out of bounds reading length", this)

    val type = get(pos).c
    beginOut.set(pos++)

    when (type) {
    // 16 bit int
        'Y' -> pos += Short.BYTES
    // 1 bit bool flag (yes/no)
        'C' -> pos++
    // 32 bit int or float
        'I', 'F' -> pos += Int.BYTES
    // double
        'D' -> pos += Double.BYTES
    // 64 bit int
        'L' -> pos += Long.BYTES
    // note: do not write cursor += ReadWord(...cursor) as this would be UB
    // raw binary data
        'R' -> {
            val length = int
            pos += length
        }
    // TODO: what is the 'b' type code? Right now we just skip over it. Take the full range we could get
        'b' -> Unit
    // array of *
        'f', 'd', 'l', 'i' -> {
            val length = int
            val encoding = int
            val compLen = int

            // compute length based on type and check against the stored value
            if (encoding == 0) {
                val stride = when (type) {
                    'f', 'i' -> 4
                    'd', 'l' -> 8
                    else -> throw Error("invalid type")
                }
                if (length * stride != compLen) tokenizeError("cannot ReadData, calculated data stride differs from what the file claims", this)
            }
            // zip/deflate algorithm (encoding==1)? take given length. anything else? die
            else if (encoding != 1) tokenizeError("cannot ReadData, unknown encoding", this)
            pos += compLen
        }
    // string
        'S' -> readString(longLength = true, allowNull = true) // 0 characters can legally happen in such strings
        else -> tokenizeError("cannot ReadData, unexpected type code: $type", this)
    }

    if (pos > capacity()) tokenizeError("cannot ReadData, the remaining size is too small for the data type: $type", this)

    // the type code is contained in the returned range
    endOut.set(pos)
}

fun readScope(outputTokens: ArrayList<Token>, input: ByteBuffer, end: Int, is64bits: Boolean): Boolean {

    // the first word contains the offset at which this block ends
    val endOffset = if (is64bits) input.double.i else input.int

    /*  we may get 0 if reading reached the end of the file - fbx files have a mysterious extra footer which I don't
        know how to extract any information from, but at least it always starts with a 0. */
    if (endOffset == 0) return false

    if (endOffset > end) tokenizeError("block offset is out of range", input)
    else if (endOffset < input.pos) tokenizeError("block offset is negative out of range", input)

    // the second data word contains the number of properties in the scope
    val propCount = if (is64bits) input.double.i else input.int

    // the third data word contains the length of the property list
    val propLength = if (is64bits) input.double.i else input.int

    // now comes the name of the scope/key
    input.readString(::sBeg, ::sEnd)

    outputTokens.add(Token(sBeg, sEnd, TokenType.KEY, input.pos))

    // now come the individual properties
    val beginCursor = input.pos
    repeat(propCount) {
        val begin = input.pos
        input.readData(::sBeg, ::sEnd)

        outputTokens.add(Token(sBeg, sEnd, TokenType.DATA, input.pos))

        if (it != propCount - 1) outputTokens.add(Token(input.pos, input.pos + 1, TokenType.COMMA, input.pos))
    }

    if (input.pos - beginCursor != propLength) tokenizeError("property length not reached, something is wrong", input)

    /*  at the end of each nested block, there is a NUL record to indicate that the sub-scope exists
        (i.e. to distinguish between P: and P : {})
        this NUL record is 13 bytes long on 32 bit version and 25 bytes long on 64 bit. */
    val sentinelBlockLength = (if (is64bits) Long.BYTES else Int.BYTES) * 3 + 1

    if (input.pos < endOffset) {
        if (endOffset - input.pos < sentinelBlockLength) tokenizeError("insufficient padding bytes at block end", input)

        outputTokens.add(Token(input.pos, input.pos + 1, TokenType.OPEN_BRACKET, input.pos))

        // XXX this is vulnerable to stack overflowing ..
        while (input.pos < endOffset - sentinelBlockLength)
            readScope(outputTokens, input, endOffset - sentinelBlockLength, is64bits)
        outputTokens.add(Token(input.pos, input.pos + 1, TokenType.CLOSE_BRACKET, input.pos))

        for (i in 0 until sentinelBlockLength)
            if (input.get(input.pos + i) != 0.b)
                tokenizeError("failed to read nested block sentinel, expected all bytes to be 0", input)
        input.pos += sentinelBlockLength
    }

    if (input.pos != endOffset) tokenizeError("scope length not reached, something is wrong", input)

    return true
}

private var sBeg = -1
private var sEnd = -1

// TODO: Test FBX Binary files newer than the 7500 version to check if the 64 bits address behaviour is consistent
fun tokenizeBinary(outputTokens: ArrayList<Token>, input: ByteBuffer) {

    if (input.size < 0x1b) tokenizeError("file is too short", 0)

    /*Result ignored*/ input.get()
    /*Result ignored*/ input.get()
    /*Result ignored*/ input.get()
    /*Result ignored*/ input.get()
    /*Result ignored*/ input.get()
    val version = input.int
    val is64bits = version >= 7500
    while (true)
        if (!readScope(outputTokens, input, input.capacity(), is64bits))
            break
}


