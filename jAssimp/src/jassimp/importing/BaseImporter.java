/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.components.AiScene;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author gbarbieri
 */
public abstract class BaseImporter {

    public AiScene readFile(Importer pImp, File pFile) throws IOException {

        // Gather configuration properties for this run
        setupProperties(pImp);

        // create a scene object to hold the data
        AiScene sc = new AiScene();

        // dispatch importing
        return internalRead(pFile, sc);
    }

    protected void setupProperties(Importer pImp) {

    }

    public abstract AiScene internalRead(File pFile, AiScene sc) throws IOException;

    public abstract boolean canRead(File pFile, boolean checkSig) throws IOException;

    protected String getExtension(File pFile) {

        int pos = pFile.toString().lastIndexOf('.');

        if (pos == 0) {

            return "";
        }

        String ret = pFile.toString().substring(pos + 1);

        ret = ret.toLowerCase();

        return ret;
    }

    protected boolean checkMagicToken(File pFile, int _magic) throws IOException {

        if (_magic == 0) {
            throw new Error("_magic is zero");
        }

        FileChannel fileChannel;
        boolean result;
        try (FileInputStream fileInputStream = new FileInputStream(pFile)) {
            fileChannel = fileInputStream.getChannel();
            ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) pFile.length());
            result = _magic == byteBuffer.getInt();
        }
        fileChannel.close();
        
        return result;
    }

}
