package assimp.obj

import assimp.AiPrimitiveType
import assimp.AiShadingMode
import assimp.Importer
import assimp.getResource
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import java.util.*

object spider {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            with(rootNode) {

                name shouldBe "spider.obj"
                transformation shouldBe Mat4()
                numChildren shouldBe 19

                val names = listOf("HLeib01", "OK", "Bein1Li", "Bein1Re", "Bein2Li", "Bein2Re", "Bein3Re", "Bein3Li", "Bein4Re",
                        "Bein4Li", "Zahn", "klZahn", "Kopf", "Brust", "Kopf2", "Zahn2", "klZahn2", "Auge", "Duplicate05")

                (0 until numChildren).map {
                    children[it].name shouldBe names[it]
                    children[it].meshes[0] shouldBe it
                }

                numMeshes shouldBe 0
            }
            numMeshes shouldBe 19

            with(meshes[0]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 240
                numFaces shouldBe 80

                vertices[0] shouldBe Vec3(x = 1.160379, y = 4.512684, z = 6.449167)
                vertices[numVertices - 1] shouldBe Vec3(x = -4.421391, y = -3.605049, z = -20.462471)

                normals[0] shouldBe Vec3(-0.537588000, -0.0717979968, 0.840146005)
                normals[numVertices - 1] shouldBe Vec3(-0.728103995, -0.400941998, -0.555975974)

                // TODO check for kotlintest 2.0 array check
                Arrays.equals(textureCoords[0][0], floatArrayOf(0.186192f, 0.222718f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.103881f, 0.697021f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(237, 238, 239)

                materialIndex shouldBe 3

                name shouldBe "HLeib01"
            }
            with(meshes[1]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 180
                numFaces shouldBe 60

                vertices[0] shouldBe Vec3(x = -41.8566132f, y = -0.754845977f, z = 9.43077183f)
                vertices[numVertices - 1] shouldBe Vec3(x = -49.7138367f, y = -2.98359, z = -21.4211159f)

                normals[0] shouldBe Vec3(x = -0.236278996f, y = 0.0291850008f, z = 0.971247017f)
                normals[numVertices - 1] shouldBe Vec3(x = -0.862017989f, y = 0.0830229968f, z = -0.500032008f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(-0.0658710003f, -0.410016000f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(-0.318565995f, 1.05051804f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(177, 178, 179)

                materialIndex shouldBe 1

                name shouldBe "OK"
            }
            with(meshes[18]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 114
                numFaces shouldBe 38

                vertices[0] shouldBe Vec3(x = -59.4670486f, y = 18.1400757f, z = -17.1943588)
                vertices[numVertices - 1] shouldBe Vec3(x = -62.2673569f, y = 15.2776031f, z = -14.7453232f)

                normals[0] shouldBe Vec3(x = 0.0751359984f, y = 0.741809011f, z = -0.666388988f)
                normals[numVertices - 1] shouldBe Vec3(x = -0.776385009f, y = -0.629855990f, z = 0.0225169994f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.899282992f, 0.970311999f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.372330993f, 0.198948994f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(111, 112, 113)

                materialIndex shouldBe 5

                name shouldBe "Duplicate05"
            }

            with(materials[0]) {

                name shouldBe "DefaultMaterial"
                shadingModel shouldBe AiShadingMode.gouraud

                with(color!!) {
                    ambient shouldBe Vec3(0)
                    diffuse shouldBe Vec3(0.600000024f)
                    specular shouldBe Vec3(0)
                    emissive shouldBe Vec3(0)
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    refracti shouldBe 1f
                }
            }
            with(materials[1]) {

                name shouldBe "Skin"
                shadingModel shouldBe AiShadingMode.gouraud

                with(color!!) {
                    ambient shouldBe Vec3(0.200000003f)
                    diffuse shouldBe Vec3(0.827450991f, 0.792156994f, 0.772548974f)
                    specular shouldBe Vec3(0)
                    emissive shouldBe Vec3(0)
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    refracti shouldBe 1f
                }
                textures[0].file shouldBe ".\\wal67ar_small.jpg"
            }
            with(materials[2]) {

                name shouldBe "Brusttex"
                shadingModel shouldBe AiShadingMode.gouraud

                with(color!!) {
                    ambient shouldBe Vec3(0.200000003f)
                    diffuse shouldBe Vec3(0.800000012f)
                    specular shouldBe Vec3(0)
                    emissive shouldBe Vec3(0)
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    refracti shouldBe 1f
                }
                textures[0].file shouldBe ".\\wal69ar_small.jpg"
            }
            with(materials[3]) {

                name shouldBe "HLeibTex"
                shadingModel shouldBe AiShadingMode.gouraud

                with(color!!) {
                    ambient shouldBe Vec3(0.200000003f)
                    diffuse shouldBe Vec3(0.690195978f, 0.639216006f, 0.615685999f)
                    specular shouldBe Vec3(0)
                    emissive shouldBe Vec3(0)
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    refracti shouldBe 1f
                }
                textures[0].file shouldBe ".\\SpiderTex.jpg"
            }
            with(materials[4]) {

                name shouldBe "BeinTex"
                shadingModel shouldBe AiShadingMode.gouraud

                with(color!!) {
                    ambient shouldBe Vec3(0.200000003f)
                    diffuse shouldBe Vec3(0.800000012f)
                    specular shouldBe Vec3(0)
                    emissive shouldBe Vec3(0)
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    refracti shouldBe 1f
                }
                textures[0].file shouldBe ".\\drkwood2.jpg"
            }
            with(materials[5]) {

                name shouldBe "Augentex"
                shadingModel shouldBe AiShadingMode.gouraud

                with(color!!) {
                    ambient shouldBe Vec3(0.200000003f)
                    diffuse shouldBe Vec3(0.800000012f)
                    specular shouldBe Vec3(0)
                    emissive shouldBe Vec3(0)
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    refracti shouldBe 1f
                }
                textures[0].file shouldBe ".\\engineflare1.jpg"
            }
        }
    }
}