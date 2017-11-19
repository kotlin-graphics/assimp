package assimp

/**
 * Created by elect on 14/11/2016.
 */

/** Mixed set of flags for #AiImporterDesc, indicating some features
 *  common to many importers*/
enum class AiImporterFlags(val i: Int) {

    /** Indicates that there is a textual encoding of the file format; and that it is supported.*/
    SupportTextFlavour(0x1),

    /** Indicates that there is a binary encoding of the file format; and that it is supported.*/
    SupportBinaryFlavour(0x2),

    /** Indicates that there is a compressed encoding of the file format; and that it is supported.*/
    SupportCompressedFlavour(0x4),

    /** Indicates that the importer reads only a very particular subset of the file format. This happens commonly for
     * declarative or procedural formats which cannot easily be mapped to #aiScene */
    LimitedSupport(0x8),

    /** Indicates that the importer is highly experimental and should be used with care. This only happens for trunk
     * (i.e. SVN) versions, experimental code is not included in releases. */
    Experimental(0x10);

    infix fun or(other: AiImporterFlags) = i or other.i
    infix fun or(other: Int) = i or other
}

/** Meta information about a particular importer. Importers need to fill this structure, but they can freely decide how
 *  talkative they are.
 *  A common use case for loader meta info is a user interface in which the user can choose between various
 *  import/export file formats. Building such an UI by hand means a lot of maintenance as importers/exporters are added
 *  to Assimp, so it might be useful to have a common mechanism to query some rough importer characteristics. */
class AiImporterDesc(
        /** Full name of the importer (i.e. Blender3D importer)*/
        val name: String = "",
        /** Original author (left blank if unknown or whole assimp team) */
        val author: String = "",
        /** Current maintainer, left blank if the author maintains */
        val maintainer: String = "",
        /** Implementation comments, i.e. unimplemented features*/
        val comments: String = "",
        /** These flags indicate some characteristics common to many importers. */
        val flags: Int = 0,
        /** Minimum format version that can be loaded im major.minor format, both are set to 0 if there is either no
         *  version scheme or if the loader doesn't care. */
        val minMajor: Int = 0,
        val minMinor: Int = 0,
        /** Maximum format version that can be loaded im major.minor format, both are set to 0 if there is either no
         *  version scheme or if the loader doesn't care. Loaders that expect to be forward-compatible to potential
         *  future format versions should indicate  zero, otherwise they should specify the current maximum version.*/
        val maxMajor: Int = 0,
        val maxMinor: Int = 0,
        /** List of file extensions this importer can handle.
         *  All entries are lower case without a leading dot (i.e. ["xml","dae"] would be a valid value. Note that
         *  multiple importers may respond to the same file extension - assimp calls all importers in the order in which
         *  they are registered and each importer gets the opportunity to load the file until one importer "claims" the
         *  file.
         *  Apart from file extension checks, importers typically use other methods to quickly reject files (i.e. magic
         *  words) so this does not mean that common or generic file extensions such as XML would be tediously slow. */
        val fileExtensions: List<String> = ArrayList()
)