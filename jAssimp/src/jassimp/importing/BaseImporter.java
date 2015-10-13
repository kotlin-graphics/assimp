/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.components.AiScene;
import static jassimp.util.ByteArrayUtil.readInteger;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 *
 * @author gbarbieri
 */
public abstract class BaseImporter {

    public AiScene readFile(File pFile) throws IOException {

        // Gather configuration properties for this run
        setupProperties();

        // create a scene object to hold the data
        AiScene sc = new AiScene();

        return internalRead(pFile, sc);
    }

    protected void setupProperties() {

    }

    public abstract AiScene internalRead(File pFile, AiScene sc) throws IOException;

    public abstract boolean canRead(File pFile) throws IOException;

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
        byte[] pStream = Files.readAllBytes(pFile.toPath());

        return _magic == readInteger(pStream, 0);
    }

}
