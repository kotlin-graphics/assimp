package X

import assimp.Importer
import assimp.getResource
import io.kotlintest.specs.StringSpec

class anim_test : StringSpec() {

    init {
        val anim_test = "anim_test.x"
        val anim_test_a = "anim_test.assbin"

        anim_test {
            with(Importer().readFile(getResource("$x/$anim_test"))!!) {
//                val compare  = Importer().readFile(x_ass + anim_test_a)!!
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

