package assimp

import io.kotlintest.*
import org.lwjgl.BufferUtils
import java.io.*
import java.net.URL

/**
 * Created by elect on 14/01/2017.
 */

val models = "models"
val modelsNonBsd = "models-nonbsd"


val assbin = "$models/Assbin/"

/**
 * Note, we need URI -> Path to clean any possible leading slash
 *
 * https://stackoverflow.com/a/31957696/1047713
 */
fun getResource(resource: String): URL = ClassLoader.getSystemResource(resource)


/**
 * calls both [Importer.readFile] and [Importer.readFileFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testFile(path: URL, flags: AiPostProcessStepsFlags = 0, failOnNull: Boolean = true, verify: AiScene.() -> Unit = {}): AiScene? {
	return testFile(path.file, flags, failOnNull, verify)
}


// TODO temp
var testReadFile = true
var testReadFromMemory = true

/**
 * calls both [Importer.readFile] and [Importer.readFileFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testFile(path: String, flags: AiPostProcessStepsFlags = 0, failOnNull: Boolean = true, verify: AiScene.() -> Unit = {}): AiScene? {
	logger.info { "Testing read $path:" }
	var scene: AiScene? = null
	if(testReadFile) {
		logger.info { "reading from file:"}
		// test readFile
		scene = readFile(path, flags)
		if (scene == null && failOnNull) {
			fail("readFile returned 'null' for $path")
		} else {
			scene?.verify()
		}
	}

	if(testReadFromMemory) {
		logger.info { "reading from memory:" }
		// test readFileFromMemory
		val bytes = FileInputStream(File(path)).readBytes()
		val buffer = BufferUtils.createByteBuffer(bytes.size).also { it.put(bytes); it.flip() }

		val hintStart = path.indexOfLast { it == '.' }
		val hint = path.substring(hintStart + 1)

		val memScene = readFileFromMemory(buffer, flags, hint)
		if (memScene == null && failOnNull) {
			fail("readFileFromMemory returned 'null' for $path")
		} else {
			memScene?.verify()
		}
	}

	return scene
}