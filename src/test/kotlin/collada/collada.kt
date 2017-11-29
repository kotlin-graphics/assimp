package collada

import assimp.models
import io.kotlintest.specs.StringSpec

class collada  : StringSpec() {

    val path = models + "/Collada/"

    init {
        "anims full rot"  { animFullRot(path + "anims_with_full_rotations_between_keys.dae") }
        "cameras"  { cameras(path + "cameras.dae") }
        "concave poly"  { concavePoly(path + "ConcavePolygon.dae") }
    }
}