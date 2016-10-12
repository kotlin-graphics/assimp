/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assimp.processes;

import assimp.components.AiBone;
import assimp.components.AiFace;
import assimp.components.AiMesh;
import static assimp.components.AiMesh.AI_MAX_BONE_WEIGHTS;
import static assimp.components.AiMesh.AI_MAX_FACES;
import static assimp.components.AiMesh.AI_MAX_FACE_INDICES;
import static assimp.components.AiMesh.AI_MAX_NUMBER_OF_COLOR_SETS;
import static assimp.components.AiMesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;
import static assimp.components.AiMesh.AI_MAX_VERTICES;
import assimp.components.AiNode;
import static assimp.components.AiPrimitiveType.*;
import assimp.components.AiScene;
import static assimp.components.AiScene.AI_SCENE_FLAGS_NON_VERBOSE_FORMAT;
import static assimp.importing.AiPostProcessSteps.aiProcess_ValidateDataStructure;
import java.util.HashSet;

/**
 *
 * @author gbarbieri
 */
public class ValidateDSProcess extends BaseProcess {

    private static AiScene mScene;

    public ValidateDSProcess() {
        mScene = new AiScene();
    }

    // ------------------------------------------------------------------------------------------------
    // Returns whether the processing step is present in the given flag field.
    @Override
    public boolean isActive(int pFlags) {

        return (pFlags & aiProcess_ValidateDataStructure.value) != 0;
    }

    private <T> void doValidation(T[] pArray, int size, String firstName, String secondName) {

        // validate all entries
        if (size > 0) {

            if (pArray == null) {

                throw new Error("aiScene." + firstName + " is NULL (aiScene." + secondName + " is " + size + ")");
            }
            for (int i = 0; i < size; i++) {

                if (pArray[i] == null) {

                    throw new Error("aiScene." + firstName + "[" + i + "] is NULL (aiScene." + secondName + " is " + size + ")");
                }
//                switch(pArray[i] instanceof) {
//                    case  AiMesh.class:
//                }
                if (pArray[i] instanceof AiMesh) {
                    validate((AiMesh) pArray[i]);
                }
            }
        }
    }

    @Override
    public void execute(AiScene pScene) {

        mScene = pScene;

        // validate the node graph of the scene
        validate(pScene.mRootNode);

        // validate all meshes
        if (pScene.mNumMeshes > 0) {
            doValidation(pScene.mMeshes, pScene.mNumMeshes, "mMeshes", "mNumMeshes");
        }
    }

    private void validate(AiNode pNode) {

        if (pNode == null) {
            throw new Error("A node of the scenegraph is NULL");
        }
        if (pNode != mScene.mRootNode && pNode.mParent == null) {
            throw new Error("A node has no valid parent (aiNode.mParent is NULL)");
        }

        if (pNode.mNumMeshes > 0) {

            if (pNode.mMeshes == null) {
                throw new Error("aiNode.mMeshes is NULL (aiNode.mNumMeshes is " + pNode.mNumMeshes + ")");
            }
            HashSet<Integer> abHadMesh = new HashSet<>();
            for (int i = 0; i < pNode.mNumMeshes; i++) {

                if (pNode.mMeshes[i] >= mScene.mNumMeshes) {
                    throw new Error("aiNode::mMeshes[" + pNode.mMeshes[i] + "] is out of range (maximum is " + (mScene.mNumMeshes - 1) + ")");
                }

                if (!abHadMesh.add(pNode.mMeshes[i])) {
                    throw new Error("aiNode::mMeshes[" + i + "] is already referenced by this node (value: " + pNode.mMeshes[i] + ")");
                }
            }
        }
        if (pNode.mNumChildren > 0) {

            if (pNode.mChildren == null) {
                throw new Error("aiNode.mChildren is NULL (aiNode.mNumChildren is " + pNode.mNumChildren + ")");
            }

            for (int i = 0; i < pNode.mNumChildren; i++) {
                validate(pNode.mChildren[i]);
            }
        }
    }

    private void validate(AiMesh pMesh) {

        // validate the material index of the mesh
        if (mScene.mNumMaterials > 0 && pMesh.mMaterialIndex >= mScene.mNumMaterials) {

            throw new Error("aiMesh.mMaterialIndex is invalid (value: " + pMesh.mMaterialIndex + " maximum: " + (mScene.mNumMaterials - 1) + ")");
        }

        for (int i = 0; i < pMesh.mNumFaces; i++) {

            AiFace face = pMesh.mFaces[i];

            if (pMesh.mPrimitiveTypes > 0) {

                switch (face.mNumIndices) {

                    case 0:
                        throw new Error("aiMesh.mFaces[" + i + "].mNumIndices is 0");

                    case 1:
                        if (0 == (pMesh.mPrimitiveTypes & aiPrimitiveType_POINT.value)) {
                            throw new Error("aiMesh.mFaces[" + i + "] is a POINT but aiMesh.mPrimtiveTypes "
                                    + "does not report the POINT flag");
                        }
                        break;
                    case 2:
                        if (0 == (pMesh.mPrimitiveTypes & aiPrimitiveType_LINE.value)) {
                            throw new Error("aiMesh.mFaces[" + i + "] is a LINE but aiMesh.mPrimtiveTypes "
                                    + "does not report the LINE flag");
                        }
                        break;
                    case 3:
                        if (0 == (pMesh.mPrimitiveTypes & aiPrimitiveType_TRIANGLE.value)) {
                            throw new Error("aiMesh.mFaces[" + i + "] is a TRIANGLE but aiMesh.mPrimtiveTypes "
                                    + "does not report the TRIANGLE flag");
                        }
                        break;
                    default:
                        if (0 == (pMesh.mPrimitiveTypes & aiPrimitiveType_POLYGON.value)) {
                            throw new Error("aiMesh.mFaces[" + i + "] is a POLYGON but aiMesh.mPrimtiveTypes "
                                    + "does not report the POLYGON flag");
                        }
                        break;
                }
            }
            if (face.mIndices == null) {
                throw new Error("aiMesh.mFaces[" + i + "].mIndices is NULL");
            }
        }

        // positions must always be there ...
        if (pMesh.mNumVertices == 0 || (pMesh.mVertices == null && mScene.mFlags == 0)) {
            throw new Error("The mesh contains no vertices");
        }

        if (pMesh.mNumVertices > AI_MAX_VERTICES) {
            throw new Error("Mesh has too many vertices: " + pMesh.mNumVertices + ", but the limit is " + AI_MAX_VERTICES);
        }
        if (pMesh.mNumFaces > AI_MAX_FACES) {
            throw new Error("Mesh has too many faces: " + pMesh.mNumFaces + ", but the limit is " + AI_MAX_FACES);
        }

        // if tangents are there there must also be bitangent vectors ...
        if ((pMesh.mTangents != null) != (pMesh.mBitangents != null)) {
            throw new Error("If there are tangents, bitangent vectors must be present as well");
        }

        // faces, too
        if (pMesh.mNumFaces == 0 || (pMesh.mFaces == null && mScene.mFlags == 0)) {
            throw new Error("Mesh contains no faces");
        }

        // now check whether the face indexing layout is correct:
        // unique vertices, pseudo-indexed.
        boolean[] abRefList = new boolean[pMesh.mNumVertices];

        for (int i = 0; i < pMesh.mNumFaces; i++) {

            AiFace face = pMesh.mFaces[i];
            if (face.mNumIndices > AI_MAX_FACE_INDICES) {
                throw new Error("Face " + i + " has too many faces: " + face.mNumIndices + ", but the limit is " + AI_MAX_FACE_INDICES);
            }

            for (int a = 0; a < face.mNumIndices; a++) {

                if (face.mIndices[a] >= pMesh.mNumVertices) {
                    throw new Error("aiMesh.mFaces[" + i + "].mIndices[" + a + "] is out of range");
                }
                // the MSB flag is temporarily used by the extra verbose
                // mode to tell us that the JoinVerticesProcess might have
                // been executed already.
                if ((mScene.mFlags & AI_SCENE_FLAGS_NON_VERBOSE_FORMAT) == 0 && abRefList[face.mIndices[a]]) {

                    throw new Error("aiMesh.mVertices[" + face.mIndices[a] + "] is referenced twice - second "
                            + "time by aiMesh.mFaces[" + i + "].mIndices[" + a + "]");
                }
                abRefList[face.mIndices[a]] = true;
            }
        }

        // check whether there are vertices that aren't referenced by a face
        boolean b = false;
        for (int i = 0; i < pMesh.mNumVertices; i++) {
            if (!abRefList[i]) {
                b = true;
            }
        }

        if (b) {
            System.out.println("There are unreferenced vertices");
        }

        // texture channel 2 may not be set if channel 1 is zero ...
        {
            int i = 0;
            for (; i < AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
                if (!pMesh.hasTextureCoords(i)) {
                    break;
                }
            }
            for (; i < AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
                if (pMesh.hasTextureCoords(i)) {
                    throw new Error("Texture coordinate channel " + i + " exists although the previous channel was NULL.");
                }
            }
        }
        // the same for the vertex colors
        {
            int i = 0;
            for (; i < AI_MAX_NUMBER_OF_COLOR_SETS; ++i) {
                if (!pMesh.hasVertexColors(i)) {
                    break;
                }
            }
            for (; i < AI_MAX_NUMBER_OF_COLOR_SETS; ++i) {
                if (pMesh.hasVertexColors(i)) {
                    throw new Error("Vertex color channel " + i + " is exists although the previous channel was NULL.");
                }
            }
        }

        // now validate all bones
        if (pMesh.mNumBones > 0) {

            if (pMesh.mBones == null) {

                throw new Error("aiMesh.mBones is NULL (aiMesh.mNumBones is " + pMesh.mNumBones);
            }
            float[] afSum = null;
            if (pMesh.mNumVertices > 0) {

                afSum = new float[pMesh.mNumVertices];
            }

            // check whether there are duplicate bone names
            for (int i = 0; i < pMesh.mNumBones; i++) {

                AiBone bone = pMesh.mBones[i];
                if (bone.mNumWeights > AI_MAX_BONE_WEIGHTS) {
                    throw new Error("Bone " + i + " has too many weights: " + bone.mNumWeights + ", but the limit is " + AI_MAX_BONE_WEIGHTS);
                }

                if (pMesh.mBones[i] == null) {
                    throw new Error("aiMesh.mBones[" + i + "] is NULL (aiMesh.mNumBones is " + pMesh.mNumBones + ")");
                }
                validate(pMesh, pMesh.mBones[i], afSum);

                for (int a = i + 1; a < pMesh.mNumBones; a++) {

                    if (pMesh.mBones[i].mName.equals(pMesh.mBones[a].mName)) {
                        throw new Error("aiMesh.mBones[" + i + "] has the same name as aiMesh.mBones[" + a + "]");
                    }
                }
            }
            // check whether all bone weights for a vertex sum to 1.0 ...
            for (int i = 0; i < pMesh.mNumVertices; i++) {

                if (afSum[i] > 0 && (afSum[i] <= 0.94 || afSum[i] >= 1.05)) {
                    System.out.println("aiMesh.mVertices[" + i + "]: bone weight sum != 1.0 (sum is " + afSum[i] + ")");
                }
            }
        } else if (pMesh.mBones != null) {

            throw new Error("aiMesh.mBones is non-null although there are no bones");
        }
    }

    private void validate(AiMesh pMesh, AiBone pBone, float[] afSum) {

        if (pBone.mNumWeights == 0) {
            throw new Error("aiBone::mNumWeights is zero");
        }

        // check whether all vertices affected by this bone are valid
        for (int i = 0; i < pBone.mNumWeights; i++) {

            if (pBone.mWeights[i].mVertexId >= pMesh.mNumVertices) {
                throw new Error("aiBone.mWeights[" + i + "].mVertexId is out of range");
            } else if (pBone.mWeights[i].mWeight == 0 || pBone.mWeights[i].mWeight > 1.0f) {
                System.out.println("WARNING aiBone.mWeights[" + i + "].mWeight has an invalid value");
            }
            afSum[pBone.mWeights[i].mVertexId] += pBone.mWeights[i].mWeight;
        }
    }
}
