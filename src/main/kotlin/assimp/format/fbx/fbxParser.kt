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

    val elements = mutableMapOf<String, Element>()

    init {
        if (!topLevel) {
            val t = parser.currentToken!!
            if (t.type != Tt.OPEN_BRACKET) parseError("expected open bracket", t)
        }

        var n = parser.advanceToNextToken() ?: parseError("unexpected end of file")

        // note: empty scopes are allowed
        while (n.type != Tt.CLOSE_BRACKET) {
            if (n.type != Tt.KEY)
                parseError("unexpected token, expected TOK_KEY", n)

            elements[n.stringContents(buffer)] = Element(n, parser)

            // Element() should stop at the next Key token (or right after a Close token)
            val t = parser.currentToken
            if (t == null) {
                if (topLevel) break
                parseError("unexpected end of file", parser.lastToken)
            } else n = t
        }
    }

    operator fun get(index: String) = elements[index]

    fun findElementCaseInsensitive(elementName: String) = elements[elementName.toLowerCase()]

//    ElementCollection GetCollection(const std::string& index)
//    const {
//        return elements.equal_range(index)
//    }
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
        println(cursor)
        if(cursor == 8110)
            println()
        last = current
        current = tokens[cursor++].takeUnless { cursor == tokens.lastIndex }
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
//std::string ParseTokenAsString(const Token& t);
//
///* read data arrays */
//void ParseVectorDataArray(std::vector<aiVector3D>& out , const Element& el);
//void ParseVectorDataArray(std::vector<aiColor4D>& out , const Element& el);
//void ParseVectorDataArray(std::vector<aiVector2D>& out , const Element& el);
//void ParseVectorDataArray(std::vector<int>& out , const Element& el);
//void ParseVectorDataArray(std::vector<float>& out , const Element& el);
//void ParseVectorDataArray(std::vector<unsigned int>& out , const Element& el);
//void ParseVectorDataArray(std::vector<uint64_t>& out , const Element& e);
//void ParseVectorDataArray(std::vector<int64_t>& out , const Element& el);
//
//
//// extract a required element from a scope, abort if the element cannot be found
//const Element& GetRequiredElement(const Scope& sc, const std::string& index, const Element* element = NULL);
//
//// extract required compound scope
//const Scope& GetRequiredScope(const Element& el);
//// get token at a particular index
//const Token& GetRequiredToken(const Element& el, unsigned int index);
//
//
//// read a 4x4 matrix from an array of 16 floats
//aiMatrix4x4 ReadMatrix(const Element& element);
