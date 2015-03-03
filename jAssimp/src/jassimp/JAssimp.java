/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp;

import jassimp.importing.Importer;
import java.io.IOException;
import java.net.URL;

/**
 *
 * @author gbarbieri
 */
public class JAssimp {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        
        URL url = JAssimp.class.getResource("/jassimp/content/md2/phoenix_ugv.md2");
        
        Importer.readFile(url.getPath(), 0);
    }
    
}
