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

object testCubeRotateScaleTranslate {

	operator fun invoke(fileName: String) {

		val epsilon = 0.001f
		Importer().testFile(getResource(fileName)) {

			flags shouldBe 0

			with(rootNode) {

				name shouldBe "<BlenderRoot>"   // TODO should this be the filename instead??
				transformation shouldBe Mat4()
				numChildren shouldBe 12

				numMeshes shouldBe 0
				numLights shouldBe 0
				numCameras shouldBe 0
				numTextures shouldBe 0
				numAnimations shouldBe 0
			}

			/*numMeshes shouldBe 1
			with(meshes[0]) {
				primitiveTypes shouldBe AiPrimitiveType.POLYGON.i
				numVertices shouldBe 24
				numFaces shouldBe 6

				vertices[0] shouldBe Vec3(-0.5, +0.5, +0.5)
				vertices[5] shouldBe Vec3(+0.5, -0.5, -0.5)
				vertices[10] shouldBe Vec3(+0.5, -0.5, -0.5)
				vertices[15] shouldBe Vec3(-0.5, +0.5, +0.5)
				vertices[20] shouldBe Vec3(+0.5, -0.5, -0.5)
				vertices[23] shouldBe Vec3(+0.5, -0.5, +0.5)

				var i = 0
				faces.forEach {
					it.size shouldBe 4
					it shouldBe mutableListOf(i++, i++, i++, i++)
				}
			}
			*/
			numMaterials shouldBe 1
			with(materials[0]) {
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

}