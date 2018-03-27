package assimp

import java.io.*

class DefaultIOSystem : IOSystem{
    override fun Exists(pFile: String) = File(pFile).exists()

    override fun Open(pFile: String): IOStream {
        var file = File(pFile)
        println(File(".").absolutePath)
        if(!file.exists())
            throw IOException("File doesn't exist")


        return FileIOStream(file)
    }

    class FileIOStream(val file: File) : IOStream{
        override fun read() = BufferedReader(FileReader(file))

        override val name: String
            get() = file.absolutePath

        override fun parentPath() = file.parentFile.path
    }
}