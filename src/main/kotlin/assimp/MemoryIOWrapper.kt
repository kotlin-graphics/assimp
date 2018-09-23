package assimp

import glm_.*
import gli_.ByteBufferBackedInputStream
import java.io.*
import java.nio.*
import java.io.IOException

const val AI_MEMORYIO_MAGIC_FILENAME = "\$\$\$___magic___\$\$\$"
const val AI_MEMORYIO_MAGIC_FILENAME_LENGTH = 17

class MemoryIOSystem : IOSystem{

	val memoryFiles: MutableMap<String, ByteBuffer> = hashMapOf()

	constructor(buffer: ByteBuffer) {
		memoryFiles[AI_MEMORYIO_MAGIC_FILENAME] = buffer
	}

	constructor(vararg buffers: Pair<String, ByteBuffer>): this(buffers.toMap())

	constructor(buffers: Map<String, ByteBuffer>){
		memoryFiles.putAll(buffers)
	}

	/** Tests for the existence of a file at the given path. */
	override fun exists(file: String): Boolean = memoryFiles.containsKey(file)

	override fun open(file: String): IOStream {

		val buffer = memoryFiles[file] ?: throw IOException("File does not exist! $file")

		return MemoryIOStream(buffer, file, this)
	}

	class MemoryIOStream(val buffer: ByteBuffer, override val path: String, override val osSystem: MemoryIOSystem) : IOStream {

		override val filename: String = run {
			val lastIndex = path.lastIndexOf(osSystem.osSeparator)
			path.substring(lastIndex + 1)
		}

		override fun read(): InputStream {
			return ByteBufferBackedInputStream(readBytes())
		}

		override fun reader(): BufferedReader {
			return BufferedReader(InputStreamReader(read()))
		}

		override val parentPath: String = run {
			var parent = path.removeSuffix(filename)
			parent = parent.removeSuffix(osSystem.osSeparator)

			// ensures that if the path starts with "./" it will always be at least that
			if(parent == ".") parent = ".${osSystem.osSeparator}"

			parent
		}

		override val length: Long
			get() = buffer.size.toLong()

		override fun readBytes(): ByteBuffer = buffer.duplicate().order(ByteOrder.nativeOrder())
	}
}