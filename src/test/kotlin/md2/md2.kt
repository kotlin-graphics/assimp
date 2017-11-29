package md2

import assimp.*
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import uno.kotlin.uri

/**
 * Created by elect on 24/01/2017.
 */

class md2 : StringSpec() {

    val path = models + "/MD2/"

    init {
        "faerie"  { faerie(path + "faerie.md2") }
    }
}