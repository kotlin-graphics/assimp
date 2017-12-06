package assimp.assxml

import assimp.AiMatrix4x4
import assimp.format.assxml.AssxmlExporter
import io.kotlintest.specs.StringSpec

class implementation_test : StringSpec() {
    init {
        var test = "String formatting"
        test {
            var a = AssxmlExporter()
            var prefix : String = Array<Char>(1, { '\t' }).joinToString(separator = "")
            var m = AiMatrix4x4()

//            println(String.format("%s", "hello"))
//            println(String.format("%10.6f", Random().nextFloat()))
//            println(String.format("%s<Node name=\"%s\"> \n", '\t', "hello"))

            //exit(0)

            var r = String.format("%s<Node name=\"%s\"> \n" + "%s\t<Matrix4> \n" +
                                 "%s\t\t%10.6f %10.6f %10.6f %10.6f\n" +
                    "%s\t\t%10.6f %10.6f %10.6f %10.6f\n" +
                    "%s\t\t%10.6f %10.6f %10.6f %10.6f\n" +
                    "%s\t\t%10.6f %10.6f %10.6f %10.6f\n" +
                    "%s\t</Matrix4> \n",
                    prefix, "dummy", prefix,
                    prefix, m.a0, m.a1, m.a2, m.a3,
                    prefix, m.b0, m.b1, m.b2, m.b3,
                    prefix, m.c0, m.c1, m.c2, m.c3,
                    prefix, m.d0, m.d1, m.d2, m.d3,
                    prefix
            )
            println(r)
            println(r.length)
            println(r.isBlank())

            var io= StringBuilder()
            AssxmlExporter.ioprintf(io, "%s<Node name=\"%s\"> \n%s\t<Matrix4> \n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t</Matrix4> \n".format(
                            prefix, "node1", prefix,
                            prefix, m.a0, m.b0, m.c0, m.d0,
                            prefix, m.a1, m.b1, m.c1, m.d1,
                            prefix, m.a2, m.b2, m.c2, m.d2,
                            prefix, m.a3, m.b3, m.c3, m.d3, prefix))
            println(io)

           // return@test

        }
    }
}