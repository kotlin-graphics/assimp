package assimp

import java.net.URI

/**
 * Created by elect on 13/11/2016.
 */

abstract class BaseImporter {

    // -------------------------------------------------------------------
    /** Returns whether the class can handle the format of the given file.
     *
     * The implementation should be as quick as possible. A check for the file extension is enough. If no suitable
     * loader is found with this strategy, CanRead() is called again, the 'checkSig' parameter set to true this time.
     * Now the implementation is expected to perform a full check of the file structure, possibly searching the first
     * bytes of the file for magic identifiers or keywords.
     *
     * @param pFile Path and file name of the file to be examined.
     * @param pIOHandler The IO handler to use for accessing any file.
     * @param checkSig Set to true if this method is called a second time.
     *   This time, the implementation may take more time to examine the contents of the file to be loaded for magic
     *   bytes, keywords, etc, to be able to load files with unknown/not existent file extensions.
     * @return true if the class can read this file, false if not.
     */
    abstract fun canRead(
            pFile: URI,
            checkSig: Boolean
    ): Boolean

    // -------------------------------------------------------------------
    /** Imports the given file and returns the imported data.
     * If the import succeeds, ownership of the data is transferred to the caller. If the import fails, NULL is
     * returned. The function takes care that any partially constructed data is destroyed beforehand.
     *
     * @param pImp #Importer object hosting this loader.
     * @param pFile Path of the file to be imported.
     * @param pIOHandler IO-Handler used to open this and possible other files.
     * @return The imported data or NULL if failed. If it failed a human-readable error description can be retrieved by
     * calling GetErrorText()
     *
     * @note This function is not intended to be overridden. Implement InternReadFile() to do the import. If an
     * exception is thrown somewhere in InternReadFile(), this function will catch it and transform it into a suitable
     * response to the caller.
     */
    fun readFile(
            pImp: Importer,
            pFile: URI): AiScene {

        // create a scene object to hold the data
        val sc = AiScene()

        // dispatch importing
        internReadFile(pFile, sc)

        return sc
    }

    // -------------------------------------------------------------------
    /** Imports the given file into the given scene structure. The function is expected to throw an ImportErrorException
     * if there is an error. If it terminates normally, the data in aiScene is expected to be correct. Override this
     * function to implement the actual importing.
     * <br>
     *  The output scene must meet the following requirements:<br>
     * <ul>
     * <li>At least a root node must be there, even if its only purpose is to reference one mesh.</li>
     * <li>aiMesh::mPrimitiveTypes may be 0. The types of primitives in the mesh are determined automatically in this
     *   case.</li>
     * <li>the vertex data is stored in a pseudo-indexed "verbose" format. In fact this means that every vertex that is
     *   referenced by a face is unique. Or the other way round: a vertex index may not occur twice in a single
     *   aiMesh.</li>
     * <li>aiAnimation::duration may be -1. Assimp determines the length of the animation automatically in this case as
     *   the length of the longest animation channel.</li>
     * <li>aiMesh::mBitangents may be NULL if tangents and normals are given. In this case bitangents are computed as the cross product
     *   between normal and tangent.</li>
     * <li>There needn't be a material. If none is there a default material is generated. However, it is recommended
     *   practice for loaders to generate a default material for yourself that matches the default material setting for
     *   the file format better than Assimp's generic default material. Note that default materials *should* be named
     *   AI_DEFAULT_MATERIAL_NAME if they're just color-shaded or AI_DEFAULT_TEXTURED_MATERIAL_NAME if they define a
     *   (dummy) texture. </li>
     * </ul>
     * If the AI_SCENE_FLAGS_INCOMPLETE-Flag is <b>not</b> set:<ul>
     * <li> at least one mesh must be there</li>
     * <li> there may be no meshes with 0 vertices or faces</li>
     * </ul>
     * This won't be checked (except by the validation step): Assimp will crash if one of the conditions is not met!
     *
     * @param pFile Path of the file to be imported.
     * @param scene The scene object to hold the imported data.
     * NULL is not a valid parameter.
     * @param pIOHandler The IO handler to use for any file access.
     * NULL is not a valid parameter. */
    protected abstract fun internReadFile(
            pFile: URI,
            scene: AiScene)

    // ------------------------------------------------------------------------------------------------
    fun searchFileHeaderForToken(pFile: String, tokens: List<String>): Boolean {

        return false
    }
}