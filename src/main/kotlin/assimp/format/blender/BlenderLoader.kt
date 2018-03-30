package assimp.format.blender

import assimp.*
import glm_.c
import glm_.i
import glm_.toUnsignedInt
import uno.kotlin.parseInt
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
    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {

        val extension = getExtension(file)
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

    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        val fileChannel = RandomAccessFile(File(file), "r").channel
        buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        var match = buffer.strncmp(tokens)
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
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).order(ByteOrder.nativeOrder())
            // .. and retry
            match = buffer.strncmp(tokens)
            if (!match) throw Error("Found no BLENDER magic word in decompressed GZIP file")
            buffer.pos += tokens.length
        }

        val db = FileDatabase().apply {
            i64bit = buffer.get().c == '-'
            little = buffer.get().c == 'v'
        }
        val major = buffer.get().c.parseInt()
        val minor = buffer.get().c.parseInt() * 10 + buffer.get().c.parseInt()
        logger.info("Blender version is $major.$minor (64bit: ${db.i64bit}, little endian: ${db.little}")

        db.reader = buffer

        parseBlendFile(db)

//        Scene scene;
//        ExtractScene(scene,file);
//
//        ConvertBlendFile(pScene,scene,file);
    }

    fun parseBlendFile(out: FileDatabase) {

        val dnaReader = DnaParser(out)
        var dna: DNA? = null

        out.entries.ensureCapacity(128)
        // even small BLEND files tend to consist of many file blocks
        val parser = SectionParser(out.reader, out.i64bit)

        // first parse the file in search for the DNA and insert all other sections into the database
        while (true) {
            parser.next()
            val head = FileBlockHead(parser.current)

            if (head.id == "ENDB")
                break // only valid end of the file
            else if (head.id == "DNA1") {
                dnaReader.parse()
                dna = dnaReader.dna
                continue
            }

            out.entries += head
        }
        if (dna == null) throw Error("SDNA not found")

//        std::sort(out.entries.begin(), out.entries.end());
    }
}