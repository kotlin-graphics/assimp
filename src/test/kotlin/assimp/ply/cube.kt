package assimp.ply

import assimp.*
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.*

object cube {

    operator fun invoke(fileName: String) {
        with(Importer().readFile(getResource(fileName))!!) {

            numMeshes shouldBe 1
            with(meshes[0]) {
                primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
                numVertices shouldBe 24
                numFaces shouldBe 6

                vertices[0] shouldBe Vec3(0f)
                vertices[5] shouldBe Vec3(1f)
                vertices[10] shouldBe Vec3(1f, 0f, 1f)
                vertices[15] shouldBe Vec3(0f, 1f, 1f)
                vertices[20] shouldBe Vec3(0f, 1f, 0f)
                vertices[23] shouldBe Vec3(0f)

                faces[0] shouldBe mutableListOf(0, 1, 2, 3)
                faces[1] shouldBe mutableListOf(4, 5, 6, 7)
                faces[2] shouldBe mutableListOf(8, 9, 10, 11)
                faces[3] shouldBe mutableListOf(12, 13, 14, 15)
                faces[4] shouldBe mutableListOf(16, 17, 18, 19)
                faces[5] shouldBe mutableListOf(20, 21, 22, 23)
            }
            with(materials[0]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3(.05f)
                    diffuse shouldBe Vec3(.6f)
                    specular shouldBe Vec3(.6f)
                }
                twoSided shouldBe true
            }
        }
    }
}