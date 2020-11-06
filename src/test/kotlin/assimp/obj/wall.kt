package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotest.matchers.shouldBe

object wall {

    operator fun invoke(fileName: String) {

        Importer().testFile(getResource(fileName)) {

            flags shouldBe 0

            rootNode.apply {

                name shouldBe "wall.obj"
                transformation shouldBe Mat4()
                numChildren shouldBe 1

                children[0].apply {

                    name shouldBe "defaultobject"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                }
                numMeshes shouldBe 0
            }
            numMeshes shouldBe 1
            meshes[0].apply {
                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 1955769
                numFaces shouldBe 651923

                val exp = 0.001f

                vertices[0].shouldEqual(Vec3(-0.498f, 0f, 0f), exp)
                vertices[500_000].shouldEqual(Vec3(20f, 253f, -0.498f), exp)
                vertices[1_000_000].shouldEqual(Vec3(138f, 192f, 43.498f), exp)
                vertices[1_500_000].shouldEqual(Vec3(31f, 39.843f, 64f), exp)
                vertices[1_955_768].shouldEqual(Vec3(149.736f, 63f, 118f), exp)

                normals[0].shouldEqual(Vec3(-4.396f, -1.715f, -1.715f), exp)
                normals[500_000].shouldEqual(Vec3(0f, 0f, -6.283f), exp)
                normals[1_000_000].shouldEqual(Vec3(-0f, 0f, 9.424f), exp)
                normals[1_500_000].shouldEqual(Vec3(1.727f, 2.221f, 14.456f), exp)
                normals[1_955_768].shouldEqual(Vec3(3.742f, 4.025f, 7.639f), exp)

                var i = 0
                faces.forEach {
                    it.size shouldBe 3
                    it shouldBe MutableList(3) { i++ }
                }
            }

            numMaterials shouldBe 1

            materials[0].apply {
                name shouldBe AI_DEFAULT_MATERIAL_NAME
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3()
                    diffuse shouldBe Vec3(0.6)
                    specular shouldBe Vec3()
                    emissive shouldBe Vec3()
                    shininess shouldBe 0f
                    opacity shouldBe 1f
                    refracti shouldBe 1f
                }
            }
        }
    }
}