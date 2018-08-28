package assimp.assbin

import assimp.*
import io.kotlintest.specs.StringSpec

class assbin : StringSpec() {

    val assbin = "$models/Assbin/"

    init {

//        val anims_with_full_rotations_between_keys = "anims_with_full_rotations_between_keys.assbin"
//
//        anims_with_full_rotations_between_keys {
//
//            val scene = Importer().testFile(assbin + anims_with_full_rotations_between_keys)!!
//            println()
////            scene.flags shouldBe 0
//        }
//
        val boblampclean = "ogldev/boblampclean.assbin"

        boblampclean {

            val scene = Importer().testFile(getResource("$assbin/$boblampclean"))

            println()
        }

        val minigun = "MDL/MDL3 (3DGS A4)/minigun.assbin"

        minigun {

            val scene = Importer().testFile(getResource("$assbin/$minigun"))

            println()
        }
    }
}