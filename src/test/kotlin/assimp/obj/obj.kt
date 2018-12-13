package assimp.obj

import assimp.models
import io.kotlintest.specs.StringSpec

class obj : StringSpec() {

    val path = "$models/OBJ/"

    init {
        "cube"{ cube(path + "cube.obj") }
//        "wall"{ wall(path + "wall.obj") }
        "box"{ box(path + "box.obj") }

        "spider"{ spider(path + "spider.obj",
                         path + "spider.mtl",
                         path + "wal67ar_small.jpg",
                         path + "wal69ar_small.jpg",
                         path + "SpiderTex.jpg",
                         path + "drkwood2.jpg",
                         path + "engineflare1.jpg") }

        "nanosuit" { nanosuit(path + "nanosuit/nanosuit.obj",
                              path + "nanosuit/nanosuit.mtl",
                              path + "nanosuit/arm_showroom_ddn.png",
                              path + "nanosuit/arm_showroom_refl.png",
                              path + "nanosuit/arm_dif.png",
                              path + "nanosuit/arm_showroom_spec.png",
                              path + "nanosuit/body_dif.png",
                              path + "nanosuit/body_showroom_ddn.png",
                              path + "nanosuit/body_showroom_refl.png",
                              path + "nanosuit/body_showroom_spec.png",
                              path + "nanosuit/glass_ddn.png",
                              path + "nanosuit/glass_refl.png",
                              path + "nanosuit/glass_dif.png",
                              path + "nanosuit/hand_showroom_ddn.png",
                              path + "nanosuit/hand_showroom_refl.png",
                              path + "nanosuit/hand_dif.png",
                              path + "nanosuit/hand_showroom_spec.png",
                              path + "nanosuit/helmet_showroom_ddn.png",
                              path + "nanosuit/helmet_showroom_refl.png",
                              path + "nanosuit/helmet_diff.png",
                              path + "nanosuit/helmet_showroom_spec.png",
                              path + "nanosuit/leg_showroom_ddn.png",
                              path + "nanosuit/leg_showroom_refl.png",
                              path + "nanosuit/leg_dif.png",
                              path + "nanosuit/leg_showroom_spec.png") }

        "shelter" { shelter(path + "statie B01.obj",path + "statie B01.mtl")}
    }
}