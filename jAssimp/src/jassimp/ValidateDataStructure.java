/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp;

import jassimp.components.AiNode;
import jassimp.components.AiScene;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author gbarbieri
 */
public class ValidateDataStructure {

    private static AiScene mScene;

    public static void execute(AiScene pScene) {

        mScene = pScene;

        validate(pScene.mRootNode);
    }

    private static void validate(AiNode pNode) {

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
                    throw new Error("aiNode::mMeshes[" + pNode.mMeshes[i] + "] is out of range (maximum is "
                            + (mScene.mNumMeshes - 1) + ")");
                }

                if (!abHadMesh.add(pNode.mMeshes[i])) {
                    throw new Error("aiNode::mMeshes[" + i + "] is already referenced by this node (value: "
                            + pNode.mMeshes[i] + ")");
                }
            }
        }
        if (pNode.mNumChildren > 0) {

            if (pNode.mChildren == null) {
                throw new Error("aiNode.mChildren is NULL (aiNode.mNumChildren is " + pNode.mNumChildren + ")");
            }

            for (AiNode child : pNode.mChildren) {
                validate(child);
            }
        }
    }
}
