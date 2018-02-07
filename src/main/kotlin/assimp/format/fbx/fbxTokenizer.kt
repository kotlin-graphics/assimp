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

package assimp.format.fbx

import assimp.*
import glm_.c
import glm_.f
import glm_.size
import java.nio.ByteBuffer
import kotlin.reflect.KMutableProperty0


// tab width for logging columns
val ASSIMP_FBX_TAB_WIDTH = 4

/** Rough classification for text FBX tokens used for constructing the basic scope hierarchy. */
enum class TokenType {
    /** {   */
    OPEN_BRACKET,
    /** }   */
    CLOSE_BRACKET,
    /** '"blablubb"', '2', '*14' - very general token class, further processing happens at a later stage.   */
    DATA,
    BINARY_DATA,
    /** ,   */
    COMMA,
    /** blubb:  */
    KEY;

    val i = ordinal
}

/** Represents a single token in a FBX file. Tokens are classified by the TokenType enumerated types.
 *
 *  Offers iterator protocol. Tokens are immutable. */
class Token(
        var begin: Int,
        var end: Int,
        val type: TokenType,
        line: Int,
        val _column: Int = BINARY_MARKER
) {
    init {
        assert(begin != -1 && end != -1)

        if (_column == BINARY_MARKER)
            assert(end >= begin) // binary tokens may have zero length because they are sometimes dummies inserted by TokenizeBinary()
        else
            assert(end - begin > 0) // tokens must be of non-zero length
    }

    // full string copy for the sole purpose that it nicely appears in msvc's debugger window.
    var contents = ""

    var line = line
        get() {
            assert(!isBinary)
            return field
        }

    inline var offset: Int
        get() {
            assert(isBinary)
            return line
        }
        set(value) {
            line = value
        }

    val column = _column
        get() {
            assert(!isBinary)
            return field
        }

    val isBinary get() = _column == BINARY_MARKER

    override fun toString() = "$type, ${if (isBinary) "offset 0x$offset" else "line $line, col $column"}"

    val stringContents get() = String(ByteArray(end - begin, { buffer[begin + it] }))

    companion object {
        val BINARY_MARKER = -1
    }

    val parseAsString: String
        get() {
            if (type != TokenType.DATA) parseError("expected TOK_DATA token", this)

            if (isBinary) {
                if (buffer[begin].c != 'S') parseError("failed to parse S(tring), unexpected data type (binary)", this)
                // read string length
                val len = buffer.getInt(begin + 1)

                assert(end - begin == 5 + len)
                return String(ByteArray(len, { buffer[begin + 5 + it] }))
            }

            val length = end - begin
            if (length < 2) parseError("token is too short to hold a string", this)

            val s = buffer[begin].c
            val e = buffer[end - 1].c
            if (s != '"' || e != '"') parseError("expected double quoted string", this)

            return String(ByteArray(length - 2, { buffer[begin + 1 + it] }))
        }

    val parseAsInt: Int
        get() {
            if (type != TokenType.DATA) parseError("expected TOK_DATA token", this)

            if (isBinary) {
                if (buffer[begin].c != 'I') parseError("failed to parse I(nt), unexpected data type (binary)", this)

                return buffer.getInt(begin + 1)
            }

            assert(end - begin > 0)

            return buffer.strtol10(begin, end)
        }

    val parseAsId: Long
        get() {
            if (type != TokenType.DATA) parseError("expected TOK_DATA token", this)
            if (isBinary) {
                if (buffer[begin].c != 'L') parseError("failed to parse ID, unexpected data type, expected L(ong) (binary)", this)
                return buffer.getLong(begin + 1)
            }
            // XXX: should use size_t here
            val length = end - begin
            assert(length > 0)
            val beginOutMax = intArrayOf(begin, 0, length)
            val id = buffer.strtoul10_64(beginOutMax)
            if (beginOutMax[1] > end) parseError("failed to parse ID (text)", this)
            return id
        }

    val parseAsInt64: Long
        get() {
            if (type != TokenType.DATA) parseError("expected TOK_DATA token", this)
            if (isBinary) {
                if (buffer[begin].c != 'L') parseError("failed to parse Int64, unexpected data type", this)
                return buffer.getLong(begin + 1)
            }
            // XXX: should use size_t here
            val length = end - begin
            assert(length > 0)
            val beginOutMax = intArrayOf(begin, 0, length)
            val id = buffer.strtol10_64(begin, end)
            if (beginOutMax[1] > end) parseError("failed to parse Int64 (text)", this)
            return id
        }

    /** same as ID parsing, except there is a trailing asterisk  */
    val parseAsDim: Long
        get() {
            if (type != TokenType.DATA) parseError("expected TOK_DATA token", this)
            if (isBinary) {
                TODO()
                if (buffer[begin].c != 'L') parseError("failed to parse Int64, unexpected data type", this)
                return buffer.getLong(begin + 1)
            }
            if (buffer[begin].c != '*')
                parseError("expected asterisk before array dimension", this)

            val length = end - ++begin

            if (length == 0)
                parseError("expected valid integer number after asterisk", this)

            return buffer.strtol10_64(begin, end)
        }

    val parseAsFloat: Float
        get() {
            if (type != TokenType.DATA) parseError("expected TOK_DATA token", this)
            if (isBinary)
                return when (buffer.get(begin).c) {
                    'F' -> buffer.getFloat(begin + 1)
                    'D' -> buffer.getDouble(begin + 1).f
                    else -> parseError("failed to parse F(loat) or D(ouble), unexpected data type (binary)", this)
                }
            // need to copy the input string to a temporary buffer
            // first - next in the fbx token stream comes ',',
            // which fast_atof could interpret as decimal point.
//            #define MAX_FLOAT_LENGTH 31 TODO check
//            char temp [MAX_FLOAT_LENGTH + 1];
//            const size_t length = static_cast<size_t>(t.end() - t.begin());
//            std::copy(t.begin(), t.end(), temp);
//            temp[std::min(static_cast<size_t>(MAX_FLOAT_LENGTH), length)] = '\0';
            return buffer.fast_atof(begin, end).f
        }
//    val parseAsDim:
}

/** Main FBX tokenizer function. Transform input buffer into a list of preprocessed tokens.
 *
 *  Skips over comments and generates line and column numbers.
 *
 * @param outputTokens Receives a list of all tokens in the input data.
 * @param chars Textual input buffer to be processed, 0-terminated.
 * @throw Error if something goes wrong */
fun tokenize(outputTokens: ArrayList<Token>, input: ByteBuffer) {

//    assert(input.isNotEmpty())  // TODO

    // line and column numbers numbers are one-based
    var line = 1
    var column = 1

    var comment = false
    var inDoubleQuotes = false
    var pendingDataToken = false

    val chars = input

    var cur = 0
    var begin = true

    while (cur + 1 < chars.size) {

        if (!begin) column += if (chars[cur++].c == HT) ASSIMP_FBX_TAB_WIDTH else 1
        begin = false

        var c = chars[cur].c

        if (c.isLineEnd) {
            comment = false
            column = 0
            ++line
            // if we have another lineEnd at the next position (typically \f\n), move directly to next char (\n)
            if (cur + 1 < chars.size && chars[cur + 1].c.isLineEnd) c = chars[++cur].c
            continue
        }

        if (comment) continue

        if (inDoubleQuotes) {
            if (c == '\"') {
                inDoubleQuotes = false
                tokenEnd = cur

                processDataToken(outputTokens, chars, ::tokenBegin, ::tokenEnd, line, column)
                pendingDataToken = false
            }
            continue
        }
        if (c == '\"') {
            if (tokenBegin != -1) tokenizeError("unexpected double-quote", line, column)
            tokenBegin = cur
            inDoubleQuotes = true
            continue
        } else if (c == ';') {
            processDataToken(outputTokens, chars, ::tokenBegin, ::tokenEnd, line, column)
            comment = true
            continue
        } else if (c == '{') {
            processDataToken(outputTokens, chars, ::tokenBegin, ::tokenEnd, line, column)
            outputTokens += Token(cur, cur + 1, TokenType.OPEN_BRACKET, line, column)
            continue
        } else if (c == '}') {
            processDataToken(outputTokens, chars, ::tokenBegin, ::tokenEnd, line, column)
            outputTokens += Token(cur, cur + 1, TokenType.CLOSE_BRACKET, line, column)
            continue
        } else if (c == ',') {
            if (pendingDataToken)
                processDataToken(outputTokens, chars, ::tokenBegin, ::tokenEnd, line, column, TokenType.DATA, true)
            outputTokens += Token(cur, cur + 1, TokenType.COMMA, line, column)
            continue
        } else if (c == ':') {
            if (pendingDataToken)
                processDataToken(outputTokens, chars, ::tokenBegin, ::tokenEnd, line, column, TokenType.KEY, true)
            else tokenizeError("unexpected colon", line, column)
            continue
        }

        if (c.isSpaceOrNewLine) {
            if (tokenBegin != -1) {
                // peek ahead and check if the next token is a colon in which case this counts as KEY token.
                var type = TokenType.DATA
                var peek = cur
                var p = chars[peek].c
                while (p != NUL && p.isSpaceOrNewLine) {
                    if (p == ':') {
                        type = TokenType.KEY
                        cur = peek
                        break
                    }
                    p = chars[++peek].c
                }
                processDataToken(outputTokens, chars, ::tokenBegin, ::tokenEnd, line, column, type)
            }
            pendingDataToken = false
        } else {
            tokenEnd = cur
            if (tokenBegin == -1) tokenBegin = cur
            pendingDataToken = true
        }
    }
}

private var tokenBegin = -1
private var tokenEnd = -1

/** signal tokenization error, this is always unrecoverable. Throws Error.  */
fun tokenizeError(message: String, line: Int, column: Int): Nothing = throw Error(Util.addLineAndColumn("FBX-Tokenize", message, line, column))


//    /** Tokenizer function for binary FBX files.
//     *
//     *  Emits a token list suitable for direct parsing.
//     *
//     * @param output_tokens Receives a list of all tokens in the input data.
//     * @param input_buffer Binary input buffer to be processed.
//     * @param length Length of input buffer, in bytes. There is no 0-terminal.
//     * @throw DeadlyImportError if something goes wrong */
//    void TokenizeBinary(TokenList& output_tokens, const char* input, unsigned int length);

/** Process a potential data token up to 'cur', adding it to 'outputTokens'.   */
fun processDataToken(outputTokens: ArrayList<Token>, chars: ByteBuffer, start: KMutableProperty0<Int>, end: KMutableProperty0<Int>,
                     line: Int, column: Int, type: TokenType = TokenType.DATA, mustHaveToken: Boolean = false) {

    if (start() != -1 && end() != -1) {
        // sanity check:
        // tokens should have no whitespace outside quoted text and [start,end] should properly delimit the valid range.
        var inDoubleQuotes = false
        for (i in start()..end()) {
            val c = chars[i].c
            if (c == '\"')
                inDoubleQuotes = !inDoubleQuotes
            if (!inDoubleQuotes && c.isSpaceOrNewLine)
                tokenizeError("unexpected whitespace in token", line, column)
        }
        if (inDoubleQuotes)
            tokenizeError("non-terminated double quotes", line, column)
        outputTokens += Token(start(), end() + 1, type, line, column)
    } else if (mustHaveToken)
        tokenizeError("unexpected character, expected data token", line, column)

    start.set(-1)
    end.set(-1)
}