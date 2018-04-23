package assimp.stl

import assimp.AiPrimitiveType
import assimp.Importer
import assimp.getResource
import assimp.models
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

object sphereWithHole {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {

                name shouldBe "tinysphere_withhole"
                transformation shouldBe Mat4()
                parent shouldBe null
                numChildren shouldBe 0
//                children shouldBe null
                numMeshes shouldBe 1
                meshes[0] shouldBe 0
                metaData.isEmpty() shouldBe true
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

                normals[0] shouldBe Vec3(-0.129999995, -0.129999995, -0.980000019)
                normals[100] shouldBe Vec3(-0.689999998, 0.209999993, -0.689999998)
                normals[200] shouldBe Vec3(-0.870000005, 0.200000003, -0.449999988)
                normals[300] shouldBe Vec3(-0.660000026, -0.730000019, -0.180000007)
                normals[400] shouldBe Vec3(-0.129999995, -0.980000019, -0.129999995)
                normals[500] shouldBe Vec3(-0.920000017, 0.379999995, 0.119999997)
                normals[600] shouldBe Vec3(-0.159999996, 0.899999976, 0.419999987)
                normals[700] shouldBe Vec3(-0.180000007, -0.730000019, 0.660000026)
                normals[800] shouldBe Vec3(0.449999988, -0.200000003, 0.870000005)
                normals[854] shouldBe Vec3(0.129999995, 0.129999995, 0.980000019)

                tangents.isEmpty() shouldBe true
                bitangents.isEmpty() shouldBe true

                colors.all { it.isEmpty() } shouldBe true

                textureCoords.all { it.isEmpty() } shouldBe true

                faces.size shouldBe 285
                faces.all { it.size == 3 } shouldBe true
                var p = 0
                faces[0].all { it == p++ } shouldBe true

                numBones shouldBe 0

                name shouldBe ""

                mNumAnimMeshes shouldBe 0
            }

            numMaterials shouldBe 1

            with(materials[0]) {

                with(color!!) {

                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3(1f)
                    ambient shouldBe Vec3(1f)
                }
            }
        }
    }
}