/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.components.AiScene;
import jassimp.importing.importers.md2.Md2Importer;
import java.util.ArrayList;

/**
 *
 * @author GBarbieri
 */
public class ImporterPimpl {

    /**
     * Format-specific importer worker objects - one for each format we can
     * read.
     */
    public ArrayList<BaseImporter> mImporter;
    /**
     * The imported data, if ReadFile() was successful, NULL otherwise.
     */
    public AiScene mScene;
    /**
     * The error description, if there was one.
     */
    public String mErrorString;

    public ImporterPimpl() {

        mImporter = new ArrayList<>();
        
        mImporter.add(new Md2Importer());
    }
}
