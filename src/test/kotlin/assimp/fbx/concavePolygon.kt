package assimp.fbx

import assimp.AiVector3D
import assimp.Importer
import assimp.getResource
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe

object concavePolygon {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {
                name shouldBe "RootNode"
                transformation shouldBe Mat4()
                parent shouldBe null
                numChildren shouldBe 1

                with(children[0]) {
                    name shouldBe "Mesh_Object"
                    transformation shouldBe Mat4()
                    (parent === rootNode) shouldBe true
                    numChildren shouldBe 0

                    numMeshes shouldBe 1
                    meshes[0] shouldBe 0

                    metaData.get<String>("UserProperties") shouldBe ""
                    metaData.get<Boolean>("IsNull") shouldBe false
                    metaData.get<Int>("InheritType") shouldBe 1
                    metaData.get<AiVector3D>("ScalingMax") shouldBe AiVector3D()
                    metaData.get<Int>("DefaultAttributeIndex") shouldBe 0
                    metaData.get<String>("COLLADA_ID") shouldBe "Mesh_Object"
                }
            }

            numMeshes shouldBe 1

            with(meshes[0]) {

                primitiveTypes shouldBe 8
                numVertices shouldBe 66
                numFaces shouldBe 1

                vertices[0] shouldBe Vec3(-1.14600003f, 2.25515008f, 3.07623005f)
                vertices[32] shouldBe Vec3(-1.14600003f, 2.40000010f, 3.03749990f)
                vertices[65] shouldBe Vec3(-1.14600003f, 2.40000010f, 3.09050012f)

                normals.isEmpty() shouldBe true
                tangents.isEmpty() shouldBe true
                bitangents.isEmpty() shouldBe true
                colors.isEmpty() shouldBe true
                textureCoords.isEmpty() shouldBe true

                faces.size shouldBe 1
                for (i in faces[0].indices) faces[0][i] shouldBe i

                materialIndex shouldBe 0

                name shouldBe "Mesh_Object"
            }

            numMaterials shouldBe 1

            with(materials[0]) {
                name shouldBe "test_Smoothing"
                with(color!!) {
                    diffuse shouldBe AiVector3D(0.141176000f, 0.184313998f, 0.411765009f)
                    emissive shouldBe AiVector3D()
                    ambient shouldBe AiVector3D(0.200000003f)
                    specular shouldBe AiVector3D(0.200000003f)
                    shininessStrength shouldBe 1f
                    shininess shouldBe 20f
                    transparent shouldBe AiVector3D()
                    opacity shouldBe 1f
                    reflective shouldBe AiVector3D()
                    reflectivity shouldBe 1f
                    bumpScaling shouldBe 1f
                    displacementScaling shouldBe 1f
                }
            }

            metaData.get<Double>("UnitScaleFactor") shouldBe 100.0
        }
    }
}