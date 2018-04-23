package assimp.md3

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import io.kotlintest.shouldBe

object europeanFnt {

    operator fun invoke(fileName: String) {

        with(Importer().readFile(getResource(fileName))!!) {

            flags shouldBe 0

            with(rootNode) {
                name shouldBe "<MD3Root>"
                transformation shouldBe Mat4(
                        1f, 0f, 0f, 0f,
                        0f, 0f, -1f, 0f,
                        0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 1f)
                parent shouldBe null
                numChildren shouldBe 0
                children.isEmpty() shouldBe true

                numMeshes shouldBe 5
                meshes[0] shouldBe 0
            }

            with(meshes[0]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 336
                numFaces shouldBe 112

                vertices[0] shouldBe Vec3(84.7812500f, 39.1875000f, 27.0468750f)
                vertices[167] shouldBe Vec3(-63.3906250f, -40.9218750f, 13.2656250f)
                vertices[335] shouldBe Vec3(-66.7500000f, 38.8437500f, 15.7343750f)

                normals[0] shouldBe Vec3(0.5348365f, -0.013129499f, -0.8448536f)
                normals[167] shouldBe Vec3(-1.6606013E-16f, -0.9039893f, -0.42755508f)
                normals[335] shouldBe Vec3(-0.9423873f, 0.31132272f, -0.12241068f)

                textureCoords[0][0][0] shouldBe 0.0498498604f
                textureCoords[0][0][1] shouldBe 0.616048455f
                textureCoords[0][167][0] shouldBe 0.0527787320f
                textureCoords[0][167][1] shouldBe 0.612726867f
                textureCoords[0][335][0] shouldBe 0.0801433548f
                textureCoords[0][335][1] shouldBe 0.583810389f

                for (i in 0..111 * 3 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 1, i + 2)

                name.isEmpty() shouldBe true
            }
            with(meshes[1]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 528
                numFaces shouldBe 176

                vertices[0] shouldBe Vec3(76.5937500f, 21.5312500f, 16.8750000f)
                vertices[263] shouldBe Vec3(-39.5312500f, -38.6250000f, 34.0468750f)
                vertices[527] shouldBe Vec3(50.3281250f, -37.1718750f, 16.9843750f)

                normals[0] shouldBe Vec3(0.5348365f, -0.8445991f, 0.024541229f)
                normals[263] shouldBe Vec3(0.18502302f, -0.30869228f, 0.9329928f)
                normals[527] shouldBe Vec3(-0.5139479f, -0.8574703f, 0.024541229f)

                textureCoords[0][0][0] shouldBe 0.0963503048f
                textureCoords[0][0][1] shouldBe 0.951211274f
                textureCoords[0][263][0] shouldBe 0.948739946f
                textureCoords[0][263][1] shouldBe 0.898761094f
                textureCoords[0][527][0] shouldBe 0.948739946f
                textureCoords[0][527][1] shouldBe 0.755133688f

                for (i in 0..175 * 3 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 2, i + 1)

                name.isEmpty() shouldBe true
            }
            with(meshes[2]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 1050
                numFaces shouldBe 350

                vertices[0] shouldBe Vec3(65.5312500f, 12.1093750f, 54.4531250f)
                vertices[524] shouldBe Vec3(87.4843750f, -36.6250000f, 40.0625000f)
                vertices[1049] shouldBe Vec3(-79.0781250f, -33.5781250f, 29.2343750f)

                normals[0] shouldBe Vec3(-0.31792322f, 0.31792322f, 0.8932243f)
                normals[524] shouldBe Vec3(0.89080405f, -0.44839308f, 0.07356457f)
                normals[1049] shouldBe Vec3(-0.794514f, -0.58925176f, 0.14673047f)

                textureCoords[0][0][0] shouldBe 0.387118787f
                textureCoords[0][0][1] shouldBe 0.285972953f
                textureCoords[0][524][0] shouldBe 0.908088386f
                textureCoords[0][524][1] shouldBe 0.706875503f
                textureCoords[0][1049][0] shouldBe 0.923744082f
                textureCoords[0][1049][1] shouldBe 0.848480046f

                for (i in 0..349 * 3 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 2, i + 1)

                name.isEmpty() shouldBe true
            }
            with(meshes[3]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 114
                numFaces shouldBe 38

                vertices[0] shouldBe Vec3(29.1406250f, 12.8437500f, 43.5156250f)
                vertices[56] shouldBe Vec3(33.1093750f, 12.8125000f, 50.9531250f)
                vertices[113] shouldBe Vec3(29.1406250f, 12.8437500f, 43.5156250f)

                normals[0] shouldBe Vec3(-0.9984946f, -0.024511667f, 0.049067676f)
                normals[56] shouldBe Vec3(-0.47125477f, 0.011568655f, -0.8819213f)
                normals[113] shouldBe Vec3(-0.9984946f, -0.024511667f, 0.049067676f)

                textureCoords[0][0][0] shouldBe 0.125758305f
                textureCoords[0][0][1] shouldBe 0.741084099f
                textureCoords[0][56][0] shouldBe 0.250173688f
                textureCoords[0][56][1] shouldBe 0.923741937f
                textureCoords[0][113][0] shouldBe 0.0367676802f
                textureCoords[0][113][1] shouldBe 0.726932108f

                for (i in 0..37 * 3 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 1, i + 2)

                name.isEmpty() shouldBe true
            }
            with(meshes[4]) {

                primitiveTypes shouldBe 4
                numVertices shouldBe 6
                numFaces shouldBe 2

                vertices[0] shouldBe Vec3(45.9218750f, 23.6718750f, 53.6406250f)
                vertices[2] shouldBe Vec3(36.2812500f, 23.6718750f, 73.0781250f)
                vertices[5] shouldBe Vec3(45.9375000f, -23.5156250f, 53.5312500f)

                normals[0] shouldBe Vec3(0.8932243f, 0.0f, 0.44961134f)
                normals[2] shouldBe Vec3(0.8932243f, 0.0f, 0.44961134f)
                normals[5] shouldBe Vec3(0.8932243f, 0.0f, 0.44961134f)

                textureCoords[0][0][0] shouldBe 0.999992669f
                textureCoords[0][0][1] shouldBe 0.00579905510f
                textureCoords[0][2][0] shouldBe 1.00000000f
                textureCoords[0][2][1] shouldBe 0.999242306f
                textureCoords[0][5][0] shouldBe 0f
                textureCoords[0][5][1] shouldBe 0f

                for (i in 0..1 * 3 step 3) faces[i / 3] shouldBe mutableListOf(i, i + 2, i + 1)

                name.isEmpty() shouldBe true
            }
            numMaterials shouldBe 5

            with(materials[0]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3(0.0500000007)
                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3(1f)
                }
                name shouldBe "MD3_[default][windscreen]"
                textures[0].file shouldBe "textures/sfx/glass.tga.tga"
                textures[0].flags shouldBe AiTexture.Flags.ignoreAlpha.i
            }
            with(materials[1]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3(0.0500000007)
                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3(1f)
                }
                name shouldBe "MD3_[default][steering]"
                twoSided shouldBe true
                blendFunc shouldBe AiBlendMode.default
                textures[0].file shouldBe "euro_frnt_2.tga"
                textures[0].flags shouldBe AiTexture.Flags.useAlpha.i
            }
            with(materials[2]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3(0.0500000007)
                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3(1f)
                }
                name shouldBe "MD3_[default][body]"
                textures[0].file shouldBe "european_fnt.tga"
                textures[0].flags shouldBe AiTexture.Flags.ignoreAlpha.i
            }
            with(materials[3]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3(0.0500000007)
                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3(1f)
                }
                name shouldBe "MD3_[default][wheels]"
                textures[0].file shouldBe "european_fnt.tga"
                textures[0].flags shouldBe AiTexture.Flags.ignoreAlpha.i
            }
            with(materials[4]) {
                shadingModel shouldBe AiShadingMode.gouraud
                with(color!!) {
                    ambient shouldBe Vec3(0.0500000007)
                    diffuse shouldBe Vec3(1f)
                    specular shouldBe Vec3(1f)
                }
                name shouldBe "MD3_[default][wheel_arches]"
                twoSided shouldBe true
                blendFunc shouldBe AiBlendMode.default
                textures[0].file shouldBe "euro_frnt_2.tga"
                textures[0].flags shouldBe AiTexture.Flags.useAlpha.i
            }
        }
    }
}