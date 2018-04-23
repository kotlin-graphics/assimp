package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe

object wall {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {
            println()
//            with(rootNode) {
//
//                name shouldBe "box.obj"
//                transformation shouldBe Mat4()
//                numChildren shouldBe 1
//
//                with(children[0]) {
//
//                    name shouldBe "1"
//                    transformation shouldBe Mat4()
//                    numChildren shouldBe 0
//                    numMeshes shouldBe 1
//                    meshes[0] shouldBe 0
//                }
//                numMeshes shouldBe 0
//            }
//            numMeshes shouldBe 1
//            with(meshes[0]) {
//                primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
//                numVertices shouldBe 24
//                numFaces shouldBe 6
//
//                vertices[0] shouldBe Vec3(-0.5, +0.5, +0.5)
//                vertices[5] shouldBe Vec3(+0.5, -0.5, -0.5)
//                vertices[10] shouldBe Vec3(+0.5, -0.5, -0.5)
//                vertices[15] shouldBe Vec3(-0.5, +0.5, +0.5)
//                vertices[20] shouldBe Vec3(+0.5, -0.5, -0.5)
//                vertices[23] shouldBe Vec3(+0.5, -0.5, +0.5)
//
//                var i = 0
//                faces.forEach {
//                    it.size shouldBe 4
//                    it shouldBe mutableListOf(i++, i++, i++, i++)
//                }
//            }
//            with(materials[0]) {
//                name shouldBe AI_DEFAULT_MATERIAL_NAME
//                shadingModel shouldBe AiShadingMode.gouraud
//                with(color!!) {
//                    ambient shouldBe Vec3()
//                    diffuse shouldBe Vec3(0.6)
//                    specular shouldBe Vec3()
//                    emissive shouldBe Vec3()
//                    shininess shouldBe 0f
//                    opacity shouldBe 1f
//                    refracti shouldBe 1f
//                }
//            }
        }
    }
}