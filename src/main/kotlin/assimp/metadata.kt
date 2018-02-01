package assimp

import assimp.AiMetadataType as Mt

/** Enum used to distinguish data types */
enum class AiMetadataType { BOOL, INT32, UINT64, FLOAT, DOUBLE, AISTRING, AIVECTOR3D }

/**
 * Metadata entry
 *
 * The type field uniquely identifies the underlying type of the data field
 */
class AiMetadataEntry {
    var type = Mt.BOOL
    var data: Any? = null
}


/**
 * Container for holding metadata.
 *
 * Metadata is a key-value store using string keys and values.
 */
class AiMetadata {

    /** Length of the mKeys and mValues arrays, respectively */
    var numProperties = 0

    /** Arrays of keys, may not be NULL. Entries in this array may not be NULL as well. */
    val keys = arrayListOf<String>()

    /** Arrays of values, may not be NULL. Entries in this array may be NULL if the corresponding property key has no assigned value. */
    val values = arrayListOf<AiMetadataEntry>()

    fun <T: Any>set( index: Int, key: String, value: T ): Boolean {
        // In range assertion
        if ( index >= numProperties ) return false

        // Ensure that we have a valid key.
        if ( key.isEmpty() ) return false

        // Set metadata key
        keys[index] = key

        // Set metadata type
        values[index].type = value.type
        // Copy the given value to the dynamic storage
        values[index].data = value

        return true
    }

    val Any.type get() = when(this) {
        is Boolean -> Mt.BOOL
        is Int -> Mt.INT32
        is Long -> Mt.UINT64
        is Float -> Mt.FLOAT
        is Double -> Mt.DOUBLE
        is String -> Mt.AISTRING
        is AiVector3D -> Mt.AIVECTOR3D
        else -> throw Error()
    }
}