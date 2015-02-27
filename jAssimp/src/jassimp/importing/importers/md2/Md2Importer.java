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

    @Override
    protected AiScene internalRead(File file) {
        /**
         * check whether the md2 file is large enough to contain at least the
         * file header.
         */
        if (file.length() < Md2Header.size) {

            System.out.println("MD2 File is too small");

            return null;
        }
        
        FileInputStream fileInputStream;
        
        try {
            fileInputStream = new FileInputStream(file);

            Md2Header md2Header = readHeader(fileInputStream);
            
            if(!validateHeader(md2Header)){
             
                System.out.println("Invalid MD2 magic word");

            return null;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Md2Importer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Md2Header readHeader(FileInputStream fileInputStream) {

        Md2Header md2Header = new Md2Header();
        
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

    private boolean validateHeader(Md2Header md2Header) {
        
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
