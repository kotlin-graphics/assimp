/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components;

import jassimp.components.material.AiMaterial;

/**
 *
 * @author gbarbieri
 */
public class AiScene {

    /**
     * This flag is currently only set by the aiProcess_JoinIdenticalVertices
     * step. It indicates that the vertices of the output meshes aren't in the
     * internal verbose format anymore. In the verbose format all vertices are
     * unique, no vertex is ever referenced by more than one face.
     */
    public static final int AI_SCENE_FLAGS_NON_VERBOSE_FORMAT = 0x8;

    /**
     * Any combination of the AI_SCENE_FLAGS_XXX flags. By default this value is
     * 0, no flags are set. Most applications will want to reject all scenes
     * with the AI_SCENE_FLAGS_INCOMPLETE bit set.
     */
    public int mFlags;

    /**
     * The root node of the hierarchy.
     *
     * There will always be at least the root node if the import was successful
     * (and no special flags have been set). Presence of further nodes depends
     * on the format and content of the imported file.
     */
    public AiNode mRootNode;

    /**
     * The number of meshes in the scene.
     */
    public int mNumMeshes;

    /**
     * The array of meshes.
     *
     * Use the indices given in the aiNode structure to access this array. The
     * array is mNumMeshes in size. If the AI_SCENE_FLAGS_INCOMPLETE flag is not
     * set there will always be at least ONE material.
     */
    public AiMesh[] mMeshes;

    /**
     * The number of materials in the scene.
     */
    public int mNumMaterials;

    /**
     * The array of materials.
     *
     * Use the index given in each aiMesh structure to access this array. The
     * array is mNumMaterials in size. If the AI_SCENE_FLAGS_INCOMPLETE flag is
     * not set there will always be at least ONE material.
     */
    public AiMaterial[] mMaterial;
    /**
     * The number of animations in the scene.
     */
    public int mNumAnimations;
    /**
     * The array of animations.
     *
     * All animations imported from the given file are listed here. The array is
     * mNumAnimations in size.
     */
    public AiAnimation[] mAnimations;

    /**
     * The number of textures embedded into the file.
     */
    public int mNumTextures;

    /**
     * The array of embedded textures.
     *
     * Not many file formats embed their textures into the file. An example is
     * Quake's MDL format (which is also used by some GameStudio versions).
     */
    public AiTexture[] mTextures;

    /**
     * The number of light sources in the scene. Light sources are fully
     * optional, in most cases this attribute will be 0
     */
    public int mNumLights;

    /**
     * The array of light sources.
     *
     * All light sources imported from the given file are listed here. The array
     * is mNumLights in size.
     */
    public AiLight[] mLights;

    /**
     * The number of cameras in the scene. Cameras are fully optional, in most
     * cases this attribute will be 0
     */
    public int mNumCameras;

    /**
     * The array of cameras.
     *
     * All cameras imported from the given file are listed here. The array is
     * mNumCameras in size. The first camera in the array (if existing) is the
     * default camera view into the scene.
     */
    public AiCamera[] mCameras;

    /**
     * Internal data, do not touch
     */
    private ScenePrivateData mPrivate;

    public AiScene() {
        mFlags = 0;
        mRootNode = null;
        mNumMeshes = 0;
        mMeshes = null;
        mNumMaterials = 0;
        mMaterial = null;
        mNumAnimations = 0;
        mAnimations = null;
        mNumTextures = 0;
        mTextures = null;
        mNumLights = 0;
        mLights = null;
        mNumCameras = 0;
        mCameras = null;
        mPrivate = new ScenePrivateData();
    }
}
