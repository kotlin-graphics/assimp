/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components;

import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class AiMesh {

    /**
     * Maximum number of indices per face (polygon).
     */
    public static final int AI_MAX_FACE_INDICES = 0x7fff;
    /**
     * Maximum number of indices per face (polygon).
     */
    public static final int AI_MAX_BONE_WEIGHTS = 0x7fffffff;
    /**
     * Maximum number of vertices per mesh.
     */
    public static final int AI_MAX_VERTICES = 0x7fffffff;
    /**
     * Maximum number of faces per mesh.
     */
    public static final int AI_MAX_FACES = 0x7fffffff;
    /**
     * Supported number of vertex color sets per mesh.
     */
    public static final int AI_MAX_NUMBER_OF_COLOR_SETS = 0x8;
    /**
     * Supported number of texture coord sets (UV(W) channels) per mesh.
     */
    public static final int AI_MAX_NUMBER_OF_TEXTURECOORDS = 0x8;

    /**
     * Bitwise combination of the members of the #aiPrimitiveType enum. This
     * specifies which types of primitives are present in the mesh. The
     * "SortByPrimitiveType"-Step can be used to make sure the output meshes
     * consist of one primitive type each.
     */
    public int mPrimitiveType;
    /**
     * The number of vertices in this mesh. This is also the size of all of the
     * per-vertex data arrays. The maximum value for this member is
     * #AI_MAX_VERTICES.
     */
    public int mNumVertices;
    /**
     * The number of primitives (triangles, polygons, lines) in this mesh. This
     * is also the size of the mFaces array. The maximum value for this member
     * is #AI_MAX_FACES.
     */
    public int mNumFaces;
    /**
     * Vertex positions. This array is always present in a mesh. The array is
     * mNumVertices in size.
     */
    public Vec3[] mVertices;

    /**
     * Vertex normals. The array contains normalized vectors, NULL if not
     * present. The array is mNumVertices in size. Normals are undefined for
     * point and line primitives. A mesh consisting of points and lines only may
     * not have normal vectors. Meshes with mixed primitive types (i.e. lines
     * and triangles) may have normals, but the normals for vertices that are
     * only referenced by point or line primitives are undefined and set to QNaN
     * (WARN: qNaN compares to inequal to *everything*, even to qNaN itself.
     * Using code like this to check whether a field is qnan is:
     *
     * #define IS_QNAN(f) (f != f)
     *
     * still dangerous because even 1.f == 1.f could evaluate to false! (
     * remember the subtleties of IEEE754 artithmetics). Use stuff like
     * fpclassify instead. Normal vectors computed by Assimp are always
     * unit-length. However, this needn't apply for normals that have been taken
     * directly from the model file.
     */
    public Vec3[] mNormals;
    /**
     * Vertex texture coords, also known as UV channels. A mesh may contain 0 to
     * AI_MAX_NUMBER_OF_TEXTURECOORDS per vertex. NULL if not present. The array
     * is mNumVertices in size.
     */
    public Vec3[][] mTextureCoords;
    /**
     * Specifies the number of components for a given UV channel. Up to three
     * channels are supported (UVW, for accessing volume or cube maps). If the
     * value is 2 for a given channel n, the component p.z of
     * mTextureCoords[n][p] is set to 0.0f. If the value is 1 for a given
     * channel, p.y is set to 0.0f, too.
     *
     * @note 4D coords are not supported
     */
    public int[] mNumUVComponents;
    /**
     * The faces the mesh is constructed from. Each face refers to a number of
     * vertices by their indices. This array is always present in a mesh, its
     * size is given in mNumFaces. If the #AI_SCENE_FLAGS_NON_VERBOSE_FORMAT is
     * NOT set each face references an unique set of vertices.
     */
    public AiFace[] mFaces;
    /**
     * The material used by this mesh. A mesh does use only a single material.
     * If an imported model uses multiple materials, the import splits up the
     * mesh. Use this value as index into the scene's material list.
     */
    public int mMaterialIndex;

    /**
     * A mesh represents a geometry or model with a single material.
     *
     * It usually consists of a number of vertices and a series of
     * primitives/faces referencing the vertices. In addition there might be a
     * series of bones, each of them addressing a number of vertices with a
     * certain weight. Vertex data is presented in channels with each channel
     * containing a single per-vertex information such as a set of texture
     * coords or a normal vector. If a data pointer is non-null, the
     * corresponding data stream is present. From C++-programs you can also use
     * the comfort functions Has*() to test for the presence of various data
     * streams.
     *
     * A Mesh uses only a single material which is referenced by a material ID.
     * The mPositions member is usually not optional. However, vertex positions
     * *could* be missing if the #AI_SCENE_FLAGS_INCOMPLETE flag is set in
     *
     * aiScene::mFlags
     */
    public AiMesh() {

        mPrimitiveType = 0;
        mNumVertices = 0;
        mNumFaces = 0;
        mVertices = null;
        mNormals = null;
        mFaces = null;
        mMaterialIndex = 0;

        mTextureCoords = new Vec3[AI_MAX_NUMBER_OF_TEXTURECOORDS][];
    }

    /**
     * @return Check whether the anim mesh overrides the vertex positions of its
     * host mesh
     */
    public boolean hasPositions() {
        return mVertices != null;
    }

    /**
     * @return Check whether the anim mesh overrides the vertex normals of its
     * host mesh
     */
    public boolean hasNormals() {
        return mNormals != null;
    }

    /**
     * @param pIndex
     * @return Check whether the anim mesh overrides a particular set of texture
     * coordinates on his host mesh.
     */
    public boolean hasTextureCoords(int pIndex) {
        return pIndex >= AI_MAX_NUMBER_OF_TEXTURECOORDS ? false : mTextureCoords[pIndex] != null;
    }
}
