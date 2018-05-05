package assimp.collada

import assimp.AiPostProcessStep
import assimp.AiPrimitiveType
import assimp.Importer
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import java.net.URL

object `treasure_smooth Pretransform` {

    operator fun invoke(fileName: URL) {

        with(Importer().readFile(fileName, AiPostProcessStep.PreTransformVertices.i)!!) {

            flags shouldBe 0
            with(rootNode) {
                name shouldBe "Scene"
                transformation shouldBe Mat4()
                parent shouldBe null
                numChildren shouldBe 11

                with(children[0]) {
                    name shouldBe "Plane"
                    transformation shouldBe Mat4()
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                    metaData.isEmpty() shouldBe true
                }

//                with(children[16]) {
//                    name shouldBe "Cylinder_015"
//                    transformation shouldBe Mat4(
//                            0.0866885632f, -0.0170170404f, 0.01285674f, 0f,
//                            0.0152270300f, 0.08706335f, 0.01256552f,0f,
//                    -0.01493363f, -0.01000874f,0.08744479f,0f,
//                    1.314583f,-5.515009f,0.4121983f,1f)
//                    (parent === rootNode) shouldBe true
//                    numChildren shouldBe 0
//                    children.isEmpty() shouldBe true
//                    numMeshes shouldBe 2
//                    meshes[0] shouldBe 26
//                    meshes[1] shouldBe 27
//                    metaData.isEmpty() shouldBe true
//                }
//
//                with(children[34]) {
//                    name shouldBe "Cube_015"
//                    transformation shouldBe Mat4(
//                            0.122561797f, -0.0906103402f, 0.00194863102f, 0f,
//                            0.0853303894f, 0.116471097f, 0.0488739200f, 0f,
//                            -0.0305411592f, -0.0382059515f, 0.144371003f, 0f,
//                            1.10377395f, -4.13784218f, 0.766323626f, 1f)
//                    (parent === rootNode) shouldBe true
//                    numChildren shouldBe 0
//                    children.isEmpty() shouldBe true
//                    numMeshes shouldBe 1
//                    meshes[0] shouldBe 48
//                    metaData.isEmpty() shouldBe true
//                }
//
//                with(children[67]) {
//                    name shouldBe "Cylinder_027"
//                    transformation shouldBe Mat4(
//                            0.0158306006f, 0.0467648916f, 0.00405431492f, 0f,
//                            -0.0440383293f, 0.0133153200f, 0.0183664802f, 0f,
//                            0.0162486192f, -0.00947351381f, 0.0458283201f, 0f,
//                            -0.724227428f, -2.80546498f, 0.661597490f, 1f)
//                    (parent === rootNode) shouldBe true
//                    numChildren shouldBe 0
//                    children.isEmpty() shouldBe true
//                    numMeshes shouldBe 2
//                    meshes[0] shouldBe 84
//                    meshes[1] shouldBe 85
//                    metaData.isEmpty() shouldBe true
//                }
            }

//            numMeshes shouldBe 86
//            with(meshes[0]) {
//                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
//                numVertices shouldBe 1554
//                numFaces shouldBe 518
//                vertices[0] shouldBe Vec3(7.50098801f, 6.56336403f, 0f)
//                vertices[776] shouldBe Vec3(-1.62673700f, -3.29218197f, 0.503621578f)
//                vertices[1553] shouldBe Vec3(-1.15598500f, -3.56582594f, 0.488107890f)
//                normals[0] shouldBe Vec3(0.00296032405f, 0.000976616982f, 0.999995172f)
//                normals[776] shouldBe Vec3(0.600491107f, 0.295148909f, 0.743167281f)
//                normals[1553] shouldBe Vec3(0.254105508f, 0.335226387f, 0.907223225f)
//                for (i in 0 until numFaces)
//                    for (j in 0..2)
//                        faces[i][j] shouldBe i * 3 + j
//                name shouldBe "Plane"
//            }
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