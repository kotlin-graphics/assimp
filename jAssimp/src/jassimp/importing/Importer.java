/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.processes.ValidateDSProcess;
import jassimp.components.AiScene;
import static jassimp.importing.AiPostProcessSteps.aiProcess_ValidateDataStructure;
import jassimp.importing.importers.md2.Md2Importer;
import jassimp.processes.BaseProcess;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author gbarbieri
 */
public class Importer {

    private static Importer instance = new Importer();
    private static ImporterPimpl pimpl;
    /**
     * Post processing steps we can apply at the imported data.
     */
    private ArrayList<BaseProcess> mPostProcessingSteps;

    private Importer() {
        // allocate the pimpl first
        pimpl = new ImporterPimpl();
    }

    /**
     * Reads the given file and returns its contents if successful.
     *
     * @param _pFile
     * @param pFlags
     * @return
     * @throws IOException
     */
    public static AiScene readFile(String _pFile, int pFlags) throws IOException {

        File pFile = new File(_pFile);

//        if (file.exists()) {
//
//            int i = _pFile.lastIndexOf('.');
//            if (i > 0) {
//
//                String extension = _pFile.substring(i + 1);
//
//                BaseImporter importer = null;
//
//                switch (extension.toLowerCase()) {
//
//                    case Extension.MD2:
//                        importer = new Md2Importer();
//                        break;
//                }
//                if (importer != null) {
//
//                    mScene = importer.readFile(file);
//                }
//            }
//        }
        if (!pFile.exists()) {

            pimpl.mErrorString = "Unable to open file " + _pFile;
            return null;
        }

        BaseImporter imp = null;

        for (BaseImporter mImporter : pimpl.mImporter) {
            if (mImporter.canRead(pFile)) {
                imp = mImporter;
                break;
            }
        }

        // If successful, apply all active post processing steps to the imported data
        if (mScene != null) {

            // The ValidateDS process is an exception. It is executed first, even before ScenePreprocessor is called.
            if ((pFlags & aiProcess_ValidateDataStructure.value) != 0) {
                ValidateDSProcess ds = new ValidateDSProcess();
                ds.executeOnScene(pimpl);
            }
        }
        return null;
    }

    public ImporterPimpl pImpl() {
        return pimpl;
    }
}
