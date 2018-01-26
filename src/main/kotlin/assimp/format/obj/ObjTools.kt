package assimp.format.obj

/**
 *  Some helpful templates for text parsing
 */
class ObjTools {

    companion object {

        @JvmStatic
        fun getNameWithSpace(words: List<String>,start : Int) : String {
            var filename = ""
            var i = start
            while (i < words.size){
                filename += " " + words[i]
                i++
            }
            filename = filename.trim()

            return filename;
        }

        /**
         * return the value of an attribute with spaces
         * e.g:
         * line: usemtl no texture
         * return "no texture"
         */
        @JvmStatic
        fun getNameWithSpace(line: String) : String {
            // Get attribute value (support for spaces)
            val strName = line.split("\\s+".toRegex(),2)[1].trim()
            return strName;
        }
    }

}