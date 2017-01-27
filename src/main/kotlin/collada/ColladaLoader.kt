package collada

import BaseImporter
import AiImporterDesc
import java.net.URI
import AiScene
import AiMesh
import AiMaterial
import s

/**
 * Created by elect on 23/01/2017.
 */

class ColladaLoader : BaseImporter() {

    companion object {

        val desc = AiImporterDesc(
                mName = "Collada Importer",
                mComments = "http://collada.org",
                mFlags = AiImporterFlags.SupportTextFlavour.i,
                mMinMajor = 1, mMinMinor = 3,
                mMaxMajor = 1, mMaxMinor = 5,
                mFileExtensions = "dae"
        )
    }

    /** Filename, for a verbose error message */
    lateinit var mFileName: URI

    /** Which mesh-material compound was stored under which mesh ID */
//    val mMeshIndexByID = mutableMapOf<ColladaMeshIndex, Int>()
//
//    /** Which material was stored under which index in the scene */
//    val mMaterialIndexByName = mutableMapOf<String, Int>()
//
//    /** Accumulated meshes for the target scene */
//    val mMeshes = ArrayList<AiMesh>()
//
//    /** Temporary material list */
//    val newMats = ArrayList<Pair<Collada.Effect, AiMaterial>>()
//
//    /** Temporary camera list */
//    val mCameras = ArrayList<AiCamera>
//
//    /** Temporary light list */
//    val mLights = ArrayList<AiLight>()
//
//    /** Temporary texture list */
//    val mTextures = ArrayList<AiTexture>
//
//    /** Accumulated animations for the target scene */
//    val mAnims = ArrayList<AiAnimation>

    val noSkeletonMesh = false
    val ignoreUpDirection = false

    /** Used by FindNameForNode() to generate unique node names */
    val mNodeNameCounter = 0

    // ------------------------------------------------------------------------------------------------
    // Returns whether the class can handle the format of the given file.
    override fun canRead(pFile: URI, checkSig: Boolean): Boolean {

        // check file extension
        val extension = pFile.s.substring(pFile.s.lastIndexOf('.') + 1)

        if (extension == "dae")
            return true

        // XML - too generic, we need to open the file and search for typical keywords
        if (extension == "xml" || extension.isEmpty() || checkSig) {
            //TODO
        }
        return false
    }

    // ------------------------------------------------------------------------------------------------
    // Imports the given file into the given scene structure.
    override fun internReadFile(pFile: URI, pScene: AiScene) {

        mFileName = pFile

        // parse the input file
        val parser = ColladaParser(pFile)
    }
}