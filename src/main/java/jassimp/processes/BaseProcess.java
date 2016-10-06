/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.processes;

import jassimp.components.AiScene;
import jassimp.importing.Importer;

/**
 *
 * @author GBarbieri
 */
public abstract class BaseProcess {

    public void executeOnScene(Importer pImp) {

        if (!(null != pImp && null != pImp.pImpl().mScene)) {
            throw new Error("null != pImp && null != pImp.pImpl().mScene");
        }

        setupProperties(pImp);

        // catch exceptions thrown inside the PostProcess-Step
//        try {            
        execute(pImp.pImpl().mScene);
//        }catch(){
//            
//        }
    }

    protected void setupProperties(Importer imp) {
    }

    // -------------------------------------------------------------------
    /**
     * Executes the post processing step on the given imported data. A process
     * should throw an ImportErrorException* if it fails. This method must be
     * implemented by deriving classes.
     *
     * @param pScene The imported data to work at.
     */
    public abstract void execute(AiScene pScene);

    // -------------------------------------------------------------------
    /**
     * Returns whether the processing step is present in the given flag.
     *
     * @param pFlags The processing flags the importer was called with. A
     * bitwise combination of #aiPostProcessSteps.
     * @return true if the process is present in this flag fields, false if not.
     */
    public abstract boolean isActive(int pFlags);

}
