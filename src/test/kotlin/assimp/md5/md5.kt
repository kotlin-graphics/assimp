package assimp.md5

import assimp.*
import glm_.i
import io.kotlintest.specs.StringSpec

/**
 * Created by elect on 24/01/2017.
 */

class md5 : StringSpec() {

    val path = "$models/MD5/"
    val path_ = "$modelsNonBsd/MD5/"

    init {
        "simple cube"  { simpleCube(path + "SimpleCube.md5mesh") }
        "boar man"  { boarMan(path_ + "BoarMan.md5mesh") }
    }
}