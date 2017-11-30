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

package assimp

import kotlin.reflect.KMutableProperty0

fun <T : Any> setGenericProperty(list: MutableMap<Int, Any>, szName: String, value: T): Boolean {

    assert(szName.isNotEmpty())
    val hash = superFastHash(szName)

    return list.putIfAbsent(hash, value) != null
}

fun <T> getGenericProperty(list: MutableMap<Int, T>, szName: String, errorReturn: T? = null): T {

    assert(szName.isNotEmpty())
    val hash = superFastHash(szName)

    return list[hash] ?: errorReturn!!
}

/** Special version for pointer types - they will be deleted when replaced with another value
 *  passing NULL removes the whole property */
fun <T> setGenericPropertyPtr(list: MutableMap<Int, T>, szName: String, value: T? = null, wasExisting: KMutableProperty0<Boolean>? = null) {

    assert(szName.isNotEmpty())
    val hash = superFastHash(szName)

    wasExisting?.set(list.contains(hash))

    if (value == null) list -= hash
    else list[hash] = value
}

// ------------------------------------------------------------------------------------------------
fun <T> hasGenericProperty(list: MutableMap<Int, T>, szName: String): Boolean {

    assert(szName.isNotEmpty())
    val hash = superFastHash(szName)

    return list.contains(hash)
}