/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.components.AiScene;
import jassimp.importing.importers.md2.Md2Importer;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author gbarbieri
 */
public class Importer {

    /**
    Reads the given file and returns its contents if successful.
    @param fileName
    @param flags
    @return
    @throws IOException 
    */
    public static AiScene readFile(String fileName, int flags) throws IOException {

        AiScene mScene = null;

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

                    mScene = importer.readFile(file);
                }
            }
        }
        // If successful, apply all active post processing steps to the imported data
        if (mScene != null) {

            
        }
        return null;
    }
}
