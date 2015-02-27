/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing.importers.md2;

import jglm.Vec2i;

/**
 *
 * @author gbarbieri
 */
public class Md2FileData {

    /**
     * Magic number, 844121161 is the decimal value for the unicode char for the
     * small endian 2PDI, 1229213746 for IDP2.
     */
    public static final int AI_MD2_MAGIC_NUMBER_BE = 844121161;
    public static final int AI_MD2_MAGIC_NUMBER_LE = 1229213746;

    public static final int AI_MD2_VERSION = 15;
    public static final int AI_MD2_MAXQPATH = 64;
    public static final int AI_MD2_MAX_FRAMES = 512;
    public static final int AI_MD2_MAX_SKINS = 32;
    public static final int AI_MD2_MAX_VERTS = 2048;
    public static final int AI_MD2_MAX_TRIANGLES = 4096;

    public static class Header {

        public static final int size = 17 * 4;

        public int magic;
        public int version;
        public Vec2i skin;
        public int frameSize;
        public int numSkins;
        public int numVertices;
        public int numTexCoords;
        public int numTriangles;
        public int numGlCommands;
        public int numFrames;
        public int offsetSkins;
        public int offsetTexCoords;
        public int offsetTriangles;
        public int offsetFrames;
        public int offsetGlCommands;
        public int offsetEnd;
    }

    public static class Triangle {

        /**
         * uint16_t vertexIndices[3];
         *
         * uint16_t textureIndices[3];
         */
        public static final int size = 2 * 3 * 2;

        int[] vertexIndices = new int[3];
        int[] textureIndices = new int[3];
    }

    public static class Vertex {

        /**
         * uint8_t vertex[3];
         *
         * uint8_t lightNormalIndex;
         */
        public static final int size = 1 * 3 + 1;

        public int[] vertex = new int[3];
        public int lightNormalIndex;
    }

    public static class Frame {

        /**
         * float scale[3];
         *
         * float translate[3];
         *
         * char name[16];
         *
         * Vertex vertices[1];
         */
        public static final int size = 4 * 3 + 4 * 3 + 1 * 16 + Vertex.size;

        public float[] scale = new float[3];
        public float[] translate = new float[3];
        public String name;
        public Vertex[] vertices = new Vertex[1];
    }

    public static class TexCoord {

        /**
         * uint16_t s;
         *
         * uint16_t t;
         */
        public static final int size = 2 + 2;

        public int s;
        public int t;
    }

    public static class Skin {

        /**
         * char name[AI_MD2_MAXQPATH];
         *
         * #define AI_MD2_MAXQPATH	64
         */
        public static final int size = 1 * 64;

        public String name;
    }
}
