package assimp.md3

import assimp.AiShadingMode
import assimp.AiTexture
import assimp.Importer
import assimp.getResource
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe

object watercan {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

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

                numMeshes shouldBe 1
                meshes[0] shouldBe 0
            }

            with(meshes[0]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 234
                numFaces shouldBe 78

                vertices[0] shouldBe Vec3(4.71875000f, 13.9843750f, 19.0312500f)
                vertices[116] shouldBe Vec3(0.265625000f, 1.09375000f, 0.125000000f)
                vertices[233] shouldBe Vec3(13.8281250f, 3.56250000f, 21.2031250f)

                normals[0] shouldBe Vec3(0.3367884f, 0.9412605f, 0.024541229f)
                normals[116] shouldBe Vec3(-0.8445991f, -0.5348365f, 0.024541229f)
                normals[233] shouldBe Vec3(0.9996988f, 0.000000000f, 0.024541229f)

                textureCoords[0][0][0] shouldBe 0.304648668f
                textureCoords[0][0][1] shouldBe 0.921110511f
                textureCoords[0][116][0] shouldBe 0.403633356f
                textureCoords[0][116][1] shouldBe 0.0315036774f
                textureCoords[0][233][0] shouldBe 0.474252075f
                textureCoords[0][233][1] shouldBe 0.682785511f

                for (i in 0..77 * 3 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 2, i + 1)

                name.isEmpty() shouldBe true
            }
            numMaterials shouldBe 1

            with(materials[0]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3(0.0500000007)
                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3(1f)
                }
                textures[0].flags shouldBe AiTexture.Flags.ignoreAlpha.i
                textures[0].file shouldBe "water_can.tga"
                name shouldBe "MD3_[default][watercan]"
            }
        }
    }
}