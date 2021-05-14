package assimp.collada

import assimp.Importer
import java.net.URL

object floor {

    operator fun invoke(url: URL) {

        val scene = Importer().readFile(url)

        println()
    }
}