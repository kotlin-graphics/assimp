package directX

import assimp.Importer
import assimp.models
import io.kotlintest.specs.StringSpec

class xfile : StringSpec() {

    val x = models + "/X/"

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