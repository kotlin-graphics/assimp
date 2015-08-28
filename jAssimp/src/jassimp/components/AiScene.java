/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components;

import jassimp.material.AiMaterial;

/**
 *
 * @author gbarbieri
 */
public class AiScene {

    /** The root node of the hierarchy.
     *
     * There will always be at least the root node if the import
     * was successful (and no special flags have been set).
     * Presence of further nodes depends on the format and content
     * of the imported file.
     */
    public AiNode mRootNode;

    /** The number of meshes in the scene. */
    public int mNumMeshes;
    
    /** The array of meshes.
     *
     * Use the indices given in the aiNode structure to access
     * this array. The array is mNumMeshes in size. If the
     * AI_SCENE_FLAGS_INCOMPLETE flag is not set there will always
     * be at least ONE material.
     */
    public AiMesh[] mMeshes;
    
    /** The number of materials in the scene. */
    public int mNumMaterials;
    
    /** The array of materials.
     *
     * Use the index given in each aiMesh structure to access this
     * array. The array is mNumMaterials in size. If the
     * AI_SCENE_FLAGS_INCOMPLETE flag is not set there will always
     * be at least ONE material.
     */
    public AiMaterial[] mMaterial;

}
