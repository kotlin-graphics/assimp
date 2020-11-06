package assimp.assxml

import X.x
import assimp.*
import assimp.format.X.FlipWindingOrderProcess
import assimp.format.X.MakeLeftHandedProcess
import assimp.format.assxml.AssxmlExporter
import io.kotest.core.spec.style.StringSpec
import java.util.*

class anim_test : StringSpec() {
    init {
        val test1 = "anim_test.X -> anim_test.assxml"
        test1 {
            Importer().testFile(getResource("$x/anim_test.x")) {

                FlipWindingOrderProcess.Execute(this)
                MakeLeftHandedProcess.Execute(this) // TODO switch to flag

                StringBuilder().let { out ->
                    AssxmlExporter().ExportSceneAssxml(out, this)
                    println(out.toString())
                    println("succesful")
                }
            }
        }
    }
}