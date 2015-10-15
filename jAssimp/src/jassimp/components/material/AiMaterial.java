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
    public static String AI_MATKEY_SHADING_MODEL = "$mat.shadingm";
    /**
     * List of all material properties loaded..
     */
    public ArrayList<AiMaterialProperty> mProperties;

    /**
     * Number of properties in the data base
     */
    public int mNumProperties;

    /**
     * Storage allocated
     */
    public int mNumAllocated;

    /**
     * Construction. Actually the one and only way to get an aiMaterial
     * instance.
     */
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

    public void addProperty(int integer, int pNumValues, String mKey) {
        mProperties.add(new AiMaterialProperty(integer, pNumValues, mKey));
    }

    public void addProperty(Vec3 color, int pNumValues, AiMaterialKey mKey) {
        mProperties.add(new AiMaterialProperty(color, pNumValues, mKey));
    }

    public void addProperty(String string, AiMaterialKey mKey) {
        mProperties.add(new AiMaterialProperty(string, mKey));
    }
}
