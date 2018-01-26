package X

import assimp.Importer
import io.kotlintest.specs.StringSpec

class test_cube_text : StringSpec() {
    init {
        val test = "test_cube_text.x"
        test {
            with(Importer().readFile(x + "test_cube_text.x")!!) {
                printNodeNames(rootNode)
            }
        }
    }
}