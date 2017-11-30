package assimp

import glm_.mat4x4.Mat4
import io.kotlintest.specs.StringSpec

class assimp_ : StringSpec() {

    init {
        "core" {

            val imp = Importer()
            imp["quatkquak"] = false
            val a = superFastHash("quatkquak")
            println(superFastHash("quatkquak0"))
            println(superFastHash("quatkquak1"))
            imp["quatkquak0"] = 456
//            imp["quatkquak1"] = 789f
//            imp["quatkquak2"] = Mat4(10)
            val b = imp["quatkquak0"] ?: 1
            println(a)
        }
    }
}