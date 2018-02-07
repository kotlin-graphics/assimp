package assimp.format.blender

import assimp.*
import glm_.c
import glm_.i
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.GZIPInputStream



lateinit var buffer: ByteBuffer

val tokens = "BLENDER"

class BlenderImporter : BaseImporter() {

    /** Returns whether the class can handle the format of the given file.  */
    override fun canRead(file: URI, checkSig: Boolean): Boolean {

        val extension = file.extension
        if (extension == "blend") return true
        else if (extension.isEmpty() || checkSig) {
            TODO()
            // note: this won't catch compressed files
//            return SearchFileHeaderForToken(pIOHandler,pFile, TokensForSearch,1);
        }
        return false
    }

    override val info
        get() = AiImporterDesc(
                name = "Blender 3D Importer \nhttp://www.blender3d.org",
                comments = "No animation support yet",
                flags = AiImporterFlags.SupportBinaryFlavour.i,
                minMajor = 0,
                minMinor = 0,
                maxMajor = 2,
                maxMinor = 50,
                fileExtensions = listOf("blend"))

    override fun internReadFile(file: URI, scene: AiScene) {

        val fileChannel = RandomAccessFile(File(file), "r").channel
        val input = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        var match = tokens.all { it == input.get().c }
        if (!match) {
            // Check for presence of the gzip header. If yes, assume it is a
            // compressed blend file and try uncompressing it, else fail. This is to
            // avoid uncompressing random files which our loader might end up with.

            val output = File("temp")

            GZIPInputStream(FileInputStream(File(file))).use { gzip ->
                FileOutputStream(output).use({ out ->
                    val buffer = ByteArray(1024)
                    var len = gzip.read(buffer)
                    while (len != -1) {
                        out.write(buffer, 0, len)
                        len = gzip.read(buffer)
                    }
                })
            }
            val fc = RandomAccessFile(output, "r").channel
            val inp = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).order(ByteOrder.nativeOrder())
            // .. and retry
            match = tokens.all { it == inp.get().c }
            if (!match) throw Error("Found no BLENDER magic word in decompressed GZIP file")
        }

        val f = FileDatabase().apply {
            i64bit = buffer.get().c == '-'
            little = buffer.get().c == 'v'
        }
        logger.info("Blender version is ${buffer.get().i}.${buffer.get().i} (64bit: ${f.i64bit}, little endian: ${f.little}")

        f.reader = input

        parseBlendFile(f)

//        Scene scene;
//        ExtractScene(scene,file);
//
//        ConvertBlendFile(pScene,scene,file);
    }

    fun parseBlendFile(out: FileDatabase) {

//        val dnaReader = DN(out);
//        const DNA* dna = NULL;
//
//        out.entries.reserve(128); { // even small BLEND files tend to consist of many file blocks
//        SectionParser parser(*out.reader.get(),out.i64bit);
//
//        // first parse the file in search for the DNA and insert all other sections into the database
//        while ((parser.Next(),1)) {
//            const FileBlockHead& head = parser.GetCurrent();
//
//            if (head.id == "ENDB") {
//                break; // only valid end of the file
//            }
//            else if (head.id == "DNA1") {
//                dnaReader.Parse();
//                dna = &dna_reader.GetDNA();
//                continue;
//            }
//
//            out.entries.push_back(head);
//        }
//    }
//        if (!dna) {
//            ThrowException("SDNA not found");
//        }
//
//        std::sort(out.entries.begin(),out.entries.end());
    }
}