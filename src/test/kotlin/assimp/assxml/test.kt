package assimp.assxml

import X.x
import assimp.*
import assimp.format.assxml.AssxmlExporter
import io.kotlintest.specs.StringSpec
import java.util.*

class test : StringSpec() {
    init {
        val test1 = "test.x -> test.assxml"
        test1 {
            Importer().testFile(getResource("$x/test.x")) {
                StringBuilder().let { out ->
                    AssxmlExporter().ExportSceneAssxml(out, this)
                    println()
                    println()
                    println()
                    println(out.toString())
                }
            }
        }
    }
}