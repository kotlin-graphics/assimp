/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.processes;

import jassimp.components.AiFace;
import jassimp.components.AiMesh;
import static jassimp.components.AiPrimitiveType.aiPrimitiveType_POLYGON;
import static jassimp.components.AiPrimitiveType.aiPrimitiveType_TRIANGLE;
import jassimp.components.AiScene;
import static jassimp.importing.AiPostProcessSteps.aiProcess_Triangulate;
import java.util.ArrayList;
import jglm.Vec3;

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
            AiFace face = pMesh.mFaces[a];
            if (face.mNumIndices <= 4) {
                get_normals = false;
            }
            if( face.mNumIndices <= 3) {
                numOut++;
            }
            else {
                numOut += face.mNumIndices-2;
                max_out = Math.max(max_out,face.mNumIndices);
            }
        }
        
        // Just another check whether aiMesh::mPrimitiveTypes is correct
        if(numOut == pMesh.mNumFaces){
            throw new Error("numOut == pMesh.mNumFaces");
        }
        
        Vec3 nor_out = null;
        
        // if we don't have normals yet, but expect them to be a cheap side
        // product of triangulation anyway, allocate storage for them.
        if (pMesh.mNormals == null && get_normals) {
            // XXX need a mechanism to inform the GenVertexNormals process to treat these normals as preprocessed per-face normals
        //  nor_out = pMesh->mNormals = new aiVector3D[pMesh->mNumVertices];
        }
    
        // the output mesh will contain triangles, but no polys anymore
        pMesh.mPrimitiveTypes |= aiPrimitiveType_TRIANGLE.value;
        pMesh.mPrimitiveTypes &= ~aiPrimitiveType_POLYGON.value;
        
        AiFace[] out = new AiFace[numOut];
        AiFace[] curOut = out;
//        ArrayList<Vec3> temp_verts3d (max_out+2); /* temporary storage for vertices */
//        std::vector<aiVector2D> temp_verts(max_out+2);
        
        return false;
    }
}
