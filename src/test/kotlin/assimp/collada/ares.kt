package assimp.collada

import assimp.Importer
import assimp.getResource
import assimp.models
import assimp.testFile
import io.kotest.core.spec.style.StringSpec
import java.net.URL

object ares  : StringSpec() {

    @JvmStatic
    fun main(args: Array<String>) {
        "ares"  { ares(getResource("$models/Collada/ares/sketchup_2014-01-29.dae")) }
    }

    operator fun invoke(url: URL) {

        Importer().testFile(url) {

        }
    }
}