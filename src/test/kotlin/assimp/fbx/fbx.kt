package assimp.fbx

import assimp.*
import io.kotlintest.specs.StringSpec

/**
 * Created by elect on 24/01/2017.
 */

class fbx : StringSpec() {

    val path = "$models/FBX/"
    val pathNonBsd = "$modelsNonBsd/FBX/"
    val binary = "2013_BINARY/"
    val ascii = "2013_ASCII/"

    init {
        "concave polygon binary"  { concavePolygon(pathNonBsd + binary + "ConcavePolygon.fbx") }
        "concave polygon ascii"  { concavePolygon(pathNonBsd + ascii + "ConcavePolygon.fbx") }
        "anims with full rotations between keys"  { animFullRot(pathNonBsd + binary + "anims_with_full_rotations_between_keys.fbx") }
//        "spider"  { spider(path + "spider.fbx") }
    }
}