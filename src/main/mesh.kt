package main

import main.mat.Mat4
import main.vec._3.Vec3d
import main.vec._4.Vec4d
import java.util.*

/**
 * Created by elect on 13/11/2016.
 */

// TODO check long/int consts
/** @def AI_MAX_FACE_INDICES
 *  Maximum number of indices per face (polygon). */
const val AI_MAX_FACE_INDICES = 0x7fff

/** @def AI_MAX_BONE_WEIGHTS
 *  Maximum number of indices per face (polygon). */
const val AI_MAX_BONE_WEIGHTS = 0x7fffffff

/** @def AI_MAX_VERTICES
 *  Maximum number of vertices per mesh.  */
const val AI_MAX_VERTICES = 0x7fffffff

/** @def AI_MAX_FACES
 *  Maximum number of faces per mesh. */
const val AI_MAX_FACES = 0x7fffffff

/** @def AI_MAX_NUMBER_OF_COLOR_SETS
 *  Supported number of vertex color sets per mesh. */

const val AI_MAX_NUMBER_OF_COLOR_SETS = 0x8

/** @def AI_MAX_NUMBER_OF_TEXTURECOORDS
 *  Supported number of texture coord sets (UV(W) channels) per mesh */
const val AI_MAX_NUMBER_OF_TEXTURECOORDS = 0x8

// ---------------------------------------------------------------------------
/** @brief A single face in a mesh, referring to multiple vertices.
 *
 * If mNumIndices is 3, we call the face 'triangle', for mNumIndices > 3
 * it's called 'polygon' (hey, that's just a definition!).
 * <br>
 * aiMesh::mPrimitiveTypes can be queried to quickly examine which types of primitive are actually present in a mesh.
 * The #aiProcess_SortByPType flag executes a special post-processing algorithm which splits meshes with *different*
 * primitive types mixed up (e.g. lines and triangles) in several 'clean' submeshes. Furthermore there is a
 * configuration option ( #AI_CONFIG_PP_SBP_REMOVE) to force #aiProcess_SortByPType to remove specific kinds of
 * primitives from the imported scene, completely and forever.
 * In many cases you'll probably want to set this setting to
 * @code
 * aiPrimitiveType_LINE|aiPrimitiveType_POINT
 * @endcode
 * Together with the #aiProcess_Triangulate flag you can then be sure that #aiFace::mNumIndices is always 3.
 * @note Take a look at the @link data Data Structures page @endlink for more information on the layout and winding
 * order of a face.  */
data class AiFace(
        //! Number of indices defining this face.
        //! The maximum value for this member is #AI_MAX_FACE_INDICES.
        var mNumIndices: Int = 0,

        //! Pointer to the indices array. Size of the array is given in numIndices.
        var mIndices: MutableList<Int> = mutableListOf())

// ---------------------------------------------------------------------------
/** @brief A single influence of a bone on a vertex.
 */
data class AiVertexWeight(
        //! Index of the vertex which is influenced by the bone.
        var mVertexId: Int = 0,

        //! The strength of the influence in the range (0...1).
        //! The influence from all bones at one vertex amounts to 1.
        var mWeight: Float = 0f)

// ---------------------------------------------------------------------------
/** @brief A single bone of a mesh.
 *
 *  A bone has a name by which it can be found in the frame hierarchy and by which it can be addressed by animations.
 *  In addition it has a number of influences on vertices.
 */
data class AiBone(
        //! The name of the bone.
        var mName: String = "",

        //! The number of vertices affected by this bone
        //! The maximum value for this member is #AI_MAX_BONE_WEIGHTS.
        var mNumWeights: Int = 0,

        //! The vertices affected by this bone
        var mWeights: List<AiVertexWeight> = listOf(),

        //! Matrix that transforms from mesh space to bone space in bind pose
        var mOffsetMatrix: AiMatrix4x4 = AiMatrix4x4()
) {
    //! Copy constructor
//    aiBone(const aiBone& other) TODO check if in data
}

// ---------------------------------------------------------------------------
/** @brief Enumerates the types of geometric primitives supported by Assimp.
 * 1 1
 *  @see aiFace Face data structure
 *  @see aiProcess_SortByPType Per-primitive sorting of meshes
 *  @see aiProcess_Triangulate Automatic triangulation
 *  @see AI_CONFIG_PP_SBP_REMOVE Removal of specific primitive types.
 */
enum class AiPrimitiveType(val i: Int) {
    /** A point primitive.
     *
     * This is just a single vertex in the virtual world, #aiFace contains just one index for such a primitive.     */
    POINT(0x1),

    /** A line primitive.
     *
     * This is a line defined through a start and an end position.
     * #aiFace contains exactly two indices for such a primitive.     */
    LINE(0x2),

    /** A triangular primitive.
     *
     * A triangle consists of three indices.     */
    TRIANGLE(0x4),

    /** A higher-level polygon with more than 3 edges.
     *
     * A triangle is a polygon, but polygon in this context means "all polygons that are not triangles". The
     * "Triangulate"-Step is provided for your convenience, it splits all polygons in triangles (which are much easier
     * to handle).     */
    POLYGON(0x8);

    companion object {
        fun of(i: Int) = values().first { it.i == i }
    }
}

infix fun Int.or(other: AiPrimitiveType) = this or other.i

fun AI_PRIMITIVE_TYPE_FOR_N_INDICES(n: Int) = if (n > 3) AiPrimitiveType.POLYGON else AiPrimitiveType.of(1 shl (n - 1))

data class AiAnimMesh(
        /** Replacement for aiMesh::mVertices. If this array is non-NULL, it *must* contain mNumVertices entries. The
         * corresponding array in the host mesh must be non-NULL as well - animation
         *  meshes may neither add or nor remove vertex components (if
         *  a replacement array is NULL and the corresponding source
         *  array is not, the source data is taken instead)*/
        var mVertices: MutableList<AiVector3D> = mutableListOf(),

        /** Replacement for aiMesh::mNormals.  */
        var mNormals: MutableList<AiVector3D> = mutableListOf(),

        /** Replacement for aiMesh::mTangents. */
        var mTangents: MutableList<AiVector3D> = mutableListOf(),

        /** Replacement for aiMesh::mBitangents. */
        var mBitangents: MutableList<AiVector3D> = mutableListOf(),

        /** Replacement for aiMesh::mColors */
        var mColors: MutableList<AiColor4D?> = mutableListOf(),

        /** Replacement for aiMesh::mTextureCoords */
        var mTextureCoords: MutableList<AiVector3D?> = mutableListOf(),

        /** The number of vertices in the aiAnimMesh, and thus the length of all the member arrays.
         *
         * This has always the same value as the mNumVertices property in the corresponding aiMesh. It is duplicated
         * here merely to make the length of the member arrays accessible even if the aiMesh is not known, e.g. from
         * language bindings.         */
        var mNumVertices: Int = 0
) {
    /** Check whether the anim mesh overrides the vertex positions of its host mesh*/
    fun hasPositions() = mVertices.isNotEmpty()

    /** Check whether the anim mesh overrides the vertex normals of its host mesh*/
    fun hasNormals() = mNormals.isNotEmpty()

    /** Check whether the anim mesh overrides the vertex tangents and bitangents of its host mesh. As for aiMesh,
     * tangents and bitangents always go together. */
    fun hasTangentsAndBitangents() = mTangents.isNotEmpty()

    /** Check whether the anim mesh overrides a particular set of vertex colors on his host mesh.
     *  @param pIndex 0<index<AI_MAX_NUMBER_OF_COLOR_SETS */
    fun hasVertexColors(pIndex: Int) = if (pIndex >= AI_MAX_NUMBER_OF_COLOR_SETS) false else mColors[pIndex] != null

    /** Check whether the anim mesh overrides a particular set of texture coordinates on his host mesh.
     *  @param pIndex 0<index<AI_MAX_NUMBER_OF_TEXTURECOORDS */
    fun hasTextureCoords(pIndex: Int) = if (pIndex >= AI_MAX_NUMBER_OF_TEXTURECOORDS) false else mTextureCoords[pIndex] != null
}

// ---------------------------------------------------------------------------
/** @brief A mesh represents a geometry or model with a single material.
 *
 * It usually consists of a number of vertices and a series of primitives/faces referencing the vertices. In addition
 * there might be a series of bones, each of them addressing a number of vertices with a certain weight. Vertex data is
 * presented in channels with each channel containing a single per-vertex information such as a set of texture coords or
 * a normal vector.
 * If a data pointer is non-null, the corresponding data stream is present.
 * From C++-programs you can also use the comfort functions Has*() to test for the presence of various data streams.
 *
 * A Mesh uses only a single material which is referenced by a material ID.
 * @note The mPositions member is usually not optional. However, vertex positions *could* be missing if the
 * #AI_SCENE_FLAGS_INCOMPLETE flag is set in
 * @code
 * aiScene::mFlags
 * @endcode */
data class AiMesh(

        /** Bitwise combination of the members of the #aiPrimitiveType enum.
         * This specifies which types of primitives are present in the mesh.
         * The "SortByPrimitiveType"-Step can be used to make sure the output meshes consist of one primitive type each.         */
        var mPrimitiveTypes: Int = 0,

        /** The number of vertices in this mesh.
         * This is also the size of all of the per-vertex data arrays.
         * The maximum value for this member is #AI_MAX_VERTICES.         */
        var mNumVertices: Int = 0,

        /** The number of primitives (triangles, polygons, lines) in this  mesh.
         * This is also the size of the mFaces array.
         * The maximum value for this member is #AI_MAX_FACES.         */
        var mNumFaces: Int = 0,

        /** Vertex positions.
         * This array is always present in a mesh. The array is mNumVertices in size.         */
        var mVertices: MutableList<AiVector3D> = mutableListOf(),

        /** Vertex normals.
         * The array contains normalized vectors, NULL if not present.
         * The array is mNumVertices in size. Normals are undefined for point and line primitives. A mesh consisting of
         * points and lines only may not have normal vectors. Meshes with mixed primitive types (i.e. lines and
         * triangles) may have normals, but the normals for vertices that are only referenced by point or line
         * primitives are undefined and set to QNaN (WARN: qNaN compares to inequal to *everything*, even to qNaN
         * itself.
         * Using code like this to check whether a field is qnan is:
         * @code
         * #define IS_QNAN(f) (f != f)
         * @endcode
         * still dangerous because even 1.f == 1.f could evaluate to false! ( remember the subtleties of IEEE754
         * artithmetics). Use stuff like @c fpclassify instead.
         * @note Normal vectors computed by Assimp are always unit-length.
         * However, this needn't apply for normals that have been taken directly from the model file.         */
        var mNormals: MutableList<AiVector3D> = mutableListOf(),

        /** Vertex tangents.
         * The tangent of a vertex points in the direction of the positive X texture axis. The array contains normalized
         * vectors, NULL if not present. The array is mNumVertices in size. A mesh consisting of points and lines only
         * may not have normal vectors. Meshes with mixed primitive types (i.e. lines and triangles) may have normals,
         * but the normals for vertices that are only referenced by point or line primitives are undefined and set to
         * qNaN.  See the #mNormals member for a detailed discussion of qNaNs.
         * @note If the mesh contains tangents, it automatically also contains bitangents.         */
        var mTangents: List<AiVector3D>? = null,

        /** Vertex bitangents.
         * The bitangent of a vertex points in the direction of the positive Y texture axis. The array contains
         * normalized vectors, NULL if not present. The array is mNumVertices in size.
         * @note If the mesh contains tangents, it automatically also contains bitangents.         */
        var mBitangents: MutableList<AiVector3D>? = null,

        /** Vertex color sets.
         * A mesh may contain 0 to #AI_MAX_NUMBER_OF_COLOR_SETS vertex colors per vertex. NULL if not present. Each
         * array is mNumVertices in size if present.         */
        var mColors: Array<Array<AiColor4D>?> = Array(AI_MAX_NUMBER_OF_COLOR_SETS, { null }),

        /** Vertex texture coords, also known as UV channels.
         * A mesh may contain 0 to AI_MAX_NUMBER_OF_TEXTURECOORDS per vertex. NULL if not present. The array is
         * mNumVertices in size.         */
        var mTextureCoords: Array<Array<AiVector3D>?> = Array(AI_MAX_NUMBER_OF_TEXTURECOORDS, { null }),

        /** Specifies the number of components for a given UV channel.
         * Up to three channels are supported (UVW, for accessing volume or cube maps). If the value is 2 for a given
         * channel n, the component p.z of mTextureCoords[n][p] is set to 0.0f.
         * If the value is 1 for a given channel, p.y is set to 0.0f, too.
         * @note 4D coords are not supported         */
        var mNumUVComponents: IntArray = IntArray(AI_MAX_NUMBER_OF_TEXTURECOORDS),

        /** The faces the mesh is constructed from.
         * Each face refers to a number of vertices by their indices.
         * This array is always present in a mesh, its size is given in mNumFaces.
         * If the #AI_SCENE_FLAGS_NON_VERBOSE_FORMAT is NOT set each face references an unique set of vertices.         */
        var mFaces: List<AiFace> = ArrayList(),

        /** The number of bones this mesh contains.
         * Can be 0, in which case the mBones array is NULL.
         */
        var mNumBones: Int = 0,

        /** The bones of this mesh.
         * A bone consists of a name by which it can be found in the frame hierarchy and a set of vertex weights.         */
        var mBones: List<AiBone>? = null,

        /** The material used by this mesh.
         * A mesh uses only a single material. If an imported model uses multiple materials, the import splits up the
         * mesh. Use this value as index into the scene's material list.         */
        var mMaterialIndex: Int = 0,

        /** Name of the mesh. Meshes can be named, but this is not a requirement and leaving this field empty is totally
         * fine.
         * There are mainly three uses for mesh names:
         *   - some formats name nodes and meshes independently.
         *   - importers tend to split meshes up to meet the one-material-per-mesh requirement. Assigning the same
         *      (dummy) name to each of the result meshes aids the caller at recovering the original mesh partitioning.
         *   - Vertex animations refer to meshes by their names.         **/
        var mName: String = "",

        /** NOT CURRENTLY IN USE. The number of attachment meshes */
        var mNumAnimMeshes: Int = 0,

        /** NOT CURRENTLY IN USE. Attachment meshes for this mesh, for vertex-based animation.
         *  Attachment meshes carry replacement data for some of the mesh'es vertex components (usually positions,
         *  normals). */
        var mAnimMeshes: List<AiMesh>? = null
) {
    //! Check whether the mesh contains positions. Provided no special
    //! scene flags are set, this will always be true
    fun hasPositions() = mNumVertices > 0

    //! Check whether the mesh contains faces. If no special scene flags
    //! are set this should always return true
    fun hasFaces() = mNumFaces > 0

    //! Check whether the mesh contains normal vectors
    fun hasNormals() = mNormals != null && mNumVertices > 0

    //! Check whether the mesh contains tangent and bitangent vectors
    //! It is not possible that it contains tangents and no bitangents
    //! (or the other way round). The existence of one of them
    //! implies that the second is there, too.
    fun hasTangentsAndBitangents() = mTangents != null && mBitangents != null && mNumVertices > 0

    //! Check whether the mesh contains a vertex color set
    //! \param pIndex Index of the vertex color set
    fun hasVertexColors(pIndex: Int) =
            if (pIndex >= AI_MAX_NUMBER_OF_COLOR_SETS)
                false
            else
                mColors[pIndex] != null && mNumVertices > 0

    //! Check whether the mesh contains a texture coordinate set
    //! \param pIndex Index of the texture coordinates set
    fun hasTextureCoords(pIndex: Int) =
            if (pIndex >= AI_MAX_NUMBER_OF_TEXTURECOORDS)
                false
            else
                mTextureCoords[pIndex] != null && mNumVertices > 0

    //! Get the number of UV channels the mesh contains
    fun getNumUVChannels(): Int {
        var n = 0
        while (n < AI_MAX_NUMBER_OF_TEXTURECOORDS && mTextureCoords[n] != null) ++n
        return n
    }

    //! Get the number of vertex color channels the mesh contains
    fun getNumColorChannels(): Int {
        var n = 0
        while (n < AI_MAX_NUMBER_OF_COLOR_SETS && mColors[n] != null) ++n
        return n
    }

    //! Check whether the mesh contains bones
    fun hasBones() = mBones != null && mNumBones > 0
}