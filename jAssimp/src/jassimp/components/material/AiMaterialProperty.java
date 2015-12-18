/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components.material;

/**
 *
 * @author gbarbieri
 */
public class AiMaterialProperty {

//    public static AiMaterialProperty newName (String property) {
//        return new AiMaterialProperty.String(AiMaterialKey.Name, property);
//    }
    public AiMaterialKey mKey;

    private AiMaterialProperty(AiMaterialKey materialKey) {
        this.mKey = materialKey;
    }

    static class String extends AiMaterialProperty {

        public java.lang.String property;

        public String(AiMaterialKey materialKey, java.lang.String property) {

            super(materialKey);

            this.property = property;
        }
    }

    static class StringTN extends String {

        public int t;
        public int n;

        public StringTN(AiMaterialKey materialKey, java.lang.String property, int t, int n) {

            super(materialKey, property);

            this.t = t;
            this.n = n;
        }
    }

    static class Int extends AiMaterialProperty {

        public int property;

        public Int(int property, AiMaterialKey materialKey) {

            super(materialKey);

            this.property = property;
        }
    }

    static class Texture extends AiMaterialProperty {

        public int property;

        public Texture(int property, AiMaterialKey materialKey) {

            super(materialKey);

            this.property = property;
        }
    }

    static class Vec3 extends AiMaterialProperty {

        public jglm.Vec3 property;

        public Vec3(jglm.Vec3 property, AiMaterialKey materialKey) {

            super(materialKey);

            this.property = property;
        }
    }
}
