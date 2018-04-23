package assimp.fbx

import assimp.Importer
import assimp.getResource
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe

object animFullRot {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {
//                name shouldBe "<MD5_Root>"
//                transformation shouldBe Mat4(
//                        1f, 0f, 0f, 0f,
//                        0f, 0f, -1f, 0f,
//                        0f, 1f, 0f, 0f,
//                        0f, 0f, 0f, 1f)
//                parent shouldBe null
//                numChildren shouldBe 2
//
//                with(children[0]) {
//                    name shouldBe "<MD5_Mesh>"
//                    transformation shouldBe Mat4()
//                    (parent === rootNode) shouldBe true
//                    numChildren shouldBe 0
//
//                    numMeshes shouldBe 1
//                    meshes[0] shouldBe 0
//                }
//
//                with(children[1]) {
//                    name shouldBe "<MD5_Hierarchy>"
//                    transformation shouldBe Mat4()
//                    (parent === rootNode) shouldBe true
//                    numChildren shouldBe 1
//
//                    with(children[0]) {
//                        name shouldBe "Bone"
//                        transformation shouldBe Mat4(
//                                1.00000000f, -0.000000000f, 0.000000000f, 0.000000000f,
//                                0.000000000f, -5.96046448e-07f, 1.00000000f, 0.000000000f,
//                                -0.000000000f, -1.00000000f, -5.96046448e-07f, 0.000000000f,
//                                0.000000000f, 0.000000000f, 0.000000000f, 1.00000000f
//                        )
//                        (parent === rootNode.children[1]) shouldBe true
//                        numChildren shouldBe 0
//
//                        numMeshes shouldBe 0
//                    }
//                    numMeshes shouldBe 0
//                }
//                numMeshes shouldBe 0
            }

//            with(meshes[0]) {
//
//                primitiveTypes shouldBe 4
//                numVertices shouldBe 8436
//                numFaces shouldBe 2812
//
//                vertices[0] shouldBe Vec3(2.81622696f, 0.415550232f, 24.7310295f)
//                vertices[4217] shouldBe Vec3(7.40894270f, 2.25090504f, 23.1958885f)
//                vertices[8435] shouldBe Vec3(1.2144182f, -3.79157066f, 25.3626385f)
//
//                textureCoords[0][0][0] shouldBe 0.704056978f
//                textureCoords[0][0][1] shouldBe 0.896108985f
//                textureCoords[0][4217][0] shouldBe 1.852235f
//                textureCoords[0][4217][1] shouldBe 0.437269986f
//                textureCoords[0][8435][0] shouldBe 0.303604007f
//                textureCoords[0][8435][1] shouldBe 1.94788897f
//
//                faces.asSequence().filterIndexed { i, f -> i in 0..7 }.forEachIndexed { i, f ->
//                    val idx = i * 3
//                    f shouldBe mutableListOf(idx + 1, idx + 2, idx)
//                }
//                faces[1407] shouldBe mutableListOf(4708, 4707, 4706)
//                faces[2811] shouldBe mutableListOf(8435, 8434, 8433)
//
//                numBones shouldBe 1
//
//                with(bones[0]) {
//                    name shouldBe "Bone"
//                    numWeights shouldBe 8436
//                    weights.forEachIndexed { i, w -> w.vertexId shouldBe i; w.weight shouldBe 1f }
//                    offsetMatrix shouldBe Mat4(
//                            1.00000000f, -0.000000000f, 0.000000000f, -0.000000000f,
//                            -0.000000000f, -5.96046448e-07f, -1.00000000f, 0.000000000f,
//                            0.000000000f, 1.00000000f, -5.96046448e-07f, -0.000000000f,
//                            -0.000000000f, 0.000000000f, -0.000000000f, 1.00000000f)
//                }
//                materialIndex shouldBe 0
//
//                name.isEmpty() shouldBe true
//            }
//
//            numMaterials shouldBe 1
//
//            with(materials[0]) {
//                textures[0].file shouldBe ""
//            }
        }
    }
}