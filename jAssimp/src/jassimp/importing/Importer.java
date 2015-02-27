/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.AiScene;
import jassimp.importing.importers.md2.Md2Importer;
import java.io.File;

/**
 *
 * @author gbarbieri
 */
public class Importer {
    
    public static AiScene readFile(String fileName, int flags) {

        AiScene aiScene;        
        File file = new File(fileName);

        if (file.exists()) {

            int i = fileName.lastIndexOf('.');
            if (i > 0) {

                String extension = fileName.substring(i + 1);

                BaseImporter importer = null;

                switch (extension.toLowerCase()) {

                    case Extension.MD2:
                        importer = new Md2Importer();
                        break;
                }
                if (importer != null) {
                    
                    aiScene = importer.readFile(file);
                }
            }
        }
        return null;
    }
}
