package X

import assimp.Importer
import io.kotlintest.specs.StringSpec

class Testwuson : StringSpec() {
    init {
        val testwuson = "Testwuson.X"

        testwuson {
            with(Importer().readFile(x + testwuson)!!) {
                with(rootNode) {
                    X.printNodeNames(rootNode)
                }
            }
        }
    }

}