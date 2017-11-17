package directX

import assimp.AiNode
import assimp.Importer
import assimp.models
import io.kotlintest.specs.StringSpec

fun printNodeNames(n : AiNode, done : MutableList<AiNode> = MutableList<AiNode>(0, {AiNode()})) {
    println(n.name); done.add(n)
    for(a in n.children){
//        if(a in done) {
//            continue
//        }
        printNodeNames(a, done)
    }
}

class xfile : StringSpec() {

    val x = models + "/X/"

    init {

        val mozd02x = "mozd02.X"

        mozd02x {
            with(Importer().readFile(x + mozd02x)!!) {
                println("Node names: ")
                printNodeNames(rootNode)
                with(rootNode) {

                }
            }
        }

        val anim_test = "anim_test.x"

        anim_test {
            with(Importer().readFile(x + anim_test)!!) {
                println("Node names: ")
                printNodeNames(rootNode)
                with(rootNode) {

                }
            }
        }

        val BCN_Epileptic = "BCN_Epileptic.X"

        BCN_Epileptic {
            with(Importer().readFile(x + BCN_Epileptic)!!) {
                println("Node names: ")
                printNodeNames(rootNode)
                with(rootNode) {

                }
            }
        }


//        val testwuson = "Testwuson.X"
//
//        testwuson {
//            with(Importer().readFile(x + testwuson)!!) {
//                with(rootNode) {
//                    println(rootNode.name)
//					println(children.size)
//                    rootNode.children.forEach({e -> println(e.name)})
//                }
//            }
//        }
    }

}