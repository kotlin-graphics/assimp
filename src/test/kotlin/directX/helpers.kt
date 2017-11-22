package directX

import assimp.AiNode
import assimp.models

val x = models + "/X/"
val x_ass = models + "/models-assbin-db/X/"

fun printNodeNames(n : AiNode, done : MutableList<AiNode> = MutableList<AiNode>(0, { AiNode() })) {
    println(n.name); done.add(n)
    for(a in n.children){
//        if(a in done) {
//            continue
//        }
        printNodeNames(a, done)
    }
}