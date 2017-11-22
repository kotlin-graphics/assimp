package X

import assimp.Importer
import io.kotlintest.specs.StringSpec

class test_cube_text : StringSpec() {
    init {
        val test = "test_cube_text.X"
        test {
            with(Importer().readFile(x + "test_cube_text.X")!!) {
                printNodeNames(rootNode)
            }
        }
    }
}