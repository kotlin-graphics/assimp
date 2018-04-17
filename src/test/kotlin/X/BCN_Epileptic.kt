package X

import assimp.Importer
import assimp.getResource
import io.kotlintest.specs.StringSpec



class BCN_Epileptic : StringSpec() {

    init {
        val BCN_Epileptic = "BCN_Epileptic.X"

        BCN_Epileptic {
            with(Importer().readFile(getResource("$x/$BCN_Epileptic"))!!) {
                println("Node names: ")
                printNodeNames(rootNode)
                with(rootNode) {

                }
            }
        }
    }

}