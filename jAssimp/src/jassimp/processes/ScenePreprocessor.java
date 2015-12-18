/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.processes;

import jassimp.components.AiAnimation;
import jassimp.components.AiFace;
import jassimp.components.AiMesh;
import static jassimp.components.AiMesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;
import jassimp.components.AiNodeAnim;
import static jassimp.components.AiPrimitiveType.*;
import jassimp.components.AiScene;
import jassimp.components.AiVectorKey;
import jglm.Vec3;

/**
 *
 * @author GBarbieri
 */
public class ScenePreprocessor {

    private AiScene scene;

    public ScenePreprocessor(AiScene _scene) {
        scene = _scene;
    }

    public void processScene() {

        if (scene == null) {
            throw new Error("scene == null");
        }

        // Process all meshes
        for (int i = 0; i < scene.mNumMeshes; i++) {
            processMesh(scene.mMeshes[i]);
        }

        // - nothing to do for nodes for the moment
        // - nothing to do for textures for the moment
        // - nothing to do for lights for the moment
        // - nothing to do for cameras for the moment
        // Process all animations
        for (int i = 0; i < scene.mNumAnimations; ++i) {
            processAnimation(scene.mAnimations[i]);
        }
        
//        // Generate a default material if none was specified
//    if (!scene->mNumMaterials && scene->mNumMeshes) {
//        scene->mMaterials      = new aiMaterial*[2];
//        aiMaterial* helper;
//
//        aiString name;
//
//        scene->mMaterials[scene->mNumMaterials] = helper = new aiMaterial();
//        aiColor3D clr(0.6f,0.6f,0.6f);
//        helper->AddProperty(&clr,1,AI_MATKEY_COLOR_DIFFUSE);
//
//        // setup the default name to make this material identifiable
//        name.Set(AI_DEFAULT_MATERIAL_NAME);
//        helper->AddProperty(&name,AI_MATKEY_NAME);
//
//        DefaultLogger::get()->debug("ScenePreprocessor: Adding default material \'" AI_DEFAULT_MATERIAL_NAME  "\'");
//
//        for (unsigned int i = 0; i < scene->mNumMeshes;++i) {
//            scene->mMeshes[i]->mMaterialIndex = scene->mNumMaterials;
//        }
//
//        scene->mNumMaterials++;
//    }
    }

    private void processMesh(AiMesh mesh) {

        // If aiMesh::mNumUVComponents is *not* set assign the default value of 2
        for (int i = 0; i < AI_MAX_NUMBER_OF_TEXTURECOORDS; i++) {
            if (mesh.mTextureCoords[i] == null) {
                mesh.mNumUVComponents[i] = 0;
            } else {
                if (mesh.mNumUVComponents[i] == 0) {
                    mesh.mNumUVComponents[i] = 2;
                }

                Vec3 p = mesh.mTextureCoords[i];
                Vec3 end = mesh.mTextureCoords[i + mesh.mNumVertices];

                // Ensure unsued components are zeroed. This will make 1D texture channels work
                // as if they were 2D channels .. just in case an application doesn't handle
                // this case
//            if (2 == mesh.mNumUVComponents[i]) {
//                for (; p != end; ++p)
//                    p.z = 0.f;
//            }
//            else if (1 == mesh->mNumUVComponents[i]) {
//                for (; p != end; ++p)
//                    p->z = p->y = 0.f;
//            }
//            else if (3 == mesh->mNumUVComponents[i]) {
//                // Really 3D coordinates? Check whether the third coordinate is != 0 for at least one element
//                for (; p != end; ++p) {
//                    if (p->z != 0)
//                        break;
//                }
//                if (p == end) {
//                    DefaultLogger::get()->warn("ScenePreprocessor: UVs are declared to be 3D but they're obviously not. Reverting to 2D.");
//                    mesh->mNumUVComponents[i] = 2;
//                }
//            }
            }
        }
        // If the information which primitive types are there in the
        // mesh is currently not available, compute it.
        if (mesh.mPrimitiveTypes == 0) {
            for (int a = 0; a < mesh.mNumFaces; a++) {
                AiFace face = mesh.mFaces[a];
                switch (face.mNumIndices) {
                    case 3:
                        mesh.mPrimitiveTypes |= aiPrimitiveType_TRIANGLE.value;
                        break;

                    case 2:
                        mesh.mPrimitiveTypes |= aiPrimitiveType_LINE.value;
                        break;

                    case 1:
                        mesh.mPrimitiveTypes |= aiPrimitiveType_POINT.value;
                        break;

                    default:
                        mesh.mPrimitiveTypes |= aiPrimitiveType_POLYGON.value;
                        break;
                }
            }
        }

        // If tangents and normals are given but no bitangents compute them
        if (mesh.mTangents != null && mesh.mNormals != null && mesh.mBitangents == null) {

            mesh.mBitangents = new Vec3[mesh.mNumVertices];
            for (int i = 0; i < mesh.mNumVertices; i++) {
//            mesh.mBitangents[i] = mesh.mNormals[i] ^ mesh.mTangents[i];
            }
        }
    }

    private void processAnimation(AiAnimation anim) {

        double first = 10e10, last = -10e10;

        for (int i = 0; i < anim.mNumChannels; ++i) {
            AiNodeAnim channel = anim.mChannels[i];

            /*  If the exact duration of the animation is not given
             *  compute it now.
             */
            if (anim.mDuration == -1.) {

                // Position keys
                for (int j = 0; j < channel.mNumPositionKeys; j++) {
                    AiVectorKey key = channel.mPositionKeys[j];
                    first = Math.min(first, key.mTime);
                    last = Math.max(last, key.mTime);
                }

                // Scaling keys
                for (int j = 0; j < channel.mNumScalingKeys; j++) {
                    AiVectorKey key = channel.mScalingKeys[j];
                    first = Math.min(first, key.mTime);
                    last = Math.max(last, key.mTime);
                }

                // Rotation keys
//            for (int j = 0; j < channel.mNumRotationKeys;++j) {
//                aiQuatKey& key = channel->mRotationKeys[j];
//                first = std::min (first, key.mTime);
//                last  = std::max (last,  key.mTime);
//            }
            }

            /*  Check whether the animation channel has no rotation
             *  or position tracks. In this case we generate a dummy
             *  track from the information we have in the transformation
             *  matrix of the corresponding node.
             */
//        if (!channel->mNumRotationKeys || !channel->mNumPositionKeys || !channel->mNumScalingKeys)  {
//            // Find the node that belongs to this animation
//            aiNode* node = scene->mRootNode->FindNode(channel->mNodeName);
//            if (node) // ValidateDS will complain later if 'node' is NULL
//            {
//                // Decompose the transformation matrix of the node
//                aiVector3D scaling, position;
//                aiQuaternion rotation;
//
//                node->mTransformation.Decompose(scaling, rotation,position);
//
//                // No rotation keys? Generate a dummy track
//                if (!channel->mNumRotationKeys) {
//                    channel->mNumRotationKeys = 1;
//                    channel->mRotationKeys = new aiQuatKey[1];
//                    aiQuatKey& q = channel->mRotationKeys[0];
//
//                    q.mTime  = 0.;
//                    q.mValue = rotation;
//
//                    DefaultLogger::get()->debug("ScenePreprocessor: Dummy rotation track has been generated");
//                }
//
//                // No scaling keys? Generate a dummy track
//                if (!channel->mNumScalingKeys)  {
//                    channel->mNumScalingKeys = 1;
//                    channel->mScalingKeys = new aiVectorKey[1];
//                    aiVectorKey& q = channel->mScalingKeys[0];
//
//                    q.mTime  = 0.;
//                    q.mValue = scaling;
//
//                    DefaultLogger::get()->debug("ScenePreprocessor: Dummy scaling track has been generated");
//                }
//
//                // No position keys? Generate a dummy track
//                if (!channel->mNumPositionKeys) {
//                    channel->mNumPositionKeys = 1;
//                    channel->mPositionKeys = new aiVectorKey[1];
//                    aiVectorKey& q = channel->mPositionKeys[0];
//
//                    q.mTime  = 0.;
//                    q.mValue = position;
//
//                    DefaultLogger::get()->debug("ScenePreprocessor: Dummy position track has been generated");
//                }
//            }
//        }
        }
    }
}
