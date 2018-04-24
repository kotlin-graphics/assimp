package assimp

import assimp.format.ProgressHandler

/** Internal PIMPL implementation for Assimp::Importer   */
class ImporterPimpl {

    var isDefaultHandler = true

    /** Progress handler for feedback. */
    var progressHandler: ProgressHandler = DefaultProgressHandler()
    var isDefaultProgressHandler = true

    var ioSystem: IOSystem = DefaultIOSystem()

    /** Format-specific importer worker objects - one for each format we can read.*/
    val importer = importerInstanceList

    /** Post processing steps we can apply at the imported data. */
    val postProcessingSteps = postProcessingStepInstanceList

    /** The imported data, if ReadFile() was successful, NULL otherwise. */
    var scene: AiScene? = null

    /** The error description, if there was one. */
    var errorString = ""

    val properties = mutableMapOf<Int, Any>()

    /** Used for testing - extra verbose mode causes the ValidateDataStructure-Step to be executed before and after
     *  every single postprocess step
     *  disable extra verbose mode by default    */
    var extraVerbose = false

    /** Used by post-process steps to share data
     *  Allocate a SharedPostProcessInfo object and store pointers to it in all post-process steps in the list. */
    var ppShared = SharedPostProcessInfo().also { info -> postProcessingSteps.forEach { it.shared = info } }
}

/** FOR IMPORTER PLUGINS ONLY: A helper class to the pleasure of importers that need to load many external meshes
 *  recursively.
 *
 *  The class uses several threads to load these meshes (or at least it could, this has not yet been implemented
 *  at the moment).
 *
 *  @note The class may not be used by more than one thread */
class BatchLoader(validate: Boolean = true) {

    /** No need to have that in the public API ... */
    val data = BatchData(validate)

    var validation
        /** Sets the validation step. True for enable validation during postprocess.
         *  @param  enable  True for validation.     */
        set(value) {
            data.validate = value
        }
        /** Returns the current validation step.
         *  @return The current validation step.     */
        get() = data.validate

    /** Add a new file to the list of files to be loaded.
     *  @param file File to be loaded
     *  @param steps Post-processing steps to be executed on the file
     *  @param map Optional configuration properties
     *  @return 'Load request channel' - an unique ID that can later be used to access the imported file data.
     *  @see GetImport */
    fun addLoadRequest(file: String, steps: Int = 0, map: MutableMap<Int, Any> = mutableMapOf()): Int {
        assert(file.isNotEmpty())
        // check whether we have this loading request already
        data.requests.filter { it.file == file && it.map == map }.map {
            it.refCnt++
            return it.id
        }
        // no, we don't have it. So add it to the queue ...
        data.requests.add(LoadRequest(file, steps, map, data.nextId))
        return data.nextId++
    }

    /** Get an imported scene.
     *  This polls the import from the internal request list.
     *  If an import is requested several times, this function can be called several times, too.
     *
     *  @param which LRWC returned by AddLoadRequest().
     *  @return null if there is no scene with this file name in the queue of the scene hasn't been loaded yet. */
    fun getImport(which: Int): AiScene? {
        val rq = data.requests.find { it.id == which && it.loaded }
        return if (rq != null) {
            val sc = rq.scene
            if (--rq.refCnt == 0) data.requests.remove(rq)
            sc
        } else null
    }

    /** Waits until all scenes have been loaded. This returns immediately if no scenes are queued.*/
    fun loadAll() {
        // no threaded implementation for the moment
        data.requests.forEach {
            // force validation in debug builds
            var pp = it.flags
            if (data.validate)
                pp = pp or AiPostProcessStep.ValidateDataStructure

            // setup config properties if necessary
            data.importer.impl.properties += it.map

            logger.info { "%%% BEGIN EXTERNAL FILE %%%" }
            logger.info { "File: ${it.file}" }

            data.importer.readFile(it.file, pp)
            it.scene = data.importer.orphanedScene
            it.loaded = true

            logger.info { "%%% END EXTERNAL FILE %%%" }
        }
    }
}

/** BatchLoader::pimpl data structure   */
class BatchData(
        /** Validation enabled state    */
        var validate: Boolean
) {
    /** Importer used to load all meshes    */
    var importer = Importer()
    /** List of all imports */
    var requests = ArrayList<LoadRequest>()
    // Base path
    var pathBase = ""
    // Id for next item
    var nextId = 0xffff
}

/** Represents an import request    */
class LoadRequest(val file: String, var flags: Int, val map: MutableMap<Int, Any> = mutableMapOf(), val id: Int) {

    var scene: AiScene? = null
    var loaded = false
    var refCnt = 1

//    bool operator == ( const std::string& f )
//    const {
//        return file == f;
//    }
}