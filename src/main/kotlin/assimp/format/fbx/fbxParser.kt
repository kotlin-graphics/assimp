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

import assimp.ASSIMP.DEBUG
import assimp.AiMatrix4x4
import assimp.AiVector3D
import glm_.*
import glm_.buffer.bufferBig
import java.nio.ByteBuffer
import java.util.zip.Inflater
import kotlin.reflect.KMutableProperty0
import assimp.format.fbx.TokenType as Tt


/** FBX data entity that consists of a key:value tuple.
 *
 *  Example:
 *  @verbatim
 *    AnimationCurve: 23, "AnimCurve::", "" {
 *        [..]
 *    }
 *  @endverbatim
 *
 *  As can be seen in this sample, elements can contain nested Scope as their trailing member.  **/
class Element(val keyToken: Token, parser: Parser) {

    val tokens = ArrayList<Token>()
    var compound: Scope? = null

    var begin = 0
    var type = ' '
    var count = 0

    init {
        lateinit var n: Token
        do {
            n = parser.advanceToNextToken() ?: parseError("unexpected end of file, expected closing bracket", parser.lastToken)
            if (n.type == Tt.DATA) {
                tokens.add(n)
                val prev = n
                n = parser.advanceToNextToken() ?: parseError("unexpected end of file, expected bracket, comma or key", parser.lastToken)
                val ty = n.type
                // some exporters are missing a comma on the next line
                if (ty == Tt.DATA && prev.type == Tt.DATA && (n.line == prev.line + 1)) {
                    tokens.add(n)
                    continue
                }
                if (ty != Tt.OPEN_BRACKET && ty != Tt.CLOSE_BRACKET && ty != Tt.COMMA && ty != Tt.KEY)
                    parseError("unexpected token; expected bracket, comma or key", n)
            }
            if (n.type == Tt.OPEN_BRACKET) {
                compound = Scope(parser)

                // current token should be a TOK_CLOSE_BRACKET
                n = parser.currentToken!!

                if (n.type != Tt.CLOSE_BRACKET) parseError("expected closing bracket", n)

                parser.advanceToNextToken()
                break
            }
        } while (n.type != Tt.KEY && n.type != Tt.CLOSE_BRACKET)
    }

    val scope get() = compound ?: parseError("expected compound scope", this)

    /** peek into an element and check if it contains a FBX property, if so return its name.    */
    val peekPropertyName: String
        get() {
            assert(keyToken.stringContents == "P")
            return if (tokens.size < 4) "" else tokens[0].parseAsString
        }

    /** read a typed property out of a FBX element. The return value is NULL if the property cannot be read. */
    fun readTypedProperty(): Property? {
        assert(keyToken.stringContents == "P")

        assert(tokens.size >= 5)
        val t = tokens[4]
        return when (tokens[1].parseAsString) {
            "KString" -> TypedProperty(t.parseAsString)
            "bool", "Bool" -> TypedProperty(t.parseAsInt != 0)
            "int", "Int", "enum", "Enum" -> TypedProperty(t.parseAsInt)
            "ULongLong" -> TypedProperty(t.parseAsId)
            "KTime" -> TypedProperty(t.parseAsInt64)
            "Vector3D", "ColorRGB", "Vector", "Color", "Lcl Translation", "Lcl Rotation", "Lcl Scaling" ->
                TypedProperty(AiVector3D({ tokens[4 + it].parseAsFloat }))
            "double", "Number", "Float", "FieldOfView", "UnitScaleFactor" -> TypedProperty(t.parseAsFloat)
            else -> null
        }
    }

    /** get token at a particular index */
    operator fun get(index: Int) = tokens.getOrNull(index) ?: parseError("missing token at index $index", this)

    /** read a 4x4 matrix from an array of 16 floats */
    fun readMatrix(): AiMatrix4x4 {
        val values = ArrayList<Float>()
        parseFloatsDataArray(values)

        if (values.size != 16) parseError("expected 16 matrix elements")

        return AiMatrix4x4(values)
    }

    inline fun <reified T> parseVectorDataArray(out: ArrayList<T>) = when (T::class) {
        AiVector3D::class -> parseVec3DataArray(out as ArrayList<AiVector3D>)
        else -> throw Error()
    }

    /** read an array of floats */
    fun parseFloatsDataArray(out: ArrayList<Float>) {
        out.clear()
        if (tokens.isEmpty()) parseError("unexpected empty element", this)
        if (tokens[0].isBinary) {
            begin = tokens[0].begin
            val end = tokens[0].end

            readBinaryDataArrayHead(::begin, end, ::type, ::count)

            if (count == 0) return

            if (type != 'd' && type != 'f') parseError("expected float or double array (binary)", this)

            val buff = readBinaryDataArray(type, count, ::begin, end)

            assert(begin == end && buff.size == count * if (type == 'd') 8 else 4)

            if (type == 'd') {
                val d = buff.asDoubleBuffer()
                for (i in 0 until count) out += d[i].f
            } else if (type == 'f') {
                val f = buff.asFloatBuffer()
                for (i in 0 until count) out += f[i]
            }
            return
        }
        TODO()
//        val dim = tokens[0].parseAsDim

        // see notes in ParseVectorDataArray()
//        out.reserve(dim)

//        const Scope & scope = GetRequiredScope (el)
//        const Element & a = GetRequiredElement (scope, "a", &el)
//
//        for (TokenList:: const_iterator it = a . Tokens ().begin(), end = a.Tokens().end(); it != end; ) {
//            const float ival = ParseTokenAsFloat(** it ++)
//            out.push_back(ival)
//        }
    }

    /** read an array of ints */
    fun parseIntsDataArray(out: ArrayList<Int>) {
        out.clear()
        if (tokens.isEmpty()) parseError("unexpected empty element", this)
        if (tokens[0].isBinary) {
            begin = tokens[0].begin
            val end = tokens[0].end

            readBinaryDataArrayHead(::begin, end, ::type, ::count)

            if (count == 0) return

            if (type != 'i') parseError("expected int array (binary)", this)

            val buff = readBinaryDataArray(type, count, ::begin, end)

            assert(begin == end && buff.size == count * if (type == 'd') 8 else 4)

            val ip = buff.asIntBuffer()
            for (i in 0 until count) out += ip[i]
            return
        }
        val dim = tokens[0].parseAsDim

        // see notes in ParseVectorDataArray()
        out.ensureCapacity(dim.i)

        val a = getRequiredElement(scope, "a", this)

        for (it in a.tokens)
            out += it.parseAsInt
    }

    /** read an array of int64_ts   */
    fun parseLongsDataArray(out: ArrayList<Long>) {
        out.clear()
        if (tokens.isEmpty()) parseError("unexpected empty element", this)
        if (tokens[0].isBinary) {
            begin = tokens[0].begin
            val end = tokens[0].end

            readBinaryDataArrayHead(::begin, end, ::type, ::count)

            if (count == 0) return

            if (type != 'l') parseError("expected long array (binary)")

            val buff = readBinaryDataArray(type, count, ::begin, end)

            assert(begin == end && buff.size == count * 8)

            val ip = buff.asLongBuffer()
            for (i in 0 until count) out += ip[i]

            return
        }
        TODO()
//        const size_t dim = ParseTokenAsDim(*tok[0])
//
//        // see notes in ParseVectorDataArray()
//        out.reserve(dim)
//
//        const Scope & scope = GetRequiredScope (el)
//        const Element & a = GetRequiredElement (scope, "a", &el)
//
//        for (TokenList:: const_iterator it = a . Tokens ().begin(), end = a.Tokens().end(); it != end;) {
//            const int64_t ival = ParseTokenAsInt64(** it ++)
//
//            out.push_back(ival)
//        }
    }

    /** read an array of float3 tuples */
    fun parseVec3DataArray(out: ArrayList<AiVector3D>) { // TODO consider returning directly `out`
        out.clear()

        if (tokens.isEmpty()) parseError("unexpected empty element", this)

        if (tokens[0].isBinary) {
            begin = tokens[0].begin
            val end = tokens[0].end

            readBinaryDataArrayHead(::begin, end, ::type, ::count)

            if (count % 3 != 0) parseError("number of floats is not a multiple of three (3) (binary)", this)

            if (count == 0) return

            if (type != 'd' && type != 'f') parseError("expected float or double array (binary)", this)

            val buff = readBinaryDataArray(type, count, ::begin, end)

            assert(begin == end && buff.size == count * if (type == 'd') 8 else 4)

            val count3 = count / 3
            out.ensureCapacity(count3)

            if (type == 'd') {
                val d = buff.asDoubleBuffer()
                for (i in 0 until count3) out += AiVector3D(d.get(), d.get(), d.get())
                // for debugging
                /*for ( size_t i = 0; i < out.size(); i++ ) {
                    aiVector3D vec3( out[ i ] );
                    std::stringstream stream;
                    stream << " vec3.x = " << vec3.x << " vec3.y = " << vec3.y << " vec3.z = " << vec3.z << std::endl;
                    DefaultLogger::get()->info( stream.str() );
                }*/
            } else if (type == 'f') {
                val f = buff.asFloatBuffer()
                for (i in 0 until count3) out += AiVector3D(f.get(), f.get(), f.get())
            }
            return
        }

        val dim = tokens[0].parseAsDim

        // may throw bad_alloc if the input is rubbish, but this need
        // not to be prevented - importing would fail but we wouldn't
        // crash since assimp handles this case properly.
        out.ensureCapacity(dim.i)

        val a = getRequiredElement(scope, "a", this)

        if (a.tokens.size % 3 != 0)
            parseError("number of floats is not a multiple of three (3)", this)

        var i = 0
        while (i < a.tokens.size)
            out += AiVector3D(
                    x = a.tokens[i++].parseAsFloat,
                    y = a.tokens[i++].parseAsFloat,
                    z = a.tokens[i++].parseAsFloat)
    }

    /** read the type code and element count of a binary data array and stop there */
    fun readBinaryDataArrayHead(begin: KMutableProperty0<Int>, end: Int, type: KMutableProperty0<Char>,
                                count: KMutableProperty0<Int>) {
        if (end - begin() < 5) parseError("binary data array is too short, need five (5) bytes for type signature and element count", this)

        // data type
        type.set(buffer[begin()].c)

        // read number of elements
        val len = buffer.getInt(begin() + 1)

        count.set(len)
        begin.set(begin() + 5)
    }

    /** read binary data array, assume cursor points to the 'compression mode' field (i.e. behind the header) */
    fun readBinaryDataArray(type: Char, count: Int, begin: KMutableProperty0<Int>, end: Int): ByteBuffer {
        val encMode = buffer.getInt(begin())
        begin.set(begin() + Int.BYTES)

        // next comes the compressed length
        val compLen = buffer.getInt(begin())
        begin.set(begin() + Int.BYTES)

        assert(begin() + compLen == end)

        // determine the length of the uncompressed data by looking at the type signature
        val stride = when (type) {
            'f', 'i' -> Float.BYTES
            'd', 'l' -> Double.BYTES
            else -> throw Error()
        }

        val fullLength = stride * count
        val buff = bufferBig(fullLength)

        if (encMode == 0) {
            assert(fullLength == compLen)
            // plain data, no compression
            for (i in 0 until fullLength)
                buff[i] = buffer[begin() + i]
        } else if (encMode == 1) {
            val input = ByteArray(fullLength, { buffer.get(begin() + it) })
            val decompresser = Inflater().apply { setInput(input) }
            val result = ByteArray(fullLength)
            val resultLength = decompresser.inflate(result)
            decompresser.end()
            for (i in 0 until resultLength)
                buff[i] = result[i]
        } else // runtime check for this happens at tokenization stage
            if (DEBUG) throw Error()

        begin.set(begin() + compLen)
        assert(begin() == end)

        return buff
    }
}


/** FBX data entity that consists of a 'scope', a collection of not necessarily unique #Element instances.
 *
 *  Example:
 *  @verbatim
 *    GlobalSettings:  {
 *        Version: 1000
 *        Properties70:
 *        [...]
 *    }
 *  @endverbatim  */
class Scope(parser: Parser, topLevel: Boolean = false) {

    val elements = HashMap<String, ArrayList<Element>>()

    init {
        if (!topLevel) {
            val t = parser.currentToken!!
            if (t.type != Tt.OPEN_BRACKET) parseError("expected open bracket", t)
        }

        var n = parser.advanceToNextToken() ?: parseError("unexpected end of file")

        // note: empty scopes are allowed
        while (n.type != Tt.CLOSE_BRACKET) {
            if (n.type != Tt.KEY) parseError("unexpected token, expected TOK_KEY", n)

            elements.getOrPut(n.stringContents, ::arrayListOf) += Element(n, parser)

            // Element() should stop at the next Key token (or right after a Close token)
            val t = parser.currentToken
            if (t == null) {
                if (topLevel) break
                parseError("unexpected end of file", parser.lastToken)
            } else n = t
        }
    }

    operator fun get(index: String) = elements[index]?.get(0)
    fun getArray(index: String) = elements[index]!!

    fun findElementCaseInsensitive(elementName: String) = elements[elementName.toLowerCase()]

    fun getCollection(index: String) = elements[index] ?: arrayListOf()

    infix fun hasElement(index: String) = get(index) != null
}


/** FBX parsing class, takes a list of input tokens and generates a hierarchy
 *  of nested #Scope instances, representing the fbx DOM.*/
class Parser
/** Parse given a token list. Does not take ownership of the tokens -
 *  the objects must persist during the entire parser lifetime */
constructor(val tokens: ArrayList<Token>, val isBinary: Boolean) {

    var last: Token? = null
    var current: Token? = null
    var cursor = 0
    var root = Scope(this, true)

    fun advanceToNextToken(): Token? {
        last = current
        current = tokens.getOrNull(cursor++)
        return current
    }

    val lastToken get() = last
    val currentToken get() = current
}


/** signal parse error, this is always unrecoverable. Throws Error. */
fun parseError(message: String, element: Element? = null): Nothing {
    element?.let { parseError(message, it.keyToken) }
    throw Error("FBX-Parser $message")
}

fun parseError(message: String, token: Token?): Nothing {
    token?.let { throw Error(Util.addTokenText("FBX-Parser", message, token)) }
    parseError(message)
}

// Initially, we did reinterpret_cast, breaking strict aliasing rules.
// This actually caused trouble on Android, so let's be safe this time.
// https://github.com/assimp/assimp/issues/24
//template <typename T>
//T SafeParse(const char* data , const char* end) {
//    // Actual size validation happens during Tokenization so
//    // this is valid as an assertion.
//    ai_assert(static_cast<size_t>(end - data) >= sizeof(T))
//    T result = static_cast < T >(0)
//    ::memcpy(& result, data, sizeof(T))
//    return result
//}

/* token parsing - this happens when building the DOM out of the parse-tree*/
//uint64_t ParseTokenAsID(const Token& t, const char*& err_out);
//size_t ParseTokenAsDim(const Token& t, const char*& err_out);
//
//float ParseTokenAsFloat(const Token& t, const char*& err_out);
//int ParseTokenAsInt(const Token& t, const char*& err_out);
//int64_t ParseTokenAsInt64(const Token& t, const char*& err_out);
//std::string ParseTokenAsString(const Token& t, const char*& err_out);
//
//
///* wrapper around ParseTokenAsXXX() with DOMError handling */
//uint64_t ParseTokenAsID(const Token& t);
//size_t ParseTokenAsDim(const Token& t);
//float ParseTokenAsFloat(const Token& t);
//int ParseTokenAsInt(const Token& t);
//int64_t ParseTokenAsInt64(const Token& t);

//
///* read data arrays */
//void ParseVectorDataArray(std::vector<aiVector3D>& out , const Element& el);
//void ParseVectorDataArray(std::vector<aiColor4D>& out , const Element& el);
//void ParseVectorDataArray(std::vector<aiVector2D>& out , const Element& el);
//void ParseVectorDataArray(std::vector<int>& out , const Element& el);

//void ParseVectorDataArray(std::vector<unsigned int>& out , const Element& el);
//void ParseVectorDataArray(std::vector<uint64_t>& out , const Element& e);
//void ParseVectorDataArray(std::vector<int64_t>& out , const Element& el);
//
//
/** extract a required element from a scope, abort if the element cannot be found */
fun getRequiredElement(sc: Scope, index: String, element: Element? = null) =
        sc[index] ?: parseError("did not find required element \"$index\"", element)
//
//// extract required compound scope
//const Scope& GetRequiredScope(const Element& el);
/** get token at a particular index */
