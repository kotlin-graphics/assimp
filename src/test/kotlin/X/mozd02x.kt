package X

import assimp.*
import io.kotest.core.spec.style.StringSpec



class mozd02x : StringSpec() {



    init {

        val mozd02x = "mozd02.X"
        val mozd02x_a = "mozd02.assbin"

        mozd02x {
            Importer().testFile(getResource("$x/$mozd02x")) {
                printNodeNames(rootNode)
                with(rootNode) {

                }
            }
        }
    }
}