package assimp

import glm_.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import java.util.*

/**
 * Created by elect on 18/12/2016.
 */

class ply : io.kotlintest.specs.StringSpec() {

    val ply = models + "/PLY/"

    init {

        val cube = "cube.ply"
        cube {

            with(Importer().readFile(ply + cube)!!) {

                numMeshes shouldBe 1
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

        val wuson = "Wuson.ply"
        wuson {

            with(Importer().readFile(ply + wuson)!!) {

                numMeshes shouldBe 1
                with(mMeshes[0]) {
                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 11_196
                    mNumFaces shouldBe 3732

                    mVertices[0] shouldBe Vec3(0.163313001f, 0.540615022f, -0.268687993f)
                    mVertices[3_000] shouldBe Vec3(0.0515949987f, 0.703427017f, 0.678973973f)
                    mVertices[6_000] shouldBe Vec3(-0.163461000f, 1.03988898f, -1.39270401f)
                    mVertices[9_000] shouldBe Vec3(-0.348796010f, 1.29999999e-05f, -0.704774022f)
                    mVertices[11_195] shouldBe Vec3(-0.338613003f, 1.06906497f, -1.14677405f)

                    mNormals[0] shouldBe Vec3(0.241918996f, -0.961129010f, 0.133063003f)
                    mNormals[3_000] shouldBe Vec3(-0.876681030f, -0.469485015f, 0.104950003f)
                    mNormals[6_000] shouldBe Vec3(-0.705595016f, 0.0227199998f, -0.708250999f)
                    mNormals[9_000] shouldBe Vec3(-0.997608006f, 0.0688209981f, -0.00645099999f)
                    mNormals[11_195] shouldBe Vec3(0.0657600015f, -0.889011025f, 0.453139007f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.681180000f, 0.275678009f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][3_000], floatArrayOf(0.646326005f, 0.366248012f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][6_000], floatArrayOf(0.454632014f, 0.379783005f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][9_000], floatArrayOf(0.587324023f, 0.309864014f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][11_195], floatArrayOf(0.470634013f, 0.297468990f)) shouldBe true

                    mFaces[0] shouldBe mutableListOf(0, 1, 2)
                    mFaces[1_000] shouldBe mutableListOf(3_000, 3_001, 3_002)
                    mFaces[2_000] shouldBe mutableListOf(6_000, 6_001, 6_002)
                    mFaces[3_000] shouldBe mutableListOf(9_000, 9_001, 9_002)
                    mFaces[3_731] shouldBe mutableListOf(11_193, 11_194, 11_195)
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

        val pond0 = "pond.0.ply"
        pond0 {

            with(Importer().readFile(ply + pond0)!!) {

                numMeshes shouldBe 1
                with(mMeshes[0]) {
                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 70_050
                    mNumFaces shouldBe 23_350

//                    mVertices[0] shouldBe Vec3(1.17750001f, 0.875235021f, -1.23556006f)
//                    mVertices[35_000] shouldBe Vec3(1.17750001f, 0.875235021f, -1.23556006f)
//                    mVertices[70_000] shouldBe Vec3(1.17750001f, 0.875235021f, -1.23556006f)
//                    mVertices[105_000] shouldBe Vec3(0.000000000f, y=0.000000000 z=0.000000000 })
//                    mVertices[11_195] shouldBe Vec3(-0.338613003f, 1.06906497f, -1.14677405f)
//
//                    mNormals[0] shouldBe Vec3(0.241918996f, -0.961129010f, 0.133063003f)
//                    mNormals[3_000] shouldBe Vec3(-0.876681030f, -0.469485015f, 0.104950003f)
//                    mNormals[6_000] shouldBe Vec3(-0.705595016f, 0.0227199998f, -0.708250999f)
//                    mNormals[9_000] shouldBe Vec3(-0.997608006f, 0.0688209981f, -0.00645099999f)
//                    mNormals[11_195] shouldBe Vec3(0.0657600015f, -0.889011025f, 0.453139007f)
//
//                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.681180000f, 0.275678009f)) shouldBe true
//                    Arrays.equals(mTextureCoords[0][3_000], floatArrayOf(0.646326005f, 0.366248012f)) shouldBe true
//                    Arrays.equals(mTextureCoords[0][6_000], floatArrayOf(0.454632014f, 0.379783005f)) shouldBe true
//                    Arrays.equals(mTextureCoords[0][9_000], floatArrayOf(0.587324023f, 0.309864014f)) shouldBe true
//                    Arrays.equals(mTextureCoords[0][11_195], floatArrayOf(0.470634013f, 0.297468990f)) shouldBe true
//
//                    mFaces[0] shouldBe mutableListOf(0, 1, 2)
//                    mFaces[1_000] shouldBe mutableListOf(3_000, 3_001, 3_002)
//                    mFaces[2_000] shouldBe mutableListOf(6_000, 6_001, 6_002)
//                    mFaces[3_000] shouldBe mutableListOf(9_000, 9_001, 9_002)
//                    mFaces[3_731] shouldBe mutableListOf(11_193, 11_194, 11_195)
                }
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