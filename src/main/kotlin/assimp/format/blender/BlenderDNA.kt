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
enum class FieldFlags { Pointer, Array;

    val i = ordinal + 1
}

infix fun Int.or(ff: FieldFlags) = or(ff.i)

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

//// -------------------------------------------------------------------------------
///** Range of possible behaviours for fields absend in the input file. Some are
// *  mission critical so we need them, while others can silently be default
// *  initialized and no animations are harmed. */
//// -------------------------------------------------------------------------------
//enum ErrorPolicy {
//    /** Substitute default value and ignore */
//    ErrorPolicy_Igno,
//            /** Substitute default value and write to log */
//    ErrorPolicy_Warn,
//            /** Substitute a massive error message and crash the whole matrix. Its time for another zion */
//    ErrorPolicy_Fail
//};
//
//#ifdef ASSIMP_BUILD_BLENDER_DEBUG
//#   define ErrorPolicy_Igno ErrorPolicy_Warn
//#endif
//
/** Represents a data structure in a BLEND file. A Structure defines n fields and their locations and encodings the input stream. Usually, every
 *  Structure instance pertains to one equally-named data structure in the
 *  BlenderScene.h header. This class defines various utilities to map a
 *  binary `blob` read from the file to such a structure instance with
 *  meaningful contents. */
// -------------------------------------------------------------------------------
class Structure {

    // publicly accessible members
    var name = ""
    val fields = ArrayList<Field>()
    val indices = mutableMapOf<String, Long>()

    var size = 0L

    var cacheIdx = -1L

    /** Access a field of the structure by its canonical name. The pointer version returns NULL on failure while
     *  the reference version raises an import error. */
    operator fun get(ss: String) = indices[ss]
            ?: throw Error("BlendDNA: Did not find a field named `$ss` in structure `$name`")
//    fun get_(ss:String) =

    /** Access a field of the structure by its index */
    operator fun get(i: Long) = fields.getOrElse(i.i) { throw Error("BlendDNA: There is no field with index `$i` in structure `$name`") }

    override fun equals(other: Any?) = other is Structure && name == other.name // name is meant to be an unique identifier

    /** Try to read an instance of the structure from the stream and attempt to convert to `T`. This is done by an
     *  appropriate specialization. If none is available, a compiler complain is the result.
     *  @param dest Destination value to be written
     *  @param db File database, including input stream. */
    inline fun <reified T> convert(db: FileDatabase): T {
        return when (T::class) {
            Int::class -> convertDispatcher(db)
            Short::class -> when (name) {
            // automatic rescaling from short to float and vice versa (seems to be used by normals)
                "float" -> {
                    var f = db.reader.float
                    if (f > 1f) f = 1f
                    (f * 32767f).s
                }
                "double" -> db.reader.double * 32767.0
                else -> convertDispatcher(db)
            }
            Char::class -> when (name) {
            // automatic rescaling from char to float and vice versa (seems useful for RGB colors)
                "float" -> db.reader.float * 255f
                "double" -> db.reader.double * 255f
                else -> convertDispatcher(db)
            }
            Float::class, Double::class -> when (name) {
            // automatic rescaling from char to float and vice versa (seems useful for RGB colors)
                "char" -> db.reader.get() / 255f
            // automatic rescaling from short to float and vice versa (used by normals)
                "short" -> db.reader.short / 32767f
                else -> convertDispatcher(db)
            }
            else -> Unit
        } as T
    }

    fun <T> convertDispatcher(db: FileDatabase): T = when (name) {
        "int" -> db.reader.int as T
        "short" -> db.reader.short as T
        "char" -> db.reader.get().c as T
        "float" -> db.reader.float as T
        "double" -> db.reader.double as T
        else -> throw Error("Unknown source for conversion to primitive data type: $name")
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fields.hashCode()
        result = 31 * result + indices.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + cacheIdx.hashCode()
        return result
    }

//    // --------------------------------------------------------
//    // field parsing for 1d arrays
//    template <int error_policy, typename T, size_t M>
//    void ReadFieldArray(T (& out )[M], const char* name,
//    const FileDatabase& db) const
//
//    // --------------------------------------------------------
//    // field parsing for 2d arrays
//    template <int error_policy, typename T, size_t M, size_t N>
//    void ReadFieldArray2(T (& out )[M][N], const char* name,
//    const FileDatabase& db) const
//
//    // --------------------------------------------------------
//    // field parsing for pointer or dynamic array types
//    // (std::shared_ptr)
//    // The return value indicates whether the data was already cached.
//    template <int error_policy, template <typename>
//    class TOUT, typename T>
//    bool ReadFieldPtr(TOUT<T>& out , const char* name,
//    const FileDatabase& db,
//    bool non_recursive = false) const
//
//    // --------------------------------------------------------
//    // field parsing for static arrays of pointer or dynamic
//    // array types (std::shared_ptr[])
//    // The return value indicates whether the data was already cached.
//    template <int error_policy, template <typename>
//    class TOUT, typename T, size_t N>
//    bool ReadFieldPtr(TOUT<T> (&out )[N], const char* name,
//    const FileDatabase& db) const
//
//    // --------------------------------------------------------
//    // field parsing for `normal` values
//    // The return value indicates whether the data was already cached.
//    template <int error_policy, typename T>
//    void ReadField(T& out , const char* name,
//    const FileDatabase& db) const
//
//    private :
//
//    // --------------------------------------------------------
//    template <template <typename>
//    class TOUT, typename T>
//    bool ResolvePointer(TOUT<T>& out , const Pointer & ptrval,
//    const FileDatabase& db, const Field& f,
//    bool non_recursive = false) const
//
//    // --------------------------------------------------------
//    template <template <typename>
//    class TOUT, typename T>
//    bool ResolvePointer(vector< TOUT<T> >& out , const Pointer & ptrval,
//    const FileDatabase& db, const Field& f, bool) const
//
//    // --------------------------------------------------------
//    bool ResolvePointer( std::shared_ptr< FileOffset >& out , const Pointer & ptrval,
//    const FileDatabase& db, const Field& f, bool) const
//
//    // --------------------------------------------------------
//    inline const FileBlockHead* LocateFileBlockForAddress(
//    const Pointer & ptrval,
//    const FileDatabase& db) const
//
//    private :
//
//    // ------------------------------------------------------------------------------
//    template <typename T> T* _allocate(std::shared_ptr<T>& out , size_t& s)
//    const {
//        out = std::shared_ptr<T>(new T ())
//        s = 1
//        return out.get()
//    }
//
//    template <typename T> T* _allocate(vector<T>& out , size_t& s)
//    const {
//        out.resize(s)
//        return s ? &out.front() : NULL
//    }
//
//    // --------------------------------------------------------
//    template <int error_policy>
//    struct _defaultInitializer
//    {
//
//        template < typename T, unsigned int N>
//        void operator ()(T(& out)[N], const char* = NULL) {
//        for (unsigned int i = 0; i < N; ++i) {
//        out[i] = T()
//    }
//    }
//
//        template < typename T, unsigned int N, unsigned int M>
//        void operator ()(T(& out)[N][M], const char* = NULL) {
//        for (unsigned int i = 0; i < N; ++i) {
//        for (unsigned int j = 0; j < M; ++j) {
//        out[i][j] = T()
//    }
//    }
//    }
//
//        template < typename T >
//        void operator ()(T& out, const char* = NULL) {
//        out = T()
//    }
//    }
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

    val converters = mutableMapOf<String, Pair<ElemBase, (ElemBase, FileDatabase) -> Unit>>()
    val structures = ArrayList<Structure>()
    val indices = mutableMapOf<String, Long>()

    /** Access a structure by its canonical name, the pointer version returns NULL on failure while the reference
     *  version raises an error. */
    operator fun get(ss: String) = indices.get(ss) ?: throw Error("BlendDNA: Did not find a structure named `$ss`")

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
//        converters["Object"] = Pair(Object(), Structure::Convert<Object>);
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
//        converters["Camera"] = DNA::FactoryPair( &Structure::Allocate<Camera>, &Structure::Convert<Camera> );
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
//    // --------------------------------------------------------
//    /** Find a suitable conversion function for a given Structure.
//     *  Such a converter function takes a blob from the input
//     *  stream, reads as much as it needs, and builds up a
//     *  complete object in intermediate representation.
//     *  @param structure Destination structure definition
//     *  @param db File database.
//     *  @return A null pointer in .first if no appropriate converter is available.*/
//    FactoryPair GetBlobToStructureConverter(
//    const Structure& structure,
//    const FileDatabase& db
//    ) const
//
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
class FileBlockHead() {
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
//    bool operator < (const FileBlockHead& o)
//    const {
//        return address.
//                val <o.address.
//        val;
//    }
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
class SectionParser(val stream: ByteBuffer, val ptr64: Boolean) {

    val current = FileBlockHead()

    /** Advance to the next section.
     *  @throw DeadlyImportError if the last chunk was passed. */
    fun next() {

        if(stream.pos + current.size >= stream.size)
            println()
        stream.pos += current.size

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


//#ifndef ASSIMP_BUILD_BLENDER_NO_STATS
//// -------------------------------------------------------------------------------
///** Import statistics, i.e. number of file blocks read*/
//// -------------------------------------------------------------------------------
//class Statistics {
//
//    public:
//
//    Statistics ()
//    : fields_read       ()
//    , pointers_resolved ()
//    , cache_hits        ()
////      , blocks_read       ()
//    , cached_objects    ()
//    {}
//
//    public:
//
//    /** total number of fields we read */
//    unsigned int fields_read;
//
//    /** total number of resolved pointers */
//    unsigned int pointers_resolved;
//
//    /** number of pointers resolved from the cache */
//    unsigned int cache_hits;
//
//    /** number of blocks (from  FileDatabase::entries)
//    we did actually read from. */
//    // unsigned int blocks_read;
//
//    /** objects in FileData::cache */
//    unsigned int cached_objects;
//};
//#endif
//
//// -------------------------------------------------------------------------------
///** The object cache - all objects addressed by pointers are added here. This
// *  avoids circular references and avoids object duplication. */
//// -------------------------------------------------------------------------------
//template <template <typename> class TOUT>
//class ObjectCache
//{
//    public:
//
//    typedef std::map< Pointer, TOUT<ElemBase> > StructureCache;
//
//    public:
//
//    ObjectCache(const FileDatabase& db)
//    : db(db)
//    {
//        // currently there are only ~400 structure records per blend file.
//        // we read only a small part of them and don't cache objects
//        // which we don't need, so this should suffice.
//        caches.reserve(64);
//    }
//
//    public:
//
//    // --------------------------------------------------------
//    /** Check whether a specific item is in the cache.
//     *  @param s Data type of the item
//     *  @param out Output pointer. Unchanged if the
//     *   cache doens't know the item yet.
//     *  @param ptr Item address to look for. */
//    template <typename T> void get (
//    const Structure& s,
//    TOUT<T>& out,
//    const Pointer& ptr) const;
//
//    // --------------------------------------------------------
//    /** Add an item to the cache after the item has
//     * been fully read. Do not insert anything that
//     * may be faulty or might cause the loading
//     * to abort.
//     *  @param s Data type of the item
//     *  @param out Item to insert into the cache
//     *  @param ptr address (cache key) of the item. */
//    template <typename T> void set
//    (const Structure& s,
//    const TOUT<T>& out,
//    const Pointer& ptr);
//
//    private:
//
//    mutable vector<StructureCache> caches;
//    const FileDatabase& db;
//};
//
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

//    public :
//
//    Statistics& stats()
//    const {
//        return _stats
//    }
//
//    // For all our templates to work on both shared_ptr's and vector's
//    // using the same code, a dummy cache for arrays is provided. Actually,
//    // arrays of objects are never cached because we can't easily
//    // ensure their proper destruction.
//    template <typename T>
//    ObjectCache<std::shared_ptr>& cache(std::shared_ptr<T>& /*in*/)
//    const {
//        return _cache
//    }
//
//    template <typename T>
//    ObjectCache<vector>& cache(vector<T>& /*in*/)
//    const {
//        return _cacheArrays
//    }
//
//    private :
//
//
//    #ifndef ASSIMP_BUILD_BLENDER_NO_STATS
//    mutable Statistics _stats
//    #endif
//
//    mutable ObjectCache<vector> _cacheArrays
//    mutable ObjectCache<std::shared_ptr> _cache
//
//    mutable size_t next_cache_idx
}

/** Factory to extract a #DNA from the DNA1 file block in a BLEND file. */
class DnaParser(
        /** Bind the parser to a empty DNA and an input stream */
        val db: FileDatabase) {

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
                    f.flags = f.flags or FieldFlags.Pointer
                }

                /*  arrays, however, specify the size of a single element so we need to parse the (possibly
                    multi-dimensional) array declaration in order to obtain the actual size of the array in the file.
                    Also we need to alter the lookup name to include no array brackets anymore or size fixup won't work
                    (if our size does not match the size read from the DNA).    */
                if (f.name.contains(']')) {
                    val rb = f.name.indexOf('[')
                    if (rb == -1) throw Error("BlenderDNA: Encountered invalid array declaration ${f.name}")

                    f.flags = f.flags or FieldFlags.Array
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