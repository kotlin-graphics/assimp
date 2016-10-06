/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components;

/**
 *
 * @author GBarbieri
 */
public class AiNodeAnim {

    /**
     * The number of position keys
     */
    public int mNumPositionKeys;
    /**
     * The position keys of this animation channel. Positions are specified as
     * 3D vector. The array is mNumPositionKeys in size.
     *
     * If there are position keys, there will also be at least one scaling and
     * one rotation key.
     */
    public AiVectorKey[] mPositionKeys;
    /**
     * The number of scaling keys
     */
    public int mNumScalingKeys;
    /**
     * The scaling keys of this animation channel. Scalings are specified as 3D
     * vector. The array is mNumScalingKeys in size.
     *
     * If there are scaling keys, there will also be at least one position and
     * one rotation key.
     */
    public AiVectorKey[] mScalingKeys;
//    /**
//     * The number of rotation keys
//     */
//    public int mNumRotationKeys;
//    /**
//     * The rotation keys of this animation channel. Rotations are given as
//     * quaternions, which are 4D vectors. The array is mNumRotationKeys in size.
//     *
//     * If there are rotation keys, there will also be at least one scaling and
//     * one position key.
//     */
//    public int mNumRotationKeys;

}
