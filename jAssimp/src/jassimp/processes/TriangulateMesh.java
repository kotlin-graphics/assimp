/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.processes;

import jassimp.components.AiFace;
import jassimp.components.AiMesh;
import static jassimp.components.AiPrimitiveType.aiPrimitiveType_POLYGON;
import jassimp.components.AiScene;
import static jassimp.importing.AiPostProcessSteps.aiProcess_Triangulate;

/**
 *
 * @author GBarbieri
 */
public class TriangulateMesh extends BaseProcess {

    @Override
    public boolean isActive(int pFlags) {

        return (pFlags & aiProcess_Triangulate.value) != 0;
    }

    @Override
    public void execute(AiScene pScene) {

        boolean bHas = false;
        for (int a = 0; a < pScene.mNumMeshes; a++) {

//            if () {
//                
//            }
        }
    }

    /**
     * Triangulates the given mesh.
     * @param pMesh
     * @return 
     */
    private boolean triangulateMesh(AiMesh pMesh) {

        // Now we have aiMesh::mPrimitiveTypes, so this is only here for test cases
        if (pMesh.mPrimitiveTypes == 0)    {
            boolean bNeed = false;

            for( int a = 0; a < pMesh.mNumFaces; a++) {
                AiFace face = pMesh.mFaces[a];

                if( face.mNumIndices != 3)  {
                    bNeed = true;
                }
            }
            if (!bNeed)
                return false;
        }
        else if ((pMesh.mPrimitiveTypes & aiPrimitiveType_POLYGON.value) == 0) {
            return false;
        }
        
        // Find out how many output faces we'll get
        int numOut = 0, max_out = 0;
        boolean get_normals = true;
        for( int a = 0; a < pMesh.mNumFaces; a++) {
//            AiFace face = pMesh->mFaces[a];
//            if (face.mNumIndices <= 4) {
//                get_normals = false;
//            }
//            if( face.mNumIndices <= 3) {
//                numOut++;
//
//            }
//            else {
//                numOut += face.mNumIndices-2;
//                max_out = std::max(max_out,face.mNumIndices);
//            }
        }
        return false;
    }
}
