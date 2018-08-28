package X

import assimp.*
import io.kotlintest.specs.StringSpec

class test : StringSpec() {
    init {
        val test = "test.x"
        test {
            Importer().testFile(getResource("$x/$test")) {
                printNodeNames(rootNode)
            }
        }
        val test_c = "test.assbin"
        test_c {
//            val model2 = Importer().testFile(x_ass + "test.assbin")
//            val model1 = Importer().testFile(x + "test.x")!!
//
//            compareScenes(model1, model2)
        }

    }
}