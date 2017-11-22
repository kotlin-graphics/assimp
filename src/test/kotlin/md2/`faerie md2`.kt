package md2

import assimp.*
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import uno.kotlin.uri

/**
 * Created by elect on 24/01/2017.
 */

class `faerie md2` : StringSpec() {

    init {
        val file = "faerie.md2"
        file {
            with(Importer().readFile((md2 + file).uri)!!) {

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
//
//                numAnimations shouldBe 1
//
//                with(animations[0]) {
//
//                    name shouldBe ""
//                    duration shouldBe 11.966667175292969
//                    ticksPerSecond shouldBe 1.0
//                    numChannels shouldBe 64
//
//                    with(channels[0]!!) {
//                        nodeName shouldBe "Box001"
//                        numPositionKeys shouldBe 5
//                        with(positionKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Vec3()
//                        }
//                        with(positionKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Vec3()
//                        }
//                        with(positionKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Vec3()
//                        }
//                        numRotationKeys shouldBe 5
//                        with(rotationKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Quat(1f, 0f, 0f, 2.38497613e-08f)
//                        }
//                        with(rotationKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Quat(2.90066708e-07f, 0f, 0f, 1f)
//                        }
//                        with(rotationKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Quat(1f, 0f, 0f, 3.49691106e-07f)
//                        }
//                        numScalingKeys shouldBe 5
//                        with(scalingKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Vec3(1f)
//                        }
//                        with(scalingKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Vec3(1f)
//                        }
//                        with(scalingKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Vec3(1f)
//                        }
//                        preState shouldBe AiAnimBehaviour.DEFAULT
//                        postState shouldBe AiAnimBehaviour.DEFAULT
//                    }
//
//                    with(channels[31]!!) {
//                        nodeName shouldBe "Box032"
//                        numPositionKeys shouldBe 5
//                        with(positionKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Vec3()
//                        }
//                        with(positionKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Vec3()
//                        }
//                        with(positionKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Vec3()
//                        }
//                        numRotationKeys shouldBe 5
//                        with(rotationKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Quat(1f, 0f, 0f, 2.38497613e-08f)
//                        }
//                        with(rotationKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Quat(2.90066708e-07f, 0f, 0f, 1f)
//                        }
//                        with(rotationKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Quat(1f, 0f, 0f, 3.49691106e-07f)
//                        }
//                        numScalingKeys shouldBe 5
//                        with(scalingKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Vec3(1f)
//                        }
//                        with(scalingKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Vec3(1f)
//                        }
//                        with(scalingKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Vec3(1f)
//                        }
//                        preState shouldBe AiAnimBehaviour.DEFAULT
//                        postState shouldBe AiAnimBehaviour.DEFAULT
//                    }
//
//                    with(channels[63]!!) {
//                        nodeName shouldBe "Box064"
//                        numPositionKeys shouldBe 5
//                        with(positionKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Vec3()
//                        }
//                        with(positionKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Vec3()
//                        }
//                        with(positionKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Vec3()
//                        }
//                        numRotationKeys shouldBe 5
//                        with(rotationKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Quat(1f, 0f, 0f, 2.38497613e-08f)
//                        }
//                        with(rotationKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Quat(2.90066708e-07f, 0f, 0f, 1f)
//                        }
//                        with(rotationKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Quat(1f, 0f, 0f, 3.49691106e-07f)
//                        }
//                        numScalingKeys shouldBe 5
//                        with(scalingKeys[0]) {
//                            time shouldBe 0.033332999795675278
//                            value shouldBe Vec3(1f)
//                        }
//                        with(scalingKeys[2]) {
//                            time shouldBe 6.0
//                            value shouldBe Vec3(1f)
//                        }
//                        with(scalingKeys[4]) {
//                            time shouldBe 11.966667175292969
//                            value shouldBe Vec3(1f)
//                        }
//                        preState shouldBe AiAnimBehaviour.DEFAULT
//                        postState shouldBe AiAnimBehaviour.DEFAULT
//                    }
//                }
            }
        }
    }
}