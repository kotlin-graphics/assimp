/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assimp;

/**
 *
 * @author elect
 */
public class GenericProperty {

    public static int get(String szName, int errorReturn) {

        if (szName == null) {
            throw new Error("szName is null");
        }
        
        long a = szName.hashCode();
        
        
        
        return 0;
    }
}
