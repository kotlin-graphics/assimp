package X

import assimp.*
import io.kotlintest.specs.StringSpec

class kwxport_test_cubewithvcolors : StringSpec() {
    init {
        val test = "kwxport_test_cubewithvcolors.x"
        test {
            Importer().testFile(getResource("$x/$test")) {
                printNodeNames(rootNode)
            }
        }
    }
}