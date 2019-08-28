package assimp.blender

import assimp.models
import glm_.mat4x4.Mat4
import io.kotlintest.specs.StringSpec

class blender : StringSpec() {

    val path = "$models/BLEND/"

    init {

        "testCubeRotateScaleTranslate" { testCubeRotateScaleTranslate(path + "testCubeRotateScaleTranslate.blend") }
        "blender default 250 compressed" { blenderDefault_250_compressed(path + "BlenderDefault_250_Compressed.blend") }
    }
}

/**
 * generates a transformation matrix, scaling first, then rotating and at last translating
 */
internal fun generateTrans(x: Float = 0f, y: Float = 0f, z: Float = 0f,
                  rotation: Float = 0f, rX: Float = 0.0f, rY: Float = 0f, rZ: Float = 0f,
                  sX: Float = 1f, sY: Float = 1f, sZ: Float = 1f): Mat4 {

	val result = Mat4().translate(x, y, z)

	if (rX != 0f || rY != 0f || rZ != 0f) result.rotate(rotation, rX, rY, rZ, result)

	return result.scale(sX, sY, sZ, result)
}