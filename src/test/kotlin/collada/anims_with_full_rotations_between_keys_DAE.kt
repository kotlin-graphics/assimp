package collada

import assimp.Importer
import assimp.collada
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import uno.kotlin.uri
import java.util.*

/**
 * Created by elect on 24/01/2017.
 */

class anims_with_full_rotations_between_keys_DAE : StringSpec() {

    init {
        val file = "anims_with_full_rotations_between_keys.dae"
        file {
            with(Importer().readFile((collada + file).uri)!!) {

                flags shouldBe 0

                with(rootNode) {
                    name shouldBe "Array Test 001"
                    transformation shouldBe Mat4(
                            1f, 0f, 0f, 0f,
                            0f, 0f, -1f, 0f,
                            0f, 1f, 0f, 0f,
                            0f, 0f, 0f, 1f)
                    parent shouldBe null
                    numChildren shouldBe 1

                    with(children[0]) {
                        name shouldBe "Box001"
                        transformation shouldBe Mat4(
                                1f, 5.235988e-08f, 0f, 0f,
                                -5.235988e-08f, 1f, 0f, 0f,
                                0f, 0f, 1f, 0f,
                                0f, 0f, 0f, 1f)
                        (parent === rootNode) shouldBe true
                        numChildren shouldBe 2

                        with(children[0]) {
                            name shouldBe "Box001-Pivot"
                            transformation shouldBe Mat4(
                                    1f, 0f, 0f, 0f,
                                    0f, 1f, 0f, 0f,
                                    0f, 0f, 1f, 0f,
                                    0.185947001f, 0f, 0f, 1f)
                            (parent === rootNode.children[0]) shouldBe true
                            numChildren shouldBe 0

                            numMeshes shouldBe 1
                            meshes[0] shouldBe 0
                            metaData shouldBe null
                        }

                        with(children[1]) {
                            name shouldBe "Box002"
                            transformation shouldBe Mat4(
                                    1f, 5.235988e-08f, 0f, 0f,
                                    -5.235988e-08f, 1f, 0f, 0f,
                                    0f, 0f, 1f, 0f,
                                    0f, 0f, 0f, 1f)
                            (parent === rootNode.children[0]) shouldBe true
                            numChildren shouldBe 2

                            with(children[0]) {
                                name shouldBe "Box002-Pivot"
                                transformation shouldBe Mat4(
                                        1f, 0f, 0f, 0f,
                                        0f, 1f, 0f, 0f,
                                        0f, 0f, 1f, 0f,
                                        0.185947001f, 1.89182305f, 0f, 1f)
                                (parent === rootNode.children[0].children[1]) shouldBe true
                                numChildren shouldBe 0

                                numMeshes shouldBe 1
                                meshes[0] shouldBe 1
                                metaData shouldBe null
                            }

                            with(children[1]) {
                                name shouldBe "Box003"
                                transformation shouldBe Mat4(
                                        1f, 5.235988e-08f, 0f, 0f,
                                        -5.235988e-08f, 1f, 0f, 0f,
                                        0f, 0f, 1f, 0f,
                                        0f, 0f, 0f, 1f)
                                (parent === rootNode.children[0].children[1]) shouldBe true
                                numChildren shouldBe 2

                                // TODO continue?

                                numMeshes shouldBe 0
                                meshes.isEmpty() shouldBe true
                                metaData shouldBe null
                            }

                            numMeshes shouldBe 0
                            meshes.isEmpty() shouldBe true
                            metaData shouldBe null
                        }

                        numMeshes shouldBe 0
                        meshes.isEmpty() shouldBe true
                        metaData shouldBe null
                    }
                }

                numMeshes shouldBe 64

                with(meshes[0]) {

                    primitiveTypes shouldBe 8
                    numVertices shouldBe 24
                    numFaces shouldBe 6

                    vertices[0] shouldBe Vec3(-0.5f, -0.5f, 0f)
                    vertices[11] shouldBe Vec3(-0.5f, -0.5f, 1f)
                    vertices[23] shouldBe Vec3(-0.5f, 0.5f, 1f)

                    normals[0] shouldBe Vec3(0f, 0f, -1f)
                    normals[11] shouldBe Vec3(0f, -1f, 0f)
                    normals[23] shouldBe Vec3(-1f, 0f, 0f)

                    textureCoords[0][0][0] shouldBe 1f
                    textureCoords[0][0][1] shouldBe 0f
                    textureCoords[0][11][0] shouldBe 0f
                    textureCoords[0][11][1] shouldBe 1f
                    textureCoords[0][23][0] shouldBe 0f
                    textureCoords[0][23][1] shouldBe 1f

                    for (i in 0..23 step 4) faces[i / 4] shouldBe mutableListOf(i, i + 1, i + 2, i + 3)

                    name shouldBe "Box001Mesh"
                }

                // for further test, follow this issue, https://github.com/assimp/assimp/issues/1561
            }
        }
    }
}