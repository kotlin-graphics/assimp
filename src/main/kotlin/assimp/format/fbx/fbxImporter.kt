/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/

package assimp.format.fbx

import assimp.*
import assimp.format.AiConfig
import tokenizeBinary
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** @file  FbxImporter.h
 *  @brief Declaration of the FBX main importer class */

// TinyFormatter.h
//namespace Formatter {
//    template < typename T, typename TR, typename A> class basic_formatter
//    typedef class basic_formatter<char, std::char_traits<char>, std::allocator<char> > format
//}

// -------------------------------------------------------------------------------------------
/** Load the Autodesk FBX file format.

See http://en.wikipedia.org/wiki/FBX
 */
// -------------------------------------------------------------------------------------------
class FbxImporter : BaseImporter() {

    val settings = ImportSettings()

    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {

        val extension = getExtension(file)
        if (extension == info.fileExtensions[0]) return true
        else if (extension.isEmpty() || checkSig) {
            TODO()
            // at least ASCII-FBX files usually have a 'FBX' somewhere in their head
//            const char * tokens [] = { "fbx" };
//            return SearchFileHeaderForToken(pIOHandler, pFile, tokens, 1);
        }
        return false
    }

    override val info
        get() = AiImporterDesc(
                name = "Autodesk FBX Importer",
                flags = AiImporterFlags.SupportTextFlavour.i,
                fileExtensions = listOf("fbx")
        )

    override fun setupProperties(imp: Importer) {
        with(settings) {
            with(AiConfig.Import.Fbx.Read) {
                readAllLayers = imp[ALL_GEOMETRY_LAYERS] ?: true
                readAllMaterials = imp[ALL_MATERIALS] ?: false
                readMaterials = imp[MATERIALS] ?: true
                readTextures = imp[TEXTURES] ?: true
                readCameras = imp[CAMERAS] ?: true
                readLights = imp[LIGHTS] ?: true
                readAnimations = imp[ANIMATIONS] ?: true
            }
            with(AiConfig.Import.Fbx) {
                strictMode = imp[STRICT_MODE] ?: false
                preservePivots = imp[PRESERVE_PIVOTS] ?: true
                optimizeEmptyAnimationCurves = imp[OPTIMIZE_EMPTY_ANIMATION_CURVES] ?: true
                searchEmbeddedTextures = imp[SEARCH_EMBEDDED_TEXTURES] ?: false
            }
        }
    }

    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        val f = File(file)
        if (!f.canRead()) throw Error("Could not open file for reading")

        /*  read entire file into memory - no streaming for this, fbx files can grow large, but the assimp output data
            structure then becomes very large, too. Assimp doesn't support streaming for its output data structures so
            the net win with streaming input data would be very low. */
        val fileChannel = RandomAccessFile(f, "r").channel
        val input = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        /*  broadphase tokenizing pass in which we identify the core syntax elements of FBX (brackets, commas,
            key:value mappings)         */
        val tokens = ArrayList<Token>()
//        try {
        buffer = input
        var isBinary = false
        if (input.startsWith("Kaydara FBX Binary")) {
            isBinary = true
            tokenizeBinary(tokens, input)
        } else tokenize(tokens, input)


        // use this information to construct a very rudimentary parse-tree representing the FBX scope structure
        val parser = Parser(tokens, isBinary)

        // take the raw parse-tree and convert it to a FBX DOM
        val doc = Document(parser, settings)

        // convert the FBX DOM to aiScene
        convertToAssimpScene(scene, doc)
//
//            std::for_each(tokens.begin(),tokens.end(),Util::delete_fun<Token>());
//        }
//        catch(exc: Exception) {
////            std::for_each(tokens.begin(),tokens.end(),Util::delete_fun<Token>());
//            throw Error(exc.toString())
//        }
    }
}

lateinit var buffer: ByteBuffer