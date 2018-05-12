package assimp.collada

import assimp.getResource
import assimp.models
import io.kotlintest.specs.StringSpec

class collada  : StringSpec() {

    val path = "$models/Collada/"

    init {
        "anims full rot"  { animFullRot(getResource("$path/anims_with_full_rotations_between_keys.DAE")) }
        "cameras"  { cameras(getResource("$path/cameras.dae")) }
        "concave poly"  { concavePoly(getResource("$path/ConcavePolygon.dae")) }
        "treasure smooth"  { treasure_smooth(getResource("$path/treasure_smooth.dae")) }
        "treasure smooth Pretransform"  { `treasure_smooth Pretransform`(getResource("$path/treasure_smooth.dae")) }
        "color teapot spheres"  { `color teapot spheres`(getResource("$path/color_teapot_spheres.dae")) }
        "voyager"  { voyager(getResource("$path/voyager/voyager.dae")) }
    }
}