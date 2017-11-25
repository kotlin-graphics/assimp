package assimp

import assimp.format.AiConfig
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

    val properties = mutableMapOf<Int, Any>()
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

/** FOR IMPORTER PLUGINS ONLY: A helper class to the pleasure of importers that need to load many external meshes
 *  recursively.
 *
 *  The class uses several threads to load these meshes (or at least it could, this has not yet been implemented
 *  at the moment).
 *
 *  @note The class may not be used by more than one thread */
class BatchLoader {

    /** Wraps a full list of configuration properties for an importer.
     *  Properties can be set using SetGenericProperty */
    lateinit var config: AiConfig

    /** Sets the validation step. True for enable validation during postprocess.
     *  @param  enable  True for validation.     */
//    void setValidation( bool enabled );

    /** Returns the current validation step.
     *  @return The current validation step.     */
//    bool getValidation() const ;

    /** Add a new file to the list of files to be loaded.
     *  @param file File to be loaded
     *  @param steps Post-processing steps to be executed on the file
     *  @param map Optional configuration properties
     *  @return 'Load request channel' - an unique ID that can later
     *    be used to access the imported file data.
     *  @see GetImport */
//    unsigned int AddLoadRequest (
//    const std::string& file,
//    unsigned int steps = 0,
//    const PropertyMap* map = NULL
//    );
//
//    // -------------------------------------------------------------------
//    /** Get an imported scene.
//     *  This polls the import from the internal request list.
//     *  If an import is requested several times, this function
//     *  can be called several times, too.
//     *
//     *  @param which LRWC returned by AddLoadRequest().
//     *  @return NULL if there is no scene with this file name
//     *  in the queue of the scene hasn't been loaded yet. */
//    aiScene* GetImport(
//    unsigned int which
//    );
//
//    // -------------------------------------------------------------------
//    /** Waits until all scenes have been loaded. This returns
//     *  immediately if no scenes are queued.*/
//    void LoadAll();
//
//    private :
//    // No need to have that in the public API ...
//    BatchData *m_data;
}

/** BatchLoader::pimpl data structure   */
//class BatchData {
//    /** Importer used to load all meshes    */
//    var importer: Importer? = null
//    /** List of all imports */
//    var requests = ArrayList<LoadRequest>()
//
//    // Base path
//    std::string pathBase;
//
//    // Id for next item
//    unsigned int next_id;
//
//    // Validation enabled state
//    bool validate;
//    BatchData( IOSystem* pIO, bool validate )
//    : pIOSystem( pIO )
//    , pImporter( nullptr )
//    , next_id(0xffff)
//    , validate( validate ) {
//        ai_assert( NULL != pIO );
//
//        pImporter = new Importer();
//        pImporter->SetIOHandler( pIO );
//    }
//
//    ~BatchData() {
//        pImporter->SetIOHandler( NULL ); /* get pointer back into our possession */
//        delete pImporter;
//    }
//}
//
///** Represents an import request    *//
//class LoadRequest(
//    val file:String,
//    val         flags:Int,
//    aiScene                 *scene;
//    bool                     loaded;
//    BatchLoader::PropertyMap map;
//    unsigned int             id){
//
//    unsigned int             refCnt;
//    LoadRequest(const std::string& _file, unsigned int _flags,const BatchLoader::PropertyMap* _map, unsigned int _id)
//    : file(_file)
//    , flags(_flags)
//    , refCnt(1)
//    , scene(NULL)
//    , loaded(false)
//    , id(_id) {
//        if ( _map ) {
//            map = *_map;
//        }
//    }
//
//    bool operator== ( const std::string& f ) const {
//        return file == f;
//    }
//}