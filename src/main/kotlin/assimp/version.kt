/*
---------------------------------------------------------------------------
Open Asset Import Library (assimp)
---------------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team


All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the following
conditions are met:

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
---------------------------------------------------------------------------
*/

package assimp

import glm_.i

/** @brief Returns a string with legal copyright and licensing information about Assimp. The string may include multiple
 *  lines.
 *  @return Pointer to static string.
 */
val legalString ="""
    Open Asset Import Library (Assimp).
    A free C/C++ library to import various 3D file formats into applications

    (c) 2008-2017, assimp team
    License under the terms and conditions of the 3-clause BSD license
    http://assimp.sourceforge.net\n"""

/** @brief Returns the current minor version number of Assimp.
 *  @return Minor version of the Assimp runtime the application was linked/built against
 */
val versionMinor = 0

/** @brief Returns the current major version number of Assimp.
 *  @return Major version of the Assimp runtime the application was linked/built against
 */
val versionMajor = 4

/** @brief Returns the repository revision of the Assimp runtime.
 *  @return SVN Repository revision number of the Assimp runtime the application was linked/built against.
 */
val versionRevision = 0xee56ffa1
val branch = "master"

/** @brief Returns assimp's compile flags
 *  @return Any bitwise combination of the ASSIMP_CFLAGS_xxx constants.
 */
val compileFlags = ASSIMP.DEBUG.i