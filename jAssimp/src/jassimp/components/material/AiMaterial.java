/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components.material;

import java.util.ArrayList;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class AiMaterial {

    public static String AI_DEFAULT_MATERIAL_NAME = "DefaultMaterial";
    public ArrayList<AiMaterialProperty> mProperties;

    public AiMaterial() {
        mProperties = new ArrayList<>();
    }

    public int getTextureCount(AiMaterialKey materialKey) {

        int textureCount = 0;

        for (AiMaterialProperty materialProperty : mProperties) {

            if (materialProperty.mKey == materialKey) {

                textureCount++;
            }
        }
        return textureCount;
    }

    public String getTextureFilename(AiMaterialKey materialKey) {

        String textureFilename = "";

        for (AiMaterialProperty materialProperty : mProperties) {

            if (materialProperty.mKey == materialKey) {

                textureFilename = materialProperty.string;
            }
        }        
        return textureFilename;
    }

    public void addProperty(int integer, int pNumValues, AiMaterialKey mKey) {
        mProperties.add(new AiMaterialProperty(integer, pNumValues, mKey));
    }

    public void addProperty(Vec3 color, int pNumValues, AiMaterialKey mKey) {
        mProperties.add(new AiMaterialProperty(color, pNumValues, mKey));
    }

    public void addProperty(String string, AiMaterialKey mKey) {
        mProperties.add(new AiMaterialProperty(string, mKey));
    }
}
