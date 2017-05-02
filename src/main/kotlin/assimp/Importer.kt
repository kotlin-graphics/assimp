package assimp

import java.io.FileNotFoundException
import java.net.URI

/**
 * Created by elect on 13/11/2016.
 */

// ---------------------------------------------------------------------------
/** @brief Internal PIMPL implementation for Assimp::Importer   */

class ImporterPimpl {

    /** Format-specific importer worker objects - one for each format we can read.*/
    val mImporter: List<BaseImporter>

    /** Post processing steps we can apply at the imported data. */
    val mPostProcessingSteps: List<BaseProcess>

    /** The imported data, if ReadFile() was successful, NULL otherwise. */
    lateinit var mScene: AiScene

    /** The error description, if there was one. */
    val mErrorString: String = ""

    constructor(mImporter: List<BaseImporter>, mPostProcessingSteps: List<BaseProcess>) {
        this.mImporter=mImporter
        this.mPostProcessingSteps=mPostProcessingSteps
    }
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

    fun readFile(context: Class<*>, _pFile: String, pFlags: Int = 0) = readFile(context::class.java.classLoader.getResource(_pFile).toURI(), pFlags)

    fun readFile(_pFile: URI, pFlags: Int = 0): AiScene? {

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

            ScenePreprocessor(pimpl.mScene!!).processScene()
        }

        return pimpl.mScene
    }
}

