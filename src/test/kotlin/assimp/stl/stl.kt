package assimp.stl

import assimp.models
import io.kotlintest.specs.StringSpec

/**
 * Created by elect on 18/11/2016.
 */
class stl : StringSpec() {

    val path = "$models/STL/"

    init {
        "triangle"  { triangle("$path/triangle.stl") }
        "sphereWithHole"  { sphereWithHole("$path/sphereWithHole.stl") }
        "spiderBinary"  { spiderBinary("$path/Spider_binary.stl") }
    }
}