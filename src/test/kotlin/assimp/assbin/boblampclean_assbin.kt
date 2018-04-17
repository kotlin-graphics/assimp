package assimp.assbin

import assimp.Importer
import assimp.assbin
import assimp.getResource
import io.kotlintest.specs.StringSpec

// TODO
class `boblampclean assbin` : StringSpec() {

    val boblampclean = "ogldev/boblampclean.assbin"

    init {

//        val anims_with_full_rotations_between_keys = "anims_with_full_rotations_between_keys.assbin"
//
//        anims_with_full_rotations_between_keys {
//
//            val scene = Importer().readFile(assbin + anims_with_full_rotations_between_keys)!!
//            println()
////            scene.flags shouldBe 0
//        }
//


        boblampclean {

            val scene = Importer().readFile(getResource("$assbin/$boblampclean"))

            println()
        }

        val minigun = "MDL/MDL3 (3DGS A4)/minigun.assbin"

        minigun {

            val scene = Importer().readFile(getResource("$assbin/$minigun"))

            println()
        }
    }
}