/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components.material;

import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class AiMaterialProperty {
    
    public Vec3 color;
    public int pInput;
    public String string;
    public int pNumValues;
    public AiMaterialKey mKey;
    
    public AiMaterialProperty(int integer, int pNumValues, AiMaterialKey mKey) {
        
        this.pInput = integer;
        this.pNumValues = pNumValues;
        this.mKey = mKey;
    }
    
    public AiMaterialProperty(Vec3 color, int pNumValues, AiMaterialKey mKey) {
        
        this.color = color;
        this.pNumValues = pNumValues;
        this.mKey = mKey;
    }
    
    public AiMaterialProperty(String string, AiMaterialKey mKey) {
        
        this.string = string;
        this.mKey = mKey;
    }
}
