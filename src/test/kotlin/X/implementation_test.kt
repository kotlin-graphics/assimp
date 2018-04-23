package X

import assimp.format.X.Pointer
import assimp.format.X.XFileParser
import io.kotlintest.specs.StringSpec
import assimp.format.X.fast_atoreal_move
import io.kotlintest.shouldBe

class implementation_test : StringSpec() {

    init {
        var floattest = "floattest"
        floattest {
            val sd = "1.000000000000 "
            var P = Pointer<Char>(Array<Char>(sd.length, {i -> sd[i]}))
            var result = Pointer<Float>(arrayOf(Float.NaN))
            fast_atoreal_move(P, result)
            result.value shouldBe 1.0F
            println(result.value)
        }

        var Xparsetest1 = "Xparsetest1"

        Xparsetest1 {
            var parser = XFileParser()
            parser.P = Pointer<Char>(arrayOf(
                 //   '}','}',
                    '}'
            ))
            parser.End = Pointer<Char>(arrayOf(
                //    '}','}',
                    '}'
            ), 2)
            var token = parser.GetNextToken()
            println(token)
        }

        var animationstest = "animationstest"
        animationstest {
            var s = //"AnimationSet " +
                    "Epileptisch {\n" +
                    " Animation Anim-Epileptisch-Legs {\n" +
                    "  \n" +
                    "  { Legs }\n" +
                    "\n" +
                    "  AnimationOptions {\n" +
                    "   1;\n" +
                    "   0;\n" +
                    "  }\n" +
                    "\n" +
                    "  AnimationKey rot {\n" +
                    "   0;\n" +
                    "   1;\n" +
                    "   0;4;0.707107,-0.707107,0.000000,0.000000;;;\n" +
                    "  }\n" +
                    "\n" +
                    "  AnimationKey scale {\n" +
                    "   1;\n" +
                    "   1;\n" +
                    "   0;3;1.000000,1.000000,1.000000;;;\n" +
                    "  }\n" +
                    "\n" +
                    "  AnimationKey pos {\n" +
                    "   2;\n" +
                    "   1;\n" +
                    "   0;3;-0.000000,0.000001,0.000000;;;\n" +
                    "  }\n" +
                    " }\n" +
                    "}"
            var parser = XFileParser()
            parser.P = Pointer<Char>(Array<Char>(s.length, {i -> s[i]}))
            parser.End = Pointer<Char>(Array<Char>(s.length, {i -> s[i]}))
            parser.End.pointer = parser.End.lastIndex+1
            parser.ParseDataObjectAnimationSet()
        }

    }

}