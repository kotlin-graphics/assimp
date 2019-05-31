import assimp.*
import java.io.*
import javax.swing.*

val filePath: String? = "/Users/burkhard/Projects/Kotlin/kotlin-graphics/temp/data/spider.fbx"

fun main() {

	val file = filePath?.let { File(it) }?.takeIf { it.exists() } ?: run {
		val fc = JFileChooser()

		//In response to a button click:
		fc.showOpenDialog(null)

		fc.selectedFile
	}

	println("path: ${file.absolutePath}")

	val scene = Importer().readFile(file.absolutePath)

	if (scene == null) {
		println("Error loading file")
		return
	}

	println("scene has ${scene.numMeshes} meshes")
	println("scene has ${scene.numMaterials} materials")
	println("scene has ${scene.numLights} lights")
	println("scene has ${scene.numCameras} cameras")
	println("scene has ${scene.numTextures} textures")
	println("scene has ${scene.numAnimations} animations")

	for (mesh in scene.meshes) {
		println("mesh '${mesh.name}' has ${mesh.numVertices} vertices")
	}
}