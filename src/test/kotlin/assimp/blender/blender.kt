package assimp.blender

import assimp.*
import glm_.mat4x4.*
import glm_.vec3.*
import io.kotlintest.specs.StringSpec

class blender : StringSpec() {

    val path = models + "/BLEND/"

    init {

        ASSIMP.BLENDER_DEBUG = false // TODO temp

        "testCubeRotateScaleTranslate" { testCubeRotateScaleTranslate(path + "testCubeRotateScaleTranslate.blend") }
        "blender default 250 compressed" { blenderDefault_250_compressed(path + "BlenderDefault_250_Compressed.blend") }
        //"spider"{ spider(path + "spider.obj") }
        //"nanosuit" { nanosuit(path + "nanosuit/nanosuit.obj") }
        //"shelter" { shelter(path + "statie B01.obj")}
    }
}


fun generateTrans(x: Float = 0f, y: Float = 0f, z: Float = 0f,
                  rotation: Float = 0f, rX: Float = 1.0f, rY: Float = 0f, rZ: Float = 0f,
                  sX: Float = 1f, sY: Float = 1f, sZ: Float = 1f): Mat4 {

	val pos = translation(Vec3(x, y, z))
	val rotation = if(rX == 0f && rY == 0f && rZ == 0f) Mat4() else Mat4().rotate(rotation, rX, rY, rZ)
	val scale = Mat4().scale(sX, sY, sZ)

	return pos * rotation * scale
}