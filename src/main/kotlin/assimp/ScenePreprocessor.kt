package assimp

/**
 * Created by elect on 18/11/2016.
 */

// ----------------------------------------------------------------------------------
/** ScenePreprocessor: Preprocess a scene before any post-processing steps are executed.
 *
 *  The step computes data that needn't necessarily be provided by the importer, such as aiMesh::mPrimitiveTypes.
 */
// ----------------------------------------------------------------------------------
class ScenePreprocessor(
        protected val scene: AiScene
) {

    fun processScene() {

        // scene cant be null

        // Process all meshes
        scene.mMeshes.forEach { it.process() }

        // - nothing to do for nodes for the moment
        // - nothing to do for textures for the moment
        // - nothing to do for lights for the moment
        // - nothing to do for cameras for the moment

        // Process all animations
//        scene.mAnimations.forEach { it.process() }

        // Generate a default material if none was specified
        if(scene.mNumMaterials == 0 && scene.mNumMeshes > 0) {
            scene.mMaterials = ArrayList(2) // TODO useless

            val helper = AiMaterial()

            // TODO scene.mMaterials
        }
    }

    fun AiMesh.process() {

        // If aiMesh::mNumUVComponents is *not* set assign the default value of 2
        // TODO change -> for in mTextureCoords
//        for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
//            if (mTextureCoords[i].isEmpty())
//                mNumUVComponents[i] = 0
//            else {
//                if (mNumUVComponents[i] == 0)
//                    mNumUVComponents[i] = 2
//
//                // Ensure unsued components are zeroed. This will make 1D texture channels work as if they were 2D
//                // channels .. just in case an application doesn't handle this case
//                // TODO
//            }
//        }

        // If the information which primitive types are there in the mesh is currently not available, compute it.
        if (mPrimitiveTypes == 0) {
            mFaces.forEach {
                when (it.size) {
                    3 -> mPrimitiveTypes = mPrimitiveTypes or AiPrimitiveType.TRIANGLE
                    2 -> mPrimitiveTypes = mPrimitiveTypes or AiPrimitiveType.LINE
                    1 -> mPrimitiveTypes = mPrimitiveTypes or AiPrimitiveType.POINT
                    else -> mPrimitiveTypes = mPrimitiveTypes or AiPrimitiveType.POLYGON
                }
            }
        }

        // If tangents and normals are given but no bitangents compute them
        if (mTangents.isNotEmpty() && mNormals.isNotEmpty() && mBitangents.isEmpty()) {

            mBitangents = java.util.ArrayList(mNumVertices)
//            for (i in 0..mNumVertices - 1)
//TODO                mesh.mBitangents!![i] = mesh.mNormals!![i] crossProduct mesh.mTangents!![i]
        }
    }

//    fun AiAnimation.process() {
//
//    }
}