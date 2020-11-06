package X

import assimp.*
import io.kotest.core.spec.style.StringSpec

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