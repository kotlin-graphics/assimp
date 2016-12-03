package main

import io.kotlintest.specs.StringSpec
import main.Importer
import main.mat.Mat4
import main.vec._3.Vec3
import org.junit.Test

/**
 * Created by elect on 16/11/2016.
 */

class obj : StringSpec() {

    init {

        "box.obj" {

            with(Importer().readFile("test/resources/models/OBJ/box.obj")!!) {

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
                    mPrimitiveTypes shouldBe 8
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

        "concave_polygon.obj" {

            with(Importer().readFile("test/resources/models/OBJ/concave_polygon.obj")!!) {

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

                    mNormals.forEach { it shouldBe Vec3(1, 0, -0.0)}
                    var i = 0
                    mFaces[0].forEach { it shouldBe i++}

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