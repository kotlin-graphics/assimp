package assimp.md5

import assimp.AiShadingMode
import assimp.AiTexture
import assimp.Importer
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe

object simpleCube {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(fileName)!!) {

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

            numMaterials shouldBe 1

            with(materials[0]) {
                textures[0].file shouldBe ""
            }
        }
    }
}