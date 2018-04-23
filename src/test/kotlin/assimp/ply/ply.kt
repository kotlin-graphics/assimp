package assimp.ply

import assimp.AiPrimitiveType
import assimp.AiShadingMode
import assimp.Importer
import assimp.models
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.*

/**
 * Created by elect on 18/12/2016.
 */

class ply : StringSpec() {

    val path = "$models/PLY/"

    init {

        "cube"{ cube(path + "cube.ply") }
        "Wuson"{ wuson(path + "Wuson.ply") }
        "pond.0"{ pond0(path + "pond.0.ply") }
    }
}