package assimp.md5

import assimp.AiShadingMode
import assimp.AiTexture
import assimp.Importer
import assimp.getResource
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe

object simpleCube {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {
                name shouldBe "<MD5_Root>"
                transformation shouldBe Mat4(
                        1f, 0f, 0f, 0f,
                        0f, 0f, -1f, 0f,
                        0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 1f)
                parent shouldBe null
                numChildren shouldBe 2

                with(children[0]) {
                    name shouldBe "<MD5_Mesh>"
                    transformation shouldBe Mat4()
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0

                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                }

                with(children[1]) {
                    name shouldBe "<MD5_Hierarchy>"
                    transformation shouldBe Mat4()
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 1

                    with(children[0]) {
                        name shouldBe "origin"
                        transformation shouldBe Mat4()
                        (parent === rootNode.children[1]) shouldBe true
                        numChildren shouldBe 1

                        with(children[0]) {
                            name shouldBe "root"
                            transformation shouldBe Mat4(
                                    0.000780999660f, 1.78813934e-07f, 0.999999702f, 0.000000000f,
                                    -0.999999702f, 1.19209290e-07f, 0.000781029463f, 0.000000000f,
                                    -1.78813934e-07f, -1.00000000f, 1.19209290e-07f, 0.000000000f,
                                    0.000000000f, 0.000000000f, 0.000000000f, 1.00000000f)
                            (parent === rootNode.children[1].children[0]) shouldBe true
                            numChildren shouldBe 1

                            with(children[0]) {
                                name shouldBe "joint1"
                                transformation shouldBe Mat4(
                                        0.000781089126f, -0.999999702f, -2.96912894e-08f, 0.000000000f,
                                        0.999999642f, 0.000781059265f, 1.19302342e-07f, 0.000000000f,
                                        -5.95347665e-08f, -8.94821568e-08f, 0.999999940f, 0.000000000f,
                                        31.9803429f, 3.81097198e-06f, 3.81233167e-06f, 1.00000000f)
                                (parent === rootNode.children[1].children[0].children[0]) shouldBe true
                                numChildren shouldBe 0
                                numMeshes shouldBe 0
                            }

                            numMeshes shouldBe 0
                        }

                        numMeshes shouldBe 0
                    }

                    numMeshes shouldBe 0
                }

                numMeshes shouldBe 0
            }

            with(meshes[0]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 36
                numFaces shouldBe 12

                vertices[0] shouldBe Vec3(32.0000038f, 31.9999943f,-32.0000153f)
                vertices[5] shouldBe Vec3(-32.0000000f, -31.9999924f, 32.0000038f)
                vertices[11] shouldBe Vec3(31.9999981f, -32.0000000f, 32.0000000f)

                textureCoords[0][0][0] shouldBe 0.187500000f
                textureCoords[0][0][1] shouldBe 1.25000000f
                textureCoords[0][5][0] shouldBe -0.312500000f
                textureCoords[0][5][1] shouldBe 1.25000000f
                textureCoords[0][11][0] shouldBe 0.250000000f
                textureCoords[0][11][1] shouldBe 1.50000000f

                faces[0] shouldBe mutableListOf(0, 1, 2)
                faces[5] shouldBe mutableListOf(11, 29, 28)
                faces[11] shouldBe mutableListOf(23, 35, 34)

                name.isEmpty() shouldBe true
            }

            numMaterials shouldBe 1

            with(materials[0]) {
                textures[0].file shouldBe ""
            }
        }
    }
}