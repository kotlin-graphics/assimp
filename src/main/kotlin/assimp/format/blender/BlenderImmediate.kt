/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2018, assimp team


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

package assimp.format.blender

import assimp.*
import java.util.SortedSet

/** @file  BlenderIntermediate.h
 *  @brief Internal utility structures for the BlenderLoader. It also serves
 *    as master include file for the whole (internal) Blender subsystem.
 */


// --------------------------------------------------------------------
/** Mini smart-array to avoid pulling in even more boost stuff. usable with vector and deque */
// --------------------------------------------------------------------
//template <template <typename,typename> class TCLASS, typename T>
//struct TempArray    {
//    typedef TCLASS< T*,std::allocator<T*> > mywrap;
//
//    TempArray() {
//    }
//
//    ~TempArray () {
//    for(T* elem : arr) {
//    delete elem;
//}
//}
//
//    void dismiss() {
//        arr.clear();
//    }
//
//    mywrap* operator -> () {
//    return &arr;
//}
//
//    operator mywrap& () {
//        return arr;
//    }
//
//    operator const mywrap& () const {
//    return arr;
//}
//
//    mywrap& get () {
//    return arr;
//}
//
//    const mywrap& get () const {
//        return arr;
//    }
//
//    T* operator[] (size_t idx) const {
//        return arr[idx];
//    }
//
//    T*& operator[] (size_t idx) {
//        return arr[idx];
//    }
//
//    private:
//    // no copy semantics
//    void operator= (const TempArray&)  {
//    }
//
//    TempArray(const TempArray& /*arr*/) {
//    }
//
//    private:
//    mywrap arr;
//};


/** When keeping objects in sets, sort them by their name.  */
val objectCompare = compareBy<Object> { it.id.name }


//typedef std::set<const Object*, ObjectCompare> ObjectSet;

/** ConversionData acts as intermediate storage location for the various ConvertXXX routines in BlenderImporter.*/
class ConversionData(
        /** original file data  */
        val db: FileDatabase) {


    val objects = sortedSetOf(objectCompare)

    val meshes = ArrayList<AiMesh>()
    val cameras = ArrayList<AiCamera>()
    val lights = ArrayList<AiLight>()
    val materials = ArrayList<AiMaterial>()
    val textures = ArrayList<AiTexture>()

    /** set of all materials referenced by at least one mesh in the scene   */
    val materialsRaw = ArrayList<Material>()

    /** counter to name sentinel textures inserted as substitutes for procedural textures.  */
    var sentinelCnt = 0

    /** next texture ID for each texture type, respectively */
    var nextTexture = IntArray(AiTexture.Type.unknown.i + 1)
}