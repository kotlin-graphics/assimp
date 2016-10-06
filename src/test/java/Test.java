/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.URL;

import jassimp.importing.Importer;

/**
 *
 * @author gbarbieri
 */
public class Test {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        
        URL url = Test.class.getResource("/jassimp/test/model/md2/faerie.md2");
        
        new Importer().readFile(url.getPath(), 0);
    }
    
}
