/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.importing;

/**
 *
 * @author GBarbieri
 */
public enum AiPostProcessSteps {

    // -------------------------------------------------------------------------
    /**
     * <hr>Validates the imported scene data structure. This makes sure that all
     * indices are valid, all animations and bones are linked correctly, all
     * material references are correct .. etc.
     *
     * It is recommended that you capture Assimp's log output if you use this
     * flag, so you can easily find out what's wrong if a file fails the
     * validation. The validator is quite strict and will find *all*
     * inconsistencies in the data structure... It is recommended that plugin
     * developers use it to debug their loaders. There are two types of
     * validation failures:
     * <ul>
     * <li>Error: There's something wrong with the imported data. Further
     * postprocessing is not possible and the data is not usable at all. The
     * import fails. #Importer::GetErrorString() or #aiGetErrorString() carry
     * the error message around.</li>
     * <li>Warning: There are some minor issues (e.g. 1000000 animation
     * keyframes with the same time), but further postprocessing and use of the
     * data structure is still safe. Warning details are written to the log
     * file, <tt>#AI_SCENE_FLAGS_VALIDATION_WARNING</tt> is set in
     * #aiScene::mFlags</li>
     * </ul>
     *
     * This post-processing step is not time-consuming. Its use is not
     * compulsory, but recommended.
     */
    aiProcess_ValidateDataStructure(0x400);

    public int value;

    private AiPostProcessSteps(int value) {
        this.value = value;
    }
}
