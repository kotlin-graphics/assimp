package assimp.obj

import assimp.models
import io.kotest.core.spec.style.StringSpec

class obj : StringSpec() {

    val path = "$models/OBJ"

    init {
        "box"{ box("$path/box.obj") }
        "cube"{ cube("$path/cube.obj") }
        "nanosuit" { nanosuit("$path/nanosuit") }
        // fires "java.lang.OutOfMemoryError: Java heap space" if run together with the others
//        "wall"{ wall("$path/wall.obj") }
        "spider"{ spider.obj("$path/spider") }
        "shelter" { shelter("$path/shelter") }
        "car" { car("$path/car") }
    }
}