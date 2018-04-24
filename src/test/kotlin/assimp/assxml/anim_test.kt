package assimp.assxml

import X.x
import assimp.Importer
import assimp.format.X.FlipWindingOrderProcess
import assimp.format.X.MakeLeftHandedProcess
import assimp.format.assxml.AssxmlExporter
import assimp.getResource
import io.kotlintest.specs.StringSpec
import java.util.*

class anim_test : StringSpec() {
    init {
        val test1 = "anim_test.X -> anim_test.assxml"
        test1 {
            Importer().readFile(getResource("$x/anim_test.x"))!!.let { f1 ->
                FlipWindingOrderProcess.Execute(f1)
                MakeLeftHandedProcess.Execute(f1) // TODO switch to flag

                StringBuilder().let { out ->
                    AssxmlExporter().ExportSceneAssxml(out, f1)
                    println(out.toString())
                    println("succesful")
                }
            }
        }
    }
}