package assimp

import assimp.format.ProgressHandler

/** Internal PIMPL implementation for Assimp::Importer   */
class ImporterPimpl {

    var isDefaultHandler = true

    /** Progress handler for feedback. */
    var progressHandler: ProgressHandler = DefaultProgressHandler()
    var isDefaultProgressHandler = true

    /** Format-specific importer worker objects - one for each format we can read.*/
    val importer = importerInstanceList

    /** Post processing steps we can apply at the imported data. */
    val postProcessingSteps = postProcessingStepInstanceList

    /** The imported data, if ReadFile() was successful, NULL otherwise. */
    var scene: AiScene? = null

    /** The error description, if there was one. */
    var errorString = ""

    /** List of integer properties */
    val intProperties = mutableMapOf<Int, Int>()

    /** List of floating-point properties */
    val floatProperties = mutableMapOf<Int, Float>()

    /** List of string properties */
    val stringProperties = mutableMapOf<Int, String>()

    /** List of Matrix properties */
    val matrixProperties = mutableMapOf<Int, AiMatrix4x4>()

    /** Used for testing - extra verbose mode causes the ValidateDataStructure-Step to be executed before and after
     *  every single postprocess step
     *  disable extra verbose mode by default    */
    var extraVerbose = false

    /** Used by post-process steps to share data
     *  Allocate a SharedPostProcessInfo object and store pointers to it in all post-process steps in the list. */
    var ppShared = SharedPostProcessInfo().also { info -> postProcessingSteps.forEach { it.shared = info } }
}