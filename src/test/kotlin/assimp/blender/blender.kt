package assimp.blender

import assimp.*
import io.kotlintest.specs.StringSpec

class blender : StringSpec() {

    val path = models + "/BLEND/"

    init {

        ASSIMP.BLENDER_DEBUG = false // TODO temp

        "testCubeRotateScaleTranslate" { testCubeRotateScaleTranslate(path + "testCubeRotateScaleTranslate.blend") }
        //"blender default 250 compressed" { blenderDefault_250_compressed(path + "BlenderDefault_250_Compressed.blend") }
        //"spider"{ spider(path + "spider.obj") }
        //"nanosuit" { nanosuit(path + "nanosuit/nanosuit.obj") }
        //"shelter" { shelter(path + "statie B01.obj")}
    }
}