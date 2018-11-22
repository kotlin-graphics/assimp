import assimp.*
import java.io.*
import javax.swing.*

val filePath: String? = null

fun main() {

	val file = filePath?.let { File(it) } ?: run {
		val fc = JFileChooser()

		//In response to a button click:
		val result = fc.showOpenDialog(null)

		fc.selectedFile
	}

	println("path: ${file.absolutePath}")

	Importer().readFile(file.absolutePath)
}