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
                        it.mNumIndices shouldBe 4
                        it.mIndices shouldBe mutableListOf(i++, i++, i++, i++)
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
    }
}