package main

import io.kotlintest.specs.StringSpec
import main.mat.Mat4
import main.vec._3.Vec3
import org.junit.Test

/**
 * Created by elect on 18/11/2016.
 */
class stl : StringSpec() {

    init {

        "testTriangle.stl" {
            val scene = Importer().readFile("test/resources/models/STL/triangle.stl")!!

            scene.mFlags shouldBe 0

            with(scene) {

                with(mRootNode) {

                    mName shouldBe "testTriangle"
                    mTransformation shouldBe Mat4()
                    mParent shouldBe null
                    mNumChildren shouldBe 0
//                mChildren shouldBe null
                    mNumMeshes shouldBe 1
                    mMeshes!![0] shouldBe 0
                    mMetaData shouldBe null
                }
                mNumMeshes shouldBe 1
                mMeshes.size shouldBe 1

                with(mMeshes[0]) {

                    mPrimitiveTypes shouldBe 4

                    mNumVertices shouldBe 3
                    mNumFaces shouldBe 1

                    mVertices!![0] shouldBe Vec3(1, 1, 0)
                    mNormals!![0] shouldBe Vec3(0, 0, 1)

                    mTangents shouldBe null
                    mBitangents shouldBe null

                    mColors.size shouldBe AI_MAX_NUMBER_OF_COLOR_SETS
                    mColors.all { it == null } shouldBe true

                    mTextureCoords.size shouldBe AI_MAX_NUMBER_OF_TEXTURECOORDS
                    mTextureCoords.all { it == null } shouldBe true

                    mNumUVComponents.size shouldBe AI_MAX_NUMBER_OF_TEXTURECOORDS
                    mNumUVComponents.all { it == 0 } shouldBe true

                    mFaces.size shouldBe 1
                    mFaces[0].mNumIndices shouldBe 3
                    var p = 0
                    mFaces[0].mIndices.all { it == p++ } shouldBe true

                    mNumBones shouldBe 0

                    mName shouldBe ""

                    mNumAnimMeshes shouldBe 0
                }

            }
        }
    }
}