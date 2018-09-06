package assimp

import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.file.Path
import java.nio.file.Paths

class DefaultIOSystem : IOSystem {

    override fun exists(file: String) = File(file).exists()

    override fun open(file: String): IOStream {

        val path: Path = Paths.get(file)
        if (!exists(file))
            throw IOException("File doesn't exist: $file")

        return FileIOStream(path, this)
    }

    class FileIOStream(private val pathObject: Path, override val osSystem: DefaultIOSystem) : IOStream {

        override val path: String
            get() =  pathObject.toString()

        override fun read() = FileInputStream(file)

        override fun reader() = BufferedReader(FileReader(file))

        override val filename: String
            get() = pathObject.fileName.toString()

        override val parentPath = pathObject.parent.toAbsolutePath().toString()

        override val length: Long
            get() = file.length()

        override fun readBytes(): ByteBuffer {
            RandomAccessFile(file, "r").channel.use {fileChannel ->
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())
            }
        }

        val file: File
            get() = pathObject.toFile()
    }
}