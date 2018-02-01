/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2012, assimp team
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

import assimp.logger

/** @file  FBXDocumentUtil.h
 *  @brief FBX internal utilities used by the DOM reading code
 */

/* DOM/Parse error reporting - does not return */
fun domError(message: String, token: Token): Nothing = throw Error(Util.addTokenText("FBX-DOM", message, token))

fun domError(message: String, element: Element? = null): Nothing {
    element?.let { domError(message, element.keyToken) }
    throw Error("FBX-DOM $message")
}

// does return
fun domWarning(message: String, token: Token) = logger.warn { Util.addTokenText("FBX-DOM", message, token) }

fun domWarning(message: String, element: Element? = null) {
    element?.let {
        domWarning(message, element.keyToken)
        return
    }
    logger.warn { "FBX-DOM: $message" }
}

/** fetch a property table and the corresponding property template */
fun getPropertyTable(doc: Document, templateName: String, element: Element, sc: Scope, noWarn: Boolean = false): PropertyTable {

    val properties70 = sc["Properties70"]
    val templateProps = doc.templates[templateName].takeIf { templateName.isNotEmpty() }

    return if (properties70 == null) {
        if (!noWarn) domWarning("property table (Properties70) not found", element)
        templateProps ?: PropertyTable()
    } else PropertyTable(properties70, templateProps)
}

fun <T> processSimpleConnection(con: Connection, isObjectPropertyConn: Boolean, name: String, element: Element,
                                propNameOut: Array<String>? = null): T? {
    if (isObjectPropertyConn && !con.prop.isEmpty()) {
        domWarning("expected incoming $name link to be an object-object connection, ignoring", element)
        return null
    } else if (!isObjectPropertyConn && con.prop.isNotEmpty()) {
        domWarning("expected incoming $name link to be an object-property connection, ignoring", element)
        return null
    }

    if (isObjectPropertyConn && propNameOut != null)
        /*  note: this is ok, the return value of PropertyValue() is guaranteed to remain valid and unchanged as long as
            the document exists.         */
        propNameOut[0] = con.prop

    val ob = con.sourceObject
    if (ob == null) {
        domWarning("failed to read source object for incoming $name link, ignoring", element)
        return null
    }

    return ob as T
}