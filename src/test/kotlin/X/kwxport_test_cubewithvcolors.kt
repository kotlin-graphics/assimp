package X

import assimp.Importer
import io.kotlintest.specs.StringSpec

class kwxport_test_cubewithvcolors : StringSpec() {
    init {
        val test = "kwxport_test_cubewithvcolors.x"
        test {
            with(Importer().readFile(x + "kwxport_test_cubewithvcolors.x")!!) {
                printNodeNames(rootNode)
            }
        }
    }
}