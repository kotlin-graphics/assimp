package assimp.blender

import assimp.*
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import glm_.test.*
import io.kotlintest.*

/**
 * Created by elect on 16/11/2016.
 */

object blenderDefault_250_compressed {

    operator fun invoke(fileName: String) {

        Importer().testFile(getResource(fileName)) {

            flags shouldBe 0

            with(rootNode) {

                name shouldBe "<BlenderRoot>"   // TODO should this be the filename instead??
                transformation shouldBe Mat4()
                numChildren shouldBe 3

                with(children.first { it.name == "Camera" }){

                    transformation shouldBe (generateTrans(7.48113f, -6.50764f, 5.34367f,
                                                           77.185f.inRadians, 0.772f, 0.341f, 0.536f) plusOrMinus epsilon)

                    numChildren shouldBe 0
                    numMeshes shouldBe  0
                }
                with(children.first { it.name == "Lamp" }){

	                val expectedTrans = generateTrans(4.07625f, 1.00545f, 5.90386f,
	                                                  1.92627f, 0.206f, 0.332f, 0.921f)
	                transformation shouldBe (expectedTrans plusOrMinus epsilon)
                    numChildren shouldBe 0
                    numMeshes shouldBe 0
                }
                with(children.first { it.name == "Cube" }){

                    transformation shouldBe Mat4()
                    numChildren shouldBe 0
                    numMeshes shouldBe 1

                    meshes[0] shouldBe 0
                }

                numMeshes shouldBe 0

            }

            numMeshes shouldBe 1
            with(meshes[0]) {
                primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
                numVertices shouldBe 24
                numFaces shouldBe 6

	            vertices[0] shouldBe (Vec3(+1.0, +1.0, -1.0) plusOrMinus epsilon)
	            vertices[5] shouldBe (Vec3(-1.0, +1.0, +1.0) plusOrMinus epsilon)
	            vertices[10] shouldBe (Vec3(+1.0, -1.0, +1.0) plusOrMinus epsilon)
	            vertices[15] shouldBe (Vec3(-1.0, -1.0, -1.0) plusOrMinus epsilon)
	            vertices[20] shouldBe (Vec3(+1.0, +1.0, +1.0) plusOrMinus epsilon)
	            vertices[23] shouldBe (Vec3(-1.0, +1.0, +1.0) plusOrMinus epsilon)

                var i = 0
                faces.forEach {
                    it.size shouldBe 4
                    it shouldBe mutableListOf(i++, i++, i++, i++)
                }
            }

	        numMaterials shouldBe 1
            with(materials[0]) {
                name shouldBe "Material"
                // shadingModel shouldBe AiShadingMode.gouraud TODO ???
                with(color!!) {
                    ambient shouldBe Vec3()
                    diffuse shouldBe (Vec3(0.604f) plusOrMinus epsilon)
                    specular shouldBe Vec3(1f)
                    emissive shouldBe null
                    shininess shouldBe 50f
                    opacity shouldBe null
                    refracti shouldBe null
                }
            }

	        numLights shouldBe 1

	        with(lights[0]){
		        type shouldBe AiLightSourceType.POINT
		        colorDiffuse shouldBe AiColor3D(1f, 1f, 1f)
		        colorSpecular shouldBe AiColor3D(1f, 1f, 1f)
		        colorDiffuse shouldBe AiColor3D(1f, 1f, 1f)
	        }

	        numCameras shouldBe 1

	        with(cameras[0]){
		        clipPlaneNear shouldBe 0.1f
		        clipPlaneFar shouldBe 100f
		        // TODO
	        }

	        numTextures shouldBe 0
	        numAnimations shouldBe 0
        }
    }


//    val concavePolygon = "concave_polygon.obj"
//
//    concavePolygon
//    {
//
//        with(Importer().readFile(obj + concavePolygon)!!) {
//
//            with(rootNode) {
//
//                name shouldBe "concave_polygon.obj"
//                transformation shouldBe Mat4()
//                numChildren shouldBe 2
//
//                with(children[0]) {
//
//                    name shouldBe "concave_test.obj"
//                    transformation shouldBe Mat4()
//                    parent === rootNode
//                    numChildren shouldBe 0
//                    numMeshes shouldBe 0
//                }
//                with(children[1]) {
//
//                    name shouldBe "default"
//                    transformation shouldBe Mat4()
//                    parent === rootNode
//                    numChildren shouldBe 0
//                    numMeshes shouldBe 1
//                    meshes[0] shouldBe 0
//                }
//            }
//            with(meshes[0]) {
//
//                primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
//                numVertices shouldBe 66
//                numFaces shouldBe 1
//
//                vertices[0] shouldBe Vec3(-1.14600003, 2.25515008, 3.07623005)
//                vertices[10] shouldBe Vec3(-1.14600003, 1.78262997, 1.93549001)
//                vertices[20] shouldBe Vec3(-1.14600003, 3.01736999, 1.93549001)
//                vertices[30] shouldBe Vec3(-1.14600003, 2.54485, 3.07623005)
//                vertices[40] shouldBe Vec3(-1.14600003, 3.08750010, 2.34999990)
//                vertices[50] shouldBe Vec3(-1.14600003, 2.13690996, 1.71483)
//                vertices[60] shouldBe Vec3(-1.14600003, 1.91386, 2.83613992)
//                vertices[65] shouldBe Vec3(-1.14600003, 2.40000010, 3.0905)
//
//                normals.forEach { it shouldBe Vec3(1, 0, -0.0) }
//                var i = 0
//                faces[0].forEach { it shouldBe i++ }
//
//                materialIndex shouldBe 1
//
//                name shouldBe "default"
//            }
//            numMaterials shouldBe 2
//
//            with(materials[0]) {
//
//                name shouldBe "DefaultMaterial"
//
//                shadingModel shouldBe AiShadingMode.gouraud
//
//                with(color!!) {
//
//                    ambient!! shouldBe Vec3(0)
//                    diffuse!! shouldBe Vec3(0.600000024)
//                    specular!! shouldBe Vec3(0)
//                    emissive!! shouldBe Vec3(0)
//                    shininess!! shouldBe 0f
//                    opacity!! shouldBe 1f
//                    refracti!! shouldBe 1f
//                }
//            }
//
//            with(materials[1]) {
//
//                name shouldBe "test"
//
//                shadingModel shouldBe AiShadingMode.gouraud
//
//                with(color!!) {
//
//                    ambient!! shouldBe Vec3(0)
//                    diffuse!! shouldBe Vec3(0.141176000, 0.184313998, 0.411765009)
//                    specular!! shouldBe Vec3(0)
//                    emissive!! shouldBe Vec3(0)
//                    shininess!! shouldBe 400f
//                    opacity!! shouldBe 1f
//                    refracti!! shouldBe 1f
//                }
//            }
//        }
//    }
}