package assimp.obj

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import java.util.*

object nanosuit {

    operator fun invoke(fileName: String) {
        with(Importer().readFile(getResource(fileName))!!) {

            with(rootNode) {

                name shouldBe "nanosuit.obj"
                transformation shouldBe Mat4()
                numChildren shouldBe 7

                val names = listOf("Visor", "Legs", "hands", "Lights", "Arms", "Helmet", "Body")

                (0 until numChildren).map {
                    children[it].name shouldBe names[it]
                    children[it].meshes[0] shouldBe it
                }

                numMeshes shouldBe 0
            }

            with(meshes[0]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 156
                numFaces shouldBe 52

                vertices[0] shouldBe Vec3(x = 0.320383996f, y = 14.0575409f, z = 0.507779002)
                vertices[77] shouldBe Vec3(x = 0.00970300008f, y = 14.3252335f, z = 0.651058972)
                vertices[numVertices - 1] shouldBe Vec3(x = -0.300857008f, y = 14.0575409f, z = 0.507779002f)

                normals[0] shouldBe Vec3(x = 0.496899992f, y = -0.240799993f, z = 0.833700001)
                normals[77] shouldBe Vec3(x = 0.108800001f, y = -0.159199998f, z = 0.981199980f)
                normals[numVertices - 1] shouldBe Vec3(x = -0.496399999f, y = -0.241899997f, z = 0.833700001f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.439940989f, 0.453613013f)) shouldBe true
                Arrays.equals(textureCoords[0][77], floatArrayOf(0.0595699996f, 0.754760981f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.439940989f, 0.453613013f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[25] shouldBe listOf(75, 76, 77)
                faces[numFaces - 1] shouldBe listOf(153, 154, 155)

                materialIndex shouldBe 3

                name shouldBe "Visor"
            }

            with(meshes[1]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 15222
                numFaces shouldBe 5074

                vertices[0] shouldBe Vec3(x = 1.899165f, y = 2.31763411f, z = -0.120600000f)
                vertices[7610] shouldBe Vec3(x = -0.425206006f, y = 6.15252495f, z = 0.327194005f)
                vertices[numVertices - 1] shouldBe Vec3(x = 0.602433026f, y = 7.82357216f, z = -1.3782050f)

                normals[0] shouldBe Vec3(x = -0.710300028f, y = 0.137300000f, z = 0.690400004)
                normals[7610] shouldBe Vec3(x = 0.849799991f, y = -0.326099992f, z = 0.414099991f)
                normals[numVertices - 1] shouldBe Vec3(x = 0.0302000009f, y = -0.153899997f, z = -0.987600029f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.283690989f, 0.568114996f)) shouldBe true
                Arrays.equals(textureCoords[0][7610], floatArrayOf(1.9658200f, 0.282715f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.373046994f, 0.152344003f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[2536] shouldBe listOf(7608, 7609, 7610)
                faces[numFaces - 1] shouldBe listOf(15219, 15220, 15221)

                materialIndex shouldBe 6

                name shouldBe "Legs"
            }

            with(meshes[2]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 19350
                numFaces shouldBe 6450

                vertices[0] shouldBe Vec3(x = -3.55469608f, y = 8.31433487f, z = 1.45077002)
                vertices[9675] shouldBe Vec3(x = 3.56796789f, y = 7.70999908f, z = 1.54873502f)
                vertices[numVertices - 1] shouldBe Vec3(x = 3.46042705f, y = 7.47475290f, z = 1.01681304f)

                normals[0] shouldBe Vec3(x = -0.408199996f, y = -0.825800002f, z = -0.389200002f)
                normals[9675] shouldBe Vec3(x = 0.930800021f, y = -0.305799991f, z = 0.200299993f)
                normals[numVertices - 1] shouldBe Vec3(x = 0.365900010f, y = 0.658500016f, z = -0.657599986f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.948729992f, 0.610351980f)) shouldBe true
                Arrays.equals(textureCoords[0][9675], floatArrayOf(0.949218988f, 0.799315989f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.321045011f, 0.451660007f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[2536] shouldBe listOf(7608, 7609, 7610)
                faces[numFaces - 1] shouldBe listOf(19347, 19348, 19349)

                materialIndex shouldBe 4

                name shouldBe "hands"
            }

            with(meshes[3]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 78
                numFaces shouldBe 26

                vertices[0] shouldBe Vec3(x = 0.519383013f, y = 14.0981007f, z = 0.399747014f)
                vertices[numVertices - 1] shouldBe Vec3(x = -0.454726994f, y = 14.0737648f, z = 0.435452998f)

                normals[0] shouldBe Vec3(x = 0.663699985f, y = -0.158099994f, z = 0.731100023)
                normals[numVertices - 1] shouldBe Vec3(x = -0.663699985f, y = -0.158099994f, z = 0.731100023f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.470214993f, 0.796020985f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.148314998f, 0.648193002f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(75, 76, 77)

                materialIndex shouldBe 3

                name shouldBe "Lights"
            }

            with(meshes[4]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 6804
                numFaces shouldBe 2268

                vertices[0] shouldBe Vec3(x = 3.65801501f, y = 9.13363171f, z = 1.10836804f)
                vertices[numVertices - 1] shouldBe Vec3(x = 3.06237602f, y = 10.2043982f, z = -0.735961020f)

                normals[0] shouldBe Vec3(x = -0.949199975f, y = -0.0269000009f, z = -0.313499987f)
                normals[numVertices - 1] shouldBe Vec3(x = 0.365000010f, y = -0.602299988f, z = -0.709900022f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.451171994f, 0.0678709969f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.772948980f, 0.980515003f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(6801, 6802, 6803)

                materialIndex shouldBe 1

                name shouldBe "Arms"
            }

            with(meshes[5]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 7248
                numFaces shouldBe 2416

                vertices[0] shouldBe Vec3(x = 0.169514000f, y = 13.7492905f, z = 0.552182019f)
                vertices[numVertices - 1] shouldBe Vec3(x = 0.564511001f, y = 14.9660711f, z = 0.226483002)

                normals[0] shouldBe Vec3(x = 0.738699973f, y = -0.110500000f, z = 0.664900005f)
                normals[numVertices - 1] shouldBe Vec3(x = 0.956200004f, y = -0.109999999f, z = 0.271400005f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.153198004f, 0.172362998f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.491210997f, 0.944916010f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(7245, 7246, 7247)

                materialIndex shouldBe 5

                name shouldBe "Helmet"
            }

            with(meshes[6]) {

                primitiveTypes shouldBe AiPrimitiveType.TRIANGLE.i
                numVertices shouldBe 8316
                numFaces shouldBe 2772

                vertices[0] shouldBe Vec3(x = 1.41331196f, y = 11.7699909f, z = -1.45877004f)
                vertices[numVertices - 1] shouldBe Vec3(x = -0.137869999f, y = 13.8385210f, z = -1.14200103f)

                normals[0] shouldBe Vec3(x = 0.990599990f, y = 0.0780000016f, z = -0.112599999f)
                normals[numVertices - 1] shouldBe Vec3(x = 0.610899985f, y = 0.394199997f, z = -0.686600029f)

                Arrays.equals(textureCoords[0][0], floatArrayOf(0.819335997f, 0.985771000f)) shouldBe true
                Arrays.equals(textureCoords[0][numVertices - 1], floatArrayOf(0.0746460035f, 0.579346001f)) shouldBe true

                textureCoords[0][0].size shouldBe 2

                faces[0] shouldBe listOf(0, 1, 2)
                faces[numFaces - 1] shouldBe listOf(8313, 8314, 8315)

                materialIndex shouldBe 2

                name shouldBe "Body"
            }

            with(materials[0]) {

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

            with(materials[1]) {

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

            with(materials[2]) {

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

            with(materials[3]) {

                name shouldBe "Glass"
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
                    file shouldBe "glass_dif.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.ambient }!!) {
                    file shouldBe "glass_refl.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.height }!!) {
                    file shouldBe "glass_ddn.png"
                    uvwsrc shouldBe 0
                }
            }

            with(materials[4]) {

                name shouldBe "Hand"
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
                    file shouldBe "hand_dif.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.ambient }!!) {
                    file shouldBe "hand_showroom_refl.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.specular }!!) {
                    file shouldBe "hand_showroom_spec.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.height }!!) {
                    file shouldBe "hand_showroom_ddn.png"
                    uvwsrc shouldBe 0
                }
            }

            with(materials[5]) {

                name shouldBe "Helmet"
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
                    file shouldBe "helmet_diff.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.ambient }!!) {
                    file shouldBe "helmet_showroom_refl.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.specular }!!) {
                    file shouldBe "helmet_showroom_spec.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.height }!!) {
                    file shouldBe "helmet_showroom_ddn.png"
                    uvwsrc shouldBe 0
                }
            }

            with(materials[6]) {

                name shouldBe "Leg"
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
                    file shouldBe "leg_dif.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.ambient }!!) {
                    file shouldBe "leg_showroom_refl.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.specular }!!) {
                    file shouldBe "leg_showroom_spec.png"
                    uvwsrc shouldBe 0
                }
                with(textures.find { it.type == AiTexture.Type.height }!!) {
                    file shouldBe "leg_showroom_ddn.png"
                    uvwsrc shouldBe 0
                }
            }
        }
    }
}