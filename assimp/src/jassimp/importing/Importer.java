/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

import jassimp.Config;
import static jassimp.Config.NO_VALIDATEDS_PROCESS;
import jassimp.GenericProperty;
import jassimp.processes.ValidateDSProcess;
import jassimp.components.AiScene;
import static jassimp.importing.AiPostProcessSteps.*;
import static jassimp.importing.ImporterRegistry.getImporterInstanceList;
import jassimp.processes.BaseProcess;
import static jassimp.processes.PostStepRegistry.getPostProcessingStepInstanceList;
import jassimp.processes.ScenePreprocessor;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author gbarbieri
 */
public class Importer {

//    private static Importer instance = new Importer();
    /**
     * Just because we don't want you to know how we're hacking around.
     */
    private ImporterPimpl pimpl;

    public Importer() {

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
    public AiScene readFile(String _pFile, int pFlags) throws IOException {

        File pFile = new File(_pFile);

        // First check if the file is accessable at all
        if (!pFile.exists()) {
            throw new Error("Unable to open file " + _pFile);
        }

        // Find an worker class which can handle the file
        BaseImporter imp = null;

        for (BaseImporter mImporter : pimpl.mImporter) {
            if (mImporter.canRead(pFile, false)) {
                imp = mImporter;
                break;
            }
        }

        if (imp == null) {
            // not so bad yet ... try format auto detection.
            System.out.println("File extension not known, trying signature-based detection");
            for (BaseImporter mImporter : pimpl.mImporter) {
                if (mImporter.canRead(pFile, true)) {
                    imp = mImporter;
                    break;
                }
            }
            // Put a proper error message if no suitable importer was found
            if (imp == null) {
                throw new Error("No suitable reader found for the file format of file " + pFile);
            }
        }

        pimpl.mScene = imp.readFile(this, pFile);

        // If successful, apply all active post processing steps to the imported data
        if (pimpl.mScene != null) {

            if (!Config.NO_VALIDATEDS_PROCESS) {
                // The ValidateDS process is an exception. It is executed first, even before ScenePreprocessor is called.
                if ((pFlags & aiProcess_ValidateDataStructure.value) != 0) {

                    ValidateDSProcess ds = new ValidateDSProcess();
                    ds.executeOnScene(this);
                }
            }

            ScenePreprocessor pre = new ScenePreprocessor(pimpl.mScene);
            pre.processScene();

            // Ensure that the validation process won't be called twice
            applyPostProcessing(pFlags & ~aiProcess_ValidateDataStructure.value);
        }
        return pimpl.mScene;
    }

    public AiScene applyPostProcessing(int pFlags) {

        // If no flags are given, return the current scene with no further action
        if (pFlags == 0) {
            return pimpl.mScene;
        }

        _validateFlags(pFlags);

        if (!NO_VALIDATEDS_PROCESS) {
            // The ValidateDS process plays an exceptional role. It isn't contained in the global
            // list of post-processing steps, so we need to call it manually.
            if ((pFlags & aiProcess_ValidateDataStructure.value) != 0) {

                ValidateDSProcess ds = new ValidateDSProcess();
                ds.executeOnScene(this);
            }
        }

        for (BaseProcess process : pimpl.mPostProcessingSteps) {

            if (process.isActive(pFlags)) {

                process.executeOnScene(this);
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
    
    public int getPropertyInteger(String szName, int iErrorReturn) {
        return GenericProperty.get(szName, iErrorReturn);
    }
    
    public ImporterPimpl pImpl() {
        return pimpl;
    }
}
