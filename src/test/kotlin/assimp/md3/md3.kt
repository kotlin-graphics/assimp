package assimp.md3

import assimp.*
import glm_.i
import io.kotlintest.specs.StringSpec

/**
 * Created by elect on 24/01/2017.
 */

class md3 : StringSpec() {

    val path = "$modelsNonBsd/MD3/"

    init {
//        "watercan"  { watercan(path + "watercan.md3") }
        "european font"  { europeanFnt(path + "q3root/models/mapobjects/kt_kubalwagon/european_fnt_v2.md3") }
    }
}