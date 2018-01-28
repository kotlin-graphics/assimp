package assimp.obj

import assimp.models
import io.kotlintest.specs.StringSpec

class obj : StringSpec() {

    val path = models + "/OBJ/"

    init {
        "box"{ box(path + "box.obj") }
        "spider"{ spider(path + "spider.obj") }
        "nanosuit" { nanosuit(path + "nanosuit/nanosuit.obj") }
        "shelter" { shelter(path + "statie B01.obj")}
    }
}