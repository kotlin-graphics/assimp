package assimp

import assimp.*
import glm.mat4x4.Mat4
import glm.vec3.Vec3
import io.kotlintest.matchers.shouldBe
import java.util.*

/**
 * Created by elect on 16/11/2016.
 */

class obj : io.kotlintest.specs.StringSpec() {

    val obj = models + "/OBJ/"

    init {

        val spider = "spider.obj"

        spider {

            with(Importer().readFile((obj + spider).URI)!!) {

                with(mRootNode) {

                    mName shouldBe "spider.obj"
                    mTransformation shouldBe Mat4()
                    mNumChildren shouldBe 19

                    val names = listOf("HLeib01", "OK", "Bein1Li", "Bein1Re", "Bein2Li", "Bein2Re", "Bein3Re", "Bein3Li", "Bein4Re",
                            "Bein4Li", "Zahn", "klZahn", "Kopf", "Brust", "Kopf2", "Zahn2", "klZahn2", "Auge", "Duplicate05")

                    (0 until mNumChildren).map {
                        mChildren[it].mName shouldBe names[it]
                        mChildren[it].mMeshes!![0] shouldBe it
                    }

                    mNumMeshes shouldBe 0
                }
                mNumMeshes shouldBe 19

                with(mMeshes[0]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 240
                    mNumFaces shouldBe 80

                    mVertices[0] shouldBe Vec3(x = 1.160379, y = 4.512684, z = 6.449167)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = -4.421391, y = -3.605049, z = -20.462471)

                    mNormals[0] shouldBe Vec3(-0.537588000, -0.0717979968, 0.840146005)
                    mNormals[mNumVertices - 1] shouldBe Vec3(-0.728103995, -0.400941998, -0.555975974)

                    // TODO check for kotlintest 2.0 array check
                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.186192f, 0.222718f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.103881f, 0.697021f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[mNumFaces - 1] shouldBe listOf(237, 238, 239)

                    mMaterialIndex shouldBe 3

                    mName shouldBe "HLeib01"
                }
                with(mMeshes[1]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 180
                    mNumFaces shouldBe 60

                    mVertices[0] shouldBe Vec3(x = -41.8566132f, y = -0.754845977f, z = 9.43077183f)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = -49.7138367f, y = -2.98359, z = -21.4211159f)

                    mNormals[0] shouldBe Vec3(x = -0.236278996f, y = 0.0291850008f, z = 0.971247017f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = -0.862017989f, y = 0.0830229968f, z = -0.500032008f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(-0.0658710003f, -0.410016000f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(-0.318565995f, 1.05051804f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[mNumFaces - 1] shouldBe listOf(177, 178, 179)

                    mMaterialIndex shouldBe 1

                    mName shouldBe "OK"
                }
                with(mMeshes[18]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 114
                    mNumFaces shouldBe 38

                    mVertices[0] shouldBe Vec3(x = -59.4670486f, y = 18.1400757f, z = -17.1943588)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = -62.2673569f, y = 15.2776031f, z = -14.7453232f)

                    mNormals[0] shouldBe Vec3(x = 0.0751359984f, y = 0.741809011f, z = -0.666388988f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = -0.776385009f, y = -0.629855990f, z = 0.0225169994f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.899282992f, 0.970311999f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.372330993f, 0.198948994f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[mNumFaces - 1] shouldBe listOf(111, 112, 113)

                    mMaterialIndex shouldBe 5

                    mName shouldBe "Duplicate05"
                }

                with(mMaterials[0]) {

                    name shouldBe "DefaultMaterial"
                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0)
                        diffuse shouldBe Vec3(0.600000024f)
                        specular shouldBe Vec3(0)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                }
                with(mMaterials[1]) {

                    name shouldBe "Skin"
                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f)
                        diffuse shouldBe Vec3(0.827450991f, 0.792156994f, 0.772548974f)
                        specular shouldBe Vec3(0)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\wal67ar_small.jpg"
                }
                with(mMaterials[2]) {

                    name shouldBe "Brusttex"
                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f)
                        diffuse shouldBe Vec3(0.800000012f)
                        specular shouldBe Vec3(0)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\wal69ar_small.jpg"
                }
                with(mMaterials[3]) {

                    name shouldBe "HLeibTex"
                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f)
                        diffuse shouldBe Vec3(0.690195978f, 0.639216006f, 0.615685999f)
                        specular shouldBe Vec3(0)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\SpiderTex.jpg"
                }
                with(mMaterials[4]) {

                    name shouldBe "BeinTex"
                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f)
                        diffuse shouldBe Vec3(0.800000012f)
                        specular shouldBe Vec3(0)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\drkwood2.jpg"
                }
                with(mMaterials[5]) {

                    name shouldBe "Augentex"
                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {
                        ambient shouldBe Vec3(0.200000003f)
                        diffuse shouldBe Vec3(0.800000012f)
                        specular shouldBe Vec3(0)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                    textures[0].file shouldBe ".\\engineflare1.jpg"
                }
            }
        }

        val box = "box.obj"

        box {

            with(Importer().readFile((obj + box).URI)!!) {

                with(mRootNode) {

                    mName shouldBe "box.obj"
                    mTransformation shouldBe Mat4()
                    mNumChildren shouldBe 1

                    with(mChildren[0]) {

                        mName shouldBe "1"
                        mTransformation shouldBe Mat4()
                        mNumChildren shouldBe 0
                        mNumMeshes shouldBe 1
                        mMeshes!![0] shouldBe 0
                    }
                    mNumMeshes shouldBe 0
                }
                mNumMeshes shouldBe 1
                with(mMeshes[0]) {
                    mPrimitiveTypes shouldBe AiPrimitiveType.POLYGON.i
                    mNumVertices shouldBe 24
                    mNumFaces shouldBe 6

                    mVertices[0] shouldBe Vec3(-0.5, +0.5, +0.5)
                    mVertices[5] shouldBe Vec3(+0.5, -0.5, -0.5)
                    mVertices[10] shouldBe Vec3(+0.5, -0.5, -0.5)
                    mVertices[15] shouldBe Vec3(-0.5, +0.5, +0.5)
                    mVertices[20] shouldBe Vec3(+0.5, -0.5, -0.5)
                    mVertices[23] shouldBe Vec3(+0.5, -0.5, +0.5)

                    var i = 0
                    mFaces.forEach {
                        it.size shouldBe 4
                        it shouldBe mutableListOf(i++, i++, i++, i++)
                    }
                }
                with(mMaterials[0]) {
                    name shouldBe AI_DEFAULT_MATERIAL_NAME
                    shadingModel shouldBe AiShadingMode.gouraud
                    with(color!!) {
                        ambient shouldBe Vec3()
                        diffuse shouldBe Vec3(0.6)
                        specular shouldBe Vec3()
                        emissive shouldBe Vec3()
                        shininess shouldBe 0f
                        opacity shouldBe 1f
                        refracti shouldBe 1f
                    }
                }
            }
        }

        val concavePolygon = "concave_polygon.obj"

        concavePolygon {

            with(Importer().readFile((obj + concavePolygon).URI)!!) {

                with(mRootNode) {

                    mName shouldBe "concave_polygon.obj"
                    mTransformation shouldBe Mat4()
                    mNumChildren shouldBe 2

                    with(mChildren[0]) {

                        mName shouldBe "concave_test.obj"
                        mTransformation shouldBe Mat4()
                        mParent === mRootNode
                        mNumChildren shouldBe 0
                        mNumMeshes shouldBe 0
                    }
                    with(mChildren[1]) {

                        mName shouldBe "default"
                        mTransformation shouldBe Mat4()
                        mParent === mRootNode
                        mNumChildren shouldBe 0
                        mNumMeshes shouldBe 1
                        mMeshes!![0] shouldBe 0
                    }
                }
                with(mMeshes[0]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.POLYGON.i
                    mNumVertices shouldBe 66
                    mNumFaces shouldBe 1

                    mVertices[0] shouldBe Vec3(-1.14600003, 2.25515008, 3.07623005)
                    mVertices[10] shouldBe Vec3(-1.14600003, 1.78262997, 1.93549001)
                    mVertices[20] shouldBe Vec3(-1.14600003, 3.01736999, 1.93549001)
                    mVertices[30] shouldBe Vec3(-1.14600003, 2.54485, 3.07623005)
                    mVertices[40] shouldBe Vec3(-1.14600003, 3.08750010, 2.34999990)
                    mVertices[50] shouldBe Vec3(-1.14600003, 2.13690996, 1.71483)
                    mVertices[60] shouldBe Vec3(-1.14600003, 1.91386, 2.83613992)
                    mVertices[65] shouldBe Vec3(-1.14600003, 2.40000010, 3.0905)

                    mNormals.forEach { it shouldBe Vec3(1, 0, -0.0) }
                    var i = 0
                    mFaces[0].forEach { it shouldBe i++ }

                    mMaterialIndex shouldBe 1

                    mName shouldBe "default"
                }
                mNumMaterials shouldBe 2

                with(mMaterials[0]) {

                    name shouldBe "DefaultMaterial"

                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {

                        ambient!! shouldBe Vec3(0)
                        diffuse!! shouldBe Vec3(0.600000024)
                        specular!! shouldBe Vec3(0)
                        emissive!! shouldBe Vec3(0)
                        shininess!! shouldBe 0f
                        opacity!! shouldBe 1f
                        refracti!! shouldBe 1f
                    }
                }

                with(mMaterials[1]) {

                    name shouldBe "test"

                    shadingModel shouldBe AiShadingMode.gouraud

                    with(color!!) {

                        ambient!! shouldBe Vec3(0)
                        diffuse!! shouldBe Vec3(0.141176000, 0.184313998, 0.411765009)
                        specular!! shouldBe Vec3(0)
                        emissive!! shouldBe Vec3(0)
                        shininess!! shouldBe 1_600f
                        opacity!! shouldBe 1f
                        refracti!! shouldBe 1f
                    }
                }
            }
        }
    }
}