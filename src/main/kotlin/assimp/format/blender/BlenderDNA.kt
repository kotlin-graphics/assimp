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
import java.nio.*
import kotlin.reflect.*

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

typealias ElemBaseConstructor = KFunction0<ElemBase>
typealias ElemBaseConverter = (Structure.(KMutableProperty0<ElemBase?>) -> Unit)

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

	override fun toString(): String {
		return "[Field]: $name, type: $type"
	}
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

/** Represents a data structure in a BLEND file. A Structure defines n fields and their locations and encodings the input stream. Usually, every
 *  Structure instance pertains to one equally-named data structure in the
 *  BlenderScene.h header. This class defines various utilities to map a
 *  binary `blob` read from the file to such a structure instance with
 *  meaningful contents. */
// -------------------------------------------------------------------------------
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

    val converters: MutableMap<String, Pair<ElemBaseConstructor, ElemBaseConverter>> = mutableMapOf()
    val structures: ArrayList<Structure> = ArrayList()
    val indices: MutableMap<String, Int> = mutableMapOf()

    /** Access a structure by its canonical name, the pointer version returns NULL on failure while the reference
     *  version raises an error. */
    operator fun get(ss: String): Structure {
        val index = indices[ss] ?: throw Exception("BlendDNA: Did not find a structure named `$ss`")
        return get(index)
    }

    /** Access a structure by its index */
    operator fun get(i: Int): Structure = structures.getOrElse(i) { throw Exception("BlendDNA: There is no structure with index `$i`") }

    /** Add structure definitions for all the primitive types, i.e. integer, short, char, float */
    fun addPrimitiveStructures(db: FileDatabase) {
        /*  NOTE: these are just dummies. Their presence enforces Structure::Convert<target_type> to be called on these
            empty structures. These converters are special overloads which scan the name of the structure and perform
            the required data type conversion if one of these special names is found in the structure in question.  */

        indices["int"] = structures.size
        structures += Structure(db).apply { name = "int"; size = 4; }

        indices["short"] = structures.size
        structures += Structure(db).apply { name = "short"; size = 2; }

        indices["char"] = structures.size
        structures += Structure(db).apply { name = "char"; size = 1; }

        indices["float"] = structures.size
        structures += Structure(db).apply { name = "float"; size = 4; }

        indices["double"] = structures.size
        structures += Structure(db).apply { name = "double"; size = 8; }

        // no long, seemingly.
    }

	/**
	 *  basing on http://www.blender.org/development/architecture/notes-on-sdna/
     *  Fill the @c converters member with converters for all known data types. The implementation of this method is in
     *  BlenderScene.cpp and is machine-generated.
     *  Converters are used to quickly handle objects whose exact data type is a runtime-property and not yet known
     *  at compile time (consier Object::data).
	 */
    @Suppress("UNCHECKED_CAST")
    fun registerConverters() {
        // HINT: the conversion to ElemBaseConverter is necessary to turn the Structure argument into a receiver
		// TODO Kotlin 1.4 Unnecessary casts with new type inference (I think)
        converters["Object"] = ::Object to Structure::convertObject as ElemBaseConverter
        converters["Group"] = ::Group to Structure::convertGroup as ElemBaseConverter
        converters["MTex"] = ::MTex to Structure::convertMTex as ElemBaseConverter
        converters["TFace"] = ::TFace to Structure::convertTFace as ElemBaseConverter
        converters["SubsurfModifierData"] = ::SubsurfModifierData to Structure::convertSubsurfModifierData as ElemBaseConverter
        converters["MFace"] = ::MFace to Structure::convertMFace as ElemBaseConverter
        converters["Lamp"] = ::Lamp to Structure::convertLamp as ElemBaseConverter
        converters["MDeformWeight"] = ::MDeformWeight to Structure::convertMDeformWeight as ElemBaseConverter
        converters["PackedFile"] = ::PackedFile to Structure::convertPackedFile as ElemBaseConverter
        converters["Base"] = ::Base to Structure::convertBase as ElemBaseConverter
        converters["MTFace"] = ::MTFace to Structure::convertMTFace as ElemBaseConverter
        converters["Material"] = ::Material to Structure::convertMaterial as ElemBaseConverter
        converters["MTexPoly"] = ::MTexPoly to Structure::convertMTexPoly as ElemBaseConverter
        converters["Mesh"] = ::Mesh to Structure::convertMesh as ElemBaseConverter
        converters["MDeformVert"] = ::MDeformVert to Structure::convertMDeformVert as ElemBaseConverter
        converters["World"] = ::World to Structure::convertWorld as ElemBaseConverter
        converters["MLoopCol"] = ::MLoopCol to Structure::convertMLoopCol as ElemBaseConverter
        converters["MVert"] = ::MVert to Structure::convertMVert as ElemBaseConverter
        converters["MEdge"] = ::MEdge to Structure::convertMEdge as ElemBaseConverter
        converters["MLoopUV"] = ::MLoopUV to Structure::convertMLoopUV as ElemBaseConverter
        converters["GroupObject"] = ::GroupObject to Structure::convertGroupObject as ElemBaseConverter
        converters["ListBase"] = ::ListBase to Structure::convertListBase as ElemBaseConverter
        converters["MLoop"] = ::MLoop to Structure::convertMLoop as ElemBaseConverter
        converters["ModifierData"] = ::ModifierData to Structure::convertModifierData as ElemBaseConverter
        converters["ID"] = ::Id to Structure::convertId as ElemBaseConverter
        converters["MCol"] = ::MCol to Structure::convertMCol as ElemBaseConverter
        converters["MPoly"] = ::MPoly to Structure::convertMPoly as ElemBaseConverter
        converters["Scene"] = ::Scene to Structure::convertScene as ElemBaseConverter
        converters["Library"] = ::Library to Structure::convertLibrary as ElemBaseConverter
        converters["Tex"] = ::Tex to Structure::convertTex as ElemBaseConverter
        converters["Camera"] = ::Camera to Structure::convertCamera as ElemBaseConverter
        converters["MirrorModifierData"] = ::MirrorModifierData to Structure::convertMirrorModifierData as ElemBaseConverter
        converters["Image"] = ::Image to Structure::convertImage as ElemBaseConverter
        converters["CustomData"] = ::CustomData to Structure::convertCustomData as ElemBaseConverter
        converters["CustomDataLayer"] = ::CustomDataLayer to Structure::convertCustomDataLayer as ElemBaseConverter
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
     *  @return A null pointer if no appropriate converter is available.
     */
    fun getBlobToStructureConverter(structure: Structure): Pair<ElemBaseConstructor, ElemBaseConverter>? = converters[structure.name]

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

/** Describes a master file block header. Each master file sections holds n elements of a certain SDNA structure
 *  (or otherwise unspecified data).
 *  FileBlockHeads are ordered by their [address]
 *  */
data class FileBlockHead(
		/** points right after the header of the file block */
		var start: Int = 0,
        var id: String = "",
        var size: Int = 0,
        // original memory address of the data
        var address: Long = 0L,
        // index into DNA
        var dnaIndex: Int = 0,
        // number of structure instances to follow
        var num: Int = 0) : Comparable<FileBlockHead> {

    // file blocks are sorted by address to quickly locate specific memory addresses
    override fun compareTo(other: FileBlockHead): Int = address.compare(other.address)
}

/** Utility to read all master file blocks in turn. */
class SectionParser(val stream: ByteBuffer, val ptr64: Boolean) {

    val current = FileBlockHead()

    /** Advance to the next section.
     *  @throw DeadlyImportError if the last chunk was passed. */
    fun next() {

	    // we can not simply increment the pos, because the stream pos is undefined when next is called
        stream.pos = current.start + current.size

        val tmp = CharArray(4) { stream.get().c }
        current.id = String(tmp, 0, if (tmp[3] != NUL) 4 else if (tmp[2] != NUL) 3 else if (tmp[1] != NUL) 2 else 1)

        current.size = stream.int
        current.address = if (ptr64) stream.long else stream.int.L

        current.dnaIndex = stream.int
        current.num = stream.int

        current.start = stream.pos

	    if(ASSIMP.BLENDER_DEBUG){
		    logger.info("BLEND FileBlockHead: ${current.id}")
	    }

        if (stream.limit() - stream.pos < current.size)
            throw Exception("BLEND: invalid size of file block")
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
    val caches = ArrayList<MutableMap<Long, Any>>(64)

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

            @Suppress("UNCHECKED_CAST")
            out.set(it as T)

            if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.cacheHits
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
        caches[s.cacheIdx.i][ptr] = out() as Any

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.cachedObjects
    }
}

/** Memory representation of a full BLEND file and all its dependencies. The output AiScene is constructed from
 *  an instance of this data structure. */
class FileDatabase(var reader: ByteBuffer, var i64bit: Boolean = false, var little: Boolean = false) {
    // TODO ensure little is used everywhere to ensure endianes is correct

    val pointerSize get() = if(i64bit) 8 else 4

    var dna = DNA()
    val entries = ArrayList<FileBlockHead>()

    val stats = Statistics()

    /** For all our templates to work on both shared_ptr's and vector's using the same code, a dummy cache for arrays is
     *  provided. Actually, arrays of objects are never cached because we can't easily ensure their proper destruction. */
    val cache = ObjectCache(this)

    val cacheArrays = ArrayList<ObjectCache>()

    var nextCacheIdx = 0L
}

/** Factory to extract a #DNA from the DNA1 file block in a BLEND file. */
class DnaParser(
        /** Bind the parser to a empty DNA and an input stream */
        val db: FileDatabase) {

    /** Locate the DNA in the file and parse it. The input stream is expected to point to the beginning of the DNA1 chunk
     *  at the time this method is called and is undefined afterwards.
     *  @throw DeadlyImportError if the DNA cannot be read.
     *  @note The position of the stream pointer is undefined afterwards.   */
    fun parse() {

        val stream = db.reader

        if (stream doesntMatch "SDNA") throw Exception("BlenderDNA: Expected SDNA chunk")
        // name dictionary
        if (stream doesntMatch "NAME") throw Exception("BlenderDNA: Expected NAME field")

        val names = Array(stream.int) { stream.nextWord().also { stream.consumeNUL() } }

        // type dictionary
        while (((stream.pos - 12) and 0x3) != 0) stream.get()
        if (stream doesntMatch "TYPE") throw Exception("BlenderDNA: Expected TYPE field")

        val types = Array(stream.int) {
            Type().apply {
                name = stream.nextWord().also { stream.consumeNUL() }
            }
        }

        // type length dictionary
        while (((stream.pos - 12) and 0x3) != 0) stream.get()
        if (stream doesntMatch "TLEN") throw Exception("BlenderDNA: Expected TLEN field")

        for (s in types) s.size = stream.short.L

        // structures dictionary
        while (((stream.pos - 12) and 0x3) != 0) stream.get()
        if (stream doesntMatch "STRC") throw Exception("BlenderDNA: Expected STRC field")

        val end = stream.int
        var fields = 0

        dna.structures.ensureCapacity(end)
        for (i in 0 until end) {

            val structureTypeIndex = stream.short.i
            if (structureTypeIndex >= types.size) throw Exception("BlenderDNA: Invalid type index in structure name $structureTypeIndex (there are only ${types.size} entries)")

            // maintain separate indexes
            dna.indices[types[structureTypeIndex].name] = dna.structures.size

            val s = Structure(db).apply { name = types[structureTypeIndex].name }
            dna.structures += s
            //s.index = dna.structures.size()-1;

	        val fieldCount = stream.short.i
            s.fields.ensureCapacity(fieldCount)

            var offset = 0L
            for (fieldIndex in 0 until fieldCount) {

                var fieldTypeIndex = stream.short.i
                if (fieldTypeIndex >= types.size) throw Exception("BlenderDNA: Invalid type index in structure field $fieldTypeIndex (there are only ${types.size} entries)")

                val f = Field()
                s.fields += f
                f.offset = offset

                f.type = types[fieldTypeIndex].name
                f.size = types[fieldTypeIndex].size

                fieldTypeIndex = stream.short.i
                if (fieldTypeIndex >= names.size) throw Exception("BlenderDNA: Invalid name index in structure field $fieldTypeIndex (there are only ${names.size} entries)")

                f.name = names[fieldTypeIndex]
                f.flags = 0

                /*  pointers always specify the size of the pointee instead of their own.
                    The pointer asterisk remains a property of the lookup name.                 */
                if (f.name[0] == '*') {
                    f.size = db.pointerSize.L
                    f.flags = f.flags or FieldFlag.Pointer
                }

                /*  arrays, however, specify the size of a single element so we need to parse the (possibly
                    multi-dimensional) array declaration in order to obtain the actual size of the array in the file.
                    Also we need to alter the lookup name to include no array brackets anymore or size fixup won't work
                    (if our size does not match the size read from the DNA).    */
                if (f.name.contains(']')) {
                    val rb = f.name.indexOf('[')
                    if (rb == -1) throw Exception("BlenderDNA: Encountered invalid array declaration ${f.name}")

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

//        #ifdef ASSIMP_BUILD_BLENDER_DEBUG
//                dna.DumpToFile();  TODO
//        #endif

        dna.addPrimitiveStructures(db)
        dna.registerConverters()
    }

    /** Obtain a reference to the extracted DNA information */
    val dna get() = db.dna
}

private infix fun ByteBuffer.match(string: String) = get().c == string[0] && get().c == string[1] && get().c == string[2] && get().c == string[3]
private infix fun ByteBuffer.doesntMatch(string: String) = !match(string)

class Type {
    var size = 0L
    var name = ""

	override fun toString(): String {
		return "[Type]: $name, size: $size"
	}
}