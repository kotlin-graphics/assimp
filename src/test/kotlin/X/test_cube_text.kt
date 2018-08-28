package X

import assimp.*
import io.kotlintest.specs.StringSpec

class test_cube_text : StringSpec() {
    init {
        val test = "test_cube_text.x"
        test {
            Importer().testFile(getResource("$x/$test")) {
                printNodeNames(rootNode)
            }
        }
    }
}