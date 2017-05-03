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

        val nanosuit = "nanosuit/nanosuit.obj"

        nanosuit {

            with(Importer().readFile((obj + nanosuit).URI)!!) {

                with(mRootNode) {

                    mName shouldBe "nanosuit.obj"
                    mTransformation shouldBe Mat4()
                    mNumChildren shouldBe 7

                    val names = listOf("Visor", "Legs", "hands", "Lights", "Arms", "Helmet", "Body")

                    (0 until mNumChildren).map {
                        mChildren[it].mName shouldBe names[it]
                        mChildren[it].mMeshes!![0] shouldBe it
                    }

                    mNumMeshes shouldBe 0
                }

                with(mMeshes[0]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 156
                    mNumFaces shouldBe 52

                    mVertices[0] shouldBe Vec3(x = 0.320383996f, y = 14.0575409f, z = 0.507779002)
                    mVertices[77] shouldBe Vec3(x = 0.00970300008f, y = 14.3252335f, z = 0.651058972)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = -0.300857008f, y = 14.0575409f, z = 0.507779002f)

                    mNormals[0] shouldBe Vec3(x = 0.496899992f, y = -0.240799993f, z = 0.833700001)
                    mNormals[77] shouldBe Vec3(x = 0.108800001f, y = -0.159199998f, z = 0.981199980f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = -0.496399999f, y = -0.241899997f, z = 0.833700001f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.439940989f, 0.453613013f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][77], floatArrayOf(0.0595699996f, 0.754760981f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.439940989f, 0.453613013f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[25] shouldBe listOf(75, 76, 77)
                    mFaces[mNumFaces - 1] shouldBe listOf(153, 154, 155)

                    mMaterialIndex shouldBe 3

                    mName shouldBe "Visor"
                }

                with(mMeshes[1]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 15222
                    mNumFaces shouldBe 5074

                    mVertices[0] shouldBe Vec3(x = 1.899165f, y = 2.31763411f, z = -0.120600000f)
                    mVertices[7610] shouldBe Vec3(x = -0.425206006f, y = 6.15252495f, z = 0.327194005f)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = 0.602433026f, y = 7.82357216f, z = -1.3782050f)

                    mNormals[0] shouldBe Vec3(x = -0.710300028f, y = 0.137300000f, z = 0.690400004)
                    mNormals[7610] shouldBe Vec3(x = 0.849799991f, y = -0.326099992f, z = 0.414099991f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = 0.0302000009f, y = -0.153899997f, z = -0.987600029f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.283690989f, 0.568114996f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][7610], floatArrayOf(1.9658200f, 0.282715f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.373046994f, 0.152344003f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[2536] shouldBe listOf(7608, 7609, 7610)
                    mFaces[mNumFaces - 1] shouldBe listOf(15219, 15220, 15221)

                    mMaterialIndex shouldBe 6

                    mName shouldBe "Legs"
                }

                with(mMeshes[2]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 19350
                    mNumFaces shouldBe 6450

                    mVertices[0] shouldBe Vec3(x = -3.55469608f, y = 8.31433487f, z = 1.45077002)
                    mVertices[9675] shouldBe Vec3(x = 3.56796789f, y = 7.70999908f, z = 1.54873502f)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = 3.46042705f, y = 7.47475290f, z = 1.01681304f)

                    mNormals[0] shouldBe Vec3(x = -0.408199996f, y = -0.825800002f, z = -0.389200002f)
                    mNormals[9675] shouldBe Vec3(x = 0.930800021f, y = -0.305799991f, z = 0.200299993f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = 0.365900010f, y = 0.658500016f, z = -0.657599986f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.948729992f, 0.610351980f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][9675], floatArrayOf(0.949218988f, 0.799315989f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.321045011f, 0.451660007f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[2536] shouldBe listOf(7608, 7609, 7610)
                    mFaces[mNumFaces - 1] shouldBe listOf(19347, 19348, 19349)

                    mMaterialIndex shouldBe 4

                    mName shouldBe "hands"
                }

                with(mMeshes[3]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 78
                    mNumFaces shouldBe 26

                    mVertices[0] shouldBe Vec3(x = 0.519383013f, y = 14.0981007f, z = 0.399747014f)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = -0.454726994f, y = 14.0737648f, z = 0.435452998f)

                    mNormals[0] shouldBe Vec3(x = 0.663699985f, y = -0.158099994f, z = 0.731100023)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = -0.663699985f, y = -0.158099994f, z = 0.731100023f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.470214993f, 0.796020985f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.148314998f, 0.648193002f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[mNumFaces - 1] shouldBe listOf(75, 76, 77)

                    mMaterialIndex shouldBe 3

                    mName shouldBe "Lights"
                }

                with(mMeshes[4]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 6804
                    mNumFaces shouldBe 2268

                    mVertices[0] shouldBe Vec3(x = 3.65801501f, y = 9.13363171f, z = 1.10836804f)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = 3.06237602f, y = 10.2043982f, z = -0.735961020f)

                    mNormals[0] shouldBe Vec3(x = -0.949199975f, y = -0.0269000009f, z = -0.313499987f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = 0.365000010f, y = -0.602299988f, z = -0.709900022f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.451171994f, 0.0678709969f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.772948980f, 0.980515003f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[mNumFaces - 1] shouldBe listOf(6801, 6802, 6803)

                    mMaterialIndex shouldBe 1

                    mName shouldBe "Arms"
                }

                with(mMeshes[5]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 7248
                    mNumFaces shouldBe 2416

                    mVertices[0] shouldBe Vec3(x = 0.169514000f, y = 13.7492905f, z = 0.552182019f)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = 0.564511001f, y = 14.9660711f, z = 0.226483002)

                    mNormals[0] shouldBe Vec3(x = 0.738699973f, y = -0.110500000f, z = 0.664900005f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = 0.956200004f, y = -0.109999999f, z = 0.271400005f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.153198004f, 0.172362998f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.491210997f, 0.944916010f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[mNumFaces - 1] shouldBe listOf(7245, 7246, 7247)

                    mMaterialIndex shouldBe 5

                    mName shouldBe "Helmet"
                }

                with(mMeshes[6]) {

                    mPrimitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                    mNumVertices shouldBe 8316
                    mNumFaces shouldBe 2772

                    mVertices[0] shouldBe Vec3(x = 1.41331196f, y = 11.7699909f, z = -1.45877004f)
                    mVertices[mNumVertices - 1] shouldBe Vec3(x = -0.137869999f, y = 13.8385210f, z = -1.14200103f)

                    mNormals[0] shouldBe Vec3(x = 0.990599990f, y = 0.0780000016f, z = -0.112599999f)
                    mNormals[mNumVertices - 1] shouldBe Vec3(x = 0.610899985f, y = 0.394199997f, z = -0.686600029f)

                    Arrays.equals(mTextureCoords[0][0], floatArrayOf(0.819335997f, 0.985771000f)) shouldBe true
                    Arrays.equals(mTextureCoords[0][mNumVertices - 1], floatArrayOf(0.0746460035f, 0.579346001f)) shouldBe true

                    mTextureCoords[0][0].size shouldBe 2

                    mFaces[0] shouldBe listOf(0, 1, 2)
                    mFaces[mNumFaces - 1] shouldBe listOf(8313, 8314, 8315)

                    mMaterialIndex shouldBe 2

                    mName shouldBe "Body"
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
                        transparent shouldBe Vec3(1f)   // TODO add transparent to other tests
                        refracti shouldBe 1f
                    }
                }

                with(mMaterials[1]) {

                    name shouldBe "Arm"
                    shadingModel shouldBe AiShadingMode.phong

                    with(color!!) {
                        ambient shouldBe Vec3(0)
                        diffuse shouldBe Vec3(0.639999986f)
                        specular shouldBe Vec3(0.5f)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 96.0784302f
                        opacity shouldBe 1f
                        transparent shouldBe Vec3(1f)
                        refracti shouldBe 1f
                    }

                    with(textures.find { it.type == AiTexture.Type.diffuse }!!) {
                        file shouldBe "arm_dif.png"
                        uvwsrc shouldBe 0
                    }
                    with(textures.find { it.type == AiTexture.Type.ambient }!!) {
                        file shouldBe "arm_showroom_refl.png"
                        uvwsrc shouldBe 0
                    }
                    with(textures.find { it.type == AiTexture.Type.specular }!!) {
                        file shouldBe "arm_showroom_spec.png"
                        uvwsrc shouldBe 0
                    }
                    with(textures.find { it.type == AiTexture.Type.height }!!) {
                        file shouldBe "arm_showroom_ddn.png"
                        uvwsrc shouldBe 0
                    }
                }

                with(mMaterials[2]) {

                    name shouldBe "Body"
                    shadingModel shouldBe AiShadingMode.phong

                    with(color!!) {
                        ambient shouldBe Vec3(0)
                        diffuse shouldBe Vec3(0.639999986f)
                        specular shouldBe Vec3(0.5f)
                        emissive shouldBe Vec3(0)
                        shininess shouldBe 96.0784302f
                        opacity shouldBe 1f
                        transparent shouldBe Vec3(1f)
                        refracti shouldBe 1f
                    }

                    with(textures.find { it.type == AiTexture.Type.diffuse }!!) {
                        file shouldBe "body_dif.png"
                        uvwsrc shouldBe 0
                    }
                    with(textures.find { it.type == AiTexture.Type.ambient }!!) {
                        file shouldBe "body_showroom_refl.png"
                        uvwsrc shouldBe 0
                    }
                    with(textures.find { it.type == AiTexture.Type.specular }!!) {
                        file shouldBe "body_showroom_spec.png"
                        uvwsrc shouldBe 0
                    }
                    with(textures.find { it.type == AiTexture.Type.height }!!) {
                        file shouldBe "body_showroom_ddn.png"
                        uvwsrc shouldBe 0
                    }
                }
            }
        }

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
                        shininess!! shouldBe 400f
                        opacity!! shouldBe 1f
                        refracti!! shouldBe 1f
                    }
                }
            }
        }
    }
}