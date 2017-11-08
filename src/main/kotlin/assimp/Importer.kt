package assimp

import assimp.AiPostProcessSteps as Pps
import uno.kotlin.uri
import java.io.FileNotFoundException
import java.net.URI

/**
 * Created by elect on 13/11/2016.
 */

// ---------------------------------------------------------------------------
/** @brief Internal PIMPL implementation for Assimp::Importer   */

class ImporterPimpl(
        /** Format-specific importer worker objects - one for each format we can read.*/
        val mImporter: List<BaseImporter>,
        /** Post processing steps we can apply at the imported data. */
        val mPostProcessingSteps: List<BaseProcess>) {

    /** The imported data, if ReadFile() was successful, NULL otherwise. */
    lateinit var mScene: AiScene

    /** The error description, if there was one. */
    val mErrorString: String = ""

}

// ----------------------------------------------------------------------------------
/** CPP-API: The Importer class forms an C++ interface to the functionality of the Open Asset Import Library.
 *
 * Create an object of this class and call ReadFile() to import a file.
 * If the import succeeds, the function returns a pointer to the imported data.
 * The data remains property of the object, it is intended to be accessed read-only. The imported data will be destroyed
 * along with the Importer object. If the import fails, ReadFile() returns a NULL pointer. In this case you can retrieve
 * a human-readable error description be calling GetErrorString(). You can call ReadFile() multiple times with a single
 * Importer instance. Actually, constructing Importer objects involves quite many allocations and may take some time, so
 * it's better to reuse them as often as possible.
 *
 * If you need the Importer to do custom file handling to access the files, implement IOSystem and IOStream and supply
 * an instance of your custom IOSystem implementation by calling SetIOHandler() before calling ReadFile().
 * If you do not assign a custion IO handler, a default handler using the standard C++ IO logic will be used.
 *
 * @note One Importer instance is not thread-safe. If you use multiple threads for loading, each thread should maintain
 * its own Importer instance.
 */
class Importer(
        // Just because we don't want you to know how we're hacking around.
        private val pimpl: ImporterPimpl = ImporterPimpl(
                getImporterInstanceList(),
                getPostProcessingStepInstanceList())
) {

    companion object {
        /**
         *  @brief The upper limit for hints.
         */
        val MaxLenHint = 200
    }

    fun readFile(_pFile: String, pFlags: Int = 0) = readFile(_pFile.uri, pFlags)

    fun readFile(context: Class<*>, _pFile: String, pFlags: Int = 0) = readFile(context.classLoader.getResource(_pFile).toURI(), pFlags)

    fun readFile(_pFile: URI, flags: Int = 0): AiScene? {

        // Check whether this Importer instance has already loaded a scene. In this case we need to delete the old one
        //TODO if (pimpl.mScene != null) FreeScene()

        // First check if the file is accessible at all
        if (!_pFile.exists()) throw FileNotFoundException("Unable to open file " + _pFile)

        // Find an worker class which can handle the file
        val imp: BaseImporter? = pimpl.mImporter.firstOrNull { it.canRead(_pFile, false) }

        if (imp == null) {
            // TODO
            return null
        }

        pimpl.mScene = imp.readFile(this, _pFile)

        // If successful, apply all active post processing steps to the imported data
        if (pimpl.mScene != null) {

            ScenePreprocessor.scene = pimpl.mScene!!
            ScenePreprocessor.processScene()

            // Ensure that the validation process won't be called twice
            applyPostProcessing(flags wo Pps.ValidateDataStructure)
        }

        return pimpl.mScene
    }

    /** Apply post-processing to the currently bound scene  */
    fun applyPostProcessing(flags: Int): AiScene? {
        // Return immediately if no scene is active
        if (pimpl.mScene == null) return null
        // If no flags are given, return the current scene with no further action
        if (flags == 0) return pimpl.mScene
        // In debug builds: run basic flag validation
        assert(validateFlags(flags))
        logger.info { "Entering post processing pipeline" }

        if (!ASSIMP_BUILD_NO_VALIDATEDS_PROCESS)
        /*  The ValidateDS process plays an exceptional role. It isn't contained in the global list of post-processing
            steps, so we need to call it manually.         */
            if (flags has Pps.ValidateDataStructure) {
//                ValidateDSProcess ds;
//                ds.ExecuteOnScene(this);
//                if (!pimpl->mScene) {
//                    return NULL;
//                }
            }

//        #ifdef ASSIMP_BUILD_DEBUG
//                if (pimpl->bExtraVerbose)
//        {
//            #ifdef ASSIMP_BUILD_NO_VALIDATEDS_PROCESS
//                DefaultLogger::get()->error("Verbose Import is not available due to build settings");
//            #endif  // no validation
//            flags | = aiProcess_ValidateDataStructure;
//        }
//        #else
//        if (pimpl->bExtraVerbose) {
//            DefaultLogger::get()->warn("Not a debug build, ignoring extra verbose setting");
//        }
//        #endif // ! DEBUG
//
//        std::unique_ptr<Profiler> profiler (GetPropertyInteger(AI_CONFIG_GLOB_MEASURE_TIME, 0)?new Profiler():NULL);
//        for (unsigned int a = 0; a < pimpl->mPostProcessingSteps.size(); a++)   {
//
//            BaseProcess * process = pimpl->mPostProcessingSteps[a];
//            pimpl->mProgressHandler->UpdatePostProcess(static_cast<int>(a), static_cast<int>(pimpl->mPostProcessingSteps.size()));
//            if (process->IsActive(pFlags)) {
//
//            if (profiler) { profiler ->
//                BeginRegion("postprocess");
//            }
//
//            process->ExecuteOnScene (this);
//
//            if (profiler) { profiler ->
//                EndRegion("postprocess");
//            }
//        }
//            if (!pimpl->mScene) {
//            break;
//        }
//            #ifdef ASSIMP_BUILD_DEBUG
//
//            #ifdef ASSIMP_BUILD_NO_VALIDATEDS_PROCESS
//                continue;
//            #endif  // no validation
//
//            // If the extra verbose mode is active, execute the ValidateDataStructureStep again - after each step
//            if (pimpl->bExtraVerbose)   {
//            DefaultLogger::get()->debug("Verbose Import: revalidating data structures");
//
//            ValidateDSProcess ds;
//            ds.ExecuteOnScene(this);
//            if (!pimpl->mScene) {
//            DefaultLogger::get()->error("Verbose Import: failed to revalidate data structures");
//            break;
//        }
//        }
//            #endif // ! DEBUG
//        }
//        pimpl->mProgressHandler->UpdatePostProcess(static_cast<int>(pimpl->mPostProcessingSteps.size()), static_cast<int>(pimpl->mPostProcessingSteps.size()));
//
//        // update private scene flags
//        if (pimpl->mScene)
//        ScenePriv(pimpl->mScene)->mPPStepsApplied | = pFlags;
//
//        // clear any data allocated by post-process steps
//        pimpl->mPPShared->Clean();
//        DefaultLogger::get()->info("Leaving post processing pipeline");
//
//        ASSIMP_END_EXCEPTION_REGION(const aiScene *);
        return pimpl.mScene
    }

    /** Validate post process step flags    */
    fun validateFlags(flags: Int) = when {
        flags has Pps.GenSmoothNormals && flags has Pps.GenNormals -> {
            logger.error { "#aiProcess_GenSmoothNormals and #aiProcess_GenNormals are incompatible" }
            false
        }
        flags has Pps.OptimizeGraph && flags has Pps.PreTransformVertices -> {
            logger.error { "#aiProcess_OptimizeGraph and #aiProcess_PreTransformVertices are incompatible" }
            false
        }
        else -> true
    }
}

