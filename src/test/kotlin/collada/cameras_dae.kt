package collada

import assimp.Importer
import assimp.collada
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import uno.kotlin.uri

/**
 * Created by elect on 24/01/2017.
 */

class cameras_dae : StringSpec() {

    init {
        val file = "cameras.dae"
        file {
            with(Importer().readFile((collada + file).uri)!!) {

                flags shouldBe 1
                with(rootNode) {
                    name shouldBe "Scene"
                    transformation shouldBe Mat4(
                            1f, 0f, 0f, 0f,
                            0f, 0f, 1f, 0f,
                            0f, -1f, 0f, 0f,
                            0f, 0f, 0f, 1f)
                    parent shouldBe null
                    numChildren shouldBe 3

                    with(children[0]) {
                        name shouldBe "Camera"
                        transformation shouldBe Mat4(
                                7.54979013e-08f, 0f, 1f, 10f,
                                0f, 1f, 0f, 0f,
                                -1f, 0f, 7.54979013e-08f, 0f,
                                0f, 0f, 0f, 1f)
                        (parent === rootNode) shouldBe true
                        numChildren shouldBe 0
                        children.isEmpty() shouldBe true
                        numMeshes shouldBe 0
                        meshes.isEmpty() shouldBe true
                        metaData shouldBe null
                    }
                    with(children[1]) {
                        name shouldBe "Camera_002"
                        transformation shouldBe Mat4(
                                7.54979013e-08f, 0f, -1f, -10f,
                                0f, 1f, 0f, 0f,
                                1f, 0f, 7.54979013e-08f, 0f,
                                0f, 0f, 0f, 1f)
                        (parent === rootNode) shouldBe true
                        numChildren shouldBe 0
                        children.isEmpty() shouldBe true
                        numMeshes shouldBe 0
                        meshes.isEmpty() shouldBe true
                        metaData shouldBe null
                    }
                    with(children[2]) {
                        name shouldBe "Camera_003"
                        transformation shouldBe Mat4(
                                3.09085983e-08f, -1f, 1.58932991e-08, 0f,
                                -3.09085983e-08f, 1.58932991e-08f, 1f, 5f,
                                -1f, -3.09085983e-08f, -3.09085983e-08f, 0f,
                                0f, 0f, 0f, 1f)
                        (parent === rootNode) shouldBe true
                        numChildren shouldBe 0
                        children.isEmpty() shouldBe true
                        numMeshes shouldBe 0
                        meshes.isEmpty() shouldBe true
                        metaData shouldBe null
                    }
                }

                numMeshes shouldBe 1
                with(meshes[0]) {
                    mPrimitiveTypes shouldBe 4
                }

                numMaterials shouldBe 1
                with(materials[0]) {
                    name shouldBe "SkeletonMaterial"
                    twoSided shouldBe true
                }

                numCameras shouldBe 3
                with(cameras[0]) {
                    name shouldBe "Camera"
                    position shouldBe Vec3()
                    up shouldBe Vec3(0f, 1f, 0f)
                    lookAt shouldBe Vec3(0f, 0f, -1f)
                    horizontalFOV shouldBe 0.857555985f
                    clipPlaneNear shouldBe 0.100000001f
                    clipPlaneFar shouldBe 100f
                    aspect shouldBe 1.77777803f
                }
                with (cameras[1]) {
                    name shouldBe "Camera_002"
                    position shouldBe Vec3()
                    up shouldBe Vec3(0f, 1f, 0f)
                    lookAt shouldBe Vec3(0f, 0f, -1f)
                    horizontalFOV shouldBe 0.0523598790f
                    clipPlaneNear shouldBe 0.100000001f
                    clipPlaneFar shouldBe 100f
                    aspect shouldBe 1.77777803f
                }
                with (cameras[2]) {
                    name shouldBe "Camera_003"
                    position shouldBe Vec3()
                    up shouldBe Vec3(0f, 1f, 0f)
                    lookAt shouldBe Vec3(0f, 0f, -1f)
                    horizontalFOV shouldBe 0.521204889f
                    clipPlaneNear shouldBe 0.100000001f
                    clipPlaneFar shouldBe 50f
                    aspect shouldBe 1.77777803f
                }
            }
        }
    }
}