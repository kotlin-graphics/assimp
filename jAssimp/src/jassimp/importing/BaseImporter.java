/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.components.AiScene;
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

    public AiScene readFile(File file) throws IOException {

        return internalRead(file);
    }

    public abstract AiScene internalRead(File file) throws IOException;

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
        
        if(_magic == 0) {
            throw new Error("_magic is zero");
        }
        byte[] pStream = Files.readAllBytes(pFile.toPath());
        
        return _magic == readInteger(pStream, 0);
    }
    
    protected int readInteger(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 4).getInt();
    }

    protected float readFloat(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 4).getFloat();
    }

    protected short readShort(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 2).getShort();
    }

    protected byte readByte(byte[] bs, int offset) throws IOException {
        return readByteBuffer(bs, offset, 1).get();
    }

    protected ByteBuffer readByteBuffer(byte[] bs, int offset, int length) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bs, offset, length);
        return byteBuffer.order(ByteOrder.nativeOrder());
    }

    protected String readString(byte[] bs, int offset, int length) throws IOException {
        return new String(bs, offset, length, Charset.forName("UTF-8"));
    }
}
