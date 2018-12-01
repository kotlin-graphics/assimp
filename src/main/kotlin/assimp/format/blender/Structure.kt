package assimp.format.blender

import assimp.*
import glm_.*
import kotlin.math.min
import kotlin.reflect.*
import assimp.format.blender.ErrorPolicy as Ep

// TODO check method visibility

/** Represents a data structure in a BLEND file. A Structure defines n fields and their locations and encodings the input stream. Usually, every
 *  Structure instance pertains to one equally-named data structure in the
 *  BlenderScene.h header. This class defines various utilities to map a
 *  binary `blob` read from the file to such a structure instance with
 *  meaningful contents. */
class Structure (val db: FileDatabase) {

    // publicly accessible members
    var name = ""
    val fields = ArrayList<Field>()
    val indices = mutableMapOf<String, Long>()

    var size = 0L

    var cacheIdx = -1L

    override fun toString(): String {
        return "[Structure]: $name"
    }

    /** Access a field of the structure by its canonical name. The pointer version returns NULL on failure while
     *  the reference version raises an import error. */
    operator fun get(ss: String): Field {
        val index = indices[ss] ?: throw Exception("BlendDNA: Did not find a field named `$ss` in structure `$name`")
        return fields[index.i]
    }

    /** Access a field of the structure by its index */
    operator fun get(i: Long) = fields.getOrElse(i.i) { throw Exception("BlendDNA: There is no field with index `$i` in structure `$name`") }

    override fun equals(other: Any?) = other is Structure && name == other.name // name is meant to be an unique identifier

    fun convertInt() = when (name) {
        "int"    -> db.reader.int.i
        "short"  -> db.reader.short.i
        "char"   -> db.reader.get().c.i
        "float"  -> db.reader.float.i
        "double" -> db.reader.double.i
        else     -> throw Exception("Unknown source for conversion to primitive data type: $name")
    }

    /** Try to read an instance of the structure from the stream and attempt to convert to `T`. This is done by an
     *  appropriate specialization. If none is available, a compiler complain is the result.
     *  @param dest Destination value to be written
     *  @param db File database, including input stream. */
    val convertChar
        get() = when (name) {
        // automatic rescaling from char to float and vice versa (seems useful for RGB colors)
            "float" -> (db.reader.float * 255f).c
            "double" -> (db.reader.double * 255f).c
            "int"    -> db.reader.int.c
            "short"  -> db.reader.short.c
            "char"   -> db.reader.get().c
            else     -> throw Exception("Unknown source for conversion to primitive data type: $name")
        }

    val convertShort
        get() = when (name) {
        // automatic rescaling from short to float and vice versa (seems to be used by normals)
            "float" -> {
                var f = db.reader.float
                if (f > 1f) f = 1f
                (f * 32767f).s
            }
            "double" -> (db.reader.double * 32767.0).s
            "int"    -> db.reader.int.s
            "short"  -> db.reader.short
            "char"   -> db.reader.get().c.s
            else     -> throw Exception("Unknown source for conversion to primitive data type: $name")
        }

    val convertFloat
        get() = when (name) {
            // automatic rescaling from char to float and vice versa (seems useful for RGB colors)
            "char"   -> db.reader.get() / 255f
            // automatic rescaling from short to float and vice versa (used by normals)
            "short"  -> db.reader.short / 32767f
            "int"    -> db.reader.int.f
            "float"  -> db.reader.float
            "double" -> db.reader.double.f
            else     -> throw Exception("Unknown source for conversion to primitive data type: $name")
        }

    fun convertPointer(): Long = if (db.i64bit) db.reader.long else db.reader.int.L

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fields.hashCode()
        result = 31 * result + indices.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + cacheIdx.hashCode()
        return result
    }

    /** field parsing for 1d arrays */
    fun readFieldString(errorPolicy: Ep, name: String): String {

        var dest = ""
        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array)
                throw Exception("Field `$name` of structure `${this.name}` ought to be a string")

            db.reader.pos += f.offset.i

            val builder = StringBuilder()

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until f.arraySizes[0]) {
                val c = s.convertChar
                if (c != NUL) builder += c
                else break
            }

//            for(; i < M; ++i) {
//                _defaultInitializer<ErrorPolicy_Igno>()(out[i])
//            }
            dest = builder.toString()

        } catch (e: Exception) {
            error(errorPolicy, dest, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead

        return dest
    }

    /** field parsing for 1d arrays */
    fun readFieldFloatArray(errorPolicy: Ep, out: FloatArray, name: String) {

        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array)
                throw Exception("Field `$name` of structure `${this.name}` ought to be an array of size ${out.size}")

            db.reader.pos += f.offset.i

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until min(f.arraySizes[0].i, out.size))
                out[i] = s.convertFloat

//            for (; i < M; ++i) {
//                _defaultInitializer<ErrorPolicy_Igno>()(out[i])
//            }
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead
    }

    /** field parsing for 1d arrays */
    fun readFieldIntArray(errorPolicy: Ep, out: IntArray, name: String) {

        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array)
                throw Exception("Field `$name` of structure `${this.name}` ought to be an array of size ${out.size}")

            db.reader.pos += f.offset.i

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until min(f.arraySizes[0].i, out.size))
                out[i] = s.convertInt()

//            for (; i < M; ++i) {
//                _defaultInitializer<ErrorPolicy_Igno>()(out[i])
//            }
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead
    }

    /** field parsing for 2d arrays */
    fun <T> readFieldArray2(errorPolicy: Ep, out: Array<T>, name: String) {

        val old = db.reader.pos
        try {
            val f = get(name)
            val s = db.dna[f.type]

            // is the input actually an array?
            if (f.flags hasnt FieldFlag.Array) throw Exception("Field `$name` of structure `${this.name}` ought to be an array of size ${out.size}*N")

            db.reader.pos += f.offset.i

            // size conversions are always allowed, regardless of error_policy
            for (i in 0 until min(f.arraySizes[0].i, out.size)) {

                val n = out[i]
                if (n is FloatArray) for (j in n.indices) n[j] = s.convertFloat
            }
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
        }

        // and recover the previous stream position
        db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead
    }

    private inline fun <T>readFieldPtrPrivate(errorPolicy: Ep, out: KMutableProperty0<T?>, name: String,
                                              nonRecursive: Boolean = false,
                                              resolve: (Ep, KMutableProperty0<T?>, Long, Field, Boolean) -> Boolean): Boolean {

        val old = db.reader.pos
        val ptrval: Long
        val f: Field
        try {
            f = get(name)

            // sanity check, should never happen if the genblenddna script is right
            if (f.flags hasnt FieldFlag.Pointer) throw Exception("Field `$name` of structure `${this.name}` ought to be a pointer")

            db.reader.pos += f.offset.i

            ptrval = convertPointer()
            /*  actually it is meaningless on which Structure the Convert is called because the `Pointer` argument
                triggers a special implementation.             */
        } catch (e: Exception) {
            error(errorPolicy, out, e.message)
            out.set(null)
            return false
        }

        // resolve the pointer and load the corresponding structure
        val res = resolve(errorPolicy, out, ptrval, f, nonRecursive)
        // and recover the previous stream position
        if (!nonRecursive) db.reader.pos = old

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead

        return res
    }

    /** field parsing for pointer or dynamic array types (std::shared_ptr)
     *  The return value indicates whether the data was already cached. */
    fun <T> readFieldPtr(errorPolicy: Ep, out: KMutableProperty0<T?>, name: String, targetIsList: Boolean = false, nonRecursive: Boolean = false): Boolean =
            readFieldPtrPrivate(errorPolicy, out, name, nonRecursive) { ep, o, ptrVal, field, nonRec ->
                resolvePtr(ep, o, ptrVal, field, targetIsList, nonRec)
            }

    fun <T> readFieldPtrList(errorPolicy: Ep, out: KMutableProperty0<List<T>?>, name: String, nonRecursive: Boolean = false): Boolean =
            readFieldPtrPrivate(errorPolicy, out, name, nonRecursive) { ep, o, ptrVal, field, _ ->
                resolvePointerList(ep, o, ptrVal, field)
            }

	/**
	 * field parsing for vectors of pointers where the number of elements is defined in the file block header
	 * @return *true* when read was successful
	 */
	fun <T: ElemBase> readFieldPtrVector(errorPolicy: Ep, out: MutableList<T?>, name: String): Boolean {

		out.clear()

		val old = db.reader.pos
		val ptrval: Long
		val f: Field
		try {
			f = get(name)

			// sanity check, should never happen if the genblenddna script is right
			if (f.flags hasnt FieldFlag.Pointer) throw Exception("Field `$name` of structure `${this.name}` ought to be a pointer")

			db.reader.pos += f.offset.i

			ptrval = convertPointer()
			/*  actually it is meaningless on which Structure the Convert is called because the `Pointer` argument
				triggers a special implementation.             */
		} catch (e: Exception) {
			error(errorPolicy, out, e.message)
			return false
		}

		if(ptrval != 0L){
			// find the file block the pointer is pointing to
			val block = locateFileBlockForAddress(ptrval)

			block.setReaderPos(ptrval)

			// TODO does this work with primitives? The question is does it need to? I don't think we will ever see a pointer to a primitive
			val (constructor, converter) = db.dna.converters[f.type] ?: run {
				error(errorPolicy, out, "Failed to find a converter for the `${f.type}` structure")
				return false
			}

			val s = db.dna[f.type]
			for(i in 0 until block.num) {
				tempElemBase = constructor()
				s.converter(::tempElemBase)
				out.add(tempElemBase as T)
			}
		}

		db.reader.pos = old

		if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead

		return true
	}

	fun <T: ElemBase> readCustomDataPtr(errorPolicy: Ep, out: KMutableProperty0<T?>, cdtype: CustomDataType, name: String): Boolean {
		return readFieldPtrPrivate(errorPolicy, out, name) { _, o, ptrVal, _, _ ->

			if(ptrVal == 0L) return true


			val block = locateFileBlockForAddress(ptrVal)
			block.setReaderPos(ptrVal)

			return readCustomData(o, cdtype, block.num, db)
		}
	}

	private fun FileBlockHead.setReaderPos(ptrVal: Long) {
		db.reader.pos = start + (ptrVal - address).i
		// FIXME: basically, this could cause problems with 64 bit pointers on 32 bit systems.
		// I really ought to improve StreamReader to work with 64 bit indices exclusively.
	}

    /** field parsing for static arrays of pointer or dynamic array types (std::shared_ptr[])
     *  The return value indicates whether the data was already cached. */
    fun <T> readFieldPtr(errorPolicy: Ep, out: Array<T?>, name: String): Boolean {

        val oldPos = db.reader.pos

        val ptrval = LongArray(out.size) { 0L }

        val field: Field
        try{
            field = get(name)

            // sanity check, should never happen if the genblenddna script is right
            if(field.flags hasnt FieldFlag.Pointer) throw Exception("Field `$name` of structure `${this.name}` ought to be a pointer")

            db.reader.pos += field.offset.i

            repeat(min(out.size, field.arraySizes[0].i)) {
                ptrval[it] = convertPointer()
            }
            /*
             for(; i < N; ++i) {
	            _defaultInitializer<ErrorPolicy_Igno>()(ptrval[i]);
	        }
             */

            // actually it is meaningless on which Structure the Convert is called
            // because the `Pointer` argument triggers a special implementation.
        } catch (e: Exception) {

            error(errorPolicy, out, e.message)
            for(i in 0 until out.size) {
                out[i] = null
            }
            return false
        }

        var res = true // FIXME: check back with https://github.com/assimp/assimp/issues/2160

        val oldTempAny = tempAny
        for (i in 0 until out.size) {
            // resolve the pointer and load the corresponding structure
            res = resolvePtr(errorPolicy, ::tempAny, ptrval[i], field, false) && res
        }
        tempAny = oldTempAny

        db.reader.pos = oldPos

        if(!ASSIMP.BLENDER_NO_STATS) {
            db.stats.fieldsRead++
        }

        return res
    }

	private inline fun <T: Any> readFieldPrivate(errorPolicy: Ep, out: T, name: String, read: (Structure, T) -> Unit): T {

	    val old = db.reader.pos
	    try {
		    val f = get(name)
		    // find the structure definition pertaining to this field
		    val s = db.dna[f.type]

            db.reader.pos += f.offset.i
		    read(s, out)
	    } catch(e: Exception) {
		    error(errorPolicy, out, e.message)
	    }

	    // and recover the previous stream position
	    db.reader.pos = old


	    if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.fieldsRead

	    return out
    }

	/**
	 * field parsing for `normal` values
     * The return value indicates whether the data was already cached.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> readField(errorPolicy: Ep, out: KMutableProperty0<T>, name: String): KMutableProperty0<T> {

	    return readFieldPrivate(errorPolicy, out, name) { s, o ->
		    when (o()) {
			    is Float -> (out as KMutableProperty0<Float>).set(s.convertFloat)
			    is Short -> (out as KMutableProperty0<Short>).set(s.convertShort)
			    is Int   -> (out as KMutableProperty0<Int>).set(s.convertInt())
			    is Char  -> (out as KMutableProperty0<Char>).set(s.convertChar)
			    else     -> throw Exception("Field type is not yet supported")
		    }
	    }
    }

	fun readField(errorPolicy: Ep, out: Id, name: String): Id {
		return readFieldPrivate(errorPolicy, out, name) { s, o -> s.convert(o) }
	}

	fun readField(errorPolicy: Ep, out: ListBase, name: String): ListBase {
		return readFieldPrivate(errorPolicy, out, name) { s, o -> s.convert(o) }
	}

	fun readField(errorPolicy: Ep, out: CustomData, name: String): CustomData {
		return readFieldPrivate(errorPolicy, out, name) { s, o -> s.convert(o) }
	}

	fun readField(errorPolicy: Ep, out: ModifierData, name: String): ModifierData {
		return readFieldPrivate(errorPolicy, out, name) { s, o -> s.convert(o) }
	}

    @Suppress("UNCHECKED_CAST")
    fun <T> resolvePtr(errorPolicy: Ep, out: KMutableProperty0<T?>, ptrVal: Long, f: Field, targetIsList: Boolean, nonRecursive: Boolean = false) = when {  // TODO T: ElemBase ???
        f.type == "ElemBase" || isElem -> resolvePointer(errorPolicy, out as KMutableProperty0<ElemBase?>, ptrVal)
        else -> resolvePointer(errorPolicy, out, ptrVal, f, targetIsList, nonRecursive)
//        out is FileOffset -> resolvePointer(out, ptrVal, f, nonRecursive)
//        else -> throw Exception()
    }


    fun <T> resolvePointer(errorPolicy: Ep, out: KMutableProperty0<T?>, ptrVal: Long, f: Field, targetIsList: Boolean, nonRecursive: Boolean = false): Boolean {   // TODO T: ElemBase ???

        out.set(null) // ensure null pointers work
        if (ptrVal == 0L) return false

        val s = db.dna[f.type]
        // find the file block the pointer is pointing to
        val block = locateFileBlockForAddress(ptrVal)

        // also determine the target type from the block header and check if it matches the type which we expect.
        val ss = db.dna[block.dnaIndex]
        if (ss !== s)
            throw Exception("Expected target to be of type `${s.name}` but seemingly it is a `${ss.name}` instead")

        // try to retrieve the object from the cache
        db.cache.get(s, out, ptrVal)
        if (out() != null) return true

        // seek to this location, but save the previous stream pointer.
        val pOld = db.reader.pos
	    block.setReaderPos(ptrVal)

        // continue conversion after allocating the required storage

        // TODO does this work with primitives? The question is does it need to? I don't think we will ever see a pointer to a primitive
        val (constructor, converter) = db.dna.converters[f.type] ?: run {
            error(errorPolicy, out, "Failed to find a converter for the `${f.type}` structure")
            return false
        }

        val num = block.size / ss.size.i
        if (targetIsList) {

            val list = MutableList<ElemBase?>(num) { constructor() }

            @Suppress("UNCHECKED_CAST")
            out.set(list as T)

            // cache the object before we convert it to avoid cyclic recursion.
            db.cache.set(s, out, ptrVal)

            // if the non_recursive flag is set, we don't do anything but leave the cursor at the correct position to resolve the object.
            if (!nonRecursive) {
                for (i in 0 until num) {

                    // workaround for https://youtrack.jetbrains.com/issue/KT-16303
                    tempElemBase = list[i]
                    s.converter(::tempElemBase)
                    list[i] = tempElemBase
                }

                db.reader.pos = pOld
            }
        } else {

	        if(num != 1) {
		        error(errorPolicy, out, "Expected to write only a single value for '${f.type}' but got a block with multiple entries!")
		        return false
	        }

            @Suppress("UNCHECKED_CAST")
            out.set(constructor() as T)

            // cache the object before we convert it to avoid cyclic recursion.
            db.cache.set(s, out, ptrVal)

            // if the non_recursive flag is set, we don't do anything but leave the cursor at the correct position to resolve the object.
            if (!nonRecursive) {

                @Suppress("UNCHECKED_CAST")
                s.converter(out as KMutableProperty0<ElemBase?>)

                db.reader.pos = pOld
            }
        }

        if (!ASSIMP.BLENDER_NO_STATS && out() != null)
            ++db.stats.pointersResolved

        return false
    }

    fun resolvePointer(out: FileOffset?, ptrVal: Long) {
        // Currently used exclusively by PackedFile::data to represent a simple offset into the mapped BLEND file.
        TODO("resolvePointer(out: FileOffset?")
//        out.reset();
//        if (!ptrval.val) {
//                    return false;
//                }
//
//                // find the file block the pointer is pointing to
//                const FileBlockHead* block = LocateFileBlockForAddress(ptrval,db);
//
//        out =  std::shared_ptr< FileOffset > (new FileOffset());
//        out->val = block->start+ static_cast<size_t>((ptrval.val - block->address.val) );
//        return false;
    }

    fun resolvePointer(errorPolicy: Ep, out: KMutableProperty0<ElemBase?>, ptrVal: Long): Boolean {

        isElem = false
        /*  Special case when the data type needs to be determined at runtime.
            Less secure than in the `strongly-typed` case.         */

        out.set(null)
        if (ptrVal == 0L) return false

        // find the file block the pointer is pointing to
        val block = locateFileBlockForAddress(ptrVal)

        // determine the target type from the block header
        val s = db.dna[block.dnaIndex]

        // try to retrieve the object from the cache
        db.cache.get(s, out, ptrVal)
        if (out() != null) return true

        // seek to this location, but save the previous stream pointer.
        val pOld = db.reader.pos
	    block.setReaderPos(ptrVal)

        // continue conversion after allocating the required storage
        val (constructor, converter) = db.dna.getBlobToStructureConverter(s) ?: run {

            /*  this might happen if DNA::RegisterConverters hasn't been called so far or
                if the target type is not contained in `our` DNA.             */
            out.set(null)
            error(errorPolicy, out, "Failed to find a converter for the `${s.name}` structure")
            return false
        }

        // allocate the object hull
        out.set(constructor())

        /*  cache the object immediately to prevent infinite recursion in a circular list with a single element
            (i.e. a self-referencing element).         */
        db.cache.set(s, out, ptrVal)

        // and do the actual conversion
        s.converter(out)
        db.reader.pos = pOld

        /*  store a pointer to the name string of the actual type in the object itself. This allows the conversion code
            to perform additional type checking.         */
        out()!!.dnaType = s.name

        if (!ASSIMP.BLENDER_NO_STATS) ++db.stats.pointersResolved

        return false
    }

    fun <T> resolvePointerList(errorPolicy: Ep, out: KMutableProperty0<List<T>?>, ptrVal: Long, field: Field): Boolean {    // TODO T: ElemBase ????
        // This is a function overload, not a template specialization. According to
        // the partial ordering rules, it should be selected by the compiler
        // for array-of-pointer inputs, i.e. Object::mats.

        out.set(null)

        if(ptrVal == 0L) return false

        val block = locateFileBlockForAddress(ptrVal)

        val num = block.size / db.pointerSize

        // keep the old stream position
        val pOld = db.reader.pos
        block.setReaderPos(ptrVal)

        var res = true // FIXME: check back with https://github.com/assimp/assimp/issues/2160

        val oldTempAny = tempAny
        out.set(MutableList(num) {
            val ptr = convertPointer()
            res = resolvePtr(errorPolicy, ::tempAny, ptr, field, targetIsList = false) && res   // TODO check targetIsList is always false
            @Suppress("UNCHECKED_CAST")

            tempAny as T
        })
        tempAny = oldTempAny

        db.reader.pos = pOld

        return res
    }

    fun locateFileBlockForAddress(ptrVal: Long): FileBlockHead {

        /*  the file blocks appear in list sorted by with ascending base addresses so we can run a binary search to locate
            the pointer quickly.

            NOTE: Blender seems to distinguish between side-by-side data (stored in the same data block) and far pointers,
            which are only used for structures starting with an ID.
            We don't need to make this distinction, our algorithm works regardless where the data is stored.    */
        val it = db.entries.firstOrNull { it.address >= ptrVal } ?: run {
	        /*  This is crucial, pointers may not be invalid. This is either a corrupted file or an attempted attack.   */
	        val last = db.entries.maxBy { it.address }!!
	        throw Exception("Failure resolving pointer 0x${ptrVal.toHexString}, no file block falls into this address range. " +
	                    "The last block starts at 0x${last.address.toHexString} and ends at 0x${(last.address + last.size).toHexString}")
        }
        if (ptrVal >= it.address + it.size)
            throw Exception("Failure resolving pointer 0x${ptrVal.toHexString}, nearest file block starting at " +
                    "0x${it.address.toHexString} ends at 0x${(it.address + it.size).toHexString}")
        return it
    }

	companion object {
        // workaround for https://youtrack.jetbrains.com/issue/KT-16303
        internal var tempAny: Any? = null
		internal var tempElemBase: ElemBase? = null
		internal var tempInt = 0

        var isElem = false
    }
}