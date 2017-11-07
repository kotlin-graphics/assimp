package assimp

import glm_.mat4x4.Mat4
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import uno.kotlin.uri

/**
 * Created by elect on 24/01/2017.
 */

class collada : StringSpec() {

    val collada = models + "/Collada/"

    init {

        val cameras = "cameras.dae"
        cameras {

            with(Importer().readFile((collada + cameras).uri)!!) {

                flags shouldBe 1
                with(rootNode) {
                    name shouldBe "Scene"
                    transformation shouldBe Mat4(
                            1f, 0f, 0f, 0f,
                            0f, 0f, 1f, 0f,
                            0f, -1f, 0f, 0f,
                            0f, 0f, 0f, 1f)
                    parent shouldBe null
                    numChildren shouldBe 3
                    with(children[0]) {
                        name shouldBe "Camera"
//                        transformation shouldBe Mat4(
//                                7.54979013e-08f, 0f, 1f, 10f,
//                                0f, 1f, 0f, 0f,
//                                -1f, 0f, 7.54979013e-08f, 0f,
//                                0f, 0f, 0f, 1f)
                    }
                }
//                numMeshes shouldBe 1
//                with(meshes[0]) {
//                    mPrimitiveTypes shouldBe AiPrimitiveType.POLYGON.i
//                    mNumVertices shouldBe 24
//                    mNumFaces shouldBe 6
//
//                    mVertices[0] shouldBe Vec3(0f)
//                    mVertices[5] shouldBe Vec3(1f)
//                    mVertices[10] shouldBe Vec3(1f, 0f, 1f)
//                    mVertices[15] shouldBe Vec3(0f, 1f, 1f)
//                    mVertices[20] shouldBe Vec3(0f, 1f, 0f)
//                    mVertices[23] shouldBe Vec3(0f)
//
//                    mFaces[0] shouldBe mutableListOf(0, 1, 2, 3)
//                    mFaces[1] shouldBe mutableListOf(4, 5, 6, 7)
//                    mFaces[2] shouldBe mutableListOf(8, 9, 10, 11)
//                    mFaces[3] shouldBe mutableListOf(12, 13, 14, 15)
//                    mFaces[4] shouldBe mutableListOf(16, 17, 18, 19)
//                    mFaces[5] shouldBe mutableListOf(20, 21, 22, 23)
//                }
//                with(mMaterials[0]) {
//                    shadingModel shouldBe AiShadingMode.gouraud
//                    with(color!!) {
//                        ambient shouldBe Vec3(.05f)
//                        diffuse shouldBe Vec3(.6f)
//                        specular shouldBe Vec3(.6f)
//                    }
//                    twoSided shouldBe true
//                }
            }
        }
    }
}