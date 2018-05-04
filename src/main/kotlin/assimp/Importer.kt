/*
---------------------------------------------------------------------------
Open Asset Import Library (assimp)
---------------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team


All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the following
conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
---------------------------------------------------------------------------
*/

package assimp

import assimp.format.ProgressHandler
import assimp.postProcess.OptimizeMeshes
import assimp.postProcess.ValidateDSProcess
import glm_.BYTES
import glm_.i
import glm_.size
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KMutableProperty0
import assimp.AiPostProcessStep as Pps

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
class Importer
/** Constructor. Creates an empty importer object.
 *
 * Call readFile() to start the import process. The configuration property table is initially empty.
 */
constructor() {

    // Just because we don't want you to know how we're hacking around.
    internal val impl = ImporterPimpl() // allocate the pimpl first

    fun impl() = impl

    /** Copy constructor.
     *
     * This copies the configuration properties of another Importer.
     * If this Importer owns a scene it won't be copied.
     * Call readFile() to start the import process.
     */
    constructor(other: Importer) : this() {
        impl.properties += other.impl.properties
    }

    /** Registers a new loader.
     *
     *  @param imp Importer to be added. The Importer instance takes ownership of the pointer, so it will be
     *  automatically deleted with the Importer instance.
     *  @return AI_SUCCESS if the loader has been added. The registration fails if there is already a loader for a
     *  specific file extension.
     */
    fun registerLoader(imp: BaseImporter): AiReturn {
        /*  --------------------------------------------------------------------
            Check whether we would have two loaders for the same file extension
            This is absolutely OK, but we should warn the developer of the new loader that his code will probably never
            be called if the first loader is a bit too lazy in his file checking.
            --------------------------------------------------------------------    */
        val st = imp.extensionList
        var baked = ""
        st.forEach {
            if (ASSIMP.DEBUG && isExtensionSupported(it))
                logger.warn { "The file extension $it is already in use" }
            baked += "$it "
        }
        // add the loader
        impl.importer.add(imp)
        logger.info { "Registering custom importer for these file extensions: $baked" }
        return AiReturn.SUCCESS
    }

    /** Unregisters a loader.
     *
     *  @param imp Importer to be unregistered.
     *  @return AI_SUCCESS if the loader has been removed. The function fails if the loader is currently in use (this
     *  could happen if the Importer instance is used by more than one thread) or if it has not yet been registered.
     */
    fun unregisterLoader(imp: BaseImporter) = when (impl.importer.remove(imp)) {
        true -> logger.info { "Unregistering custom importer: " }.let { AiReturn.SUCCESS }
        else -> logger.warn { "Unable to remove custom importer: I can't find you ..." }.let { AiReturn.FAILURE }
    }

    /** Registers a new post-process step.
     *
     *  At the moment, there's a small limitation: new post processing steps are added to end of the list, or in other
     *  words, executed last, after all built-in steps.
     *  @param imp Post-process step to be added. The Importer instance takes ownership of the pointer, so it will be
     *  automatically deleted with the Importer instance.
     *  @return AI_SUCCESS if the step has been added correctly.
     */
    fun registerPPStep(imp: BaseProcess): AiReturn {
        impl.postProcessingSteps.add(imp)
        logger.info { "Registering custom post-processing step" }
        return AiReturn.SUCCESS
    }

    /** Unregisters a post-process step.
     *
     *  @param imp Step to be unregistered.
     *  @return AI_SUCCESS if the step has been removed. The function fails if the step is currently in use (this could happen
     *   if the #Importer instance is used by more than one thread) or
     *   if it has not yet been registered.
     */
    fun unregisterPPStep(imp: BaseProcess) = when (impl.postProcessingSteps.remove(imp)) {
        true -> logger.info { "Unregistering custom post-processing step" }.let { AiReturn.SUCCESS }
        else -> logger.warn { "Unable to remove custom post-processing step: I can't find you .." }.let { AiReturn.FAILURE }
    }

    operator fun <T : Any> set(szName: String, value: T) = impl.properties.put(superFastHash(szName), value)
    inline operator fun <reified T> get(szName: String): T? = impl().properties[superFastHash(szName)] as? T

    var prograssHandler: ProgressHandler?
        /** Retrieves the progress handler that is currently set.
         *  You can use #IsDefaultProgressHandler() to check whether the returned interface is the default handler
         *  provided by ASSIMP. The default handler is active as long the application doesn't supply its own custom
         *  handler via setProgressHandler().
         *  @return A valid ProgressHandler interface, never null.
         */
        get() = impl.progressHandler
        /** Supplies a custom progress handler to the importer. This interface exposes a update() callback, which is
         *  called more or less periodically (please don't sue us if it isn't as periodically as you'd like it to have
         *  ...).
         *  This can be used to implement progress bars and loading timeouts.
         *  @param value Progress callback interface. Pass null to disable progress reporting.
         *  @note Progress handlers can be used to abort the loading at almost any time.*/
        set(value) { // If the new handler is zero, allocate a default implementation.
            if (value == null) { // Release pointer in the possession of the caller
                impl.progressHandler = DefaultProgressHandler()
                impl.isDefaultProgressHandler = true
            } else if (impl.progressHandler != value) { // Otherwise register the custom handler
                impl.progressHandler = value
                impl.isDefaultProgressHandler = false
            }
        }

    var ioHandler: IOSystem
        get() = impl.ioSystem
        set(value) {
            if (value != null) impl.ioSystem = value
        }

    /** Checks whether a default progress handler is active
     *  A default handler is active as long the application doesn't supply its own custom progress handler via
     *  setProgressHandler().
     *  @return true by default
     */
    val isDefaultProgressHandler get() = impl.isDefaultProgressHandler

    /** @brief Check whether a given set of post-processing flags is supported.
     *
     *  Some flags are mutually exclusive, others are probably not available because your excluded them from your
     *  Assimp builds. Calling this function is recommended if you're unsure.
     *
     *  @param pFlags Bitwise combination of the aiPostProcess flags.
     *  @return true if this flag combination is fine.
     */
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

    /** Get the currently set progress handler  */
    val progressHandler get() = impl.progressHandler

    @JvmOverloads
    fun readFile(url: URL, flags: AiPostProcessStepsFlags = 0) = readFile(url.toURI(), flags)
    @JvmOverloads
    fun readFile(uri: URI, flags: AiPostProcessStepsFlags = 0) = readFile(Paths.get(uri), flags)
    @JvmOverloads
    fun readFile(path: Path, flags: AiPostProcessStepsFlags = 0) = readFile(path.toAbsolutePath().toString(), flags)
    fun readFile(file: String, flags: AiPostProcessStepsFlags = 0) = readFile(file, ioHandler, flags)

    /** Reads the given file and returns its contents if successful.
     *
     *  If the call succeeds, the contents of the file are returned as a pointer to an AiScene object. The returned data
     *  is intended to be read-only, the importer object keeps ownership of the data and will destroy it upon
     *  destruction. If the import fails, null is returned.
     *  A human-readable error description can be retrieved by accessing errorString. The previous scene will be deleted
     *  during this call.
     *  @param file Path and filename to the file to be imported.
     *  @param flags Optional post processing steps to be executed after a successful import. Provide a bitwise
     *  combination of the AiPostProcessSteps flags. If you wish to inspect the imported scene first in order to
     *  fine-tune your post-processing setup, consider to use applyPostProcessing().
     *  @return A pointer to the imported data, null if the import failed.
     *  The pointer to the scene remains in possession of the Importer instance. Use getOrphanedScene() to take
     *  ownership of it.
     *
     * @note Assimp is able to determine the file format of a file automatically.
     */
    @JvmOverloads
    fun readFile(file: String, ioSystem: IOSystem = this.ioHandler, flags: AiPostProcessStepsFlags = 0): AiScene? {

        writeLogOpening(file)

        // Check whether this Importer instance has already loaded a scene. In this case we need to delete the old one
        if (impl.scene != null) {
            logger.debug { "(Deleting previous scene)" }
            freeScene()
        }

        // First check if the file is accessible at all
        // handled by exception in IOSystem
        /*if (!file.exists()) {
            impl.errorString = "Unable to open file \"$file\"."
            logger.error { impl.errorString }
            return null
        }*/

//        TODO std::unique_ptr<Profiler> profiler(GetPropertyInteger(AI_CONFIG_GLOB_MEASURE_TIME,0)?new Profiler():NULL);
//        if (profiler) {
//            profiler->BeginRegion("total");
//        }

        // Find an worker class which can handle the file
        val imp = impl.importer.find { it.canRead(file, ioHandler, false) }

        if (imp == null) {
            // not so bad yet ... try format auto detection.
            TODO()
//            const std::string::size_type s = pFile.find_last_of('.');
//            if (s != std::string::npos) {
//                DefaultLogger::get()->info("File extension not known, trying signature-based detection");
//                for( unsigned int a = 0; a < pimpl->mImporter.size(); a++)  {
//
//                    if( pimpl->mImporter[a]->CanRead( pFile, pimpl->mIOHandler, true)) {
//                    imp = pimpl->mImporter[a];
//                    break;
//                }
//                }
//            }
//            // Put a proper error message if no suitable importer was found
//            if( !imp)   {
//                pimpl->mErrorString = "No suitable reader found for the file format of file \"" + pFile + "\".";
//                DefaultLogger::get()->error(pimpl->mErrorString);
//                return NULL;
//            }
        }

        // Get file size for progress handler
        val fileSize = File(file).length().i

        // Dispatch the reading to the worker class for this format
        val desc = imp.info
        val ext = desc.name
        logger.info { "Found a matching importer for this file format: $ext." }
        impl.progressHandler.updateFileRead(0, fileSize)

//        if (profiler) { TODO
//            profiler->BeginRegion("import");
//        }

        impl.scene = imp.readFile(this, ioHandler, file)
        impl.progressHandler.updateFileRead(fileSize, fileSize)

//        if (profiler) { TODO
//            profiler->EndRegion("import");
//        }

        // If successful, apply all active post processing steps to the imported data
        if (impl.scene != null) {

            if (!ASSIMP.NO.VALIDATEDS_PROCESS)
            // The ValidateDS process is an exception. It is executed first, even before ScenePreprocessor is called.
                if (flags has Pps.ValidateDataStructure) {
                    ValidateDSProcess().executeOnScene(this)
                    if (impl.scene == null) return null
                }
            // Preprocess the scene and prepare it for post-processing
//            if (profiler) profiler.BeginRegion("preprocess")

            ScenePreprocessor.processScene(impl.scene!!)

//            if (profiler) profiler.EndRegion("preprocess")

            // Ensure that the validation process won't be called twice
            applyPostProcessing(flags wo Pps.ValidateDataStructure)
        }
        // if failed, extract the error string
        else if (impl.scene == null)
            impl.errorString = imp.errorText
//        if (profiler) { profiler ->
//            EndRegion("total");
//        }
        return impl.scene
    }

    /** Reads the given file from a memory buffer and returns its contents if successful.
     *
     *  If the call succeeds, the contents of the file are returned as a pointer to an AiScene object. The returned data
     *  is intended to be read-only, the importer object keeps ownership of the data and will destroy it upon
     *  destruction. If the import fails, null is returned.
     *  A human-readable error description can be retrieved by accessing errorString. The previous scene will be deleted
     *  during this call.
     *  Calling this method doesn't affect the active IOSystem.
     *  @param buffer Pointer to the file data
     *  @param flags Optional post processing steps to be executed after a successful import. Provide a bitwise
     *  combination of the AiPostProcessSteps flags. If you wish to inspect the imported scene first in order to
     *  fine-tune your post-processing setup, consider to use applyPostProcessing().
     *  @param hint An additional hint to the library. If this is a non empty string, the library looks for a loader to
     *  support the file extension specified by hint and passes the file to the first matching loader. If this loader is
     *  unable to completely the request, the library continues and tries to determine the file format on its own, a
     *  task that may or may not be successful.
     *  Check the return value, and you'll know ...
     *  @return A pointer to the imported data, null if the import failed.
     *  The pointer to the scene remains in possession of the Importer instance. Use getOrphanedScene() to take
     *  ownership of it.
     *
     *  @note This is a straightforward way to decode models from memory buffers, but it doesn't handle model formats
     *  that spread their data across multiple files or even directories. Examples include OBJ or MD3, which outsource
     *  parts of their material info into external scripts. If you need full functionality, provide a custom IOSystem
     *  to make Assimp find these files and use the regular readFile() API.
     */
    fun readFileFromMemory(buffer: ByteBuffer, flags: Int, hint: String = ""): AiScene? {
        if (buffer.size == 0 || hint.length > MaxLenHint) {
            impl.errorString = "Invalid parameters passed to ReadFileFromMemory()"
            return null
        }
        TODO()
        // read the file and recover the previous IOSystem
//        static const size_t BufferSize(Importer::MaxLenHint + 28);
//        char fbuff[ BufferSize ];
//        ai_snprintf(fbuff, BufferSize, "%s.%s",AI_MEMORYIO_MAGIC_FILENAME,pHint);
//        ReadFile(fbuff, pFlags)
//        SetIOHandler(io)
//
//        ASSIMP_END_EXCEPTION_REGION(const aiScene *)
//        return pimpl->mScene
    }

    /** Apply post-processing to an already-imported scene.
     *
     *  This is strictly equivalent to calling readFile() with the same flags. However, you can use this separate
     *  function to inspect the imported scene first to fine-tune your post-processing setup.
     *  @param flags Provide a bitwise combination of the AiPostProcessSteps flags.
     *  @return A pointer to the post-processed data. This is still the same as the pointer returned by readFile().
     *  However, if post-processing fails, the scene could now be null.
     *  That's quite a rare case, post processing steps are not really designed to 'fail'. To be exact, the
     *  AiProcess_ValidateDS flag is currently the only post processing step which can actually cause the scene to be
     *  reset to null.
     *
     *  @note The method does nothing if no scene is currently bound to the Importer instance.  */
    fun applyPostProcessing(flags: Int): AiScene? {
        // Return immediately if no scene is active
        if (impl.scene == null) return null
        // If no flags are given, return the current scene with no further action
        if (flags == 0) return impl.scene
        // In debug builds: run basic flag validation
        assert(_validateFlags(flags))
        logger.info("Entering post processing pipeline")
        if (!ASSIMP.NO.VALIDATEDS_PROCESS)
        /*  The ValidateDS process plays an exceptional role. It isn't contained in the global list of post-processing
            steps, so we need to call it manually.         */
            if (flags has Pps.ValidateDataStructure) {
                ValidateDSProcess().executeOnScene(this)
                if (impl.scene == null) return null
            }
        if (flags has Pps.OptimizeMeshes) {
            OptimizeMeshes().executeOnScene(this)
            if (impl.scene == null) return null
        }
        var flags = flags
        if (ASSIMP.DEBUG) {
            if (impl.extraVerbose) {
                if (ASSIMP.NO.VALIDATEDS_PROCESS)
                    logger.error { "Verbose Import is not available due to build settings" }
                flags = flags or Pps.ValidateDataStructure
            }
        } else if (impl.extraVerbose)
            logger.warn("Not a debug build, ignoring extra verbose setting")

//        std::unique_ptr<Profiler> profiler (GetPropertyInteger(AI_CONFIG_GLOB_MEASURE_TIME, 0)?new Profiler():NULL); TODO
        for (a in impl.postProcessingSteps.indices) {
            val process = impl.postProcessingSteps[a]
            impl.progressHandler.updatePostProcess(a, impl.postProcessingSteps.size)
            if (process.isActive(flags)) {
//                if (profiler) { profiler -> TODO
//                    BeginRegion("postprocess")
//                }
                process.executeOnScene(this)
//            if (profiler) { profiler ->
//                    EndRegion("postprocess")
//                }
            }
            if (impl.scene == null) break
            if (ASSIMP.DEBUG) {
                if (ASSIMP.NO.VALIDATEDS_PROCESS) continue
                // If the extra verbose mode is active, execute the ValidateDataStructureStep again - after each step
                if (impl.extraVerbose) {
                    logger.debug { "Verbose Import: revalidating data structures" }
                    ValidateDSProcess().executeOnScene(this)
                    if (impl.scene == null) {
                        logger.error { "Verbose Import: failed to revalidate data structures" }
                        break
                    }
                }
            }
        }
        impl.progressHandler.updatePostProcess(impl.postProcessingSteps.size, impl.postProcessingSteps.size)
        // update private scene flags
        if (impl.scene != null)
//        scenePriv(pimpl->mScene)->mPPStepsApplied | = pFlags TODO
            logger.info { "Leaving post processing pipeline" }
        return impl.scene
    }

    fun applyCustomizedPostProcessing(rootProcess: BaseProcess?, requestValidation: Boolean): AiScene? {
        // Return immediately if no scene is active
        if (null == impl.scene) return null
        // If no flags are given, return the current scene with no further action
        if (null == rootProcess) return impl.scene
        // In debug builds: run basic flag validation
        logger.info { "Entering customized post processing pipeline" }
        if (!ASSIMP.NO.VALIDATEDS_PROCESS) {
            // The ValidateDS process plays an exceptional role. It isn't contained in the global
            // list of post-processing steps, so we need to call it manually.
            if (requestValidation) {
                ValidateDSProcess().executeOnScene(this)
                if (impl.scene == null) return null
            }
        }
        if (ASSIMP.DEBUG && impl.extraVerbose && ASSIMP.NO.VALIDATEDS_PROCESS)
            logger.error { "Verbose Import is not available due to build settings" }
        else if (impl.extraVerbose)
            logger.warn { "Not a debug build, ignoring extra verbose setting" }

//        std::unique_ptr<Profiler> profiler (GetPropertyInteger(AI_CONFIG_GLOB_MEASURE_TIME, 0) ? new Profiler() : NULL);
//        if (profiler) { profiler ->
//            BeginRegion("postprocess");
//        }
        rootProcess.executeOnScene(this)
//        if (profiler) { profiler ->
//            EndRegion("postprocess")
//        }
        // If the extra verbose mode is active, execute the ValidateDataStructureStep again - after each step
        if (impl.extraVerbose || requestValidation) {
            logger.debug { "Verbose Import: revalidating data structures" }
            ValidateDSProcess().executeOnScene(this)
            if (impl.scene == null)
                logger.error { "Verbose Import: failed to revalidate data structures" }
        }
        logger.info { "Leaving customized post processing pipeline" }
        return impl.scene
    }

    /** Frees the current scene.
     *
     *  The function does nothing if no scene has previously been read via readFile(). freeScene() is called
     *  automatically by the destructor and readFile() itself.  */
    fun freeScene() {
        impl.scene = null
        impl.errorString = ""
    }

    /** Returns an error description of an error that occurred in ReadFile().
     *
     *  Returns an empty string if no error occurred.
     *  @return A description of the last error, an empty string if no error occurred. The string is never null.
     *
     *  @note The returned function remains valid until one of the following methods is called: readFile(),
     *  freeScene(). */
    val errorString get() = impl.errorString

    /** Returns the scene loaded by the last successful call to readFile()
     *
     *  @return Current scene or null if there is currently no scene loaded */
    val scene get() = impl.scene

    /** Returns the scene loaded by the last successful call to readFile() and releases the scene from the ownership of
     *  the Importer instance. The application is now responsible for deleting the scene. Any further calls to `scene`
     *  or `orphanedScene` will return null - until a new scene has been loaded via readFile().
     *
     *  @return Current scene or null if there is currently no scene loaded
     *  @note Use this method with maximal caution, and only if you have to.
     *  By design, AiScene's are exclusively maintained, allocated and deallocated by Assimp and no one else. The
     *  reasoning behind this is the golden rule that deallocations should always be done by the module that did the
     *  original allocation because heaps are not necessarily shared. `orphanedScene` enforces you to delete the
     *  returned scene by yourself, but this will only be fine if and only if you're using the same heap as assimp.
     *  On Windows, it's typically fine provided everything is linked against the multithreaded-dll version of the
     *  runtime library.
     *  It will work as well for static linkage with Assimp.    */
    val orphanedScene: AiScene?
        get() {
            val s = impl.scene
            impl.scene = null
            impl.errorString = "" /* reset error string */
            return s
        }

    /** Returns whether a given file extension is supported by ASSIMP.
     *
     *  @param szExtension Extension to be checked.
     *  Must include a trailing dot '.'. Example: ".3ds", ".md3". Cases-insensitive.
     *  @return true if the extension is supported, false otherwise */
    fun isExtensionSupported(szExtension: String) = null != getImporter(szExtension)

    /** Get a full list of all file extensions supported by ASSIMP.
     *
     *  If a file extension is contained in the list this does of course not mean that ASSIMP is able to load all files
     *  with this extension --- it simply means there is an importer loaded which claims to handle files with this
     *  file extension.
     *  @return String containing the extension list.
     *  Format of the list: "*.3ds;*.obj;*.dae". This is useful for use with the WinAPI call GetOpenFileName(Ex). */
    val extensionList get() = impl.importer.joinToString("", "*.", ";").substringBeforeLast(';')

    /** Get the number of importers currently registered with Assimp. */
    val importerCount get() = impl.importer.size

    /** Get meta data for the importer corresponding to a specific index..
     *
     *  @param index Index to query, must be within [0, importerCount)
     *  @return Importer meta data structure, null if the index does not exist or if the importer doesn't offer meta
     *  information (importers may do this at the cost of being hated by their peers).  TODO JVM DOESNT ALLOW THIS */
    fun getImporterInfo(index: Int) = impl.importer[index].info

    /** Find the importer corresponding to a specific index.
     *
     *  @param index Index to query, must be within [0, importerCount)
     *  @return Importer instance. null if the index does not exist. */
    fun getImporter(index: Int) = impl.importer.getOrNull(index)

    /** Find the importer corresponding to a specific file extension.
     *
     *  This is quite similar to `isExtensionSupported` except a BaseImporter instance is returned.
     *  @param szExtension Extension to check for. The following formats are recognized (BAH being the file extension):
     *  "BAH" (comparison is case-insensitive), ".bah", "*.bah" (wild card and dot characters at the beginning of the
     *  extension are skipped).
     *  @return null if no importer is found*/
    fun getImporter(szExtension: String) = getImporter(getImporterIndex(szExtension))

    /** Find the importer index corresponding to a specific file extension.
     *
     *  @param szExtension Extension to check for. The following formats are recognized (BAH being the file extension):
     *  "BAH" (comparison is case-insensitive), ".bah", "*.bah" (wild card and dot characters at the beginning of the
     *  extension are skipped).
     *  @return -1 if no importer is found */
    fun getImporterIndex(szExtension: String): Int {
        assert(szExtension.isNotEmpty())
        // skip over wildcard and dot characters at string head --
        var p = 0
        while (szExtension[p] == '*' || szExtension[p] == '.') ++p
        var ext = szExtension.substring(p)
        if (ext.isEmpty()) return -1
        ext = ext.toLowerCase()
        impl.importer.forEach { i ->
            i.extensionList.forEach {
                if (ext == it) return impl.importer.indexOf(i)
            }
        }
        return -1
    }

    /** Returns the storage allocated by ASSIMP to hold the scene data in memory.
     *
     *  This refers to the currently loaded file, see readFile().
     *  @param in Data structure to be filled.
     *  @note The returned memory statistics refer to the actual size of the use data of the AiScene. Heap-related
     *  overhead is (naturally) not included.*/
    val memoryRequirements: AiMemoryInfo
        get() {
            val mem = AiMemoryInfo()
            val scene = impl.scene ?: return mem
            // return if we have no scene loaded
            mem.total = AiScene.size
            // add all meshes
            repeat(scene.numMeshes) { i ->
                mem.meshes += AiMesh.size
                if (scene.meshes[i].hasPositions)
                    mem.meshes += AiVector3D.size * scene.meshes[i].numVertices
                if (scene.meshes[i].hasNormals)
                    mem.meshes += AiVector3D.size * scene.meshes[i].numVertices
                if (scene.meshes[i].hasTangentsAndBitangents)
                    mem.meshes += AiVector3D.size * scene.meshes[i].numVertices * 2
                for (a in 0 until AI_MAX_NUMBER_OF_COLOR_SETS)
                    if (scene.meshes[i].hasVertexColors(a))
                        mem.meshes += AiColor4D.size * scene.meshes[i].numVertices
                    else break
                for (a in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS)
                    if (scene.meshes[i].hasTextureCoords(a))
                        mem.meshes += AiVector3D.size * scene.meshes[i].numVertices
                    else break
                if (scene.meshes[i].hasBones) {
                    for (p in 0 until scene.meshes[i].numBones) {
                        mem.meshes += AiBone.size
                        mem.meshes += scene.meshes[i].bones[p].numWeights * AiVertexWeight.size
                    }
                }
                mem.meshes += (3 * Int.BYTES) * scene.meshes[i].numFaces
            }
            mem.total += mem.meshes
            // add all embedded textures
            for (i in 0 until scene.numTextures) {
                val pc = scene.textures.values.elementAt(i)
                mem.textures += AiTexture.size
                mem.textures += with(pc.extent()) { if (y != 0) 4 * y * x else x }
            }
            mem.total += mem.textures
            // add all animations
            for (i in 0 until scene.numAnimations) {
                val pc = scene.animations[i]
                mem.animations += AiAnimation.size
                // add all bone anims
                for (a in 0 until pc.numChannels) {
                    val pc2 = pc.channels[i]!!
                    mem.animations += AiNodeAnim.size
                    mem.animations += pc2.numPositionKeys * AiVectorKey.size
                    mem.animations += pc2.numScalingKeys * AiVectorKey.size
                    mem.animations += pc2.numRotationKeys * AiQuatKey.size
                }
            }
            mem.total += mem.animations
            // add all cameras and all lights
            mem.cameras = AiCamera.size * scene.numCameras
            mem.total += mem.cameras
            mem.lights = AiLight.size * scene.numLights
            mem.total += mem.lights

            // add all nodes
            addNodeWeight(mem::nodes, scene.rootNode)
            mem.total += mem.nodes

            // add all materials
            for (i in 0 until scene.numMaterials)
                mem.materials += AiMaterial.size
            mem.total += mem.materials
            return mem
        }

    /** Enables "extra verbose" mode.
     *
     * 'Extra verbose' means the data structure is validated after *every* single post processing step to make sure
     *  everyone modifies the data structure in a well-defined manner. This is a debug feature and not intended for
     *  use in production environments. */
    fun setExtraVerbose(verbose: Boolean) {
        impl.extraVerbose = verbose
    }

    private fun writeLogOpening(file: String) {

        logger.info { "Load $file" }

        /*  print a full version dump. This is nice because we don't need to ask the authors of incoming bug reports for
            the library version they're using - a log dump is sufficient.   */
        val flags = compileFlags
        logger.debug {
            var message = "Assimp $versionMajor.$versionMinor.$versionRevision"
            if (ASSIMP.DEBUG) message += " debug"
        }
    }

    private fun _validateFlags(flags: Int) = when {
        flags has Pps.GenSmoothNormals && flags has Pps.GenNormals -> {
            logger.error { "AiProcess_GenSmoothNormals and AiProcess_GenNormals are incompatible" }
            false
        }
        flags has Pps.OptimizeGraph && flags has Pps.PreTransformVertices -> {
            logger.error { "AiProcess_OptimizeGraph and AiProcess_PreTransformVertices are incompatible" }
            false
        }
        else -> true
    }

    /** Get the memory requirements of a single node    */
    private fun addNodeWeight(scene: KMutableProperty0<Int>, node: AiNode) {
        scene.set(scene() + AiNode.size)
        scene.set(scene() + Int.BYTES * node.numMeshes)
        for (i in 0 until node.numChildren)
            addNodeWeight(scene, node.children[i])
    }

    companion object {
        /** The upper limit for hints. */
        val MaxLenHint = 200
    }
}

