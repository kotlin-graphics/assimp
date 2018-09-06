package assimp

import glm_.*
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

// TODO move this to a different file?
private class ByteBufferBackedInputStream(val buf: ByteBuffer) : InputStream() {

	/**
	 * Reads the next byte of data from the input stream. The value byte is
	 * returned as an <code>int</code> in the range <code>0</code> to
	 * <code>255</code>. If no byte is available because the end of the stream
	 * has been reached, the value <code>-1</code> is returned. This method
	 * blocks until input data is available, the end of the stream is detected,
	 * or an exception is thrown.
	 *
	 * <p> A subclass must provide an implementation of this method.
	 *
	 * @return     the next byte of data, or <code>-1</code> if the end of the
	 *             stream is reached.
	 * @exception  IOException  if an I/O error occurs.
	 */
	override fun read(): Int {
		return if (!buf.hasRemaining()) {
			-1
		} else buf.get().toInt() and 0xFF
	}

	/**
	 * Reads up to <code>len</code> bytes of data from the input stream into
	 * an array of bytes.  An attempt is made to read as many as
	 * <code>len</code> bytes, but a smaller number may be read.
	 * The number of bytes actually read is returned as an integer.
	 *
	 * <p> This method blocks until input data is available, end of file is
	 * detected, or an exception is thrown.
	 *
	 * <p> If <code>len</code> is zero, then no bytes are read and
	 * <code>0</code> is returned; otherwise, there is an attempt to read at
	 * least one byte. If no byte is available because the stream is at end of
	 * file, the value <code>-1</code> is returned; otherwise, at least one
	 * byte is read and stored into <code>b</code>.
	 *
	 * <p> The first byte read is stored into element <code>b[off]</code>, the
	 * next one into <code>b[off+1]</code>, and so on. The number of bytes read
	 * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
	 * bytes actually read; these bytes will be stored in elements
	 * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
	 * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
	 * <code>b[off+len-1]</code> unaffected.
	 *
	 * <p> In every case, elements <code>b[0]</code> through
	 * <code>b[off]</code> and elements <code>b[off+len]</code> through
	 * <code>b[b.length-1]</code> are unaffected.
	 *
	 * <p> The <code>read(b,</code> <code>off,</code> <code>len)</code> method
	 * for class <code>InputStream</code> simply calls the method
	 * <code>read()</code> repeatedly. If the first such call results in an
	 * <code>IOException</code>, that exception is returned from the call to
	 * the <code>read(b,</code> <code>off,</code> <code>len)</code> method.  If
	 * any subsequent call to <code>read()</code> results in a
	 * <code>IOException</code>, the exception is caught and treated as if it
	 * were end of file; the bytes read up to that point are stored into
	 * <code>b</code> and the number of bytes read before the exception
	 * occurred is returned. The default implementation of this method blocks
	 * until the requested amount of input data <code>len</code> has been read,
	 * end of file is detected, or an exception is thrown. Subclasses are encouraged
	 * to provide a more efficient implementation of this method.
	 *
	 * @param      b     the buffer into which the data is read.
	 * @param      off   the start offset in array <code>b</code>
	 *                   at which the data is written.
	 * @param      len   the maximum number of bytes to read.
	 * @return     the total number of bytes read into the buffer, or
	 *             <code>-1</code> if there is no more data because the end of
	 *             the stream has been reached.
	 * @exception  IOException If the first byte cannot be read for any reason
	 * other than end of file, or if the input stream has been closed, or if
	 * some other I/O error occurs.
	 * @exception  NullPointerException If <code>b</code> is <code>null</code>.
	 * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
	 * <code>len</code> is negative, or <code>len</code> is greater than
	 * <code>b.length - off</code>
	 * @see        java.io.InputStream#read()
	 */
	override fun read(bytes: ByteArray, off: Int, len: Int): Int {
		@Suppress("NAME_SHADOWING")
		var len = len
		if (!buf.hasRemaining()) {
			return -1
		}

		len = Math.min(len, buf.remaining())
		buf.get(bytes, off, len)
		return len
	}
}