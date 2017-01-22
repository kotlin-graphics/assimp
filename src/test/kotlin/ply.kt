import io.kotlintest.specs.StringSpec
import mat.Mat4
import vec._3.Vec3

/**
 * Created by elect on 18/12/2016.
 */

class ply : StringSpec() {

    val ply = models + "/PLY/"

    init {

        val cube = "cube.ply"
        cube {

            with(Importer().readFile((ply + cube).URI)!!) {

                mNumMeshes shouldBe 1
                with(mMeshes[0]) {
                    mPrimitiveTypes shouldBe AiPrimitiveType.POLYGON.i
                    mNumVertices shouldBe 24
                    mNumFaces shouldBe 6

                    mVertices[0] shouldBe Vec3(0f)
                    mVertices[5] shouldBe Vec3(1f)
                    mVertices[10] shouldBe Vec3(1f, 0f, 1f)
                    mVertices[15] shouldBe Vec3(0f, 1f, 1f)
                    mVertices[20] shouldBe Vec3(0f, 1f, 0f)
                    mVertices[23] shouldBe Vec3(0f)

                    mFaces[0] shouldBe mutableListOf(0, 1, 2, 3)
                    mFaces[1] shouldBe mutableListOf(4, 5, 6, 7)
                    mFaces[2] shouldBe mutableListOf(8, 9, 10, 11)
                    mFaces[3] shouldBe mutableListOf(12, 13, 14, 15)
                    mFaces[4] shouldBe mutableListOf(16, 17, 18, 19)
                    mFaces[5] shouldBe mutableListOf(20, 21, 22, 23)
                }
                with(mMaterials[0]) {
                    shadingModel shouldBe AiShadingMode.gouraud
                    with(color!!) {
                        ambient shouldBe Vec3(.05f)
                        diffuse shouldBe Vec3(.6f)
                        specular shouldBe Vec3(.6f)
                    }
                    twoSided shouldBe true
                }
            }
        }

//        val wuson = "Wuson.ply"
//        cube {
//
//            with(Importer().readFile((ply + wuson).URI)!!) {
//
//                mNumMeshes shouldBe 1
//                with(mMeshes[0]) {
//                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
//                    mNumVertices shouldBe 11_196
//                    mNumFaces shouldBe 3732
//
//                    mVertices[0] shouldBe Vec3(0.163313001f, 0.540615022f, -0.268687993f)
//                    mVertices[1_000] shouldBe Vec3(-1.70000003e-05f, 0.522634983f, -1.37093496f)
//                    mVertices[2_000] shouldBe Vec3(0.246845007f, 1.10175800f, -1.26043105f)
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
//            }
//        }
    }
}