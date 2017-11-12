package assimp

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class x : StringSpec() {

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



fun main(args: Array<String>) {
    x()
}