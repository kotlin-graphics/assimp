/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.processes;

import java.util.ArrayList;

/**
 *
 * @author GBarbieri
 */
public class PostStepRegistry {

    public static void getPostProcessingStepInstanceList(ArrayList<BaseProcess> out) {
        // ----------------------------------------------------------------------------
        // Add an instance of each post processing step here in the order
        // of sequence it is executed. Steps that are added here are not
        // validated - as RegisterPPStep() does - all dependencies must be given.
        // ----------------------------------------------------------------------------
//        out.add(null)
    }
}
