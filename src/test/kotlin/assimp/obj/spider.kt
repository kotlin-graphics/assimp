package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.test.*
import glm_.vec3.Vec3
import io.kotlintest.matchers.collections.*
import io.kotlintest.shouldBe
import java.util.*

object spider {

    operator fun invoke(fileName: String,
                        matName: String? = null,
                        img1: String? = null,
                        img2: String? = null,
                        img3: String? = null,
                        img4: String? = null,
                        img5: String? = null,
                        objFileMeshIndices: Boolean = true) {

        val urls = listOfNotNull(fileName, matName, img1, img2, img3, img4, img5).map { getResource(it) }

        Importer().testURLs(urls){

            with(rootNode) {

                transformation shouldBe Mat4()
                numChildren shouldBe 19

                val remainingNames = mutableListOf("HLeib01", "OK", "Bein1Li", "Bein1Re", "Bein2Li", "Bein2Re", "Bein3Re", "Bein3Li", "Bein4Re",
                        "Bein4Li", "Zahn", "klZahn", "Kopf", "Brust", "Kopf2", "Zahn2", "klZahn2", "Auge", "Duplicate05")

                (0 until numChildren).map {
                    val childNode = children[it]

                    if(objFileMeshIndices) remainingNames[0] shouldBe childNode.name
                    else remainingNames shouldContain childNode.name
                    remainingNames.remove(childNode.name)

                    childNode.meshes[0] shouldBe it
                }

                numMeshes shouldBe 0
            }
            numMeshes shouldBe 19

            with(meshes.find {it.name == "HLeib01"}!!) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 240
                numFaces shouldBe 80

                vertices[0] shouldBe Vec3(x = 1.160379, y = 4.512684, z = 6.449167).plusOrMinus(epsilon)
                vertices[numVertices - 1] shouldBe Vec3(x = -4.421391, y = -3.605049, z = -20.462471).plusOrMinus(epsilon)

                normals[0] shouldBe Vec3(-0.537588000, -0.0717979968, 0.840146005).plusOrMinus(epsilon)
                normals[numVertices - 1] shouldBe Vec3(-0.728103995, -0.400941998, -0.555975974).plusOrMinus(epsilon)

                // TODO check for kotlintest 2.0 array check
                Arrays.equals(textureCoords[0][0], floatArrayOf(0.186192f, 0.222718f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.103881f, 0.697021f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(237, 238, 239)

                materials[materialIndex].name shouldBe "HLeibTex"
            }
            with(meshes.find {it.name == "OK"}!!) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 180
                numFaces shouldBe 60

                vertices[0] shouldBe Vec3(x = -41.8566132f, y = -0.754845977f, z = 9.43077183f).plusOrMinus(epsilon)
                vertices[numVertices - 1] shouldBe Vec3(x = -49.7138367f, y = -2.98359, z = -21.4211159f).plusOrMinus(epsilon)

                normals[0] shouldBe Vec3(x = -0.236278996f, y = 0.0291850008f, z = 0.971247017f).plusOrMinus(epsilon)
                normals[numVertices - 1] shouldBe Vec3(x = -0.862017989f, y = 0.0830229968f, z = -0.500032008f).plusOrMinus(epsilon)

                Arrays.equals(textureCoords[0][0], floatArrayOf(-0.0658710003f, -0.410016000f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(-0.318565995f, 1.05051804f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(177, 178, 179)

                materials[materialIndex].name shouldBe "Skin"
            }
            with(meshes.find {it.name == "Duplicate05"}!!) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 114
                numFaces shouldBe 38

                vertices[0] shouldBe Vec3(x = -59.4670486f, y = 18.1400757f, z = -17.1943588).plusOrMinus(epsilon)
                vertices[numVertices - 1] shouldBe Vec3(x = -62.2673569f, y = 15.2776031f, z = -14.7453232f).plusOrMinus(epsilon)

                normals[0] shouldBe Vec3(x = 0.0751359984f, y = 0.741809011f, z = -0.666388988f).plusOrMinus(epsilon)
                normals[numVertices - 1] shouldBe Vec3(x = -0.776385009f, y = -0.629855990f, z = 0.0225169994f).plusOrMinus(epsilon)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.899282992f, 0.970311999f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.372330993f, 0.198948994f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(111, 112, 113)

                materials[materialIndex].name shouldBe "Augentex"
            }

            if(matName != null) {
                /*
                    TODO the material data in the fbx file is different from the obj file(mat file)
                        because of that we just don't check the material data in the fbx file right now.
                 */
                with(materials.find { it.name == "Skin" }!!) {

                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f).plusOrMinus(epsilon)
                        diffuse shouldBe Vec3(0.827450991f, 0.792156994f, 0.772548974f).plusOrMinus(epsilon)
                        specular shouldBe Vec3(0).plusOrMinus(epsilon)
                        emissive shouldBe Vec3(0).plusOrMinus(epsilon)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\wal67ar_small.jpg"
                }
                with(materials.find { it.name == "Brusttex" }!!) {

                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f).plusOrMinus(epsilon)
                        diffuse shouldBe Vec3(0.800000012f).plusOrMinus(epsilon)
                        specular shouldBe Vec3(0).plusOrMinus(epsilon)
                        emissive shouldBe Vec3(0).plusOrMinus(epsilon)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\wal69ar_small.jpg"
                }
                with(materials.find { it.name == "HLeibTex" }!!) {

                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f).plusOrMinus(epsilon)
                        diffuse shouldBe Vec3(0.690195978f, 0.639216006f, 0.615685999f).plusOrMinus(epsilon)
                        specular shouldBe Vec3(0).plusOrMinus(epsilon)
                        emissive shouldBe Vec3(0).plusOrMinus(epsilon)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\SpiderTex.jpg"
                }
                with(materials.find { it.name == "BeinTex" }!!) {

                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f).plusOrMinus(epsilon)
                        diffuse shouldBe Vec3(0.800000012f).plusOrMinus(epsilon)
                        specular shouldBe Vec3(0).plusOrMinus(epsilon)
                        emissive shouldBe Vec3(0).plusOrMinus(epsilon)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\drkwood2.jpg"
                }
                with(materials.find { it.name == "Augentex" }!!) {

                    name shouldBe "Augentex"
                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f).plusOrMinus(epsilon)
                        diffuse shouldBe Vec3(0.800000012f).plusOrMinus(epsilon)
                        specular shouldBe Vec3(0).plusOrMinus(epsilon)
                        emissive shouldBe Vec3(0).plusOrMinus(epsilon)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\engineflare1.jpg"
                }

                // numTextures shouldBe 5 // TODO numTextures is not set in obj
                textures.size shouldBe 5
            }
        }
    }
}