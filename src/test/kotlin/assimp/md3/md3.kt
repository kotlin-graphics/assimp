package assimp.md3

import assimp.*
import io.kotlintest.specs.StringSpec

/**
 * Created by elect on 24/01/2017.
 */

class md3 : StringSpec() {

    val path = modelsNonBsd + "/MD3/"

    init {
        "watercan"  { watercan(path + "watercan.md3") }
    }
}