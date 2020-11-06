package assimp.md2

import assimp.*
import io.kotest.core.spec.style.StringSpec

/**
 * Created by elect on 24/01/2017.
 */

class md2 : StringSpec() {

    val path = "$models/MD2/"

    init {
        "faerie"  { faerie(path + "faerie.md2", path + "faerie2.bmp") }
    }
}