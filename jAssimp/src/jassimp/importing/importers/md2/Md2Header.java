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
public class Md2Header {

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
