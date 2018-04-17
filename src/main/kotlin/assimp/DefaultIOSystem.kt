package assimp

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DefaultIOSystem : IOSystem {

    override fun exists(pFile: String) = File(pFile).exists()

    override fun open(pFile: String): IOStream {

        val path: Path = Paths.get(pFile)
        if (!Files.exists(path))
            throw IOException("File doesn't exist: $pFile")


        return FileIOStream(path)
    }

    class FileIOStream(override val path: Path) : IOStream {

        override fun read() = FileInputStream(path.toFile())

        override fun reader() = BufferedReader(FileReader(path.toFile()))

        override val filename: String
            get() = path.fileName.toString()

        override fun parentPath() = path.parent.toAbsolutePath().toString()
    }
}