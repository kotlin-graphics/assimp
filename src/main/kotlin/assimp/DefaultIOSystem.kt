package assimp

import java.io.*

class DefaultIOSystem : IOSystem{
    override fun exists(pFile: String) = File(pFile).exists()

    override fun open(pFile: String): IOStream {
        var file = File(pFile)
        println(File(".").absolutePath)
        if(!file.exists())
            throw IOException("File doesn't exist: "+pFile)


        return FileIOStream(file)
    }

    class FileIOStream(val file: File) : IOStream{
        override fun read() = FileInputStream(file)

        override fun reader() = BufferedReader(FileReader(file))

        override val path: String
            get() = file.absolutePath

        override val filename: String
            get() = file.name

        override fun parentPath() = file.parentFile.absolutePath
    }
}