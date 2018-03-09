package assimp.format.obj

import java.io.BufferedReader

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

        /**
         * return the next line from the buffered reader, considering the continuationToken
         * e.g:
         * line:    f  1095 694 694\
                    1099
         * return "f  1095 694 6941099
         */
        @JvmStatic
        fun getNextDataLine(streamBuffer: BufferedReader,continuationToken : String) : String? {
            var line : String? = ""
            line = streamBuffer.readLine()
            if(line != null) {
                if (line.endsWith(continuationToken)) {
                    line = line.dropLast(1)
                    line += " "
                    var nextLine : String? = getNextDataLine(streamBuffer,continuationToken)
                    if (nextLine != null) {
                        line += nextLine;
                    }
                }
            }

            return line;
        }
    }

}