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
import glm_.*
import java.nio.ByteBuffer
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction2
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/** @file  BlenderDNA.h
 *  @brief Blender `DNA` (file format specification embedded in
 *    blend file itself) loader.
 */


//template <bool,bool> class StreamReader;
//typedef StreamReader<true,true> StreamReaderAny;
//
//namespace Blender {
//
//class  FileDatabase;
//struct FileBlockHead;
//
//template <template <typename> class TOUT>
//class ObjectCache;
//
//// -------------------------------------------------------------------------------
///** Exception class used by the blender loader to selectively catch exceptions
// *  thrown in its own code (DeadlyImportErrors thrown in general utility
// *  functions are untouched then). If such an exception is not caught by
// *  the loader itself, it will still be caught by Assimp due to its
// *  ancestry. */
//// -------------------------------------------------------------------------------
//struct Error : DeadlyImportError {
//    Error (const std::string& s)
//    : DeadlyImportError(s) {
//    // empty
//}
//};
//
/** The only purpose of this structure is to feed a virtual dtor into its descendents. It serves as base class for
 *  all data structure fields.  */
open class ElemBase {

    /** Type name of the element. The type string points is the `c_str` of the `name` attribute of the corresponding
     *  `Structure`, that is, it is only valid as long as the DNA is not modified. The dna_type is only set if the
     *  data type is not static, i.e. a std::shared_ptr<ElemBase> in the scene description would have its type resolved
     *  at runtime, so this member is always set. */
    var dnaType = ""
}

/** Represents a generic pointer to a memory location, which can be either 32 or 64 bits. These pointers are loaded from
 *  the BLEND file and finally fixed to point to the real, converted representation of the objects they used to point to.   */
class Pointer {
    var value = 0
}

/** Represents a generic offset within a BLEND file */
class FileOffset {
    var value = 0L
}
//
//// -------------------------------------------------------------------------------
///** Dummy derivate of std::vector to be able to use it in templates simultaenously
// *  with std::shared_ptr, which takes only one template argument
// *  while std::vector takes three. Also we need to provide some special member
// *  functions of shared_ptr */
//// -------------------------------------------------------------------------------
//template <typename T>
//class vector : public std::vector<T> {
//public:
//using std::vector<T>::resize;
//using std::vector<T>::empty;
//
//void reset() {
//    resize(0);
//}
//
//operator bool () const {
//    return !empty();
//}
//};

/** Mixed flags for use in #Field */
enum class FieldFlag { Pointer, Array;

    val i = ordinal + 1
}

infix fun Int.or(ff: FieldFlag) = or(ff.i)
infix fun Int.hasnt(ff: FieldFlag) = and(ff.i) == 0

/** Represents a single member of a data structure in a BLEND file */
class Field {
    var name = ""
    var type = ""

    var size = 0L
    var offset = 0L

    /** Size of each array dimension. For flat arrays, the second dimension is set to 1. */
    val arraySizes = LongArray(2)

    /** Any of the #FieldFlags enumerated values */
    var flags = 0
}

/** Range of possible behaviours for fields absend in the input file. Some are mission critical so we need them,
 *  while others can silently be default initialized and no animations are harmed.  */
enum class ErrorPolicy {
    /** Substitute default value and write to log */
    Warn,
    /** Substitute a massive error message and crash the whole matrix. Its time for another zion */
    Fail,
    /** Substitute default value and ignore */
    Igno
}

//
//// --------------------------------------------------------
//template <>  struct Structure :: _defaultInitializer<ErrorPolicy_Warn> {
//
//    template <typename T>
//    void operator ()(T& out, const char* reason = "<add reason>") {
//    DefaultLogger::get()->warn(reason);
//
//    // ... and let the show go on
//    _defaultInitializer<0 /*ErrorPolicy_Igno*/>()(out);
//}
//};
//
//template <> struct Structure :: _defaultInitializer<ErrorPolicy_Fail> {
//
//    template <typename T>
//    void operator ()(T& /*out*/,const char* = "") {
//    // obviously, it is crucial that _DefaultInitializer is used
//    // only from within a catch clause.
//    throw;
//}
//};
//
//// -------------------------------------------------------------------------------------------------------
//template <> inline bool Structure :: ResolvePointer<std::shared_ptr,ElemBase>(std::shared_ptr<ElemBase>& out,
//const Pointer & ptrval,
//const FileDatabase& db,
//const Field& f,
//bool
//) const;
//
//

/** Represents the full data structure information for a single BLEND file.
 *  This data is extracted from the DNA1 chunk in the file.
 *  #DnaParser does the reading and represents currently the only place where DNA is altered.*/
class DNA {

    val structures = ArrayList<Structure>()
    val indices = mutableMapOf<String, Long>()

    /** Access a structure by its canonical name, the pointer version returns NULL on failure while the reference
     *  version raises an error. */
    operator fun get(ss: String): Structure {
        val index = indices[ss] ?: throw Exception("BlendDNA: Did not find a structure named `$ss`")
        return structures[index.i]
    }

    /** Access a structure by its index */
    operator fun get(i: Long) = structures.getOrElse(i.i) { throw Error("BlendDNA: There is no structure with index `$i`") }

    /** Add structure definitions for all the primitive types, i.e. integer, short, char, float */
    fun addPrimitiveStructures() {
        /*  NOTE: these are just dummies. Their presence enforces Structure::Convert<target_type> to be called on these
            empty structures. These converters are special overloads which scan the name of the structure and perform
            the required data type conversion if one of these special names is found in the structure in question.  */

        indices["int"] = structures.size.L
        structures += Structure().apply { name = "int"; size = 4; }

        indices["short"] = structures.size.L
        structures += Structure().apply { name = "short"; size = 2; }

        indices["char"] = structures.size.L
        structures += Structure().apply { name = "char"; size = 1; }

        indices["float"] = structures.size.L
        structures += Structure().apply { name = "float"; size = 4; }

        indices["double"] = structures.size.L
        structures += Structure().apply { name = "double"; size = 8; }

        // no long, seemingly.
    }

    /** basing on http://www.blender.org/development/architecture/notes-on-sdna/
     *  Fill the @c converters member with converters for all known data types. The implementation of this method is in
     *  BlenderScene.cpp and is machine-generated.
     *  Converters are used to quickly handle objects whose exact data type is a runtime-property and not yet known
     *  at compile time (consier Object::data).*/
    fun registerConverters() {
//        converters["Object"] = ::Object to ::convertObject
//        converters["Group"] = DNA::FactoryPair( &Structure::Allocate<Group>, &Structure::Convert<Group> );
//        converters["MTex"] = DNA::FactoryPair( &Structure::Allocate<MTex>, &Structure::Convert<MTex> );
//        converters["TFace"] = DNA::FactoryPair( &Structure::Allocate<TFace>, &Structure::Convert<TFace> );
//        converters["SubsurfModifierData"] = DNA::FactoryPair( &Structure::Allocate<SubsurfModifierData>, &Structure::Convert<SubsurfModifierData> );
//        converters["MFace"] = DNA::FactoryPair( &Structure::Allocate<MFace>, &Structure::Convert<MFace> );
//        converters["Lamp"] = DNA::FactoryPair( &Structure::Allocate<Lamp>, &Structure::Convert<Lamp> );
//        converters["MDeformWeight"] = DNA::FactoryPair( &Structure::Allocate<MDeformWeight>, &Structure::Convert<MDeformWeight> );
//        converters["PackedFile"] = DNA::FactoryPair( &Structure::Allocate<PackedFile>, &Structure::Convert<PackedFile> );
//        converters["Base"] = DNA::FactoryPair( &Structure::Allocate<Base>, &Structure::Convert<Base> );
//        converters["MTFace"] = DNA::FactoryPair( &Structure::Allocate<MTFace>, &Structure::Convert<MTFace> );
//        converters["Material"] = DNA::FactoryPair( &Structure::Allocate<Material>, &Structure::Convert<Material> );
//        converters["MTexPoly"] = DNA::FactoryPair( &Structure::Allocate<MTexPoly>, &Structure::Convert<MTexPoly> );
//        converters["Mesh"] = DNA::FactoryPair( &Structure::Allocate<Mesh>, &Structure::Convert<Mesh> );
//        converters["MDeformVert"] = DNA::FactoryPair( &Structure::Allocate<MDeformVert>, &Structure::Convert<MDeformVert> );
//        converters["World"] = DNA::FactoryPair( &Structure::Allocate<World>, &Structure::Convert<World> );
//        converters["MLoopCol"] = DNA::FactoryPair( &Structure::Allocate<MLoopCol>, &Structure::Convert<MLoopCol> );
//        converters["MVert"] = DNA::FactoryPair( &Structure::Allocate<MVert>, &Structure::Convert<MVert> );
//        converters["MEdge"] = DNA::FactoryPair( &Structure::Allocate<MEdge>, &Structure::Convert<MEdge> );
//        converters["MLoopUV"] = DNA::FactoryPair( &Structure::Allocate<MLoopUV>, &Structure::Convert<MLoopUV> );
//        converters["GroupObject"] = DNA::FactoryPair( &Structure::Allocate<GroupObject>, &Structure::Convert<GroupObject> );
//        converters["ListBase"] = DNA::FactoryPair( &Structure::Allocate<ListBase>, &Structure::Convert<ListBase> );
//        converters["MLoop"] = DNA::FactoryPair( &Structure::Allocate<MLoop>, &Structure::Convert<MLoop> );
//        converters["ModifierData"] = DNA::FactoryPair( &Structure::Allocate<ModifierData>, &Structure::Convert<ModifierData> );
//        converters["ID"] = DNA::FactoryPair( &Structure::Allocate<ID>, &Structure::Convert<ID> );
//        converters["MCol"] = DNA::FactoryPair( &Structure::Allocate<MCol>, &Structure::Convert<MCol> );
//        converters["MPoly"] = DNA::FactoryPair( &Structure::Allocate<MPoly>, &Structure::Convert<MPoly> );
//        converters["Scene"] = DNA::FactoryPair( &Structure::Allocate<Scene>, &Structure::Convert<Scene> );
//        converters["Library"] = DNA::FactoryPair( &Structure::Allocate<Library>, &Structure::Convert<Library> );
//        converters["Tex"] = DNA::FactoryPair( &Structure::Allocate<Tex>, &Structure::Convert<Tex> );
//        converters["Camera"] = ::Camera to Structure::convertCamera
//        converters["Camera"] = 0 to Structure::convertCamera
//        converters["Camera"] = ::Camera to 0
//        converters["MirrorModifierData"] = DNA::FactoryPair( &Structure::Allocate<MirrorModifierData>, &Structure::Convert<MirrorModifierData> );
//        converters["Image"] = DNA::FactoryPair( &Structure::Allocate<Image>, &Structure::Convert<Image> );
    }


    // --------------------------------------------------------
    /** Take an input blob from the stream, interpret it according to
     *  a its structure name and convert it to the intermediate
     *  representation.
     *  @param structure Destination structure definition
     *  @param db File database.
     *  @return A null pointer if no appropriate converter is available.*/
//    std::shared_ptr< ElemBase > ConvertBlobToStructure(
//    const Structure& structure,
//    const FileDatabase& db
//    ) const
//

    /** Find a suitable conversion function for a given Structure.
     *  Such a converter function takes a blob from the input stream, reads as much as it needs, and builds up a
     *  complete object in intermediate representation.
     *  @param structure Destination structure definition
     *  @param db File database.
     *  @return A null pointer in .first if no appropriate converter is available.  */
    fun getBlobToStructureConverter(structure: Structure) = when (structure.name) {
        "Object" -> ::Object to (Structure::convertObject as Structure.(KMutableProperty0<ElemBase>) -> Unit)
//            converters["Group"] = DNA::FactoryPair( &Structure::Allocate<Group>, &Structure::Convert<Group> );
//            converters["MTex"] = DNA::FactoryPair( &Structure::Allocate<MTex>, &Structure::Convert<MTex> );
//            converters["TFace"] = DNA::FactoryPair( &Structure::Allocate<TFace>, &Structure::Convert<TFace> );
//            converters["SubsurfModifierData"] = DNA::FactoryPair( &Structure::Allocate<SubsurfModifierData>, &Structure::Convert<SubsurfModifierData> );
//            converters["MFace"] = DNA::FactoryPair( &Structure::Allocate<MFace>, &Structure::Convert<MFace> );
//            converters["Lamp"] = DNA::FactoryPair( &Structure::Allocate<Lamp>, &Structure::Convert<Lamp> );
//            converters["MDeformWeight"] = DNA::FactoryPair( &Structure::Allocate<MDeformWeight>, &Structure::Convert<MDeformWeight> );
//            converters["PackedFile"] = DNA::FactoryPair( &Structure::Allocate<PackedFile>, &Structure::Convert<PackedFile> );
//            converters["Base"] = DNA::FactoryPair( &Structure::Allocate<Base>, &Structure::Convert<Base> );
//            converters["MTFace"] = DNA::FactoryPair( &Structure::Allocate<MTFace>, &Structure::Convert<MTFace> );
//            converters["Material"] = DNA::FactoryPair( &Structure::Allocate<Material>, &Structure::Convert<Material> );
//            converters["MTexPoly"] = DNA::FactoryPair( &Structure::Allocate<MTexPoly>, &Structure::Convert<MTexPoly> );
//            converters["Mesh"] = DNA::FactoryPair( &Structure::Allocate<Mesh>, &Structure::Convert<Mesh> );
//            converters["MDeformVert"] = DNA::FactoryPair( &Structure::Allocate<MDeformVert>, &Structure::Convert<MDeformVert> );
//            converters["World"] = DNA::FactoryPair( &Structure::Allocate<World>, &Structure::Convert<World> );
//            converters["MLoopCol"] = DNA::FactoryPair( &Structure::Allocate<MLoopCol>, &Structure::Convert<MLoopCol> );
//            converters["MVert"] = DNA::FactoryPair( &Structure::Allocate<MVert>, &Structure::Convert<MVert> );
//            converters["MEdge"] = DNA::FactoryPair( &Structure::Allocate<MEdge>, &Structure::Convert<MEdge> );
//            converters["MLoopUV"] = DNA::FactoryPair( &Structure::Allocate<MLoopUV>, &Structure::Convert<MLoopUV> );
//            converters["GroupObject"] = DNA::FactoryPair( &Structure::Allocate<GroupObject>, &Structure::Convert<GroupObject> );
//            converters["ListBase"] = DNA::FactoryPair( &Structure::Allocate<ListBase>, &Structure::Convert<ListBase> );
//            converters["MLoop"] = DNA::FactoryPair( &Structure::Allocate<MLoop>, &Structure::Convert<MLoop> );
//            converters["ModifierData"] = DNA::FactoryPair( &Structure::Allocate<ModifierData>, &Structure::Convert<ModifierData> );
//            converters["ID"] = DNA::FactoryPair( &Structure::Allocate<ID>, &Structure::Convert<ID> );
//            converters["MCol"] = DNA::FactoryPair( &Structure::Allocate<MCol>, &Structure::Convert<MCol> );
//            converters["MPoly"] = DNA::FactoryPair( &Structure::Allocate<MPoly>, &Structure::Convert<MPoly> );
//            converters["Scene"] = DNA::FactoryPair( &Structure::Allocate<Scene>, &Structure::Convert<Scene> );
//            converters["Library"] = DNA::FactoryPair( &Structure::Allocate<Library>, &Structure::Convert<Library> );
//            converters["Tex"] = DNA::FactoryPair( &Structure::Allocate<Tex>, &Structure::Convert<Tex> );
        "Camera" -> ::Camera to (Structure::convertCamera as Structure.(KMutableProperty0<ElemBase>) -> Unit)
//            converters["MirrorModifierData"] = DNA::FactoryPair( &Structure::Allocate<MirrorModifierData>, &Structure::Convert<MirrorModifierData> );
//            converters["Image"] = DNA::FactoryPair( &Structure::Allocate<Image>, &Structure::Convert<Image> );
        else -> null to null
    }

//
//    #ifdef ASSIMP_BUILD_BLENDER_DEBUG
//    // --------------------------------------------------------
//    /** Dump the DNA to a text file. This is for debugging purposes.
//     *  The output file is `dna.txt` in the current working folder*/
//    void DumpToFile()
//    #endif

    companion object {

        /** Extract array dimensions from a C array declaration, such as `...[4][6]`. Returned string would be `...[][]`.
         *  @param array_sizes Receive maximally two array dimensions, the second element is set to 1 if the array is flat.
         *    Both are set to 1 if the input is not an array.
         *  @throw DeadlyImportError if more than 2 dimensions are encountered. */
        fun extractArraySize(input: String, arraySizes: LongArray) {

            arraySizes.fill(1)
            var pos = input.indexOf('[')
            if (pos++ == -1) return
            arraySizes[0] = strtoul10(input, pos).L

            pos = input.indexOf('[', pos)
            if (pos++ == -1) return
            arraySizes[1] = strtoul10(input, pos).L
        }
    }
}
//
//// special converters for primitive types
//template <> inline void Structure :: Convert<int>       (int& dest,const FileDatabase& db) const;
//template <> inline void Structure :: Convert<short>     (short& dest,const FileDatabase& db) const;
//template <> inline void Structure :: Convert<char>      (char& dest,const FileDatabase& db) const;
//template <> inline void Structure :: Convert<float>     (float& dest,const FileDatabase& db) const;
//template <> inline void Structure :: Convert<double>    (double& dest,const FileDatabase& db) const;
//template <> inline void Structure :: Convert<Pointer>   (Pointer& dest,const FileDatabase& db) const;
//
/** Describes a master file block header. Each master file sections holds n elements of a certain SDNA structure
 *  (or otherwise unspecified data). */
class FileBlockHead() : Comparable<FileBlockHead> {
    /** points right after the header of the file block */
    var start = 0

    var id = ""
    var size = 0

    // original memory address of the data
    var address = 0L

    // index into DNA
    var dnaIndex = 0

    // number of structure instances to follow
    var num = 0

    constructor(other: FileBlockHead) : this() {
        start = other.start
        id = other.id
        size = other.size
        address = other.address
        dnaIndex = other.dnaIndex
        num = other.num
    }

    // file blocks are sorted by address to quickly locate specific memory addresses
    override fun compareTo(other: FileBlockHead) = address.compareTo(other.address)
//
//    // for std::upper_bound
//    operator const Pointer& ()
//    const {
//        return address;
//    }
}
//
//// for std::upper_bound
//inline bool operator< (const Pointer& a, const Pointer& b) {
//return a.val < b.val;
//}
//
/** Utility to read all master file blocks in turn. */
class SectionParser {

    val stream = db.reader
    val ptr64 = db.i64bit

    val current = FileBlockHead().apply { start = 12 }

    /** Advance to the next section.
     *  @throw DeadlyImportError if the last chunk was passed. */
    fun next() {

//        if(stream.pos + current.size >= stream.size)
//            println()
        stream.pos = current.start + current.size

        val tmp = CharArray(4) { stream.get().c }
        current.id = String(tmp, 0, if (tmp[3] != NUL) 4 else if (tmp[2] != NUL) 3 else if (tmp[1] != NUL) 2 else 1)

        current.size = stream.int
        current.address = if (ptr64) stream.long else stream.int.L

        current.dnaIndex = stream.int
        current.num = stream.int

        current.start = stream.pos
        if (stream.limit() - stream.pos < current.size)
            throw Error("BLEND: invalid size of file block")
    }
}


/** Import statistics, i.e. number of file blocks read*/
class Statistics {

    /** total number of fields we read */
    var fieldsRead = 0

    /** total number of resolved pointers */
    var pointersResolved = 0

    /** number of pointers resolved from the cache */
    var cacheHits = 0

    /** number of blocks (from  FileDatabase::entries) we did actually read from. */
    var blocksRead = 0

    /** objects in FileData::cache */
    var cachedObjects = 0
}

/** The object cache - all objects addressed by pointers are added here. This avoids circular references and avoids
 *  object duplication. */
class ObjectCache(val db: FileDatabase) {

    /** Currently there are only ~400 structure records per blend file. We read only a small part of them and don't
     *  cache objects which we don't need, so this should suffice.  */
    val caches = ArrayList<MutableMap<Long, ElemBase>>(64)

    /** Check whether a specific item is in the cache.
     *  @param s Data type of the item
     *  @param out Output pointer. Unchanged if the cache doens't know the item yet.
     *  @param ptr Item address to look for. */
    fun <T> get(s: Structure, out: KMutableProperty0<T>, ptr: Long) {

        if (s.cacheIdx == -1L) {
            s.cacheIdx = db.nextCacheIdx++
            caches += mutableMapOf()
            return
        }

        caches[s.cacheIdx.i][ptr]?.let {

            out.set(it as T)

            if (!ASSIMP.BUILD.BLENDER.NO_STATS) ++db.stats.cacheHits
        }
        // otherwise, out remains untouched
    }

    /** Add an item to the cache after the item has been fully read. Do not insert anything that may be faulty or might
     *  cause the loading to abort.
     *  @param s Data type of the item
     *  @param out Item to insert into the cache
     *  @param ptr address (cache key) of the item. */
    fun <T> set(s: Structure, out: KMutableProperty0<T>, ptr: Long) {
        if (s.cacheIdx == -1L) {
            s.cacheIdx = db.nextCacheIdx++
            caches.ensureCapacity(db.nextCacheIdx.i)
        }
        caches[s.cacheIdx.i][ptr] = out() as ElemBase

        if (!ASSIMP.BUILD.BLENDER.NO_STATS) ++db.stats.cachedObjects
    }
}

//// -------------------------------------------------------------------------------
//// -------------------------------------------------------------------------------
//template <> class ObjectCache<Blender::vector>
//{
//    public:
//
//    ObjectCache(const FileDatabase&) {}
//
//    template <typename T> void get(const Structure&, vector<T>&, const Pointer&) {}
//    template <typename T> void set(const Structure&, const vector<T>&, const Pointer&) {}
//};
//
//#ifdef _MSC_VER
//#   pragma warning(disable:4355)
//#endif

/** Memory representation of a full BLEND file and all its dependencies. The output AiScene is constructed from
 *  an instance of this data structure. */
class FileDatabase {

    // publicly accessible fields
    var i64bit = false
    var little = false

    var dna = DNA()
    lateinit var reader: ByteBuffer
    val entries = ArrayList<FileBlockHead>()

    val stats = Statistics()

    /** For all our templates to work on both shared_ptr's and vector's using the same code, a dummy cache for arrays is
     *  provided. Actually, arrays of objects are never cached because we can't easily ensure their proper destruction. */
    val cache = ObjectCache(this)

    val cacheArrays = ArrayList<ObjectCache>()

    var nextCacheIdx = 0L
}

/** Factory to extract a #DNA from the DNA1 file block in a BLEND file. */
class DnaParser {

    /** Locate the DNA in the file and parse it. The input stream is expected to point to the beginning of the DN1 chunk
     *  at the time this method is called and is undefined afterwards.
     *  @throw DeadlyImportError if the DNA cannot be read.
     *  @note The position of the stream pointer is undefined afterwards.   */
    fun parse() {
        val stream = db.reader
        val dna = db.dna

        if (stream doesntMatch "SDNA") throw Error("BlenderDNA: Expected SDNA chunk")
        // name dictionary
        if (stream doesntMatch "NAME") throw Error("BlenderDNA: Expected NAME field")

        val names = Array(stream.int) { stream.nextWord().also { stream.consumeNUL() } }

        // type dictionary
        while (((stream.pos - 12) and 0x3) != 0) stream.get()
        if (stream doesntMatch "TYPE") throw Error("BlenderDNA: Expected TYPE field")

        val types = Array(stream.int) {
            Type().apply {
                name = stream.nextWord().also { stream.consumeNUL() }
            }
        }

        // type length dictionary
        while (((stream.pos - 12) and 0x3) != 0) stream.get()
        if (stream doesntMatch "TLEN") throw Error("BlenderDNA: Expected TLEN field")

        for (s in types) s.size = stream.short.L

        // structures dictionary
        while (((stream.pos - 12) and 0x3) != 0) stream.get()
        if (stream doesntMatch "STRC") throw Error("BlenderDNA: Expected STRC field")

        val end = stream.int
        var fields = 0

        dna.structures.ensureCapacity(end)
        for (i in 0 until end) {

            var n = stream.short.i
            if (n >= types.size) throw Error("BlenderDNA: Invalid type index in structure name$n (there are only ${types.size} entries)")

            // maintain separate indexes
            dna.indices[types[n].name] = dna.structures.size.L

            val s = Structure().apply { name = types[n].name }
            dna.structures += s
            //s.index = dna.structures.size()-1;

            n = stream.short.i
            s.fields.ensureCapacity(n)

            var offset = 0L
            for (m in 0 until n) {

                var j = stream.short.i
                if (j >= types.size) throw Error("BlenderDNA: Invalid type index in structure field $j (there are only ${types.size} entries)")

                val f = Field()
                s.fields += f
                f.offset = offset

                f.type = types[j].name
                f.size = types[j].size

                j = stream.short.i
                if (j >= names.size) throw Error("BlenderDNA: Invalid name index in structure field $j (there are only ${names.size} entries)")

                f.name = names[j]
                f.flags = 0

                /*  pointers always specify the size of the pointee instead of their own.
                    The pointer asterisk remains a property of the lookup name.                 */
                if (f.name[0] == '*') {
                    f.size = if (db.i64bit) 8 else 4
                    f.flags = f.flags or FieldFlag.Pointer
                }

                /*  arrays, however, specify the size of a single element so we need to parse the (possibly
                    multi-dimensional) array declaration in order to obtain the actual size of the array in the file.
                    Also we need to alter the lookup name to include no array brackets anymore or size fixup won't work
                    (if our size does not match the size read from the DNA).    */
                if (f.name.contains(']')) {
                    val rb = f.name.indexOf('[')
                    if (rb == -1) throw Error("BlenderDNA: Encountered invalid array declaration ${f.name}")

                    f.flags = f.flags or FieldFlag.Array
                    DNA.extractArraySize(f.name, f.arraySizes)
                    f.name = f.name.substring(0, rb)

                    f.size *= f.arraySizes[0] * f.arraySizes[1]
                }

                // maintain separate indexes
                s.indices[f.name] = s.fields.lastIndex.L
                offset += f.size

                ++fields
            }
            s.size = offset
        }

        logger.debug("BlenderDNA: Got ${dna.structures.size} structures with totally $fields fields")

//        #ifdef ASSIMP_BUILD_BLENDER_DEBUG TODO
//                dna.DumpToFile();
//        #endif

        dna.addPrimitiveStructures()
        dna.registerConverters()
    }

    /** Obtain a reference to the extracted DNA information */
    val dna get() = db.dna
}

infix fun ByteBuffer.match(string: String) = get().c == string[0] && get().c == string[1] && get().c == string[2] && get().c == string[3]
infix fun ByteBuffer.doesntMatch(string: String) = !match(string)

class Type {
    var size = 0L
    var name = ""
}