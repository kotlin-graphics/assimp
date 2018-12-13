package assimp.blender

import assimp.*
import glm_.*
import glm_.mat4x4.*
import glm_.vec3.*
import io.kotlintest.specs.StringSpec

class blender : StringSpec() {

    val path = models + "/BLEND/"

    init {

        "testCubeRotateScaleTranslate" { testCubeRotateScaleTranslate(path + "testCubeRotateScaleTranslate.blend") }
//        "blender default 250 compressed" { blenderDefault_250_compressed(path + "BlenderDefault_250_Compressed.blend") }
        // "spider"{ spider(path + "spider.obj") }
        // "nanosuit" { nanosuit(path + "nanosuit/nanosuit.obj") }
        // "shelter" { shelter(path + "statie B01.obj")}
    }
}

/**
 * generates a transformation matrix, scaling first, then rotating and at last translating
 */
internal fun generateTrans(x: Float = 0f, y: Float = 0f, z: Float = 0f,
                  rotation: Float = 0f, rX: Float = 0.0f, rY: Float = 0f, rZ: Float = 0f,
                  sX: Float = 1f, sY: Float = 1f, sZ: Float = 1f): Mat4 {

	val result = Mat4().translate(Vec3(x, y, z))

	if (rX != 0f || rY != 0f || rZ != 0f) result.rotate(rotation, rX, rY, rZ, result)

	result.scale(sX, sY, sZ, result)

	return result
}


// TODO refactor to glm
internal val Float.inRadians: Float get() = Math.toRadians(this.d).f
internal val Float.inDegrees: Float get() = Math.toDegrees(this.d).f