package directX

import assimp.format.X.Pointer
import assimp.models
import io.kotlintest.specs.StringSpec
import assimp.format.X.fast_atoreal_move
import io.kotlintest.matchers.shouldBe

class implementation_test : StringSpec() {

    init {
        var floattest = "floattest"
        floattest {
            val sd = "1.000000000000"
            var P = Pointer<Char>(Array<Char>(sd.length, {i -> sd[i]}))
            var result = Pointer<Float>(arrayOf(Float.NaN))
            fast_atoreal_move(P, result)
            result.value shouldBe 1.0F
            println(result.value)
        }

    }

}