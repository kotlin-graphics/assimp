package assimp

/**
 * Created by elect on 16/11/2016.
 */

/** Maximum dimension for strings, ASSIMP strings are zero terminated. */
const val MAXLEN = 1024

/** Standard return type for some library functions.
 *  Rarely used, and if, mostly in the C API.
 */
enum class AiReturn(val i: Int) {
    /** Indicates that a function was successful */
    SUCCESS(0x0),
    /** Indicates that a function failed */
    FAILURE(-0x1),
    /** Indicates that not enough memory was available to perform the requested operation */
    OUTOFMEMORY(-0x3)
}

class AiMemoryInfo {
    /** Storage allocated for texture data */
    var textures = 0
    /** Storage allocated for material data  */
    var materials = 0
    /** Storage allocated for mesh data */
    var meshes = 0
    /** Storage allocated for node data */
    var nodes = 0
    /** Storage allocated for animation data */
    var animations = 0
    /** Storage allocated for camera data */
    var cameras = 0
    /** Storage allocated for light data */
    var lights = 0
    /** Total storage allocated for the full import. */
    var total = 0
}
