package assimp

import assimp.AiMetadataType as Mt

/** Enum used to distinguish data types */
enum class AiMetadataType { BOOL, INT32, UINT64, FLOAT, DOUBLE, AISTRING, AIVECTOR3D }

/**
 * Metadata entry
 *
 * The type field uniquely identifies the underlying type of the data field
 */
class AiMetadataEntry<T>(val type: Mt, val data: T) {
    constructor (d: T) : this(d!!.type, d)
}


/**
 * Container for holding metadata.
 *
 * Metadata is a key-value store using string keys and values.
 */
class AiMetadata(
        /** Arrays of keys, may not be NULL. Entries in this array may not be NULL as well.
         *  Arrays of values, may not be NULL. Entries in this array may be NULL if the corresponding property key has no
         *  assigned value. => JVM map  */
        val map: MutableMap<String, AiMetadataEntry<*>?> = mutableMapOf()
) {

    /** Length of the mKeys and mValues arrays, respectively */
    val numProperties get() = map.size

    operator fun <T> set(key: String, value: T) = when {
    // Ensure that we have a valid key.
        key.isEmpty() -> false
        else -> {
            // Set metadata key
            map[key] = AiMetadataEntry(value)
            true
        }
    }
    fun clear() = map.clear()
    fun isEmpty() = map.isEmpty()
    fun isNotEmpty() = map.isNotEmpty()
    operator fun <T> get(key: String) = map[key]?.data as? T
}

private val Any.type
    get() = when (this) {
        is Boolean -> Mt.BOOL
        is Int -> Mt.INT32
        is Long -> Mt.UINT64
        is Float -> Mt.FLOAT
        is Double -> Mt.DOUBLE
        is String -> Mt.AISTRING
        is AiVector3D -> Mt.AIVECTOR3D
        else -> throw Error()
    }