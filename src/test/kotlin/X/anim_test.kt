package X

import assimp.*
import io.kotlintest.specs.StringSpec

class anim_test : StringSpec() {

    init {
        val anim_test = "anim_test.x"
//        val anim_test_a = "anim_test.assbin"

        anim_test {
            Importer().testFile(getResource("$x/$anim_test")) {
//                val compare  = Importer().testFile(x_ass + anim_test_a)!!
//
//                kotlin.io.println("Node names: ")
//                X.printNodeNames(rootNode)
//                X.printNodeNames(compare.rootNode)
//                kotlin.with(rootNode) {
//
//                }
            }
        }
    }
}

