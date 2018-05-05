package assimp.collada

import assimp.Importer
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import java.net.URL

object cameras {

    operator fun invoke(fileName: URL) {

        with(Importer().readFile(fileName)!!) {

            flags shouldBe 1
            with(rootNode) {
                name shouldBe "Scene"
                transformation shouldBe Mat4(
                        1f, 0f, 0f, 0f,
                        0f, 0f, -1f, 0f,
                        0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 1f)
                parent shouldBe null
                numChildren shouldBe 3

                with(children[0]) {
                    name shouldBe "Camera"
                    transformation shouldBe Mat4(
                            7.54979013e-08f, 0f, -1f, 0f,
                            0f, 1f, 0f, 0f,
                            1f, 0f, 7.54979013e-08f, 0f,
                            10f, 0f, 0f, 1f)
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 0
                    meshes.isEmpty() shouldBe true
                    metaData.isEmpty() shouldBe true
                }
                with(children[1]) {
                    name shouldBe "Camera_002"
                    transformation shouldBe Mat4(
                            7.54979013e-08f, 0f, 1f, 0f,
                            0f, 1f, 0f, 0f,
                            -1f, 0f, 7.54979013e-08f, 0f,
                            -10f, 0f, 0f, 1f)
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 0
                    meshes.isEmpty() shouldBe true
                    metaData.isEmpty() shouldBe true
                }
                with(children[2]) {
                    name shouldBe "Camera_003"
                    transformation shouldBe Mat4(
                            3.09085983e-08f, -3.09085983e-08f, -1f, 0f,
                            -1f, 1.58932991e-08f, -3.09085983e-08f, 0f,
                            1.58932991e-08, 1f, -3.09085983e-08f, 0f,
                            0f, 5f, 0f, 1f)
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 0
                    meshes.isEmpty() shouldBe true
                    metaData.isEmpty() shouldBe true
                }
            }

            numMeshes shouldBe 1
            with(meshes[0]) {
                primitiveTypes shouldBe 4
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