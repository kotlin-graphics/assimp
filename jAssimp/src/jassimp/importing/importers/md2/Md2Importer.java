/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing.importers.md2;

import jassimp.AiFace;
import jassimp.material.AiMaterial;
import jassimp.AiMesh;
import jassimp.AiNode;
import jassimp.AiPrimitiveType;
import jassimp.AiScene;
import jassimp.AiShadingMode;
import jassimp.importing.BaseImporter;
import jassimp.material.AiMaterialKey;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class Md2Importer extends BaseImporter {

    private long fileSize;

    @Override
    protected AiScene internalRead(File file) throws IOException {

        fileSize = file.length();
        /**
         * check whether the md2 file is large enough to contain at least the
         * file header.
         */
        if (fileSize < Md2FileData.Header.size) {

            System.out.println("MD2 File is too small");

            return null;
        }

        byte[] mBuffer = Files.readAllBytes(file.toPath());

        Md2FileData.Header m_pcHeader = readHeader(mBuffer);

        if (validateHeader(m_pcHeader)) {

            AiScene pScene = new AiScene();
            // there won't be more than one mesh inside the file
            pScene.mNumMaterials = 1;
            pScene.mRootNode = new AiNode();
            pScene.mRootNode.mNumMeshes = 1;
            pScene.mRootNode.mMeshes = new int[]{0};
            pScene.mMaterial = new AiMaterial[]{new AiMaterial()};
            pScene.mNumMeshes = 1;
            pScene.mMeshes = new AiMesh[]{new AiMesh()};

            AiMesh pcMesh = pScene.mMeshes[0];
            pcMesh.mPrimitiveType = AiPrimitiveType.TRIANGLE;

            // navigate to the begin of the frame data
            int pcFrame = m_pcHeader.offsetFrames;

            // navigate to the begin of the triangle data
            int pcTriangle = m_pcHeader.offsetTriangles;

            // navigate to the begin of the tex coords data
            int pcTexCoords = m_pcHeader.offsetTexCoords;

            // navigate to the begin of the vertex data
            int pcVerts = pcFrame + Md2FileData.Frame.offsetVertices;

            pcMesh.mNumFaces = m_pcHeader.numTriangles;
            pcMesh.mFaces = new AiFace[m_pcHeader.numTriangles];

            // allocate output storage
            pcMesh.mNumVertices = pcMesh.mNumFaces * 3;
            pcMesh.mVertices = new Vec3[pcMesh.mNumVertices];
            pcMesh.mNormals = new Vec3[pcMesh.mNumVertices];

            // Not sure whether there are MD2 files without texture coordinates
            // NOTE: texture coordinates can be there without a texture,
            // but a texture can't be there without a valid UV channel
            AiMaterial pcHelper = pScene.mMaterial[0];
            pcHelper.addProperty(AiShadingMode.Gouraud, 1, AiMaterialKey.SHADING_MODEL);

            if (m_pcHeader.numTexCoords > 0 && m_pcHeader.numSkins > 0) {

                Vec3 clr = new Vec3(1f, 1f, 1f);
                pcHelper.addProperty(clr, 1, AiMaterialKey.COLOR_DIFFUSE);
                pcHelper.addProperty(clr, 1, AiMaterialKey.COLOR_SPECULAR);

                clr.x = clr.y = clr.z = .05f;
                pcHelper.addProperty(clr, 1, AiMaterialKey.COLOR_AMBIENT);

                String skinsName = readString(mBuffer, m_pcHeader.offsetSkins, 64);

                if (!skinsName.isEmpty()) {
                    pcHelper.addProperty(skinsName, AiMaterialKey.TEXTURE_DIFFUSE);
                } else {
                    System.out.println("Texture file name has zero length. It will be skipped.");
                }
            } else {
                // apply a default material
                Vec3 clr = new Vec3(.6f, .6f, .6f);

                pcHelper.addProperty(clr, 1, AiMaterialKey.COLOR_DIFFUSE);
                pcHelper.addProperty(clr, 1, AiMaterialKey.COLOR_SPECULAR);

                clr.x = clr.y = clr.z = .05f;
                pcHelper.addProperty(clr, 1, AiMaterialKey.COLOR_AMBIENT);

                pcHelper.addProperty(AiMaterial.AI_DEFAULT_MATERIAL_NAME, AiMaterialKey.NAME);

                // Try to guess the name of the texture file from the model file name
                String md2Name = file.getName();
                String texture = md2Name.substring(0, md2Name.length() - 3).concat("pcx");

                pcHelper.addProperty(texture, AiMaterialKey.TEXTURE_DIFFUSE);
            }
            // now read all triangles of the first frame, apply scaling and translation
            int iCurrent = 0;

            float fDivisorU = 1f;
            float fDivisorV = 1f;

            if (m_pcHeader.numTexCoords > 0) {
                // allocate storage for texture coordinates, too
                pcMesh.mTextureCoords = new Vec3[pcMesh.mNumVertices];
                pcMesh.mNumUVComponents = new int[2];

                // check whether the skin width or height are zero (this would
                // cause a division through zero)
                if (m_pcHeader.skin.x <= 0) {
                    System.out.println("MD2: No valid skin width given");
                } else {
                    fDivisorU = m_pcHeader.skin.x;
                }
                if (m_pcHeader.skin.y <= 0) {
                    System.out.println("MD2: No valid skin height given");
                } else {
                    fDivisorV = m_pcHeader.skin.y;
                }
            }
            Vec3 scale = new Vec3();
            scale.x = readFloat(mBuffer, pcFrame + 0 * 4);
            scale.y = readFloat(mBuffer, pcFrame + 1 * 4);
            scale.z = readFloat(mBuffer, pcFrame + 2 * 4);

            Vec3 translate = new Vec3();
            translate.x = readFloat(mBuffer, pcFrame + Md2FileData.Frame.offsetTranslate + 0 * 4);
            translate.y = readFloat(mBuffer, pcFrame + Md2FileData.Frame.offsetTranslate + 1 * 4);
            translate.z = readFloat(mBuffer, pcFrame + Md2FileData.Frame.offsetTranslate + 2 * 4);

            for (int i = 0; i < m_pcHeader.numTriangles; i++) {
                // Allocate the face
                pScene.mMeshes[0].mFaces[i] = new AiFace();
                pScene.mMeshes[0].mFaces[i].mIndices = new int[3];
                pScene.mMeshes[0].mFaces[i].mNumIndices = 3;

                // copy texture coordinates
                // check whether they are different from the previous value at this index.
                // In this case, create a full separate set of vertices/normals/texcoords
                for (int c = 0; c < 3; ++c, ++iCurrent) {

                    // validate vertex indices
                    int iIndex = readShort(mBuffer, pcTriangle + i * Md2FileData.Triangle.size + c * Md2FileData.Triangle.indicesSize);

                    if (iIndex >= m_pcHeader.numVertices) {
                        System.out.println("MD2: Vertex index is outside the allowed range");
                        iIndex = m_pcHeader.numVertices - 1;
                    }
                    // read x,y, and z component of the vertex
                    Vec3 vec = new Vec3();

                    vec.x = readByte(mBuffer, pcVerts + iIndex * Md2FileData.Vertex.size + 0) * scale.x;
                    vec.y = readByte(mBuffer, pcVerts + iIndex * Md2FileData.Vertex.size + 1) * scale.x;
                    vec.z = readByte(mBuffer, pcVerts + iIndex * Md2FileData.Vertex.size + 2) * scale.x;

                    vec = vec.plus(translate);

                    pcMesh.mVertices[iCurrent] = vec;

                    // read the normal vector from the precalculated normal table
                    int iNormalIndex = readByte(mBuffer, pcVerts + iIndex * Md2FileData.Vertex.size
                            + Md2FileData.Vertex.offsetLightNormalIndex);
                    System.out.println("iNormalIndex "+iNormalIndex);
                    Vec3 vNormal = lookupNormalIndex(iNormalIndex);
                    pcMesh.mNormals[iCurrent] = vNormal;

                    // flip z and y to become right-handed
                    float tmp;
                    tmp = vNormal.y;
                    vNormal.y = vNormal.z;
                    vNormal.z = tmp;
                    tmp = vec.y;
                    vec.y = vec.z;
                    vec.z = tmp;
                }
            }
        }
        return null;
    }

    // Helper function to lookup a normal in Quake 2's precalculated table
    private Vec3 lookupNormalIndex(int iNormalIndex) {

        // make sure the normal index has a valid value
        if (iNormalIndex >= Md2FileData.g_avNormals.length) {
            System.out.println("Index overflow in Quake II normal vector list");
            iNormalIndex = Md2FileData.g_avNormals.length - 1;
        }
        return Md2FileData.g_avNormals[iNormalIndex];
    }

    private Md2FileData.Header readHeader(byte[] mBuffer) throws IOException {

        Md2FileData.Header md2Header = new Md2FileData.Header();

        md2Header.magic = readInteger(mBuffer, 0 * 4);
        md2Header.version = readInteger(mBuffer, 1 * 4);
        md2Header.skin.x = readInteger(mBuffer, 2 * 4);
        md2Header.skin.y = readInteger(mBuffer, 3 * 4);
        md2Header.frameSize = readInteger(mBuffer, 4 * 4);
        md2Header.numSkins = readInteger(mBuffer, 5 * 4);
        md2Header.numVertices = readInteger(mBuffer, 6 * 4);
        md2Header.numTexCoords = readInteger(mBuffer, 7 * 4);
        md2Header.numTriangles = readInteger(mBuffer, 8 * 4);
        md2Header.numGlCommands = readInteger(mBuffer, 9 * 4);
        md2Header.numFrames = readInteger(mBuffer, 10 * 4);
        md2Header.offsetSkins = readInteger(mBuffer, 11 * 4);
        md2Header.offsetTexCoords = readInteger(mBuffer, 12 * 4);
        md2Header.offsetTriangles = readInteger(mBuffer, 13 * 4);
        md2Header.offsetFrames = readInteger(mBuffer, 14 * 4);
        md2Header.offsetGlCommands = readInteger(mBuffer, 15 * 4);
        md2Header.offsetEnd = readInteger(mBuffer, 16 * 4);

        return md2Header;
    }

    private boolean validateHeader(Md2FileData.Header md2Header) {
        // check magic number
        if (md2Header.magic != Md2FileData.AI_MD2_MAGIC_NUMBER_BE
                && md2Header.magic != Md2FileData.AI_MD2_MAGIC_NUMBER_LE) {

            System.out.println("Invalid MD2 magic word: should be IDP2 (" + Md2FileData.AI_MD2_MAGIC_NUMBER_BE
                    + ") or 2PDI (" + Md2FileData.AI_MD2_MAGIC_NUMBER_LE + "), the magic word found is " + md2Header.magic);
            return false;
        }
        // check file format version
        if (md2Header.version != 8) {
            System.out.println("WARNING, Unsupported md2 file version. Continuing happily...");
        }
        // check some values whether they are valid
        if (md2Header.numFrames == 0) {
            System.out.println("Invalid md2 file: NUM_FRAMES is 0");
            return false;
        }
        if (md2Header.offsetEnd > fileSize) {
            System.out.println("Invalid md2 file: File is too small");
            return false;
        }
        if (md2Header.offsetSkins + md2Header.numSkins * Md2FileData.Skin.size >= fileSize
                || md2Header.offsetTexCoords + md2Header.numTexCoords * Md2FileData.TexCoord.size >= fileSize
                || md2Header.offsetTriangles + md2Header.numTriangles * Md2FileData.Triangle.size >= fileSize
                || md2Header.offsetFrames + md2Header.numFrames * Md2FileData.Frame.size >= fileSize
                || md2Header.offsetEnd > fileSize) {

            System.out.println("Invalid MD2 header: some offsets are outside the file");
            return false;
        }
        if (md2Header.numSkins > Md2FileData.AI_MD2_MAX_SKINS) {
            System.out.println("The model contains more skins than Quake 2 supports");
        }
        if (md2Header.numFrames > Md2FileData.AI_MD2_MAX_FRAMES) {
            System.out.println("The model contains more frames than Quake 2 supports");
        }
        if (md2Header.numVertices > Md2FileData.AI_MD2_MAX_VERTS) {
            System.out.println("The model contains more vertices than Quake 2 supports");
        }
        return true;
    }

    private int readInteger(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 4).getInt();
    }

    private float readFloat(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 4).getFloat();
    }

    private short readShort(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 2).getShort();
    }

    private byte readByte(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 1).get();
    }

    private ByteBuffer readByteBuffer(byte[] bs, int offset, int length) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bs, offset, length);
        return byteBuffer.order(ByteOrder.nativeOrder());
    }

    private String readString(byte[] bs, int offset, int length) throws IOException {
        return new String(bs, offset, length, Charset.forName("UTF-8"));
    }
}
