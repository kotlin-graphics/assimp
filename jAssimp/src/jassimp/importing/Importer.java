/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.processes.ValidateDSProcess;
import jassimp.components.AiScene;
import static jassimp.importing.AiPostProcessSteps.*;
import static jassimp.importing.ImporterRegistry.getImporterInstanceList;
import jassimp.importing.importers.md2.Md2Importer;
import jassimp.processes.BaseProcess;
import static jassimp.processes.PostStepRegistry.getPostProcessingStepInstanceList;
import jassimp.processes.ScenePreprocessor;
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
    public static boolean NO_VALIDATEDS_PROCESS = false;

    private Importer() {
        // allocate the pimpl first
        pimpl = new ImporterPimpl();

        getImporterInstanceList(pimpl.mImporter);
        getPostProcessingStepInstanceList(pimpl.mPostProcessingSteps);
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

        if (!pFile.exists()) {
            // Finish to implement mErrorString
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

        if (imp == null) {
            return null;
        }

        pimpl.mScene = imp.readFile(pFile);

        // If successful, apply all active post processing steps to the imported data
        if (pimpl.mScene != null) {

            if (!NO_VALIDATEDS_PROCESS) {
                // The ValidateDS process is an exception. It is executed first, even before ScenePreprocessor is called.
                if ((pFlags & aiProcess_ValidateDataStructure.value) != 0) {

                    ValidateDSProcess ds = new ValidateDSProcess();
                    ds.executeOnScene(instance);
                }
            }

            ScenePreprocessor pre = new ScenePreprocessor(pimpl.mScene);
            pre.processScene();

            // Ensure that the validation process won't be called twice
            applyPostProcessing(pFlags & ~aiProcess_ValidateDataStructure.value);
        }
        return pimpl.mScene;
    }

    public static AiScene applyPostProcessing(int pFlags) {

        // If no flags are given, return the current scene with no further action
        if (pFlags == 0) {
            return pimpl.mScene;
        }

        instance._validateFlags(pFlags);

        if (!NO_VALIDATEDS_PROCESS) {
            // The ValidateDS process plays an exceptional role. It isn't contained in the global
            // list of post-processing steps, so we need to call it manually.
            if ((pFlags & aiProcess_ValidateDataStructure.value) != 0) {

                ValidateDSProcess ds = new ValidateDSProcess();
                ds.executeOnScene(instance);
            }
        }

        for (BaseProcess process : pimpl.mPostProcessingSteps) {

            if (process.isActive(pFlags)) {

                process.executeOnScene(instance);
            }
        }
        return pimpl.mScene;
    }

    /**
     * Validate post process step flags.
     *
     * @param pFlags
     * @return
     */
    private boolean _validateFlags(int pFlags) {

        if ((pFlags & aiProcess_GenSmoothNormals.value) != 0 && (pFlags & aiProcess_GenNormals.value) != 0) {
            throw new Error("#aiProcess_GenSmoothNormals and #aiProcess_GenNormals are incompatible");
        }
        if ((pFlags & aiProcess_OptimizeGraph.value) != 0 && (pFlags & aiProcess_PreTransformVertices.value) != 0) {
            throw new Error("#aiProcess_OptimizeGraph and #aiProcess_PreTransformVertices are incompatible");
        }
        return true;
    }

    public ImporterPimpl pImpl() {
        return pimpl;
    }
}
