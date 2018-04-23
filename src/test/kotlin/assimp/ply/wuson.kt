package assimp.ply

import assimp.*
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.*

object wuson {

    operator fun invoke(fileName: String) {
        with(Importer().readFile(getResource(fileName))!!) {

            numMeshes shouldBe 1
            with(meshes[0]) {
                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 11_196
                numFaces shouldBe 3732

                vertices[0] shouldBe Vec3(0.163313001f, 0.540615022f, -0.268687993f)
                vertices[3_000] shouldBe Vec3(0.0515949987f, 0.703427017f, 0.678973973f)
                vertices[6_000] shouldBe Vec3(-0.163461000f, 1.03988898f, -1.39270401f)
                vertices[9_000] shouldBe Vec3(-0.348796010f, 1.29999999e-05f, -0.704774022f)
                vertices[11_195] shouldBe Vec3(-0.338613003f, 1.06906497f, -1.14677405f)

                normals[0] shouldBe Vec3(0.241918996f, -0.961129010f, 0.133063003f)
                normals[3_000] shouldBe Vec3(-0.876681030f, -0.469485015f, 0.104950003f)
                normals[6_000] shouldBe Vec3(-0.705595016f, 0.0227199998f, -0.708250999f)
                normals[9_000] shouldBe Vec3(-0.997608006f, 0.0688209981f, -0.00645099999f)
                normals[11_195] shouldBe Vec3(0.0657600015f, -0.889011025f, 0.453139007f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.681180000f, 0.275678009f)) shouldBe true
                Arrays.equals(textureCoords[0][3_000], floatArrayOf(0.646326005f, 0.366248012f)) shouldBe true
                Arrays.equals(textureCoords[0][6_000], floatArrayOf(0.454632014f, 0.379783005f)) shouldBe true
                Arrays.equals(textureCoords[0][9_000], floatArrayOf(0.587324023f, 0.309864014f)) shouldBe true
                Arrays.equals(textureCoords[0][11_195], floatArrayOf(0.470634013f, 0.297468990f)) shouldBe true

                faces[0] shouldBe mutableListOf(0, 1, 2)
                faces[1_000] shouldBe mutableListOf(3_000, 3_001, 3_002)
                faces[2_000] shouldBe mutableListOf(6_000, 6_001, 6_002)
                faces[3_000] shouldBe mutableListOf(9_000, 9_001, 9_002)
                faces[3_731] shouldBe mutableListOf(11_193, 11_194, 11_195)
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