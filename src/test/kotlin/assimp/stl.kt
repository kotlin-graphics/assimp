package assimp

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

/**
 * Created by elect on 18/11/2016.
 */
class stl : StringSpec() {

    val stl = models + "/STL/"

    init {

        val triangle = "triangle.stl"

        triangle {

            val scene = Importer().readFile(stl + triangle)!!

            scene.flags shouldBe 0

            with(scene) {

                with(rootNode) {

                    name shouldBe "testTriangle"
                    transformation shouldBe Mat4()
                    parent shouldBe null
                    numChildren shouldBe 0
//                children shouldBe null
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                    metaData shouldBe null
                }
                numMeshes shouldBe 1
                meshes.size shouldBe 1

                with(meshes[0]) {

                    primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i

                    numVertices shouldBe 3

                    vertices[0] shouldBe Vec3(1, 1, 0)
                    vertices[1] shouldBe Vec3(-1, 1, 0)
                    vertices[2] shouldBe Vec3(0, -1, 0)

                    mNormals[0] shouldBe Vec3(0, 0, 1)
                    mNormals[1] shouldBe Vec3(0, 0, 1)
                    mNormals[2] shouldBe Vec3(0, 0, 1)

                    mTangents.isEmpty() shouldBe true
                    mBitangents.isEmpty() shouldBe true

                    mColors.all { it.isEmpty() } shouldBe true

                    mTextureCoords.all { it.isEmpty() } shouldBe true

                    faces.size shouldBe 1
                    faces[0].size shouldBe 3
                    var p = 0
                    faces[0].all { it == p++ } shouldBe true

                    mNumBones shouldBe 0

                    name shouldBe ""

                    mNumAnimMeshes shouldBe 0
                }

                numMaterials shouldBe 1

                with(materials[0]) {

                    with(color!!) {

                        diffuse shouldBe Vec3(0.6)
                        specular shouldBe Vec3(0.6)
                        ambient shouldBe Vec3(0.05)
                    }
                }
            }
        }

        val sphereWithHole = "sphereWithHole.stl"

        sphereWithHole {

            val scene = Importer().readFile(stl + sphereWithHole)!!

            scene.flags shouldBe 0

            with(scene) {

                with(rootNode) {

                    name shouldBe "tinysphere_withhole"
                    transformation shouldBe Mat4()
                    parent shouldBe null
                    numChildren shouldBe 0
//                children shouldBe null
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                    metaData shouldBe null
                }
                numMeshes shouldBe 1
                meshes.size shouldBe 1

                with(meshes[0]) {

                    primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i

                    numVertices shouldBe 855

                    vertices[0] shouldBe Vec3(1.50000000, 1.50000000, 0.000000000)
                    vertices[100] shouldBe Vec3(0.439339995, 1.50000000, 0.439339995)
                    vertices[200] shouldBe Vec3(0.144960001, 1.97835, 1.06982)
                    vertices[300] shouldBe Vec3(0.379390001, 0.602360010, 1.06585002)
                    vertices[400] shouldBe Vec3(1.50000000, 0.000000000, 1.50000000)
                    vertices[500] shouldBe Vec3(0.144960001, 1.97835, 1.93018)
                    vertices[600] shouldBe Vec3(1.08111000, 2.70614004, 2.28725004)
                    vertices[700] shouldBe Vec3(1.50000000, 0.439339995, 2.56065989)
                    vertices[800] shouldBe Vec3(1.93018, 1.02165, 2.85504)
                    vertices[854] shouldBe Vec3(1.50000000, 1.88823, 2.94888997)

                    mNormals[0] shouldBe Vec3(-0.129999995, -0.129999995, -0.980000019)
                    mNormals[100] shouldBe Vec3(-0.689999998, 0.209999993, -0.689999998)
                    mNormals[200] shouldBe Vec3(-0.870000005, 0.200000003, -0.449999988)
                    mNormals[300] shouldBe Vec3(-0.660000026, -0.730000019, -0.180000007)
                    mNormals[400] shouldBe Vec3(-0.129999995, -0.980000019, -0.129999995)
                    mNormals[500] shouldBe Vec3(-0.920000017, 0.379999995, 0.119999997)
                    mNormals[600] shouldBe Vec3(-0.159999996, 0.899999976, 0.419999987)
                    mNormals[700] shouldBe Vec3(-0.180000007, -0.730000019, 0.660000026)
                    mNormals[800] shouldBe Vec3(0.449999988, -0.200000003, 0.870000005)
                    mNormals[854] shouldBe Vec3(0.129999995, 0.129999995, 0.980000019)

                    mTangents.isEmpty() shouldBe true
                    mBitangents.isEmpty() shouldBe true

                    mColors.all { it.isEmpty() } shouldBe true

                    mTextureCoords.all { it.isEmpty() } shouldBe true

                    faces.size shouldBe 285
                    faces.all { it.size == 3 } shouldBe true
                    var p = 0
                    faces[0].all { it == p++ } shouldBe true

                    mNumBones shouldBe 0

                    name shouldBe ""

                    mNumAnimMeshes shouldBe 0
                }

                numMaterials shouldBe 1

                with(materials[0]) {

                    with(color!!) {

                        diffuse shouldBe Vec3(0.6)
                        specular shouldBe Vec3(0.6)
                        ambient shouldBe Vec3(0.05)
                    }
                }
            }
        }

        val spiderBinary = "Spider_binary.stl"

        spiderBinary {

            val scene = Importer().readFile(stl + spiderBinary)!!

            scene.flags shouldBe 0

            with(scene) {

                with(rootNode) {

                    name shouldBe "<STL_BINARY>"
                    transformation shouldBe Mat4()
                    parent shouldBe null
                    numChildren shouldBe 0
//                children shouldBe null
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                    metaData shouldBe null
                }
                numMeshes shouldBe 1
                meshes.size shouldBe 1

                with(meshes[0]) {

                    primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i

                    numVertices shouldBe 4104

                    vertices[0] shouldBe Vec3(0.907127976, 0.646165013, 0.795193374)
                    vertices[500] shouldBe Vec3(-2.70630598, -3.77559018, -1.13675642)
                    vertices[1000] shouldBe Vec3(-2.83839631, 2.59127927, -1.22905695)
                    vertices[1500] shouldBe Vec3(-0.870797396, 2.30535197, 0.676904559)
                    vertices[2000] shouldBe Vec3(0.164021820, -1.73031521, 1.32070541)
                    vertices[2500] shouldBe Vec3(0.796747267, -1.43064785, 1.25715435)
                    vertices[3000] shouldBe Vec3(-1.71063125, 0.356572717, 0.689150035)
                    vertices[3500] shouldBe Vec3(-1.77611852, -0.319954246, 0.903541803)
                    vertices[4000] shouldBe Vec3(-1.74193895, -0.297622085, 0.848268032)
                    vertices[4103] shouldBe Vec3(-1.86195970, -0.243324131, 0.762536407)

                    mNormals[0] shouldBe Vec3(0.468281955, -0.863497794, -0.187306240)
                    mNormals[500] shouldBe Vec3(0.622135758, 0.587329984, -0.517678082)
                    mNormals[1000] shouldBe Vec3(-0.836838484, -0.476067126, 0.270298779)
                    mNormals[1500] shouldBe Vec3(0.413947791, 0.814008772, 0.407476395)
                    mNormals[2000] shouldBe Vec3(0.521721005, 0.567762673, -0.636751771)
                    mNormals[2500] shouldBe Vec3(-0.567528188, -0.492581815, -0.659753680)
                    mNormals[3000] shouldBe Vec3(0.676752925, -0.386138558, -0.626819372)
                    mNormals[3500] shouldBe Vec3(0.380660713, 0.715228200, -0.586128056)
                    mNormals[4000] shouldBe Vec3(0.147421882, 0.461364329, -0.874877036)
                    mNormals[4103] shouldBe Vec3(0.780922532, 0.308308363, 0.543236554)

                    mTangents.isEmpty() shouldBe true
                    mBitangents.isEmpty() shouldBe true

                    mColors.all { it.isEmpty() } shouldBe true

                    mTextureCoords.all { it.isEmpty() } shouldBe true

                    faces.size shouldBe 1368
                    faces.all { it.size == 3 } shouldBe true
                    var p = 0
                    faces[0].all { it == p++ } shouldBe true

                    mNumBones shouldBe 0

                    name shouldBe ""

                    mNumAnimMeshes shouldBe 0
                }

                numMaterials shouldBe 1

                with(materials[0]) {

                    with(color!!) {

                        diffuse shouldBe Vec3(0.6)
                        specular shouldBe Vec3(0.6)
                        ambient shouldBe Vec3(0.05)
                    }
                }
            }
        }
    }
}