package assimp.collada

import assimp.AiAnimBehaviour
import assimp.Importer
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import java.net.URL

object voyager {

    operator fun invoke(url: URL) {

        with(Importer().readFile(url)!!) {

            flags shouldBe 0

//            with(rootNode) {
//                name shouldBe "Array Test 001"
//                transformation shouldBe Mat4(
//                        1f, 0f, 0f, 0f,
//                        0f, 0f, -1f, 0f,
//                        0f, 1f, 0f, 0f,
//                        0f, 0f, 0f, 1f)
//                parent shouldBe null
//                numChildren shouldBe 1
//
//                with(children[0]) {
//                    name shouldBe "Box001"
//                    transformation shouldBe Mat4(
//                            1f, 5.235988e-08f, 0f, 0f,
//                            -5.235988e-08f, 1f, 0f, 0f,
//                            0f, 0f, 1f, 0f,
//                            0f, 0f, 0f, 1f)
//                    (parent === rootNode) shouldBe true
//                    numChildren shouldBe 2
//
//                    with(children[0]) {
//                        name shouldBe "Box001-Pivot"
//                        transformation shouldBe Mat4(
//                                1f, 0f, 0f, 0f,
//                                0f, 1f, 0f, 0f,
//                                0f, 0f, 1f, 0f,
//                                0.185947001f, 0f, 0f, 1f)
//                        (parent === rootNode.children[0]) shouldBe true
//                        numChildren shouldBe 0
//
//                        numMeshes shouldBe 1
//                        meshes[0] shouldBe 0
//                        metaData.isEmpty() shouldBe true
//                    }
//
//                    with(children[1]) {
//                        name shouldBe "Box002"
//                        transformation shouldBe Mat4(
//                                1f, 5.235988e-08f, 0f, 0f,
//                                -5.235988e-08f, 1f, 0f, 0f,
//                                0f, 0f, 1f, 0f,
//                                0f, 0f, 0f, 1f)
//                        (parent === rootNode.children[0]) shouldBe true
//                        numChildren shouldBe 2
//
//                        with(children[0]) {
//                            name shouldBe "Box002-Pivot"
//                            transformation shouldBe Mat4(
//                                    1f, 0f, 0f, 0f,
//                                    0f, 1f, 0f, 0f,
//                                    0f, 0f, 1f, 0f,
//                                    0.185947001f, 1.89182305f, 0f, 1f)
//                            (parent === rootNode.children[0].children[1]) shouldBe true
//                            numChildren shouldBe 0
//
//                            numMeshes shouldBe 1
//                            meshes[0] shouldBe 1
//                            metaData.isEmpty() shouldBe true
//                        }
//
//                        with(children[1]) {
//                            name shouldBe "Box003"
//                            transformation shouldBe Mat4(
//                                    1f, 5.235988e-08f, 0f, 0f,
//                                    -5.235988e-08f, 1f, 0f, 0f,
//                                    0f, 0f, 1f, 0f,
//                                    0f, 0f, 0f, 1f)
//                            (parent === rootNode.children[0].children[1]) shouldBe true
//                            numChildren shouldBe 2
//
//                            // TODO continue?
//
//                            numMeshes shouldBe 0
//                            meshes.isEmpty() shouldBe true
//                            metaData.isEmpty() shouldBe true
//                        }
//
//                        numMeshes shouldBe 0
//                        meshes.isEmpty() shouldBe true
//                        metaData.isEmpty() shouldBe true
//                    }
//
//                    numMeshes shouldBe 0
//                    meshes.isEmpty() shouldBe true
//                    metaData.isEmpty() shouldBe true
//                }
//            }

            numMeshes shouldBe 6

//            with(meshes[0]) {
//
//                primitiveTypes shouldBe 8
//                numVertices shouldBe 24
//                numFaces shouldBe 6
//
//                vertices[0] shouldBe Vec3(-0.5f, -0.5f, 0f)
//                vertices[11] shouldBe Vec3(-0.5f, -0.5f, 1f)
//                vertices[23] shouldBe Vec3(-0.5f, 0.5f, 1f)
//
//                normals[0] shouldBe Vec3(0f, 0f, -1f)
//                normals[11] shouldBe Vec3(0f, -1f, 0f)
//                normals[23] shouldBe Vec3(-1f, 0f, 0f)
//
//                textureCoords[0][0][0] shouldBe 1f
//                textureCoords[0][0][1] shouldBe 0f
//                textureCoords[0][11][0] shouldBe 0f
//                textureCoords[0][11][1] shouldBe 1f
//                textureCoords[0][23][0] shouldBe 0f
//                textureCoords[0][23][1] shouldBe 1f
//
//                for (i in 0..23 step 4) faces[i / 4] shouldBe mutableListOf(i, i + 1, i + 2, i + 3)
//
//                name shouldBe "Box001Mesh"
//            }
//
//            // for further mesh test, follow this issue, https://github.com/assimp/assimp/issues/1561
//
//            numMaterials shouldBe 1
//
//            with(materials[0]) {
//                color!!.diffuse shouldBe Vec3(0.600000024f)
//                name shouldBe "DefaultMaterial"
//            }
//
//            numAnimations shouldBe 1
//
//            with(animations[0]) {
//
//                name shouldBe ""
//                duration shouldBe 11.966667175292969
//                ticksPerSecond shouldBe 1.0
//                numChannels shouldBe 64
//
//                with(channels[0]!!) {
//                    nodeName shouldBe "Box001"
//                    numPositionKeys shouldBe 5
//                    with(positionKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Vec3()
//                    }
//                    with(positionKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Vec3()
//                    }
//                    with(positionKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Vec3()
//                    }
//                    numRotationKeys shouldBe 5
//                    with(rotationKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Quat(1f, 0f, 0f, 2.38497613e-08f)
//                    }
//                    with(rotationKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Quat(2.90066708e-07f, 0f, 0f, 1f)
//                    }
//                    with(rotationKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Quat(1f, 0f, 0f, 3.49691106e-07f)
//                    }
//                    numScalingKeys shouldBe 5
//                    with(scalingKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Vec3(1f)
//                    }
//                    with(scalingKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Vec3(1f)
//                    }
//                    with(scalingKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Vec3(1f)
//                    }
//                    preState shouldBe AiAnimBehaviour.DEFAULT
//                    postState shouldBe AiAnimBehaviour.DEFAULT
//                }
//
//                with(channels[31]!!) {
//                    nodeName shouldBe "Box032"
//                    numPositionKeys shouldBe 5
//                    with(positionKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Vec3()
//                    }
//                    with(positionKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Vec3()
//                    }
//                    with(positionKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Vec3()
//                    }
//                    numRotationKeys shouldBe 5
//                    with(rotationKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Quat(1f, 0f, 0f, 2.38497613e-08f)
//                    }
//                    with(rotationKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Quat(2.90066708e-07f, 0f, 0f, 1f)
//                    }
//                    with(rotationKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Quat(1f, 0f, 0f, 3.49691106e-07f)
//                    }
//                    numScalingKeys shouldBe 5
//                    with(scalingKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Vec3(1f)
//                    }
//                    with(scalingKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Vec3(1f)
//                    }
//                    with(scalingKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Vec3(1f)
//                    }
//                    preState shouldBe AiAnimBehaviour.DEFAULT
//                    postState shouldBe AiAnimBehaviour.DEFAULT
//                }
//
//                with(channels[63]!!) {
//                    nodeName shouldBe "Box064"
//                    numPositionKeys shouldBe 5
//                    with(positionKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Vec3()
//                    }
//                    with(positionKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Vec3()
//                    }
//                    with(positionKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Vec3()
//                    }
//                    numRotationKeys shouldBe 5
//                    with(rotationKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Quat(1f, 0f, 0f, 2.38497613e-08f)
//                    }
//                    with(rotationKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Quat(2.90066708e-07f, 0f, 0f, 1f)
//                    }
//                    with(rotationKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Quat(1f, 0f, 0f, 3.49691106e-07f)
//                    }
//                    numScalingKeys shouldBe 5
//                    with(scalingKeys[0]) {
//                        time shouldBe 0.033332999795675278
//                        value shouldBe Vec3(1f)
//                    }
//                    with(scalingKeys[2]) {
//                        time shouldBe 6.0
//                        value shouldBe Vec3(1f)
//                    }
//                    with(scalingKeys[4]) {
//                        time shouldBe 11.966667175292969
//                        value shouldBe Vec3(1f)
//                    }
//                    preState shouldBe AiAnimBehaviour.DEFAULT
//                    postState shouldBe AiAnimBehaviour.DEFAULT
//                }
//            }
        }
    }
}