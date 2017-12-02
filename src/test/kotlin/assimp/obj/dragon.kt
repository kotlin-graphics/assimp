package assimp.obj

import assimp.Importer

// TODO
object dragon {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(fileName)!!) {

            with(rootNode) {

                ////                    name shouldBe "nanosuit.obj"
////                    transformation shouldBe Mat4()
////                    numChildren shouldBe 7
////
////                    val names = listOf("Visor", "Legs", "hands", "Lights", "Arms", "Helmet", "Body")
////
////                    (0 until numChildren).map {
////                        children[it].name shouldBe names[it]
////                        children[it].meshes!![0] shouldBe it
////                    }
////
////                    numMeshes shouldBe 0
            }
        }
    }
}