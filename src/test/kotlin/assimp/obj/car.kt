package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotest.matchers.shouldBe

/**
 * Created by Sunny on 19/01/2018.
 */

object car {

    operator fun invoke(directory: String) {

        val obj = getResource("$directory/car_cpl_completed.obj")
        val mtl = getResource("$directory/car_cpl_completed.mtl")

        Importer().testURLs(obj, mtl) {

            flags shouldBe 0

            rootNode.apply {

                name shouldBe "car_cpl_completed.obj"
                transformation shouldBe Mat4()
                numChildren shouldBe 108

                children[0].apply {
                    name shouldBe "0"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 0
                }
                with(children[1]) {
                    name shouldBe "1"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                }
                with(children[2]) {

                    name shouldBe "2"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 1
                }
                with(children[50]) {

                    name shouldBe "50"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 0
                }
                with(children[100]) {

                    name shouldBe "100"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 69
                }
                with(children[107]) {

                    name shouldBe "107"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 74
                }

                numMeshes shouldBe 0
            }

            numMeshes shouldBe 75

            meshes[0].apply {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 1800
                numFaces shouldBe 600

                val eps = 0.01f

                vertices[0].shouldEqual(Vec3(1.32f, -7.5f, -1.5f), eps)
                vertices[500].shouldEqual(Vec3(15f, 7.5f, -1.5f), eps)
                vertices[1_000].shouldEqual(Vec3(15f, -1.98f, 5.22f), eps)
                vertices[1_500].shouldEqual(Vec3(1.08f, 4.41f, -1.67f), eps)
                vertices[1_799].shouldEqual(Vec3(15f, -1.67f, -6.58f), eps)

                normals[0].shouldEqual(Vec3(0f, -1f, 0f), eps)
                normals[500].shouldEqual(Vec3(0f, 1f, 0f), eps)
                normals[1_000].shouldEqual(Vec3(1f, 0f, 0f), eps)
                normals[1_500].shouldEqual(Vec3(0f, 0.54f, 0.83f), eps)
                normals[1_799].shouldEqual(Vec3(0f, 0.83f, 0.54f), eps)

                textureCoords[0][0].shouldEqual2(0.27f, 0.42f, eps)
                textureCoords[0][500].shouldEqual2(0.57f, 0.46f, eps)
                textureCoords[0][1_000].shouldEqual2(0.47f, 0.62f, eps)
                textureCoords[0][1_500].shouldEqual2(0.71f, 0.36f, eps)
                textureCoords[0][1_799].shouldEqual2(0.48f, 0.34f, eps)

                var i = 0
                faces.forEach {
                    it.size shouldBe 3
                    it shouldBe MutableList(3) { i++ }
                }

                materialIndex shouldBe 1
                name shouldBe "1"
            }

            meshes[37].apply {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 1392
                numFaces shouldBe 464

                val eps = 0.01f

                vertices[0].shouldEqual(Vec3(-165f, -7.5f, -7.5f), eps)
                vertices[300].shouldEqual(Vec3(-170.5f, 3.5f, -37.5f), eps)
                vertices[600].shouldEqual(Vec3(-174.44f, -5.97f, -33.5f), eps)
                vertices[900].shouldEqual(Vec3(-166.98f, 1.99f, -7.5f), eps)
                vertices[1391].shouldEqual(Vec3(-165.91f, -1.67f, -37.5f), eps)

                normals[0].shouldEqual(Vec3(0f, -1f, 0f), eps)
                normals[300].shouldEqual(Vec3(0f, 0f, -1f), eps)
                normals[600].shouldEqual(Vec3(0.97f, 0.23f, 0f), eps)
                normals[900].shouldEqual(Vec3(0f, 0f, 1f), eps)
                normals[1391].shouldEqual(Vec3(-0.54f, 0.83f, 0f), eps)

                textureCoords[0][0].shouldEqual2(0f, 0.17f, eps)
                textureCoords[0][300].shouldEqual2(0.99f, 0f, eps)
                textureCoords[0][600].shouldEqual2(0f, 0.03f, eps)
                textureCoords[0][900].shouldEqual2(0.99f, 0.17f, eps)
                textureCoords[0][1391].shouldEqual2(0f, 0f, eps)

                var i = 0
                faces.forEach {
                    it.size shouldBe 3
                    it shouldBe MutableList(3) { i++ }
                }

                materialIndex shouldBe 1
            }

            meshes[74].apply {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 2436
                numFaces shouldBe 812

                val eps = 0.01f

                vertices[0].shouldEqual(Vec3(7.30f, -8.1f, -18.62f), eps)
                vertices[600].shouldEqual(Vec3(4.56f, -5.09f, -20.05f), eps)
                vertices[1_200].shouldEqual(Vec3(-113.86, -6.3f, -17.99f), eps)
                vertices[1_800].shouldEqual(Vec3(-112.69f, -6.3f, -18.67f), eps)
                vertices[2_435].shouldEqual(Vec3(-80.44f, -5.8f, -20.64f), eps)

                normals[0].shouldEqual(Vec3(0f, -1f, 0f), eps)
                normals[600].shouldEqual(Vec3(0.98f, 0f, -0.18f), eps)
                normals[1_200].shouldEqual(Vec3(0.45f, 0f, -0.88f), eps)
                normals[1_800].shouldEqual(Vec3(0f, 1f, 0f), eps)
                normals[2_435].shouldEqual(Vec3(0f, 1f, 0f), eps)

                textureCoords[0][0].shouldEqual2(0.84f, 0.21f, eps)
                textureCoords[0][600].shouldEqual2(0.84f, 0.04f, eps)
                textureCoords[0][1_200].shouldEqual2(0f, 0.99f, eps)
                textureCoords[0][1_800].shouldEqual2(0f, 0.98f, eps)
                textureCoords[0][2_435].shouldEqual2(0.01f, 0.9f, eps)

                var i = 0
                faces.forEach {
                    it.size shouldBe 3
                    it shouldBe MutableList(3) { i++ }
                }

                materialIndex shouldBe 8
            }

            numMaterials shouldBe 12

            materials[0].apply {
                name shouldBe AI_DEFAULT_MATERIAL_NAME
                shadingModel shouldBe AiShadingMode.gouraud
                color!!.apply {
                    ambient shouldBe Vec3()
                    diffuse shouldBe Vec3(0.6f)
                    specular shouldBe Vec3()
                    emissive shouldBe Vec3()
                    transparent shouldBe Vec3(1f)
                }
                shininess shouldBe 0f
                opacity shouldBe 1f
                refracti shouldBe 1f
            }

            materials[6].apply {
                name shouldBe "MTL4"
                shadingModel shouldBe AiShadingMode.gouraud
                color!!.apply {
                    ambient shouldBe Vec3()
                    diffuse!!.shouldEqual(Vec3(0.66f, 0.4f, 0.8f), 0.01f)
                    specular shouldBe Vec3()
                    emissive shouldBe Vec3()
                    transparent shouldBe Vec3(1f)
                }
                shininess shouldBe 0f
                opacity shouldBe 1f
                refracti shouldBe 1f
            }

            materials[11].apply {
                name shouldBe "MTL9"
                shadingModel shouldBe AiShadingMode.gouraud
                color!!.apply {
                    ambient shouldBe Vec3()
                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3()
                    emissive shouldBe Vec3()
                    transparent shouldBe Vec3(1f)
                }
                shininess shouldBe 0f
                opacity shouldBe 1f
                refracti shouldBe 1f
            }
        }
    }
}