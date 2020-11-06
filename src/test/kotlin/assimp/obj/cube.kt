package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import io.kotest.matchers.shouldBe
import uno.kotlin.uri
import uno.kotlin.url

/**
 * Created by elect on 16/11/2016.
 */

object cube {

    operator fun invoke(fileName: String) {

        Importer().testFile(getResource(fileName)) {

            with(rootNode) {
                name shouldBe "cube.obj"
                transformation shouldBe Mat4()
                numChildren shouldBe 2

                with(children[0]) {
                    name shouldBe "default"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                    metaData.isEmpty() shouldBe true
                }
                with(children[1]) {
                    name shouldBe ""
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 0
                    meshes.isEmpty() shouldBe true
                    metaData.isEmpty() shouldBe true
                }
                numMeshes shouldBe 0
            }
            numMeshes shouldBe 1
            with(meshes[0]) {
                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 36
                numFaces shouldBe 12

                val eps = 0.001f

                vertices[0].shouldEqual(Vec3(-5f, -5f, -5f), eps)
                vertices[10].shouldEqual(Vec3(-5f, 5f, 5f), eps)
                vertices[20].shouldEqual(Vec3(5f, 5f, 5f), eps)
                vertices[30].shouldEqual(Vec3(-5f, 5f, -5f), eps)
                vertices[35].shouldEqual(Vec3(-5f, 5f, -5f), eps)

                normals[0].shouldEqual(Vec3(0f, 0f, -2f), eps)
                normals[10].shouldEqual(Vec3(0f, -0f, 1f), eps)
                normals[20].shouldEqual(Vec3(0f, -0f, 2f), eps)
                normals[30].shouldEqual(Vec3(0f, 0f, -1f), eps)
                normals[35].shouldEqual(Vec3(0f, 0f, -1f), eps)

                Vec2(textureCoords[0][0]).shouldEqual(Vec2(1f, 0f), eps)
                Vec2(textureCoords[0][10]).shouldEqual(Vec2(0f, 1f), eps)
                Vec2(textureCoords[0][20]).shouldEqual(Vec2(1f, 1f), eps)
                Vec2(textureCoords[0][30]).shouldEqual(Vec2(0f, 0f), eps)
                Vec2(textureCoords[0][35]).shouldEqual(Vec2(0f, 0f), eps)

                textureCoords[0][0].size shouldBe 2

                var i = 0
                faces.forEach {
                    it.size shouldBe 3
                    it shouldBe mutableListOf(i++, i++, i++)
                }

                name shouldBe "default"
            }
            numMaterials shouldBe 1
            materials[0].apply {
                name shouldBe AI_DEFAULT_MATERIAL_NAME
                shadingModel shouldBe AiShadingMode.gouraud
                color!!.apply {
                    ambient shouldBe Vec3()
                    diffuse!!.shouldEqual(Vec3(0.6), 0.001f)
                    specular shouldBe Vec3()
                    emissive shouldBe Vec3()
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    transparent!!.shouldEqual(Vec3(1f), 0.001f)
                    refracti shouldBe 1f
                }
            }
        }
    }
}