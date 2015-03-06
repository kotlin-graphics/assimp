/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp;

import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class AiMesh {
    
    public int mPrimitiveType;
    public int mNumFaces;
    public AiFace[] mFaces;
    public int mNumVertices;
    public Vec3[] mVertices;
    public Vec3[] mNormals;
    public Vec3[][] mTextureCoords;
    public int[] mNumUVComponents;
}
