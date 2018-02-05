package assimp

import glm_.BYTES
import glm_.mat4x4.Mat4

/**
 * Created by elect on 13/11/2016.
 */

// -------------------------------------------------------------------------------
/**
 * A node in the imported hierarchy.
 *
 * Each node has name, a parent node (except for the root node), a transformation relative to its parent and possibly
 * several child nodes.
 * Simple file formats don't support hierarchical structures - for these formats the imported scene does consist of only
 * a single root node without children. */
// -------------------------------------------------------------------------------

data class AiNode(

        /** The name of the node.
         *
         * The name might be empty (length of zero) but all nodes which need to be referenced by either bones or
         * animations are named.
         * Multiple nodes may have the same name, except for nodes which are referenced by bones (see #aiBone and
         * #aiMesh::bones). Their names *must* be unique.
         *
         * Cameras and lights reference a specific node by name - if there are multiple nodes with this name, they are
         * assigned to each of them.
         * <br>
         * There are no limitations with regard to the characters contained in the name string as it is usually taken
         * directly from the source file.
         *
         * Implementations should be able to handle tokens such as whitespace, tabs, line feeds, quotation marks,
         * ampersands etc.
         *
         * Sometimes old introduces new nodes not present in the source file into the hierarchy (usually out of
         * necessity because sometimes the source hierarchy format is simply not compatible). Their names are surrounded
         * by @verbatim <> @endverbatim e.g.
         *  @verbatim<DummyRootNode> @endverbatim.         */
        var name: String = "",

        /** The transformation relative to the node's parent. */
        var transformation: AiMatrix4x4 = AiMatrix4x4(),

        /** Parent node. NULL if this node is the root node. */
        var parent: AiNode? = null,

        /** The number of child nodes of this node. */
        var numChildren: Int = 0,

        /** The child nodes of this node. NULL if numChildren is 0. */
        var children: MutableList<AiNode> = mutableListOf(),

        /** The number of meshes of this node. */
        var numMeshes: Int = 0,

        /** The meshes of this node. Each entry is an index into the mesh list of the #aiScene.     */
        var meshes: IntArray = intArrayOf(),

        /** Metadata associated with this node or empty if there is no metadata.
         *  Whether any metadata is generated depends on the source file format. See the @link importer_notes
         *  @endlink page for more information on every source file format. Importers that don't document any metadata
         *  don't write any.         */
        var metaData: AiMetadata = AiMetadata()
) {

    constructor(other: AiNode) : this(other.name, AiMatrix4x4(other.transformation), other.parent, other.numChildren,
            MutableList(other.children.size, {other.children[it]}), other.numMeshes, other.meshes.clone()) // TODO metadata

    fun findNode(name: String): AiNode? {
        if(this.name == name) return this
        return children.firstOrNull { it.findNode(name) != null }
    }

//    infix fun put(other: AiNode) {
//        name = other.name
//        transformation put other.transformation
//        parent = other.parent
//        numChildren = other.numChildren
//        children.clear()
//        children.addAll(other.children)
//        numMeshes = other.numMeshes
//        meshes = other.meshes.clone()
//        metaData = other.metaData   // TODO copy?
//    }

    companion object {
        val size = Mat4.size + 2 * Int.BYTES
    }
}

// -------------------------------------------------------------------------------
/**
 * Specifies that the scene data structure that was imported is not complete.
 * This flag bypasses some internal validations and allows the import of animation skeletons, material libraries or
 * camera animation paths using Assimp. Most applications won't support such data. */
const val AI_SCENE_FLAGS_INCOMPLETE = 0x1

/**
 * This flag is set by the validation postprocess-step (aiPostProcess_ValidateDS) if the validation is successful. In a
 * validated scene you can be sure that any cross references in the data structure (e.g. vertex indices) are valid. */
const val AI_SCENE_FLAGS_VALIDATED = 0x2

/**
 * This flag is set by the validation postprocess-step (aiPostProcess_ValidateDS) if the validation is successful but
 * some issues have been found.
 * This can for example mean that a texture that does not exist is referenced by a material or that the bone weights for
 * a vertex don't sum to 1.0 ... .
 * In most cases you should still be able to use the import. This flag could be useful for applications which don't
 * capture Assimp's log output. */
const val AI_SCENE_FLAGS_VALIDATION_WARNING = 0x4

/**
 * This flag is currently only set by the aiProcess_JoinIdenticalVertices step.
 * It indicates that the vertices of the output meshes aren't in the internal verbose format anymore. In the verbose
 * format all vertices are unique, no vertex is ever referenced by more than one face. */
const val AI_SCENE_FLAGS_NON_VERBOSE_FORMAT = 0x8

/**
 * Denotes pure height-map terrain data. Pure terrains usually consist of quads, sometimes triangles, in a regular grid.
 * The x,y coordinates of all vertex positions refer to the x,y coordinates on the terrain height map, the z-axis stores
 * the elevation at a specific point.
 *
 * TER (Terragen) and HMP (3D Game Studio) are height map formats.
 * @note Assimp is probably not the best choice for loading *huge* terrains - fully triangulated data takes extremely
 * much free store and should be avoided as long as possible (typically you'll do the triangulation when you actually
 * need to render it). */
const val AI_SCENE_FLAGS_TERRAIN = 0x10

/**
 * Specifies that the scene data can be shared between structures. For example: one vertex in few faces.
 * \ref AI_SCENE_FLAGS_NON_VERBOSE_FORMAT can not be used for this because \ref AI_SCENE_FLAGS_NON_VERBOSE_FORMAT has
 * internal meaning about postprocessing steps. */
const val AI_SCENE_FLAGS_ALLOW_SHARED = 0x20

// -------------------------------------------------------------------------------
/** The root structure of the imported data.
 *
 *  Everything that was imported from the given file can be accessed from here.
 *  Objects of this class are generally maintained and owned by Assimp, not by the caller. You shouldn't want to
 *  instance it, nor should you ever try to delete a given scene on your own. */
// -------------------------------------------------------------------------------
class AiScene {

    /** Any combination of the AI_SCENE_FLAGS_XXX flags. By default this value is 0, no flags are set. Most
     * applications will want to reject all scenes with the AI_SCENE_FLAGS_INCOMPLETE bit set.         */
    var flags = 0

    /** The root node of the hierarchy.
     *
     * There will always be at least the root node if the import was successful (and no special flags have been set).
     * Presence of further nodes depends on the format and content of the imported file.         */
    lateinit var rootNode: AiNode

    /** The number of meshes in the scene. */
    var numMeshes = 0

    /** The array of meshes.
     *
     * Use the indices given in the aiNode structure to access this array. The array is numMeshes in size. If the
     * AI_SCENE_FLAGS_INCOMPLETE flag is not set there will always be at least ONE material.         */
    var meshes = ArrayList<AiMesh>()

    /** The number of materials in the scene. */
    var numMaterials = 0

    /** The array of materials.
     *
     * Use the index given in each aiMesh structure to access this array. The array is numMaterials in size. If the
     * AI_SCENE_FLAGS_INCOMPLETE flag is not set there will always be at least ONE material.         */
    var materials = ArrayList<AiMaterial>()

    /** The number of animations in the scene. */
    var numAnimations = 0

    /** The array of animations.
     *
     * All animations imported from the given file are listed here.
     * The array is numAnimations in size.         */
    var animations = ArrayList<AiAnimation>()

    /** The number of textures embedded into the file */
    var numTextures = 0

    /** JVM ASSIMP CUSTOM, the array of the textures used in the scene.
     *
     * Not many file formats embed their textures into the file.
     * An example is Quake's MDL format (which is also used by some GameStudio versions)
     */
    val textures = mutableMapOf<String, gli_.Texture>()

    /** The number of light sources in the scene. Light sources are fully optional, in most cases this attribute
     * will be 0         */
    var numLights = 0

    /** The array of light sources.
     *
     * All light sources imported from the given file are listed here. The array is numLights in size.         */
    var lights = ArrayList<AiLight>()

    /** The number of cameras in the scene. Cameras are fully optional, in most cases this attribute will be 0         */
    var numCameras = 0

    /** The array of cameras.
     *
     * All cameras imported from the given file are listed here.
     * The array is numCameras in size. The first camera in the array (if existing) is the default camera view into
     * the scene.         */
    var cameras = ArrayList<AiCamera>()

    /** The global metadata assigned to the scene itself.
     *
     *  This data contains global metadata which belongs to the scene like unit-conversions, versions, vendors or
     *  other model-specific data. This can be used to store format-specific metadata as well.     */
    var metaData = AiMetadata()

    /** Check whether the scene contains meshes
     *  Unless no special scene flags are set this will always be true. */
    fun hasMeshes() = meshes.isNotEmpty()

    /** Check whether the scene contains materials
     *  Unless no special scene flags are set this will always be true. */
    fun hasMaterials() = materials.isNotEmpty()

    /** Check whether the scene contains lights */
    fun hasLights() = lights.isNotEmpty()

    /** Check whether the scene contains textures   */
    fun hasTextures() = textures.isNotEmpty()

    /** Check whether the scene contains cameras    */
    fun hasCameras() = cameras.isNotEmpty()

    /** Check whether the scene contains animations */
    fun hasAnimations() = animations.isNotEmpty()

    companion object {
        val size = 7 * Int.BYTES
    }
}