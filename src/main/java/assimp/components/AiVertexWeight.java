/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assimp.components;

/**
 *
 * @author GBarbieri
 */
public class AiVertexWeight {

    /**
     * Index of the vertex which is influenced by the bone.
     */
    public int mVertexId;
    /**
     * The strength of the influence in the range (0...1).
     *
     * The influence from all bones at one vertex amounts to 1.
     */
    public float mWeight;

}
