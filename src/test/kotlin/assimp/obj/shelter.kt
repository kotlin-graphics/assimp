package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotest.matchers.shouldBe

/**
 * Created by Sunny on 19/01/2018.
 */

object shelter {

    operator fun invoke(directory: String) {

        val obj = getResource("$directory/statie B01.obj")
        val mtl = getResource("$directory/statie B01.mtl")

        Importer().testURLs(obj, mtl) {

            with(rootNode) {

                name shouldBe "statie B01.obj"
                transformation shouldBe Mat4()
                numChildren shouldBe 9

                with(children[0]) {

                    name shouldBe "Box17"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0

                }

                with(children[1]) {

                    name shouldBe "Box15"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 1
                }
                with(children[2]) {

                    name shouldBe "Box03"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 2
                }
                with(children[3]) {

                    name shouldBe "Box14"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 3
                }
                with(children[4]) {

                    name shouldBe "Object14"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 4
                }
                with(children[5]) {

                    name shouldBe "Box16"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 5
                }
                with(children[6]) {

                    name shouldBe "Box13"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 6
                }
                with(children[7]) {

                    name shouldBe "Box18"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 7
                }
                with(children[8]) {

                    name shouldBe "Box02"
                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 8
                }

                numMeshes shouldBe 0
            }

            numMeshes shouldBe 9
            with(meshes[0]) {
                primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
                numVertices shouldBe 24
                numFaces shouldBe 6

                val exp = 0.0001f

                vertices[0].shouldEqual(Vec3(221.4917, 238.7550, 410.0556), exp)
                vertices[1].shouldEqual(Vec3(221.4917, 238.7550, 300.0556), exp)
                vertices[2].shouldEqual(Vec3(281.4918, 238.7550, 300.0556), exp)
                vertices[3].shouldEqual(Vec3(281.4918, 238.7550, 410.0556), exp)

                vertices[4].shouldEqual(Vec3(221.4917, 239.7550, 410.0556), exp)
                vertices[5].shouldEqual(Vec3(281.4918, 239.7550, 410.0556), exp)
                vertices[6].shouldEqual(Vec3(281.4918, 239.7550, 300.0556), exp)
                vertices[7].shouldEqual(Vec3(221.4917, 239.7550, 300.0556), exp)

                vertices[8].shouldEqual(Vec3(221.4917, 238.7550, 410.0556), exp)
                vertices[9].shouldEqual(Vec3(281.4918, 238.7550, 410.0556), exp)
                vertices[10].shouldEqual(Vec3(281.4918, 239.7550, 410.0556), exp)
                vertices[11].shouldEqual(Vec3(221.4917, 239.7550, 410.0556), exp)

                vertices[12].shouldEqual(Vec3(281.4918, 238.7550, 410.0556), exp)
                vertices[13].shouldEqual(Vec3(281.4918, 238.7550, 300.0556), exp)
                vertices[14].shouldEqual(Vec3(281.4918, 239.7550, 300.0556), exp)
                vertices[15].shouldEqual(Vec3(281.4918, 239.7550, 410.0556), exp)

                vertices[16].shouldEqual(Vec3(281.4918, 238.7550, 300.0556), exp)
                vertices[17].shouldEqual(Vec3(221.4917, 238.7550, 300.0556), exp)
                vertices[18].shouldEqual(Vec3(221.4917, 239.7550, 300.0556), exp)
                vertices[19].shouldEqual(Vec3(281.4918, 239.7550, 300.0556), exp)

                vertices[20].shouldEqual(Vec3(221.4917, 238.7550, 300.0556), exp)
                vertices[21].shouldEqual(Vec3(221.4917, 238.7550, 410.0556), exp)
                vertices[22].shouldEqual(Vec3(221.4917, 239.7550, 410.0556), exp)
                vertices[23].shouldEqual(Vec3(221.4917, 239.7550, 300.0556), exp)

                var i = 0
                faces.forEach {
                    it.size shouldBe 4
                    it shouldBe MutableList(4) { i++ }
                }

                materialIndex shouldBe 1
            }

            with(materials[1]) {
                name shouldBe "wire_008008136"
                shadingModel shouldBe AiShadingMode.phong
                with(color!!) {
                    ambient shouldBe Vec3()
                    diffuse shouldBe Vec3(0.0314, 0.0314, 0.5333)
                    specular shouldBe Vec3(0.3500, 0.3500, 0.3500)
                }
            }
        }
    }
}