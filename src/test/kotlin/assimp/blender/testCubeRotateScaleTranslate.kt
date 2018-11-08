package assimp.blender

import assimp.*
import glm_.*
import glm_.mat4x4.*
import glm_.vec3.*
import io.kotlintest.*
import kotlin.math.*

// TODO refactor to glm
val Float.inRadians: Float get() = Math.toRadians(this.d).f
val Float.inDegrees: Float get() = Math.toDegrees(this.d).f

// TODO refactor to glm-test project (to be created)
infix fun Mat4.plusOrMinus(epsilon: Float): Matcher<Mat4> = object : Matcher<Mat4>{
	override fun test(other: Mat4): Result {
		val expected = this@plusOrMinus

		val diff = other.array.zip(expected.array) { a, b -> abs(a - b) }
		val passed = diff.all { it < epsilon }

		val diffMat = Mat4(diff.map { if(it < epsilon) 0f else it })
		return Result(passed,
		              "Matrices are not equal with tolerance of $epsilon!\nexpected:$expected\nbut was: $other\nwith difference of:$diffMat",
		              "Matrices should not be equal with tolerance of $epsilon")
	}

}

// TODO tests:
//      column vs row major
//      negated x/y/z coordinates?
//      flipped axis (I think blender uses different y/z compared to game graphics (is this switched by assimp??)
//      rotation
//      scale

// TODO also test linked meshes

private data class TransformDesciption(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f,
                                       val rX: Float = 0f, val rY: Float = 0f, val rZ: Float = 0f,
                                       val sX: Float = 1f, val sY: Float = 1f, val sZ: Float = 1f)
private val cubeDescriptions = mapOf(
		"CubeX1" to TransformDesciption(x = 1f),
		"CubeY1" to TransformDesciption(y = 1f),
		"CubeZ1" to TransformDesciption(z = 1f),
		"CubeXN1" to TransformDesciption(x = -1f),
		"CubeYN1" to TransformDesciption(y = -1f),
		"CubeZN1" to TransformDesciption(z = -1f),
		"CubeRX45" to TransformDesciption(rX = 45f.inRadians),
		"CubeRY45" to TransformDesciption(rY = 45f.inRadians),
		"CubeRZ45" to TransformDesciption(rZ = 45f.inRadians),
		"CubeSX2" to TransformDesciption(sX = 2f),
		"CubeSY2" to TransformDesciption(sY = 2f),
		"CubeSZ2" to TransformDesciption(sZ = 2f)
                                     )

object testCubeRotateScaleTranslate {

	private fun AiNode.check(des: TransformDesciption) {

		transformation shouldBe (translation(Vec3(des.x, des.y, des.z))
				.rotateXYZ(des.rX, des.rY, des.rZ)
				.scale(des.sX, des.sY, des.sZ)
				plusOrMinus epsilon)

		numChildren shouldBe 0
		numMeshes shouldBe  1
	}

	operator fun invoke(fileName: String) {

		Importer().testFile(getResource(fileName)) {

			flags shouldBe 0

			with(rootNode) {

				name shouldBe "<BlenderRoot>"   // TODO should this be the filename instead??
				transformation shouldBe Mat4()
				numChildren shouldBe 12

				cubeDescriptions.forEach { (name, description) ->
					children.first { it.name == name }.check(description)
				}

				numMeshes shouldBe 0
				numLights shouldBe 0
				numCameras shouldBe 0
				numTextures shouldBe 0
				numAnimations shouldBe 0
			}

			numMeshes shouldBe 12
			repeat(12) { meshInd ->
				with(meshes[meshInd]) {
					primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
					numVertices shouldBe 24
					numFaces shouldBe 6

					vertices[0] shouldBe Vec3(-0.5, -0.5, +0.5)
					vertices[5] shouldBe Vec3(+0.5, +0.5, +0.5)
					vertices[10] shouldBe Vec3(+0.5, -0.5, -0.5)
					vertices[15] shouldBe Vec3(+0.5, -0.5, -0.5)
					vertices[20] shouldBe Vec3(+0.5, -0.5, +0.5)
					vertices[23] shouldBe Vec3(-0.5, -0.5, +0.5)

					var i = 0
					faces.forEach {
						it.size shouldBe 4
						it shouldBe mutableListOf(i++, i++, i++, i++)
					}
				}
			}

			numMaterials shouldBe 1
			with(materials[0]) {
				name shouldBe AI_DEFAULT_MATERIAL_NAME
				// shadingModel shouldBe AiShadingMode.gouraud  TODO ???
				with(color!!) {
					ambient shouldBe Vec3()
					diffuse shouldBe Vec3(0.6)
					specular shouldBe Vec3(0.6)
					emissive shouldBe null
					shininess shouldBe null
					opacity shouldBe null
					refracti shouldBe null
				}
			}
		}
	}

}