package assimp.collada

import assimp.AiShadingMode
import assimp.Importer
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import java.net.URL

object concavePoly {

    operator fun invoke(fileName: URL) {

        with(Importer().readFile(fileName)!!) {

            flags shouldBe 0
            with(rootNode) {
                name shouldBe "VisualSceneNode"
                transformation shouldBe Mat4()
                parent shouldBe null
                numChildren shouldBe 1

                with(children[0]) {
                    name shouldBe "Mesh_Object"
                    transformation shouldBe Mat4()
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0
                    children.isEmpty() shouldBe true
                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0
                    metaData.isEmpty() shouldBe true
                }
            }

            numMeshes shouldBe 1
            with(meshes[0]) {
                primitiveTypes shouldBe 8
                numVertices shouldBe 66
                numFaces shouldBe 1
                vertices[0] shouldBe Vec3(-1.14600003f, 2.25515008f, 3.07623005f)
                vertices[32] shouldBe Vec3(-1.14600003f,2.40000010f,3.03749990f)
                vertices[65] shouldBe Vec3(-1.14600003f, 2.40000010f, 3.0905f)
                faces[0].forEachIndexed { i, it -> it shouldBe i }
                name shouldBe "Mesh_Object"
            }

            numMaterials shouldBe 1
            with(materials[0]) {
                name shouldBe "test_Smoothing"
                shadingModel shouldBe AiShadingMode.blinn
                twoSided shouldBe false
                wireframe shouldBe false
                with(color!!) {
                    ambient shouldBe Vec3(0.100000001f)
                    diffuse shouldBe Vec3(0.141176000f, 0.184313998f, 0.411765009f)
                    specular shouldBe Vec3(0.400000006f)
                    emissive shouldBe Vec3()
                    reflective shouldBe Vec3()
                }
                shininess shouldBe 10f
                reflectivity shouldBe 0f
                refracti shouldBe 1f
            }
        }
    }
}