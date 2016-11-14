/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assimp.components;

/**
 *
 * @author gbarbieri
 */
public class AiFace {

    /**
     * Number of indices defining this face. The maximum value for this member
     * is #AI_MAX_FACE_INDICES.
     */
    public int mNumIndices;
    /**
     * The indices array. Size of the array is given in numIndices.
     */
    public int[] mIndices;

    /**
     * A single face in a mesh, referring to multiple vertices.
     *
     * If mNumIndices is 3, we call the face 'triangle', for mNumIndices > 3
     * it's called 'polygon' (hey, that's just a definition!).
     *
     * aiMesh::mPrimitiveTypes can be queried to quickly examine which types of
     * primitive are actually present in a mesh. The #aiProcess_SortByPType flag
     * executes a special post-processing algorithm which splits meshes with
     * *different* primitive types mixed up (e.g. lines and triangles) in
     * several 'clean' submeshes. Furthermore there is a configuration option (
     * #AI_CONFIG_PP_SBP_REMOVE) to force #aiProcess_SortByPType to remove
     * specific kinds of primitives from the imported scene, completely and
     * forever. In many cases you'll probably want to set this setting to
     *
     * aiPrimitiveType_LINE|aiPrimitiveType_POINT
     *
     * Together with the #aiProcess_Triangulate flag you can then be sure that
     * #aiFace::mNumIndices is always 3.
     *
     * Take a look at the Data Structures page for more information on the
     * layout and winding order of a face.
     */
    public AiFace() {

        mNumIndices = 0;
        mIndices = null;
    }
}
