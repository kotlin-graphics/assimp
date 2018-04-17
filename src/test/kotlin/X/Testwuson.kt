package X

import assimp.Importer
import assimp.getResource
import io.kotlintest.specs.StringSpec

class Testwuson : StringSpec() {
    init {
        val testwuson = "Testwuson.X"

        testwuson {
            with(Importer().readFile(getResource("$x/$testwuson"))!!) {
                with(rootNode) {
                    X.printNodeNames(rootNode)
                }
            }
        }
    }

}