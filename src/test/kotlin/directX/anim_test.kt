package directX

import assimp.Importer
import io.kotlintest.specs.StringSpec

class anim_test : StringSpec() {

    init {
        val anim_test = "anim_test.x"
        val anim_test_a = "anim_test.assbin"

        anim_test {
            with(Importer().readFile(x + anim_test)!!) {
                val compare  = Importer().readFile(x + anim_test_a)!!

                kotlin.io.println("Node names: ")
                directX.printNodeNames(rootNode)
                directX.printNodeNames(compare.rootNode)
                kotlin.with(rootNode) {

                }
            }
        }
    }
}

