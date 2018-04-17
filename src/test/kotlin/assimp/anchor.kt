package assimp

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