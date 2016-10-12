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
public class AiAnimation {

    /**
     * Duration of the animation in ticks.
     */
    public double mDuration;
    /**
     * The number of bone animation channels. Each channel affects a single
     * node.
     */
    public int mNumChannels;

    /**
     * The node animation channels. Each channel affects a single node. The
     * array is mNumChannels in size.
     */
    public AiNodeAnim[] mChannels;

}
