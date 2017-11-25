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

import glm_.BYTES
import glm_.i
import glm_.shl

/** ------------------------------------------------------------------------------------------------
 *  Hashing function taken from http://www.azillionmonkeys.com/qed/hash.html (incremental version)
 *
 *  This code is Copyright 2004-2008 by Paul Hsieh. It is used here in the belief that Assimp's
 *  license is considered compatible with Pauls's derivative license as specified on his web page.
 *
 *  (stdint.h should have been been included here)
 *  ------------------------------------------------------------------------------------------------    */
fun superFastHash(string: String, len: Int = string.length, hash: Int = 0): Int {

    if (string.isEmpty()) return 0

    val rem = len and 3
    var len = len ushr 2
    var hash = hash
    val chars = string.toCharArray()
    var data = 0

    /* Main loop */
    while (len > 0) {
        hash += chars.get16bits(data)
        val tmp = (chars.get16bits(data + 2) shl 11) xor hash
        hash = (hash shl 16) xor tmp
        data += 2 * Short.BYTES
        hash += hash ushr 11
        len--
    }

    /* Handle end cases */
    when (rem) {
        3 -> {
            hash += chars.get16bits(data)
            hash = hash xor (hash shl 16)
            hash = hash xor (chars[data + Short.BYTES] shl 18)
            hash += hash ushr 11
        }
        2 -> {
            hash += chars.get16bits(data)
            hash = hash xor (hash shl 11)
            hash += hash ushr 17
        }
        1 -> {
            hash += chars[data].i
            hash = hash xor (hash shl 10)
            hash += hash ushr 1
        }
    }

    /* Force "avalanching" of final 127 bits */
    hash = hash xor (hash shl 3)
    hash += hash ushr 5
    hash = hash xor (hash shl 4)
    hash += hash ushr 17
    hash = hash xor (hash shl 25)
    hash += hash ushr 6

    return hash
}

private fun CharArray.get16bits(ptr: Int) = get(ptr).i or (get(ptr + 1) shl 8)