package assimp

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class assbin : StringSpec() {

    val assbin = models + "/Assbin/"

    init {

        val anims_with_full_rotations_between_keys = "anims_with_full_rotations_between_keys.assbin"

        anims_with_full_rotations_between_keys {

            val scene = Importer().readFile(assbin + anims_with_full_rotations_between_keys)!!
            println()
//            scene.mFlags shouldBe 0
        }

        val boblampclean = "ogldev/boblampclean.assbin"

        boblampclean {

            val scene = Importer().readFile(assbin + boblampclean)

            println()
        }
    }
}