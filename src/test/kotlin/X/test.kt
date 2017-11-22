package X

import assimp.Importer
import io.kotlintest.specs.StringSpec
import assimp.models

class test : StringSpec() {
    init {
        val test = "text.X"
        test {
            with(Importer().readFile(x + "test.X")!!) {
                printNodeNames(rootNode)
            }
        }
        val test_c = "test.assbin"
        test_c {
            val model2 = Importer().readFile(models + "/models-assbin-db/X/test.assbin")!!
            val model1 = Importer().readFile(x + "test.X")!!

            compareScenes(model1, model2)
        }

    }
}