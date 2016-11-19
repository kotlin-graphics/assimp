import main.Importer
import org.junit.Test

/**
 * Created by elect on 16/11/2016.
 */

class utImporter {

    @Test fun stl() {

        val pImp = Importer()

        pImp.readFile("test/resources/models/STL/triangle.stl")
    }
}