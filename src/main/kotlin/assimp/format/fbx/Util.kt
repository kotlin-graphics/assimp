package assimp.format.fbx

object Util {

    /** Get a string representation for a #TokenType. */
//    const char* TokenTypeString(TokenType t);

    /** Format log/error messages using a given offset in the source binary file
     *
     *  @param prefix Message prefix to be preprended to the location info.
     *  @param text Message text
     *  @param line Line index, 1-based
     *  @param column Column index, 1-based
     *  @return A string of the following format: {prefix} (offset 0x{offset}) {text}*/
    fun addOffset(prefix: String, text: String, offset: Int) = "$prefix (offset 0x${java.lang.Integer.toHexString(offset)}) $text"


    /** Format log/error messages using a given line location in the source file.
     *
     *  @param prefix Message prefix to be preprended to the location info.
     *  @param text Message text
     *  @param line Line index, 1-based
     *  @param column Column index, 1-based
     *  @return A string of the following format: {prefix} (line {line}, col {column}) {text}*/
    fun addLineAndColumn(prefix: String, text: String, line: Int, column: Int) = "$prefix (line $line, col $column) $text"

    /** Format log/error messages using a given cursor token.
     *
     *  @param prefix Message prefix to be preprended to the location info.
     *  @param text Message text
     *  @param tok Token where parsing/processing stopped
     *  @return A string of the following format: {prefix} ({token-type}, line {line}, col {column}) {text}*/
    fun addTokenText(prefix: String, text: String, tok: Token) = "$prefix ($tok) $text"
}