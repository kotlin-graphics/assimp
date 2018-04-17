package assimp.assxml

import X.x
import assimp.Importer
import assimp.format.assxml.AssxmlExporter
import assimp.getResource
import io.kotlintest.specs.StringSpec
import java.util.*

class test : StringSpec() {
    init {
        val test1 = "test.x -> test.assxml"
        test1 {
            Importer().readFile(getResource("$x/test.x"))!!.let { f1 ->
                StringBuilder().let { out ->
                    AssxmlExporter().ExportSceneAssxml(out, f1)
                    println()
                    println()
                    println()
                    println(out.toString())
                }
            }
        }
    }
}