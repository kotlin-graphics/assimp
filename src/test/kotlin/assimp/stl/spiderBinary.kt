package assimp.stl

import assimp.AiPrimitiveType
import assimp.Importer
import assimp.getResource
import assimp.models
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

object spiderBinary {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {

                name shouldBe "<STL_BINARY>"
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

                normals[0] shouldBe Vec3(0.468281955, -0.863497794, -0.187306240)
                normals[500] shouldBe Vec3(0.622135758, 0.587329984, -0.517678082)
                normals[1000] shouldBe Vec3(-0.836838484, -0.476067126, 0.270298779)
                normals[1500] shouldBe Vec3(0.413947791, 0.814008772, 0.407476395)
                normals[2000] shouldBe Vec3(0.521721005, 0.567762673, -0.636751771)
                normals[2500] shouldBe Vec3(-0.567528188, -0.492581815, -0.659753680)
                normals[3000] shouldBe Vec3(0.676752925, -0.386138558, -0.626819372)
                normals[3500] shouldBe Vec3(0.380660713, 0.715228200, -0.586128056)
                normals[4000] shouldBe Vec3(0.147421882, 0.461364329, -0.874877036)
                normals[4103] shouldBe Vec3(0.780922532, 0.308308363, 0.543236554)

                tangents.isEmpty() shouldBe true
                bitangents.isEmpty() shouldBe true

                colors.all { it.isEmpty() } shouldBe true

                textureCoords.all { it.isEmpty() } shouldBe true

                faces.size shouldBe 1368
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