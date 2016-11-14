/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assimp.md2;

import glm.vec._2.i.Vec2i;
import glm.vec._3.Vec3;
import java.nio.ByteBuffer;

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
        public Vec2i skin = new Vec2i();
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

        public Header(ByteBuffer mBuffer) {

            magic = mBuffer.getInt();
            version = mBuffer.getInt();
            skin.x = mBuffer.getInt();
            skin.y = mBuffer.getInt();
            frameSize = mBuffer.getInt();
            numSkins = mBuffer.getInt();
            numVertices = mBuffer.getInt();
            numTexCoords = mBuffer.getInt();
            numTriangles = mBuffer.getInt();
            numGlCommands = mBuffer.getInt();
            numFrames = mBuffer.getInt();
            offsetSkins = mBuffer.getInt();
            offsetTexCoords = mBuffer.getInt();
            offsetTriangles = mBuffer.getInt();
            offsetFrames = mBuffer.getInt();
            offsetGlCommands = mBuffer.getInt();
            offsetEnd = mBuffer.getInt();
        }
    }

    public static class Triangle {

        /**
         * uint16_t vertexIndices[3];
         *
         * uint16_t textureIndices[3];
         */
        public static final int indicesSize = Short.BYTES;
        public static final int offsetTextureIndices = indicesSize * 3;
        public static final int sizeOf = indicesSize * 3 * 2;

        int[] vertexIndices = new int[3];
        int[] textureIndices = new int[3];
    }

    public static class Vertex {

        /**
         * uint8_t vertex[3];
         *
         * uint8_t lightNormalIndex;
         */
        public static final int offsetLightNormalIndex = Byte.BYTES * 3;
        public static final int sizeOf = offsetLightNormalIndex + Byte.BYTES;

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
        public static final int offsetVertices = 3 * Float.BYTES + 3 * Float.BYTES + 16 * Byte.BYTES;
        public static final int offsetTranslate = 3 * Float.BYTES;
        public static final int sizeOf = offsetVertices + Vertex.sizeOf;

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
        public static final int offsetT = Short.BYTES;
        public static final int sizeOf = offsetT + Short.BYTES;

        public int s;
        public int t;
    }

    public static class Skin {

        /**
         * char name[AI_MD2_MAXQPATH];
         *
         * #define AI_MD2_MAXQPATH	64
         */
        public static final int sizeOf = 64 * Byte.BYTES;

        public String name;
    }

    public static Vec3[] g_avNormals = new Vec3[]{
        new Vec3(-0.525731f, 0.000000f, 0.850651f),
        new Vec3(-0.442863f, 0.238856f, 0.864188f),
        new Vec3(-0.295242f, 0.000000f, 0.955423f),
        new Vec3(-0.309017f, 0.500000f, 0.809017f),
        new Vec3(-0.162460f, 0.262866f, 0.951056f),
        new Vec3(0.000000f, 0.000000f, 1.000000f),
        new Vec3(0.000000f, 0.850651f, 0.525731f),
        new Vec3(-0.147621f, 0.716567f, 0.681718f),
        new Vec3(0.147621f, 0.716567f, 0.681718f),
        new Vec3(0.000000f, 0.525731f, 0.850651f),
        new Vec3(0.309017f, 0.500000f, 0.809017f),
        new Vec3(0.525731f, 0.000000f, 0.850651f),
        new Vec3(0.295242f, 0.000000f, 0.955423f),
        new Vec3(0.442863f, 0.238856f, 0.864188f),
        new Vec3(0.162460f, 0.262866f, 0.951056f),
        new Vec3(-0.681718f, 0.147621f, 0.716567f),
        new Vec3(-0.809017f, 0.309017f, 0.500000f),
        new Vec3(-0.587785f, 0.425325f, 0.688191f),
        new Vec3(-0.850651f, 0.525731f, 0.000000f),
        new Vec3(-0.864188f, 0.442863f, 0.238856f),
        new Vec3(-0.716567f, 0.681718f, 0.147621f),
        new Vec3(-0.688191f, 0.587785f, 0.425325f),
        new Vec3(-0.500000f, 0.809017f, 0.309017f),
        new Vec3(-0.238856f, 0.864188f, 0.442863f),
        new Vec3(-0.425325f, 0.688191f, 0.587785f),
        new Vec3(-0.716567f, 0.681718f, -0.147621f),
        new Vec3(-0.500000f, 0.809017f, -0.309017f),
        new Vec3(-0.525731f, 0.850651f, 0.000000f),
        new Vec3(0.000000f, 0.850651f, -0.525731f),
        new Vec3(-0.238856f, 0.864188f, -0.442863f),
        new Vec3(0.000000f, 0.955423f, -0.295242f),
        new Vec3(-0.262866f, 0.951056f, -0.162460f),
        new Vec3(0.000000f, 1.000000f, 0.000000f),
        new Vec3(0.000000f, 0.955423f, 0.295242f),
        new Vec3(-0.262866f, 0.951056f, 0.162460f),
        new Vec3(0.238856f, 0.864188f, 0.442863f),
        new Vec3(0.262866f, 0.951056f, 0.162460f),
        new Vec3(0.500000f, 0.809017f, 0.309017f),
        new Vec3(0.238856f, 0.864188f, -0.442863f),
        new Vec3(0.262866f, 0.951056f, -0.162460f),
        new Vec3(0.500000f, 0.809017f, -0.309017f),
        new Vec3(0.850651f, 0.525731f, 0.000000f),
        new Vec3(0.716567f, 0.681718f, 0.147621f),
        new Vec3(0.716567f, 0.681718f, -0.147621f),
        new Vec3(0.525731f, 0.850651f, 0.000000f),
        new Vec3(0.425325f, 0.688191f, 0.587785f),
        new Vec3(0.864188f, 0.442863f, 0.238856f),
        new Vec3(0.688191f, 0.587785f, 0.425325f),
        new Vec3(0.809017f, 0.309017f, 0.500000f),
        new Vec3(0.681718f, 0.147621f, 0.716567f),
        new Vec3(0.587785f, 0.425325f, 0.688191f),
        new Vec3(0.955423f, 0.295242f, 0.000000f),
        new Vec3(1.000000f, 0.000000f, 0.000000f),
        new Vec3(0.951056f, 0.162460f, 0.262866f),
        new Vec3(0.850651f, -0.525731f, 0.000000f),
        new Vec3(0.955423f, -0.295242f, 0.000000f),
        new Vec3(0.864188f, -0.442863f, 0.238856f),
        new Vec3(0.951056f, -0.162460f, 0.262866f),
        new Vec3(0.809017f, -0.309017f, 0.500000f),
        new Vec3(0.681718f, -0.147621f, 0.716567f),
        new Vec3(0.850651f, 0.000000f, 0.525731f),
        new Vec3(0.864188f, 0.442863f, -0.238856f),
        new Vec3(0.809017f, 0.309017f, -0.500000f),
        new Vec3(0.951056f, 0.162460f, -0.262866f),
        new Vec3(0.525731f, 0.000000f, -0.850651f),
        new Vec3(0.681718f, 0.147621f, -0.716567f),
        new Vec3(0.681718f, -0.147621f, -0.716567f),
        new Vec3(0.850651f, 0.000000f, -0.525731f),
        new Vec3(0.809017f, -0.309017f, -0.500000f),
        new Vec3(0.864188f, -0.442863f, -0.238856f),
        new Vec3(0.951056f, -0.162460f, -0.262866f),
        new Vec3(0.147621f, 0.716567f, -0.681718f),
        new Vec3(0.309017f, 0.500000f, -0.809017f),
        new Vec3(0.425325f, 0.688191f, -0.587785f),
        new Vec3(0.442863f, 0.238856f, -0.864188f),
        new Vec3(0.587785f, 0.425325f, -0.688191f),
        new Vec3(0.688191f, 0.587785f, -0.425325f),
        new Vec3(-0.147621f, 0.716567f, -0.681718f),
        new Vec3(-0.309017f, 0.500000f, -0.809017f),
        new Vec3(0.000000f, 0.525731f, -0.850651f),
        new Vec3(-0.525731f, 0.000000f, -0.850651f),
        new Vec3(-0.442863f, 0.238856f, -0.864188f),
        new Vec3(-0.295242f, 0.000000f, -0.955423f),
        new Vec3(-0.162460f, 0.262866f, -0.951056f),
        new Vec3(0.000000f, 0.000000f, -1.000000f),
        new Vec3(0.295242f, 0.000000f, -0.955423f),
        new Vec3(0.162460f, 0.262866f, -0.951056f),
        new Vec3(-0.442863f, -0.238856f, -0.864188f),
        new Vec3(-0.309017f, -0.500000f, -0.809017f),
        new Vec3(-0.162460f, -0.262866f, -0.951056f),
        new Vec3(0.000000f, -0.850651f, -0.525731f),
        new Vec3(-0.147621f, -0.716567f, -0.681718f),
        new Vec3(0.147621f, -0.716567f, -0.681718f),
        new Vec3(0.000000f, -0.525731f, -0.850651f),
        new Vec3(0.309017f, -0.500000f, -0.809017f),
        new Vec3(0.442863f, -0.238856f, -0.864188f),
        new Vec3(0.162460f, -0.262866f, -0.951056f),
        new Vec3(0.238856f, -0.864188f, -0.442863f),
        new Vec3(0.500000f, -0.809017f, -0.309017f),
        new Vec3(0.425325f, -0.688191f, -0.587785f),
        new Vec3(0.716567f, -0.681718f, -0.147621f),
        new Vec3(0.688191f, -0.587785f, -0.425325f),
        new Vec3(0.587785f, -0.425325f, -0.688191f),
        new Vec3(0.000000f, -0.955423f, -0.295242f),
        new Vec3(0.000000f, -1.000000f, 0.000000f),
        new Vec3(0.262866f, -0.951056f, -0.162460f),
        new Vec3(0.000000f, -0.850651f, 0.525731f),
        new Vec3(0.000000f, -0.955423f, 0.295242f),
        new Vec3(0.238856f, -0.864188f, 0.442863f),
        new Vec3(0.262866f, -0.951056f, 0.162460f),
        new Vec3(0.500000f, -0.809017f, 0.309017f),
        new Vec3(0.716567f, -0.681718f, 0.147621f),
        new Vec3(0.525731f, -0.850651f, 0.000000f),
        new Vec3(-0.238856f, -0.864188f, -0.442863f),
        new Vec3(-0.500000f, -0.809017f, -0.309017f),
        new Vec3(-0.262866f, -0.951056f, -0.162460f),
        new Vec3(-0.850651f, -0.525731f, 0.000000f),
        new Vec3(-0.716567f, -0.681718f, -0.147621f),
        new Vec3(-0.716567f, -0.681718f, 0.147621f),
        new Vec3(-0.525731f, -0.850651f, 0.000000f),
        new Vec3(-0.500000f, -0.809017f, 0.309017f),
        new Vec3(-0.238856f, -0.864188f, 0.442863f),
        new Vec3(-0.262866f, -0.951056f, 0.162460f),
        new Vec3(-0.864188f, -0.442863f, 0.238856f),
        new Vec3(-0.809017f, -0.309017f, 0.500000f),
        new Vec3(-0.688191f, -0.587785f, 0.425325f),
        new Vec3(-0.681718f, -0.147621f, 0.716567f),
        new Vec3(-0.442863f, -0.238856f, 0.864188f),
        new Vec3(-0.587785f, -0.425325f, 0.688191f),
        new Vec3(-0.309017f, -0.500000f, 0.809017f),
        new Vec3(-0.147621f, -0.716567f, 0.681718f),
        new Vec3(-0.425325f, -0.688191f, 0.587785f),
        new Vec3(-0.162460f, -0.262866f, 0.951056f),
        new Vec3(0.442863f, -0.238856f, 0.864188f),
        new Vec3(0.162460f, -0.262866f, 0.951056f),
        new Vec3(0.309017f, -0.500000f, 0.809017f),
        new Vec3(0.147621f, -0.716567f, 0.681718f),
        new Vec3(0.000000f, -0.525731f, 0.850651f),
        new Vec3(0.425325f, -0.688191f, 0.587785f),
        new Vec3(0.587785f, -0.425325f, 0.688191f),
        new Vec3(0.688191f, -0.587785f, 0.425325f),
        new Vec3(-0.955423f, 0.295242f, 0.000000f),
        new Vec3(-0.951056f, 0.162460f, 0.262866f),
        new Vec3(-1.000000f, 0.000000f, 0.000000f),
        new Vec3(-0.850651f, 0.000000f, 0.525731f),
        new Vec3(-0.955423f, -0.295242f, 0.000000f),
        new Vec3(-0.951056f, -0.162460f, 0.262866f),
        new Vec3(-0.864188f, 0.442863f, -0.238856f),
        new Vec3(-0.951056f, 0.162460f, -0.262866f),
        new Vec3(-0.809017f, 0.309017f, -0.500000f),
        new Vec3(-0.864188f, -0.442863f, -0.238856f),
        new Vec3(-0.951056f, -0.162460f, -0.262866f),
        new Vec3(-0.809017f, -0.309017f, -0.500000f),
        new Vec3(-0.681718f, 0.147621f, -0.716567f),
        new Vec3(-0.681718f, -0.147621f, -0.716567f),
        new Vec3(-0.850651f, 0.000000f, -0.525731f),
        new Vec3(-0.688191f, 0.587785f, -0.425325f),
        new Vec3(-0.587785f, 0.425325f, -0.688191f),
        new Vec3(-0.425325f, 0.688191f, -0.587785f),
        new Vec3(-0.425325f, -0.688191f, -0.587785f),
        new Vec3(-0.587785f, -0.425325f, -0.688191f),
        new Vec3(-0.688191f, -0.587785f, -0.425325f)
    };
}
