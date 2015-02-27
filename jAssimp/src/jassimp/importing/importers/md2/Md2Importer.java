/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing.importers.md2;

import jassimp.AiScene;
import jassimp.importing.BaseImporter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gbarbieri
 */
public class Md2Importer extends BaseImporter {

    private long fileSize;

    @Override
    protected AiScene internalRead(File file) {

        fileSize = file.length();
        /**
         * check whether the md2 file is large enough to contain at least the
         * file header.
         */
        if (fileSize < Md2FileData.Header.size) {

            System.out.println("MD2 File is too small");

            return null;
        }

        FileInputStream fileInputStream;

        try {
            fileInputStream = new FileInputStream(file);

            Md2FileData.Header md2Header = readHeader(fileInputStream);

            if (!validateHeader(md2Header)) {

                System.out.println("Invalid MD2 magic word");

                return null;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Md2Importer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Md2FileData.Header readHeader(FileInputStream fileInputStream) {

        Md2FileData.Header md2Header = new Md2FileData.Header();

        md2Header.magic = readInteger(fileInputStream);
        md2Header.version = readInteger(fileInputStream);
        md2Header.skin.x = readInteger(fileInputStream);
        md2Header.skin.y = readInteger(fileInputStream);
        md2Header.frameSize = readInteger(fileInputStream);
        md2Header.numSkins = readInteger(fileInputStream);
        md2Header.numVertices = readInteger(fileInputStream);
        md2Header.numTexCoords = readInteger(fileInputStream);
        md2Header.numTriangles = readInteger(fileInputStream);
        md2Header.numGlCommands = readInteger(fileInputStream);
        md2Header.numFrames = readInteger(fileInputStream);
        md2Header.offsetSkins = readInteger(fileInputStream);
        md2Header.offsetTexCoords = readInteger(fileInputStream);
        md2Header.offsetTriangles = readInteger(fileInputStream);
        md2Header.offsetFrames = readInteger(fileInputStream);
        md2Header.offsetGlCommands = readInteger(fileInputStream);
        md2Header.offsetEnd = readInteger(fileInputStream);

        return md2Header;
    }

    private boolean validateHeader(Md2FileData.Header md2Header) {
        // check magic number
        if (md2Header.magic == Md2FileData.AI_MD2_MAGIC_NUMBER_BE
                || md2Header.magic == Md2FileData.AI_MD2_MAGIC_NUMBER_LE) {

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
        if (md2Header.offsetEnd > Md2FileData.Header.size) {
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
        return false;
    }

    private int readInteger(FileInputStream fileInputStream) {

        int integer = 0;

        byte[] content = new byte[4];

        try {
            fileInputStream.read(content);
            ByteBuffer byteBuffer = ByteBuffer.wrap(content);
            byteBuffer.order(ByteOrder.nativeOrder());
            integer = byteBuffer.getInt();

        } catch (IOException ex) {
            Logger.getLogger(Md2Importer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return integer;
    }
}
