package assimp.stl

import assimp.AiPrimitiveType
import assimp.Importer
import assimp.models
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

/**
 * Created by elect on 18/11/2016.
 */
class stl : StringSpec() {

    val path = "$models/STL/"

    init {
        "triangle"  { triangle(path + "triangle.stl") }
        "sphereWithHole"  { sphereWithHole(path + "sphereWithHole.stl") }
        "spiderBinary"  { spiderBinary(path + "Spider_binary.stl") }
    }
}