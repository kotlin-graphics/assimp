package assimp.postProcess

import assimp.AiFace
import assimp.AiMesh
import assimp.AiScene
import glm_.vec3.Vec3
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions

fun main() {
    println(0 % 300)
    println(1 % 300)
    println(12 % 300)
    println(30 % 300)
    println(299 % 300)
    println(300 % 300)
    println(301 % 300)
    println(312 % 300)
}

class JointVerticesTest : StringSpec() {

    init {
        "JointVertices Test1" {
            val scene = setUp()

            // construct the process
            val piProcess = JoinVertices()
            piProcess.execute(scene)
            //to do
                // execute the step on the given data
                    piProcess.processMesh(pcMesh, 0)       //ProcessMesh needs to be implemented

                // the number of faces shouldn't change

            Assertions.assertEquals(300U, pcMesh.mNumFaces)
            Assertions.assertEquals(300U, pcMesh.mNumVertices)
            Assertions.assertTrue(pcMesh.mNormals != null)
            Assertions.assertTrue(pcMesh.mTangents != null)
            Assertions.assertTrue(pcMesh.mBitTangents != null)
            Assertions.assertTrue(pcMesh.mTextureCoords[0] != null)
                //better to make these nullable in pcMesh?
            /*
                //native:
                ASSERT_EQ(300U, pcMesh->mNumFaces);
                ASSERT_EQ(300U, pcMesh->mNumVertices);
                ASSERT_TRUE(NULL != pcMesh->mNormals);
                ASSERT_TRUE(NULL != pcMesh->mTangents);
                ASSERT_TRUE(NULL != pcMesh->mBitangents);
                ASSERT_TRUE(NULL != pcMesh->mTextureCoords[0]);

            */

                // the order doesn't care

            var fSum = 0F
            for (i in 0..300) {
                fSum += pcMesh.mVertices[i].x + pcMesh.mVertices[i].y + pcMesh.mVertices[i].z

                                                            //these are floats and not boolean, so why does native expect false?
                check(!pcMesh.mNormals[i].x) { "mNormals is false" }
                check(!pcMesh.mTangents[i].x) { "mNormals is false" }
                check(!pcMesh.mBitangents[i].x) { "mNormals is false" }
                check(!pcMesh.mTextureCoords[0][i].x) { "mNormals is false" }
            }
            check(150F*299F*3F == fSum) { "gaussian sum equation" }
            /*
                float fSum = 0.f;
                for (unsigned int i = 0; i < 300;++i)
                {
                    aiVector3D& v = pcMesh->mVertices[i];
                    fSum += v.x + v.y + v.z;
to do
                    EXPECT_FALSE(pcMesh->mNormals[i].x);
                    EXPECT_FALSE(pcMesh->mTangents[i].x);
                    EXPECT_FALSE(pcMesh->mBitangents[i].x);
                    EXPECT_FALSE(pcMesh->mTextureCoords[0][i].x);
                }
                EXPECT_EQ(150.f*299.f*3.f, fSum); // gaussian sum equation
            */

        }
    }

    fun setUp(): AiScene {
        val scene = AiScene()

        // create a quite small mesh for testing purposes -
        // the mesh itself is *something* but it has redundant vertices
        val pcMesh = AiMesh()
        scene.meshes.add(pcMesh)
        //testing mesh has 900 vertices
        pcMesh.numVertices = 900

        //aiVector3D is missing - in native it's in vector3.h
        //.mVertices for AiMesh is missing in mesh.h's

        pcMesh.vertices = MutableList(pcMesh.numVertices) {
            val a = it % 300
            Vec3(a, a, a)
        }

        // generate faces - each vertex is referenced once
        pcMesh.numFaces = 300        //aiVector3D is missing - in native it's in vector3.h
        //.mVertices for AiMesh is missing in mesh.h's
        pcMesh.faces = MutableList(300) {
            mutableListOf(0, 1, 2)
        }

        // generate extra members - set them to zero to make sure they're identical
        pcMesh.textureCoords.add(mutableListOf())
        for (i in 0 until 900)
            pcMesh.textureCoords[0].add(floatArrayOf(0f, 0f))

        for (i in 0 until 900)
            pcMesh.normals.add(Vec3())

        for (i in 0 until 900)
            pcMesh.tangents.add(Vec3())

        for (i in 0 until 900)
            pcMesh.bitangents.add(Vec3())

        return scene

    }
}