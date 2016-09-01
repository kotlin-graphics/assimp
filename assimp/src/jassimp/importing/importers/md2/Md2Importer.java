/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing.importers.md2;

import glm.vec._3.Vec3;
import jassimp.md2.Md2FileData;
import jassimp.components.AiFace;
import jassimp.components.material.AiMaterial;
import jassimp.components.AiMesh;
import jassimp.components.AiNode;
import static jassimp.components.AiPrimitiveType.aiPrimitiveType_TRIANGLE;
import jassimp.components.AiScene;
//import jassimp.components.AiShadingMode;
import jassimp.importing.BaseImporter;
import static jassimp.md2.Md2FileData.AI_MD2_MAGIC_NUMBER_LE;
import jassimp.components.material.AiMaterialKey;
import jassimp.components.material.AiShadingMode;
import jassimp.components.material.AiTextureType;
import jassimp.importing.Importer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 *
 * @author gbarbieri
 */
public class Md2Importer extends BaseImporter {

    /**
     * Configuration option: frame to be loaded
     */
    private int configFrameID;

    /**
     * Header of the MD2 file
     */
    private Md2FileData.Header m_pcHeader;

    /**
     * Buffer to hold the loaded file
     */
    private ByteBuffer mBuffer;

    /**
     * Size of the file, in bytes
     */
    private long fileSize;

    /**
     * Setup configuration properties.
     *
     * @param pImp
     */
    @Override
    protected void setupProperties(Importer pImp) {
        /**
         * The AI_CONFIG_IMPORT_MD2_KEYFRAME option overrides the
         * AI_CONFIG_IMPORT_GLOBAL_KEYFRAME option.
         */
//        configFrameID = pImp.getPropertyInteger(Config.AI_CONFIG_IMPORT_MD2_KEYFRAME, -1);
        configFrameID = 0;
    }

    @Override
    public AiScene internalRead(File pFile, AiScene pScene) throws IOException {

        // Check whether we can read from the file
        if (!pFile.canRead()) {
            throw new Error("Failed to open MD2 file " + pFile);
        }

        fileSize = pFile.length();
        /**
         * check whether the md2 file is large enough to contain at least the
         * file header.
         */
        if (fileSize < Md2FileData.Header.size) {
            throw new Error("MD2 File is too small");
        }

        FileInputStream fileInputStream = new FileInputStream(pFile);
        FileChannel fileChannel = fileInputStream.getChannel();

        mBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) pFile.length()).order(ByteOrder.nativeOrder());

        m_pcHeader = new Md2FileData.Header(mBuffer);

        validateHeader();

        // there won't be more than one mesh inside the file
        pScene.mNumMaterials = 1;
        pScene.mRootNode = new AiNode();
        pScene.mRootNode.mNumMeshes = 1;
        pScene.mRootNode.mMeshes = new int[]{0};
        pScene.mMaterial = new AiMaterial[]{new AiMaterial()};
        pScene.mNumMeshes = 1;
        pScene.mMeshes = new AiMesh[1];

        AiMesh pcMesh = pScene.mMeshes[0] = new AiMesh();
        pcMesh.mPrimitiveTypes = aiPrimitiveType_TRIANGLE.value;

        // navigate to the begin of the frame data
        int pcFrame = m_pcHeader.offsetFrames;

        pcFrame += configFrameID;

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
        int iMode = AiShadingMode.Gouraud.value;
        pcHelper.addProperty(iMode, AiMaterialKey.SHADING_MODEL);

        if (m_pcHeader.numTexCoords > 0 && m_pcHeader.numSkins > 0) {

            Vec3 clr = new Vec3(1f, 1f, 1f);
            pcHelper.addProperty(clr, AiMaterialKey.COLOR_DIFFUSE);
            pcHelper.addProperty(clr, AiMaterialKey.COLOR_SPECULAR);

            clr.x = clr.y = clr.z = .05f;
            pcHelper.addProperty(clr, AiMaterialKey.COLOR_AMBIENT);

            String skinsName = readString(mBuffer, m_pcHeader.offsetSkins, 64);

            if (!skinsName.isEmpty()) {
                pcHelper.addProperty(skinsName, AiMaterialKey.TEXTURE, AiTextureType.DIFFUSE.value, 0);
            } else {
                System.out.println("Texture file name has zero length. It will be skipped.");
            }
        } else {
            // apply a default material
            Vec3 clr = new Vec3(.6f, .6f, .6f);

            pcHelper.addProperty(clr, AiMaterialKey.COLOR_DIFFUSE);
            pcHelper.addProperty(clr, AiMaterialKey.COLOR_SPECULAR);

            clr.x = clr.y = clr.z = .05f;
            pcHelper.addProperty(clr, AiMaterialKey.COLOR_AMBIENT);

            pcHelper.addProperty(AiMaterial.AI_DEFAULT_MATERIAL_NAME, AiMaterialKey.NAME);

            // Try to guess the name of the texture file from the model file name
            String md2Name = pFile.getName();
            String texture = md2Name.substring(0, md2Name.length() - 3).concat("bmp");

            pcHelper.addProperty(texture, AiMaterialKey.TEXTURE, AiTextureType.DIFFUSE.value, 0);
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
        scale.x = mBuffer.getFloat(pcFrame + 0 * 4);
        scale.y = mBuffer.getFloat(pcFrame + 1 * 4);
        scale.z = mBuffer.getFloat(pcFrame + 2 * 4);

        Vec3 translate = new Vec3();
        translate.x = mBuffer.getFloat(pcFrame + Md2FileData.Frame.offsetTranslate + 0 * 4);
        translate.y = mBuffer.getFloat(pcFrame + Md2FileData.Frame.offsetTranslate + 1 * 4);
        translate.z = mBuffer.getFloat(pcFrame + Md2FileData.Frame.offsetTranslate + 2 * 4);

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
                int iIndex = mBuffer.getShort(pcTriangle + i * Md2FileData.Triangle.sizeOf + c * Md2FileData.Triangle.indicesSize);

                if (iIndex >= m_pcHeader.numVertices) {
                    System.out.println("MD2: Vertex index is outside the allowed range");
                    iIndex = m_pcHeader.numVertices - 1;
                }
                // read x,y, and z component of the vertex
                Vec3 vec = new Vec3();

                vec.x = (mBuffer.get(pcVerts + iIndex * Md2FileData.Vertex.sizeOf + 0) & 0xff) * scale.x;
                vec.y = (mBuffer.get(pcVerts + iIndex * Md2FileData.Vertex.sizeOf + 1) & 0xff) * scale.y;
                vec.z = (mBuffer.get(pcVerts + iIndex * Md2FileData.Vertex.sizeOf + 2) & 0xff) * scale.z;

                vec.add(translate);

                pcMesh.mVertices[iCurrent] = vec;

                // read the normal vector from the precalculated normal table                    
                byte b = mBuffer.get(pcVerts + iIndex * Md2FileData.Vertex.sizeOf
                        + Md2FileData.Vertex.offsetLightNormalIndex);
                int iNormalIndex = b & 0xff;
                Vec3 vNormal = lookupNormalIndex(iNormalIndex);
                pcMesh.mNormals[iCurrent] = vNormal;

                // flip z and y to become right-handed
                float tmp = vNormal.y;
                vNormal.y = vNormal.z;
                vNormal.z = tmp;
                tmp = vec.y;
                vec.y = vec.z;
                vec.z = tmp;

                if (m_pcHeader.numTexCoords > 0) {
                    // validate texture coordinates
                    iIndex = mBuffer.getShort(pcTriangle + i * Md2FileData.Triangle.sizeOf
                            + Md2FileData.Triangle.offsetTextureIndices
                            + c * Md2FileData.Triangle.indicesSize);

                    if (iIndex >= m_pcHeader.numTexCoords) {
                        System.out.println("MD2: UV index is outside the allowed range");
                        iIndex = m_pcHeader.numTexCoords - 1;
                    }
                    Vec3 pcOut = new Vec3();
                    // the texture coordinates are absolute values but we
                    // need relative values between 0 and 1
                    pcOut.x = mBuffer.getShort(pcTexCoords + iIndex * Md2FileData.TexCoord.sizeOf);
                    pcOut.x /= fDivisorU;
                    pcOut.y = mBuffer.getShort(pcTexCoords + iIndex * Md2FileData.TexCoord.sizeOf + Md2FileData.TexCoord.offsetT);
                    pcOut.y = 1 - pcOut.y / fDivisorV;

                    pcMesh.mTextureCoords[iCurrent] = pcOut;
                }
                pScene.mMeshes[0].mFaces[i].mIndices[c] = iCurrent;
            }
        }
        return pScene;
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

    private void validateHeader() {

        // check magic number
        if (m_pcHeader.magic != Md2FileData.AI_MD2_MAGIC_NUMBER_BE
                && m_pcHeader.magic != Md2FileData.AI_MD2_MAGIC_NUMBER_LE) {

            throw new Error("Invalid MD2 magic word: should be IDP2 (" + Md2FileData.AI_MD2_MAGIC_NUMBER_BE
                    + ") or 2PDI (" + Md2FileData.AI_MD2_MAGIC_NUMBER_LE + "), the magic word found is "
                    + m_pcHeader.magic);
        }

        // check file format version
        if (m_pcHeader.version != 8) {
            System.err.println("WARNING, Unsupported md2 file version. Continuing happily...");
        }

        // check some values whether they are valid
        if (m_pcHeader.numFrames == 0) {
            throw new Error("Invalid md2 file: NUM_FRAMES is 0");
        }

        if (m_pcHeader.offsetEnd > fileSize) {
            throw new Error("Invalid md2 file: File is too small");
        }

        if (m_pcHeader.offsetSkins + m_pcHeader.numSkins * Md2FileData.Skin.sizeOf >= fileSize
                || m_pcHeader.offsetTexCoords + m_pcHeader.numTexCoords * Md2FileData.TexCoord.sizeOf >= fileSize
                || m_pcHeader.offsetTriangles + m_pcHeader.numTriangles * Md2FileData.Triangle.sizeOf >= fileSize
                || m_pcHeader.offsetFrames + m_pcHeader.numFrames * Md2FileData.Frame.sizeOf >= fileSize
                || m_pcHeader.offsetEnd > fileSize) {

            throw new Error("Invalid MD2 header: some offsets are outside the file");
        }

        if (m_pcHeader.numSkins > Md2FileData.AI_MD2_MAX_SKINS) {
            System.err.println("The model contains more skins than Quake 2 supports");
        }
        if (m_pcHeader.numFrames > Md2FileData.AI_MD2_MAX_FRAMES) {
            System.err.println("The model contains more frames than Quake 2 supports");
        }
        if (m_pcHeader.numVertices > Md2FileData.AI_MD2_MAX_VERTS) {
            System.err.println("The model contains more vertices than Quake 2 supports");
        }

        if (m_pcHeader.numFrames <= configFrameID) {
            throw new Error("The requested frame is not existing the file");
        }
    }

    /**
     * Returns whether the class can handle the format of the given file.
     *
     * @param pFile
     * @param checkSig
     * @return
     * @throws IOException
     */
    @Override
    public boolean canRead(File pFile, boolean checkSig) throws IOException {

        String extension = getExtension(pFile);

        if (extension.equals("md2")) {
            return true;
        }
        // if check for extension is not enough, check for the magic tokens
        if (extension.isEmpty() || checkSig) {
            int token = AI_MD2_MAGIC_NUMBER_LE;
            return checkMagicToken(pFile, token);
        }
        return false;
    }
}
