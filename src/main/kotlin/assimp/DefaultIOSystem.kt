package assimp

import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DefaultIOSystem : IOSystem {

    override fun exists(file: String) = File(file).exists()

    override fun open(file: String): IOStream {

        val path: Path = Paths.get(file)
        if (!Files.exists(path))
            throw IOException("File doesn't exist: $file")


        return FileIOStream(path)
    }

    class FileIOStream(override val path: Path) : IOStream {

        override fun read() = FileInputStream(path.toFile())

        override fun reader() = BufferedReader(FileReader(path.toFile()))

        override val filename: String
            get() = path.fileName.toString()

        override fun parentPath() = path.parent.toAbsolutePath().toString()

        override val length: Long
            get() = path.toFile().length()

        override fun readBytes(): ByteBuffer {
            RandomAccessFile(path.toFile(), "r").channel.use {fileChannel ->
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())
            }
        }
    }
}