package directX

import assimp.Importer
import io.kotlintest.specs.StringSpec

class Testwuson : StringSpec() {
    init {
        val testwuson = "Testwuson.X"

        testwuson {
            with(Importer().readFile(x + testwuson)!!) {
                with(rootNode) {
                    println(rootNode.name)
					println(children.size)
                    rootNode.children.forEach({e -> println(e.name)})
                }
            }
        }
    }

}