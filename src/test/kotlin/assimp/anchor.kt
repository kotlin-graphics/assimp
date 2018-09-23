package assimp

import io.kotlintest.*
import java.io.*
import java.net.*
import java.nio.*
import java.nio.file.*

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
fun Importer.testFile(path: URL,
                      flags: AiPostProcessStepsFlags = 0,
                      failOnNull: Boolean = true,
                      verify: AiScene.() -> Unit = {}): AiScene? {
	return testFile(path.toURI(), flags, failOnNull, verify)
}

/**
 * calls both [Importer.readFile] and [Importer.readFileFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testFile(path: URI,
                      flags: AiPostProcessStepsFlags = 0,
                      failOnNull: Boolean = true,
                      verify: AiScene.() -> Unit = {}): AiScene? {
	return testFile(Paths.get(path).toAbsolutePath().toString(), flags, failOnNull, verify)
}

/**
 * calls both [Importer.readFile] and [Importer.readFileFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testFile(path: String,
                      flags: AiPostProcessStepsFlags = 0,
                      failOnNull: Boolean = true,
                      verify: AiScene.() -> Unit = {}): AiScene? {

	logger.info { "Testing read $path:" }

	logger.info { "reading from file:"}

	var scene = readFile(path, flags)
	if (scene == null && failOnNull) {
		fail("readFile returned 'null' for $path")
	}
	scene?.verify()

	logger.info { "reading from memory:" }

	val bytes = FileInputStream(File(path)).readBytes()
	val buffer = ByteBuffer.wrap(bytes)

	val hintStart = path.indexOfLast { it == '.' }
	val hint = path.substring(hintStart + 1)

	val memScene = readFileFromMemory(buffer, flags, hint)
	if (memScene == null && failOnNull) {
		fail("readFileFromMemory returned 'null' for $path")
	}
	memScene?.verify()

	if(scene == null) scene = memScene

	return scene
}

/**
 * calls both [Importer.readFile] and [Importer.readFilesFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * The first path in [paths] will be used for the base path
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testURLs(vararg paths: URL,
                      flags: AiPostProcessStepsFlags = 0,
                      failOnNull: Boolean = true,
                      verify: AiScene.() -> Unit = {}): AiScene? {
	return testURLs(listOf(*paths), flags, failOnNull, verify)
}

/**
 * calls both [Importer.readFile] and [Importer.readFilesFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * The first path in [paths] will be used for the base path
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testURLs(paths: List<URL>,
                      flags: AiPostProcessStepsFlags = 0,
                      failOnNull: Boolean = true,
                      verify: AiScene.() -> Unit = {}): AiScene? {
	return testURIs(paths.map(URL::toURI), flags, failOnNull, verify)
}

/**
 * calls both [Importer.readFile] and [Importer.readFilesFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * The first path in [paths] will be used for the base path
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testURIs(vararg paths: URI,
                      flags: AiPostProcessStepsFlags = 0,
                      failOnNull: Boolean = true,
                      verify: AiScene.() -> Unit = {}): AiScene? {
	return testURIs(listOf(*paths), flags, failOnNull, verify)
}

/**
 * calls both [Importer.readFile] and [Importer.readFilesFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * The first path in [paths] will be used for the base path
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testURIs(paths: List<URI>,
                      flags: AiPostProcessStepsFlags = 0,
                      failOnNull: Boolean = true,
                      verify: AiScene.() -> Unit = {}): AiScene? {
	return testFiles(paths.map { Paths.get(it).toAbsolutePath().toString() }, flags, failOnNull, verify)
}

/**
 * calls both [Importer.readFile] and [Importer.readFilesFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * The first path in [paths] will be used for the base path
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testFiles(vararg paths: String,
                       flags: AiPostProcessStepsFlags = 0,
                       failOnNull: Boolean = true,
                       verify: AiScene.() -> Unit = {}): AiScene? {
	return testFiles(listOf(*paths), flags, failOnNull, verify)
}

/**
 * calls both [Importer.readFile] and [Importer.readFilesFromMemory] and verifies it using [verify].
 * This fails if [failOnNull] is set and either of the above returns null.
 *
 * The first path in [paths] will be used for the base path
 *
 * @return the result of [Importer.readFile]
 */
fun Importer.testFiles(paths: List<String>,
                       flags: AiPostProcessStepsFlags = 0,
                       failOnNull: Boolean = true,
                       verify: AiScene.() -> Unit = {}): AiScene? {

	val baseFile = paths[0]

	logger.info { "Testing read $baseFile:" }

	logger.info { "reading from file:"}
	// test readFile
	var scene = readFile(baseFile, flags)
	if (scene == null && failOnNull) {
		fail("readFile returned 'null' for $baseFile")
	} else {
		scene?.verify()
	}


	logger.info { "reading from memory:" }

	val files = paths.map { it to ByteBuffer.wrap(FileInputStream(File(it)).readBytes()) }.toMap()

	val memScene = readFilesFromMemory(baseFile, files, flags)
	if (memScene == null && failOnNull) {
		fail("readFileFromMemory returned 'null' for $baseFile")
	} else {
		memScene?.verify()
	}

	if(scene == null) scene = memScene

	return scene
}