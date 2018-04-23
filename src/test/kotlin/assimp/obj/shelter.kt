package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import java.io.File
import java.net.URI

/**
 * Created by Sunny on 19/01/2018.
 */

object shelter {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

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

                vertices[0] shouldBe Vec3(221.4917, 238.7550, 410.0556)
                vertices[1] shouldBe Vec3(221.4917, 238.7550, 300.0556)
                vertices[2] shouldBe Vec3(281.4918, 238.7550, 300.0556)
                vertices[3] shouldBe Vec3(281.4918, 238.7550, 410.0556)

                vertices[4] shouldBe Vec3(221.4917, 239.7550, 410.0556)
                vertices[5] shouldBe Vec3(281.4918, 239.7550, 410.0556)
                vertices[6] shouldBe Vec3(281.4918, 239.7550, 300.0556)
                vertices[7] shouldBe Vec3(221.4917, 239.7550, 300.0556)

                vertices[8] shouldBe Vec3(221.4917, 238.7550, 410.0556)
                vertices[9] shouldBe Vec3(281.4918, 238.7550, 410.0556)
                vertices[10] shouldBe Vec3(281.4918, 239.7550, 410.0556)
                vertices[11] shouldBe Vec3(221.4917, 239.7550, 410.0556)

                vertices[12] shouldBe Vec3(281.4918, 238.7550, 410.0556)
                vertices[13] shouldBe Vec3(281.4918, 238.7550, 300.0556)
                vertices[14] shouldBe Vec3(281.4918, 239.7550, 300.0556)
                vertices[15] shouldBe Vec3(281.4918, 239.7550, 410.0556)

                vertices[16] shouldBe Vec3(281.4918, 238.7550, 300.0556)
                vertices[17] shouldBe Vec3(221.4917, 238.7550, 300.0556)
                vertices[18] shouldBe Vec3(221.4917, 239.7550, 300.0556)
                vertices[19] shouldBe Vec3(281.4918, 239.7550, 300.0556)

                vertices[20] shouldBe Vec3(221.4917, 238.7550, 300.0556)
                vertices[21] shouldBe Vec3(221.4917, 238.7550, 410.0556)
                vertices[22] shouldBe Vec3(221.4917, 239.7550, 410.0556)
                vertices[23] shouldBe Vec3(221.4917, 239.7550, 300.0556)

                var i = 0
                faces.forEach {
                    it.size shouldBe 4
                    it shouldBe mutableListOf(i++, i++, i++, i++)
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