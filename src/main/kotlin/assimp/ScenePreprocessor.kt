package assimp

import kotlin.math.max
import kotlin.math.min

/**
 * Created by elect on 18/11/2016.
 */

// ----------------------------------------------------------------------------------
/** ScenePreprocessor: Preprocess a scene before any post-processing steps are executed.
 *
 *  The step computes data that needn't necessarily be provided by the importer, such as aiMesh::primitiveTypes.
 */
// ----------------------------------------------------------------------------------
object ScenePreprocessor {
    /** Scene we're currently working on    */
    lateinit var scene: AiScene

    /** Preprocess the current scene     */
    fun processScene(scene: AiScene) {

        // scene cant be null
        this.scene = scene

        // Process all meshes
        scene.meshes.forEach { it.process() }

        // - nothing to do for nodes for the moment
        // - nothing to do for textures for the moment
        // - nothing to do for lights for the moment
        // - nothing to do for cameras for the moment

        // Process all animations
        scene.animations.forEach { it.process() }

        // Generate a default material if none was specified
        if (scene.numMaterials == 0 && scene.numMeshes > 0) {
            scene.materials.add(AiMaterial().apply {
                color = AiMaterial.Color().apply { diffuse = AiColor3D(0.6f) }
                // setup the default name to make this material identifiable
                name = AI_DEFAULT_MATERIAL_NAME
            })
            logger.debug{"ScenePreprocessor: Adding default material '$AI_DEFAULT_MATERIAL_NAME'"}

            scene.meshes.forEach { it.materialIndex = scene.numMaterials }

            scene.numMaterials++
        }
    }

    fun AiMesh.process() {

        // TODO change -> for in textureCoords
        textureCoords.forEach {
            // If aiMesh::mNumUVComponents is *not* set assign the default value of 2
            for (i in 0 until it.size)
                if (it[i].isEmpty())
                    it[i] = FloatArray(2)

            /*  Ensure unsued components are zeroed. This will make 1D texture channels work as if they were 2D channels..
                just in case an application doesn't handle this case    */
//            if (it[0].size == 2)
//                for (uv in it)
//                    uv[2] = 0f
//            else if (it[0].size == 1)
//                for (uv in it) {
//                    uv[2] = 0f
//                    uv[1] = 0f
//                }
//            else if (it[0].size == 3) {
//                // Really 3D coordinates? Check whether the third coordinate is != 0 for at least one element
//                var coord3d = false
//                for (uv in it)
//                    if (uv[2] != 0f)
//                        coord3d = true
//                if (!coord3d) {
//                    logger.warn { "ScenePreprocessor: UVs are declared to be 3D but they're obviously not. Reverting to 2D." }
//                    for (i in 0 until it.size)
//                        it[i] = FloatArray(2)
//                }
//            }
        }

        // If the information which primitive types are there in the mesh is currently not available, compute it.
        if (primitiveTypes == 0) faces.forEach {
            primitiveTypes = when (it.size) {
                3 -> primitiveTypes or AiPrimitiveType.TRIANGLE
                2 -> primitiveTypes or AiPrimitiveType.LINE
                1 -> primitiveTypes or AiPrimitiveType.POINT
                else -> primitiveTypes or AiPrimitiveType.POLYGON
            }
        }

        // If tangents and normals are given but no bitangents compute them
        if (tangents.isNotEmpty() && normals.isNotEmpty() && bitangents.isEmpty()) {
            bitangents = ArrayList(numVertices)
            for (i in 0 until numVertices)
                bitangents[i] = normals[i] cross tangents[i]
        }
    }

    fun AiAnimation.process() {
        var first = 10e10
        var last = -10e10
        channels.forEach { channel ->
            //  If the exact duration of the animation is not given compute it now.
            if (duration == -1.0) {
                channel!!.positionKeys.forEach {
                    // Position keys
                    first = min(first, it.time)
                    last = max(last, it.time)
                }
                channel.scalingKeys.forEach {
                    // Scaling keys
                    first = min(first, it.time)
                    last = max(last, it.time)
                }
                channel.rotationKeys.forEach {
                    // Rotation keys
                    first = min(first, it.time)
                    last = max(last, it.time)
                }
            }
            /*  Check whether the animation channel has no rotation or position tracks. In this case we generate a dummy
             *  track from the information we have in the transformation matrix of the corresponding node.  */
            if (channel!!.numRotationKeys == 0 || channel.numPositionKeys == 0 || channel.numScalingKeys == 0)
            // Find the node that belongs to this animation
                scene.rootNode.findNode(channel.nodeName)?.let {
                    // ValidateDS will complain later if 'node' is NULL
                    // Decompose the transformation matrix of the node
                    val scaling = AiVector3D()
                    val position = AiVector3D()
                    val rotation = AiQuaternion()
                    it.transformation.decompose(scaling, rotation, position)
                    if (channel.numRotationKeys == 0) { // No rotation keys? Generate a dummy track
                        channel.numRotationKeys = 1
                        channel.rotationKeys = arrayListOf(AiQuatKey(0.0, rotation))
                        logger.debug { "ScenePreprocessor: Dummy rotation track has been generated" }
                    }
                    if (channel.numScalingKeys == 0) { // No scaling keys? Generate a dummy track
                        channel.numScalingKeys = 1
                        channel.scalingKeys = arrayListOf(AiVectorKey(0.0, scaling))
                        logger.debug { "ScenePreprocessor: Dummy scaling track has been generated" }
                    }
                    if (channel.numPositionKeys == 0) { // No position keys? Generate a dummy track
                        channel.numPositionKeys = 1
                        channel.positionKeys = arrayListOf(AiVectorKey(0.0, position))
                        logger.debug { "ScenePreprocessor: Dummy position track has been generated" }
                    }
                }
        }
        if (duration == -1.0) {
            logger.debug { "ScenePreprocessor: Setting animation duration" }
            duration = last - min(first, 0.0)
        }
    }
}