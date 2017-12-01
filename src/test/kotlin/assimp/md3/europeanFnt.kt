package assimp.md3

import assimp.AiShadingMode
import assimp.AiTexture
import assimp.Importer
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe

object europeanFnt {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(fileName)!!) {

            flags shouldBe 0

            with(rootNode) {
                name shouldBe "<MD3Root>"
                transformation shouldBe Mat4(
                        1f, 0f, 0f, 0f,
                        0f, 0f, -1f, 0f,
                        0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 1f)
                parent shouldBe null
                numChildren shouldBe 0
                children.isEmpty() shouldBe true

                numMeshes shouldBe 5
                meshes[0] shouldBe 0
            }

            with(meshes[0]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 336
                numFaces shouldBe 112

                vertices[0] shouldBe Vec3(84.7812500f, 39.1875000f, 27.0468750f)
                vertices[167] shouldBe Vec3(-63.3906250f, -40.9218750f, 13.2656250f)
                vertices[335] shouldBe Vec3(-66.7500000f, 38.8437500f, 15.7343750f)

                normals[0] shouldBe Vec3(0.5348365f, -0.013129499f, -0.8448536f)
//                normals[167] shouldBe Vec3(-0.8445991f, -0.5348365f, 0.024541229f)
//                normals[335] shouldBe Vec3(0.9996988f, 0.000000000f, 0.024541229f)
//
//                textureCoords[0][0][0] shouldBe 0.304648668f
//                textureCoords[0][0][1] shouldBe 0.921110511f
//                textureCoords[0][116][0] shouldBe 0.403633356f
//                textureCoords[0][116][1] shouldBe 0.0315036774f
//                textureCoords[0][233][0] shouldBe 0.474252075f
//                textureCoords[0][233][1] shouldBe 0.682785511f
//
//                for (i in 0..77 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 2, i + 1)
//
//                name.isEmpty() shouldBe true
            }
//            numMaterials shouldBe 1
//
//            with(materials[0]) {
//                shadingModel shouldBe AiShadingMode.gouraud
//                with(color!!) {
//                    ambient shouldBe Vec3(0.0500000007)
//                    diffuse shouldBe Vec3(1f)
//                    specular shouldBe Vec3(1f)
//                }
//                textures[0].flags shouldBe AiTexture.Flags.ignoreAlpha.i
//                textures[0].file shouldBe "water_can.tga"
//                name shouldBe "MD3_[default][watercan]"
//            }
        }
    }
}