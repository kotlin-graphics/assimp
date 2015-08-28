/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp;

import jassimp.components.AiNode;
import jassimp.components.AiScene;

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
//        if(pNode != mScene.mRootNode && pNode.mP)
    }
}
