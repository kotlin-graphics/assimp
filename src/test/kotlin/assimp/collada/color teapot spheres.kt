package assimp.collada

import assimp.AiPostProcessStep
import assimp.AiPrimitiveType
import assimp.Importer
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import java.net.URL

object `color teapot spheres` {

    operator fun invoke(fileName: URL) {

        with(Importer().readFile(fileName)!!) {

            flags shouldBe 0
            with(rootNode) {
                name shouldBe "unnamed_scene"
                transformation shouldBe Mat4(
                        0.0253999997f, 0f, 0f, 0f,
                        0f, 0f, -0.0253999997f, 0f,
                        0f, 0.0253999997f, 0f, 0f,
                        0f, 0f, 0f, 1f)
                parent shouldBe null
                numChildren shouldBe 9

                with(children[0]) {
                    name shouldBe "Sphere01"
                    transformation shouldBe Mat4(
                            1f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f,
                            0f, 0f, 1f, 0f,
                            0f, -295.276001f, 78.7401962f, 1f)
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                    metaData.isEmpty() shouldBe true
                }

                with(children[4]) {
                    name shouldBe "Sphere08"
                    transformation shouldBe Mat4(
                            -0.707107008f, 0.707107008f, 0f, 0f,
                            -0.707107008f, -0.707107008f, 0f, 0f,
                            0.000000000f, 0f, 1f, 0f,
                            208.791000f, 208.791000f, 78.7401962f, 1f)
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 4
                    metaData.isEmpty() shouldBe true
                }

                with(children[8]) {
                    name shouldBe "Teapot01"
                    transformation shouldBe Mat4()
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 8
                    metaData.isEmpty() shouldBe true
                }
            }

            numMeshes shouldBe 9

            with(meshes[0]) {
                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 2880
                numFaces shouldBe 960
                vertices[0] shouldBe Vec3(-0f,8.96846008f,45.0873985f)
                vertices[1439] shouldBe Vec3(8.79601955f,44.2212982f,8.96846008f)
                vertices[2879] shouldBe Vec3(0f,0f,-45.9709015f)
                normals[0] shouldBe Vec3(-0.00574300019f,0.214348003f,0.976740003f)
                normals[1439] shouldBe Vec3(0.190725997f,0.962058008f,0.195107996f)
                normals[2879] shouldBe Vec3(0f,0f,-1f)
                textureCoords[0][0][0] shouldBe 0f
                textureCoords[0][0][1] shouldBe 0.937500000f
                textureCoords[0][1439][0] shouldBe 0.968750000f
                textureCoords[0][1439][1] shouldBe 0.562500000f
//                for (i in 0 until numFaces)
//                    for (j in 0..2)
//                        faces[i][j] shouldBe i * 3 + j
//                name shouldBe "Plane"
            }
//            // submesh
//            with(meshes[1]) {
//                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
//                numVertices shouldBe 342
//                numFaces shouldBe 114
//                vertices[0] shouldBe Vec3(0.860741317f, -3.45404100f, 0.648772180f)
//                vertices[170] shouldBe Vec3(-0.884529710f, -1.541123f, -0.220538393f)
//                vertices[341] shouldBe Vec3(-0.593643427f, -1.76318300f, -0.361052006f)
//                normals[0] shouldBe Vec3(-0.639659882f, 0.316945910f, 0.700271904f)
//                normals[170] shouldBe Vec3(0.697514117f, -0.194711894f, 0.689609706f)
//                normals[341] shouldBe Vec3(0.263714999f, 0.0831033513f, 0.961014211f)
//                for(i in 0 until numFaces)
//                    for(j in 0..2)
//                        faces[i][j] shouldBe i * 3 + j
//                name shouldBe "Plane"
//            }

//            numMaterials shouldBe 1
//            with(materials[0]) {
//                name shouldBe "test_Smoothing"
//                shadingModel shouldBe AiShadingMode.blinn
//                twoSided shouldBe false
//                wireframe shouldBe false
//                with(color!!) {
//                    ambient shouldBe Vec3(0.100000001f)
//                    diffuse shouldBe Vec3(0.141176000f, 0.184313998f, 0.411765009f)
//                    specular shouldBe Vec3(0.400000006f)
//                    emissive shouldBe Vec3()
//                    reflective shouldBe Vec3()
//                }
//                shininess shouldBe 10f
//                reflectivity shouldBe 0f
//                refracti shouldBe 1f
//            }
        }
    }
}