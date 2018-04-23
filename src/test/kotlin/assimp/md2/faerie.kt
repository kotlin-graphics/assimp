package assimp.md2

import assimp.AiShadingMode
import assimp.AiTexture
import assimp.Importer
import assimp.getResource
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe

object faerie {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {
                name.isEmpty() shouldBe true
                transformation shouldBe Mat4()
                parent shouldBe null
                numChildren shouldBe 0
                children.isEmpty() shouldBe true

                numMeshes shouldBe 1
                meshes[0] shouldBe 0
            }

            with(meshes[0]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 1962
                numFaces shouldBe 654

                vertices[0] shouldBe Vec3(-9.96106529f, 26.6228905f, 6.63490105f)
                vertices[980] shouldBe Vec3(-1.92686939f, 13.9874496f, 4.68170929f)
                vertices[1961] shouldBe Vec3(-2.08440304f, 3.79757690f, 3.44811630f)

                normals[0] shouldBe Vec3(-0.525731027f, 0f,  -0.850651026f)
                normals[980] shouldBe Vec3(0.262865990f, 0.162459999f, 0.951056004f)
                normals[1961] shouldBe Vec3(-0.716566980f, 0.147621006f, 0.681717992f)

                textureCoords[0][0][0] shouldBe 0.645454526f
                textureCoords[0][0][1] shouldBe 0.766839385f
                textureCoords[0][980][0] shouldBe 0.368181825f
                textureCoords[0][980][1] shouldBe 0.694300532f
                textureCoords[0][1961][0] shouldBe 0.836363614f
                textureCoords[0][1961][1] shouldBe 0.502590656f

                for (i in 0..653 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 1, i + 2)

                name.isEmpty() shouldBe true
            }
            numMaterials shouldBe 1

            with(materials[0]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    diffuse shouldBe Vec3(0.600000024f)
                    specular shouldBe Vec3(0.600000024f)
                    ambient shouldBe Vec3(0.0500000007)
                }
                textures[0].type shouldBe AiTexture.Type.diffuse
                textures[0].file shouldBe "faerie.bmp"
                name shouldBe "DefaultMaterial"
            }
        }
    }
}