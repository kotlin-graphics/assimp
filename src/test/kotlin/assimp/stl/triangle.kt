package assimp.stl

import assimp.AiPrimitiveType
import assimp.Importer
import assimp.getResource
import assimp.models
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

object triangle {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {

                name shouldBe "testTriangle"
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

                numVertices shouldBe 3

                vertices[0] shouldBe Vec3(1, 1, 0)
                vertices[1] shouldBe Vec3(-1, 1, 0)
                vertices[2] shouldBe Vec3(0, -1, 0)

                normals[0] shouldBe Vec3(0, 0, 1)
                normals[1] shouldBe Vec3(0, 0, 1)
                normals[2] shouldBe Vec3(0, 0, 1)

                tangents.isEmpty() shouldBe true
                bitangents.isEmpty() shouldBe true

                colors.all { it.isEmpty() } shouldBe true

                textureCoords.all { it.isEmpty() } shouldBe true

                faces.size shouldBe 1
                faces[0].size shouldBe 3
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