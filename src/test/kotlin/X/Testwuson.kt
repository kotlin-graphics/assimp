package X

import assimp.*
import io.kotest.core.spec.style.StringSpec

class Testwuson : StringSpec() {
    init {
        val testwuson = "Testwuson.X"

        testwuson {
            Importer().testFile(getResource("$x/$testwuson")) {
                with(rootNode) {
                    X.printNodeNames(rootNode)
                }
            }
        }
    }

}