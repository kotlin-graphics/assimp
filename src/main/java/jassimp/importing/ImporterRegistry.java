/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.Config;
import jassimp.importing.importers.md2.Md2Importer;
import java.util.ArrayList;

/**
 *
 * @author GBarbieri
 */
public class ImporterRegistry {

    public static void getImporterInstanceList(ArrayList<BaseImporter> out) {
        // ----------------------------------------------------------------------------
        // Add an instance of each worker class here
        // (register_new_importers_here)
        // ----------------------------------------------------------------------------
        if (!Config.NO_MD2_IMPORTER) {
            out.add(new Md2Importer());
        }
    }
}
