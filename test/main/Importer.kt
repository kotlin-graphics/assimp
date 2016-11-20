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

                    val v = mVertices
                    if (v != null) {
                        v[0] shouldBe Vec3(1, 1, 0)
                        v[1] shouldBe Vec3(-1, 1, 0)
                        v[2] shouldBe Vec3(0, -1, 0)
                    }
                    val n = mNormals
                    if (n != null) {
                        n[0] shouldBe Vec3(0, 0, 1)
                        n[1] shouldBe Vec3(0, 0, 1)
                        n[2] shouldBe Vec3(0, 0, 1)
                    }
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

                mNumMaterials shouldBe 1

                with(mMaterials[0]) {

                    with(color!!) {

                        diffuse shouldBe Vec3(0.6)
                        specular shouldBe Vec3(0.6)
                        ambient shouldBe Vec3(0.05)
                    }
                }
            }
        }

        "sphereWithHole.stl" {
            val scene = Importer().readFile("test/resources/models/STL/sphereWithHole.stl")!!

            scene.mFlags shouldBe 0

            with(scene) {

                with(mRootNode) {

                    mName shouldBe "tinysphere_withhole"
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

                    mNumVertices shouldBe 855

                    val v = mVertices
                    if (v != null) {
                        v[0] shouldBe Vec3(1.50000000, 1.50000000, 0.000000000)
                        v[100] shouldBe Vec3(0.439339995, 1.50000000, 0.439339995)
                        v[200] shouldBe Vec3(0.144960001, 1.97835, 1.06982)
                        v[300] shouldBe Vec3(0.379390001, 0.602360010, 1.06585002)
                        v[400] shouldBe Vec3(1.50000000, 0.000000000, 1.50000000)
                        v[500] shouldBe Vec3(0.144960001, 1.97835, 1.93018)
                        v[600] shouldBe Vec3(1.08111000, 2.70614004, 2.28725004)
                        v[700] shouldBe Vec3(1.50000000, 0.439339995, 2.56065989)
                        v[800] shouldBe Vec3(1.93018, 1.02165, 2.85504)
                        v[854] shouldBe Vec3(1.50000000, 1.88823, 2.94888997)
                    }
                    val n = mNormals
                    if (n != null) {
                        n[0] shouldBe Vec3(-0.129999995, -0.129999995, -0.980000019)
                        n[100] shouldBe Vec3(-0.689999998, 0.209999993, -0.689999998)
                        n[200] shouldBe Vec3(-0.870000005, 0.200000003, -0.449999988)
                        n[300] shouldBe Vec3(-0.660000026, -0.730000019, -0.180000007)
                        n[400] shouldBe Vec3(-0.129999995, -0.980000019, -0.129999995)
                        n[500] shouldBe Vec3(-0.920000017, 0.379999995, 0.119999997)
                        n[600] shouldBe Vec3(-0.159999996, 0.899999976, 0.419999987)
                        n[700] shouldBe Vec3(-0.180000007, -0.730000019, 0.660000026)
                        n[800] shouldBe Vec3(0.449999988, -0.200000003, 0.870000005)
                        n[854] shouldBe Vec3(0.129999995, 0.129999995, 0.980000019)
                    }

                    mTangents shouldBe null
                    mBitangents shouldBe null

                    mColors.size shouldBe AI_MAX_NUMBER_OF_COLOR_SETS
                    mColors.all { it == null } shouldBe true

                    mTextureCoords.size shouldBe AI_MAX_NUMBER_OF_TEXTURECOORDS
                    mTextureCoords.all { it == null } shouldBe true

                    mNumUVComponents.size shouldBe AI_MAX_NUMBER_OF_TEXTURECOORDS
                    mNumUVComponents.all { it == 0 } shouldBe true

                    mFaces.size shouldBe 285
                    mFaces.all { it.mNumIndices == 3 } shouldBe true
                    var p = 0
                    mFaces[0].mIndices.all { it == p++ } shouldBe true

                    mNumBones shouldBe 0

                    mName shouldBe ""

                    mNumAnimMeshes shouldBe 0
                }

                mNumMaterials shouldBe 1

                with(mMaterials[0]) {

                    with(color!!) {

                        diffuse shouldBe Vec3(0.6)
                        specular shouldBe Vec3(0.6)
                        ambient shouldBe Vec3(0.05)
                    }
                }
            }
        }

        "Spider_binary.stl" {
            val scene = Importer().readFile("test/resources/models/STL/Spider_binary.stl")!!

            scene.mFlags shouldBe 0

            with(scene) {

                with(mRootNode) {

                    mName shouldBe "<STL_BINARY>"
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

                    mNumVertices shouldBe 855

                    val v = mVertices
                    if (v != null) {
                        v[0] shouldBe Vec3(1.50000000, 1.50000000, 0.000000000)
                        v[100] shouldBe Vec3(0.439339995, 1.50000000, 0.439339995)
                        v[200] shouldBe Vec3(0.144960001, 1.97835, 1.06982)
                        v[300] shouldBe Vec3(0.379390001, 0.602360010, 1.06585002)
                        v[400] shouldBe Vec3(1.50000000, 0.000000000, 1.50000000)
                        v[500] shouldBe Vec3(0.144960001, 1.97835, 1.93018)
                        v[600] shouldBe Vec3(1.08111000, 2.70614004, 2.28725004)
                        v[700] shouldBe Vec3(1.50000000, 0.439339995, 2.56065989)
                        v[800] shouldBe Vec3(1.93018, 1.02165, 2.85504)
                        v[854] shouldBe Vec3(1.50000000, 1.88823, 2.94888997)
                    }
                    val n = mNormals
                    if (n != null) {
                        n[0] shouldBe Vec3(-0.129999995, -0.129999995, -0.980000019)
                        n[100] shouldBe Vec3(-0.689999998, 0.209999993, -0.689999998)
                        n[200] shouldBe Vec3(-0.870000005, 0.200000003, -0.449999988)
                        n[300] shouldBe Vec3(-0.660000026, -0.730000019, -0.180000007)
                        n[400] shouldBe Vec3(-0.129999995, -0.980000019, -0.129999995)
                        n[500] shouldBe Vec3(-0.920000017, 0.379999995, 0.119999997)
                        n[600] shouldBe Vec3(-0.159999996, 0.899999976, 0.419999987)
                        n[700] shouldBe Vec3(-0.180000007, -0.730000019, 0.660000026)
                        n[800] shouldBe Vec3(0.449999988, -0.200000003, 0.870000005)
                        n[854] shouldBe Vec3(0.129999995, 0.129999995, 0.980000019)
                    }

                    mTangents shouldBe null
                    mBitangents shouldBe null

                    mColors.size shouldBe AI_MAX_NUMBER_OF_COLOR_SETS
                    mColors.all { it == null } shouldBe true

                    mTextureCoords.size shouldBe AI_MAX_NUMBER_OF_TEXTURECOORDS
                    mTextureCoords.all { it == null } shouldBe true

                    mNumUVComponents.size shouldBe AI_MAX_NUMBER_OF_TEXTURECOORDS
                    mNumUVComponents.all { it == 0 } shouldBe true

                    mFaces.size shouldBe 285
                    mFaces.all { it.mNumIndices == 3 } shouldBe true
                    var p = 0
                    mFaces[0].mIndices.all { it == p++ } shouldBe true

                    mNumBones shouldBe 0

                    mName shouldBe ""

                    mNumAnimMeshes shouldBe 0
                }

                mNumMaterials shouldBe 1

                with(mMaterials[0]) {

                    with(color!!) {

                        diffuse shouldBe Vec3(0.6)
                        specular shouldBe Vec3(0.6)
                        ambient shouldBe Vec3(0.05)
                    }
                }
            }
        }
    }
}