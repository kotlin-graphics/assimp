package directX

import assimp.AiNode
import assimp.Importer
import assimp.models
import io.kotlintest.specs.StringSpec



class BCN_Epileptic : StringSpec() {

    init {
        val BCN_Epileptic = "BCN_Epileptic.X"

        BCN_Epileptic {
            with(Importer().readFile(x + BCN_Epileptic)!!) {
                println("Node names: ")
                printNodeNames(rootNode)
                with(rootNode) {

                }
            }
        }
    }

}