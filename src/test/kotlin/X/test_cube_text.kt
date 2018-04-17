package X

import assimp.Importer
import assimp.getResource
import io.kotlintest.specs.StringSpec

class test_cube_text : StringSpec() {
    init {
        val test = "test_cube_text.x"
        test {
            with(Importer().readFile(getResource("$x/$test"))!!) {
                printNodeNames(rootNode)
            }
        }
    }
}