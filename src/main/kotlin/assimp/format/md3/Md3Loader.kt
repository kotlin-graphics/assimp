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

package assimp.format.md3

import assimp.*
import assimp.format.AiConfig
import glm_.BYTES
import glm_.i
import glm_.size
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min
import assimp.AI_INT_MERGE_SCENE as Ms
import assimp.format.md3.Q3Shader.BlendFunc as Bf


object Q3Shader {

    /** @brief Tiny utility data structure to hold the data of a .skin file */
    class SkinData {
        /** A single entryin texture list   */
        class TextureEntry(val first: String, val second: String) {
            /** did we resolve this texture entry?  */
            var resolved = false
        }

        /** List of textures    */
        val textures = ArrayList<TextureEntry>()
        // rest is ignored for the moment
    }

    /** @brief Specifies cull modi for Quake shader files. */
    enum class ShaderCullMode { NONE, CW, CCW }

    /** @brief Specifies alpha blend modi (src + dest) for Quake shader files */
    enum class BlendFunc { NONE, GL_ONE, GL_ZERO, GL_DST_COLOR, GL_ONE_MINUS_DST_COLOR, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA }

    /** @brief Specifies alpha test modi for Quake texture maps */
    enum class AlphaTestFunc { NONE, GT0, LT128, GE128 }

    /** @brief Tiny utility data structure to hold a .shader map data block */
    class ShaderMapBlock {
        /** Name of referenced map  */
        var name = ""
        //! Blend and alpha test settings for texture
        var blendSrc = Bf.NONE
        var blendDest = Bf.NONE
        var alphaTest = AlphaTestFunc.NONE
        fun blend(src: Bf, dest: Bf) {
            blendSrc = src
            blendDest = dest
        }
    }

    /** @brief Tiny utility data structure to hold a .shader data block */
    class ShaderDataBlock {
        /** Name of referenced data element */
        var name = ""
        /** Cull mode for the element   */
        var cull = ShaderCullMode.CW
        /** Maps defined in the shader  */
        val maps = ArrayList<ShaderMapBlock>()
    }

    /** @brief Tiny utility data structure to hold the data of a .shader file */
    class ShaderData {
        /** Shader data blocks  */
        val blocks = ArrayList<ShaderDataBlock>()
    }

    /** @brief Load a shader file
     *
     *  Generally, parsing is error tolerant. There's no failure.
     *  @param fill Receives output data
     *  @param file File to be read.
     *  @param io IOSystem to be used for reading
     *  @return false if file is not accessible
     */
    fun loadShader(fill: ShaderData, file: String): Boolean {
        val f = File(file)
        if (!f.exists()) return false // if we can't access the file, don't worry and return

        logger.info { "Loading Quake3 shader file $file" }
        // read file in memory and remove comments from it (C++ style) and empty or blank lines
        val lines = f.readLines().filter { !it.startsWith("//") && it.isNotEmpty() && it.isNotBlank() }
                .map { it.trim() } // and trim it

        var curData: Q3Shader.ShaderDataBlock? = null
        var curMap: Q3Shader.ShaderMapBlock? = null

        // read line per line
        var i = 0
        while (i < lines.size) {

            var line = lines[i++]

            if (line[0] == '{') {
                // append to last section, if any
                if (curData == null) {
                    logger.error { "Q3Shader: Unexpected shader section token '{'" }
                    return true // still no failure, the file is there
                }
                // read this data section
                while (i < lines.size) {
                    line = lines[i++]
                    if (line[0] == '{') {
                        // add new map section
                        curMap = Q3Shader.ShaderMapBlock()
                        curData!!.maps.add(curMap)

                        while (i < lines.size) {
                            line = lines[i++]
                            // 'map' - Specifies texture file name
                            if (line.startsWith("map") || line.startsWith("clampmap")) curMap!!.name = line.words[1]
                            // 'blendfunc' - Alpha blending mode
                            else if (line.startsWith("blendfunc")) {
                                val words = line.split(Regex("\\s+"))
                                when (words[1]) {
                                    "add" -> curMap!!.blend(Bf.GL_DST_COLOR, Bf.GL_ZERO)
                                    "filter" -> curMap!!.blend(Bf.GL_DST_COLOR, Bf.GL_ZERO)
                                    "blend" -> curMap!!.blend(Bf.GL_SRC_ALPHA, Bf.GL_ONE_MINUS_SRC_ALPHA)
                                    else -> curMap!!.blend(words[1].toBF, words[2].toBF)
                                }
                            }
                            // 'alphafunc' - Alpha testing mode
                            else if (line.startsWith("alphaFunc")) when (line.words[1]) {
                                "GT0" -> curMap!!.alphaTest = AlphaTestFunc.GT0
                                "LT128" -> curMap!!.alphaTest = AlphaTestFunc.LT128
                                "GE128" -> curMap!!.alphaTest = AlphaTestFunc.GE128
                            }
                            else if (line[0] == '}') {
                                curMap = null // close this map section
                                break
                            }
                        }
                    } else if (line[0] == '}') {
                        curData = null
                        break
                    }
                    // 'cull' specifies culling behaviour for the model
                    else if (line.startsWith("cull")) when (line.words[1]) {
                        "back" -> curData!!.cull = ShaderCullMode.CCW
                        "front" -> curData!!.cull = ShaderCullMode.CW
                        "none", "disable" -> curData!!.cull = ShaderCullMode.NONE
                        else -> logger.error { "Q3Shader: Unrecognized cull mode" }
                    }
                }
            } else   // add new section and get name
                curData = Q3Shader.ShaderDataBlock().apply { name = line }.also { fill.blocks.add(it) }
        }
        return true
    }

    /** Convert a Q3 shader blend function to the appropriate enum value */
    val String.toBF
        get() = when (this) {
            "GL_ONE" -> Bf.GL_ONE
            "GL_ZERO" -> Bf.GL_ZERO
            "GL_SRC_ALPHA" -> Bf.GL_SRC_ALPHA
            "GL_ONE_MINUS_SRC_ALPHA" -> Bf.GL_ONE_MINUS_SRC_ALPHA
            "GL_ONE_MINUS_DST_COLOR" -> Bf.GL_ONE_MINUS_DST_COLOR
            else -> Bf.NONE.also { logger.error { "Q3Shader: Unknown blend function: $this" } }
        }


    /** @brief Convert a Q3Shader to an aiMaterial
     *
     *  @param[out] out Material structure to be filled.
     *  @param[in] shader Input shader
     */
    fun convertShaderToMaterial(out: AiMaterial, shader: ShaderDataBlock) {

        /*  IMPORTANT: This is not a real conversion. Actually we're just guessing and hacking around to build an
            AiMaterial that looks nearly equal to the original Quake 3 shader. We're missing some important features
            like animatable material properties in our material system, but at least multiple textures should be
            handled correctly.         */

        // Two-sided material?
        out.twoSided = shader.cull == ShaderCullMode.NONE

        // Iterate through all textures
        shader.maps.forEach {

            /*  CONVERSION BEHAVIOUR:

                If the texture is additive
                - if it is the first texture, assume additive blending for the whole material
                - otherwise register it as emissive texture.

                If the texture is using standard blend (or if the blend mode is unknown)
                - if first texture: assume default blending for material
                - in any case: set it as diffuse texture

                If the texture is using 'filter' blending
                - take as lightmap

                Textures with alpha funcs
                - AiTextureFlags_UseAlpha is set (otherwise AiTextureFlags_NoAlpha is explicitly set) */
            val texture = AiMaterial.Texture()
            texture.file = it.name
            texture.type = when {
                it.blendSrc == Bf.GL_ONE && it.blendDest == Bf.GL_ONE ->
                    if (it === shader.maps[0]) {
                        out.blendFunc = AiBlendMode.additive
                        AiTexture.Type.diffuse
                    } else AiTexture.Type.emissive
                it.blendSrc == Bf.GL_DST_COLOR && it.blendDest == Bf.GL_ZERO -> AiTexture.Type.lightmap
                else -> {
                    out.blendFunc = AiBlendMode.default
                    AiTexture.Type.diffuse
                }
            }
            out.textures.add(texture) // setup texture

            // setup texture flags
            texture.flags = if (it.alphaTest != AlphaTestFunc.NONE) AiTexture.Flags.useAlpha.i else AiTexture.Flags.ignoreAlpha.i
        }
        // If at least one emissive texture was set, set the emissive base color to 1 to ensure the texture is actually displayed.
        out.textures.find { it.type == AiTexture.Type.emissive }?.let {
            out.color = AiMaterial.Color(emissive = AiVector3D(1f))
        }
    }

    /** @brief Load a skin file
     *
     *  Generally, parsing is error tolerant. There's no failure.
     *  @param fill Receives output data
     *  @param file File to be read.
     *  @param io IOSystem to be used for reading
     *  @return false if file is not accessible
     */
    fun loadSkin(fill: SkinData, file: String): Boolean {
        val f = File(file)
        if (!f.canRead()) return false // if we can't access the file, don't worry and return

        logger.info { "Loading Quake3 skin file $file" }

        // read file in memory
        val s = f.length()
        TODO()
//        std::vector<char> _buff(s+1);const char* buff = &_buff[0];
//        f->Read(&_buff[0],s,1);
//        _buff[s] = 0;
//
//        // remove commas
//        std::replace(_buff.begin(),_buff.end(),',',' ');
//
//        // read token by token and fill output table
//        for (;*buff;) {
//            SkipSpacesAndLineEnd(&buff);
//
//            // get first identifier
//            std::string ss = GetNextToken(buff);
//
//            // ignore tokens starting with tag_
//            if (!::strncmp(&ss[0],"tag_",std::min((size_t)4, ss.length())))
//            continue;
//
//            fill.textures.push_back(SkinData::TextureEntry());
//            SkinData::TextureEntry& s = fill.textures.back();
//
//            s.first  = ss;
//            s.second = GetNextToken(buff);
//        }
//        return true
    }
}

/** @brief Importer class to load MD3 files */
class Md3Importer : BaseImporter() {
    /** Configuration option: frame to be loaded */
    var configFrameID = 0
    /** Configuration option: process multi-part files */
    var configHandleMP = true
    /** Configuration option: name of skin file to be read */
    var configSkinFile = ""
    /** Configuration option: name or path of shader */
    var configShaderFile = ""
    /** Configuration option: speed flag was set? */
    var configSpeedFlag = false
    /** Header of the MD3 file */
    lateinit var header: MD3.Header
    /** File buffer  */
    lateinit var buffer: ByteBuffer
    /** Size of the file, in bytes */
    var fileSize = 0
    /** Current file name */
    var file = ""
    /** Current base directory  */
    var path = ""
    /** Pure file we're currently reading */
    var filename = ""
    /** Output scene to be filled */
    var scene: AiScene? = null

    /** Returns whether the class can handle the format of the given file.
     * See BaseImporter.canRead() for details.  */
    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {
        val extension = getExtension(file)
        if (extension == "md3") return true
        // if check for extension is not enough, check for the magic tokens
        if (extension.isNotEmpty() || checkSig) {
//            TODO()
//            uint32_t tokens[1];
//            tokens[0] = AI_MD3_MAGIC_NUMBER_LE;
//            return CheckMagicToken(pIOHandler,pFile,tokens,1);
        }
        return false
    }

    /** Called prior to readFile().
     *  The function is a request to the importer to update its configuration basing on the Importer's configuration
     *  property list.
     */
    override fun setupProperties(imp: Importer) {
        // The AI_CONFIG_IMPORT_MD3_KEYFRAME option overrides the AI_CONFIG_IMPORT_GLOBAL_KEYFRAME option.
        configFrameID = imp[AiConfig.Import.Md3.KEYFRAME] ?: imp[AiConfig.Import.Md3.KEYFRAME] ?: 0
        // AI_CONFIG_IMPORT_MD3_HANDLE_MULTIPART
        configHandleMP = imp[AiConfig.Import.Md3.HANDLE_MULTIPART] ?: true
        // AI_CONFIG_IMPORT_MD3_SKIN_NAME
        configSkinFile = imp[AiConfig.Import.Md3.SKIN_NAME] ?: "default"
        // AI_CONFIG_IMPORT_MD3_SHADER_SRC
        configShaderFile = imp[AiConfig.Import.Md3.SHADER_SRC] ?: ""
        // AI_CONFIG_FAVOUR_SPEED
        configSpeedFlag = imp[AiConfig.FAVOUR_SPEED] ?: false
    }

    /** Return importer meta information.
     *  See BaseImporter.info for the details     */
    override val info
        get() = AiImporterDesc(
                name = "Quake III Mesh Importer",
                flags = AiImporterFlags.SupportBinaryFlavour.i,
                fileExtensions = listOf("md3"))

    /** Imports the given file into the given scene structure.
     *  See BaseImporter.internReadFile() for details     */
    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        this.file = file
        this.scene = scene

        // get base path and file name
        // todo ... move to PathConverter
        filename = file.substringAfterLast(File.separatorChar).toLowerCase()
        val last = file.lastIndexOf(File.separatorChar)
        path = if (last == -1) file else file.substring(0, last + 1)

        // Load multi-part model file, if necessary
        if (configHandleMP && readMultipartFile()) return

        // Check whether we can read from the file
        val f = File(file)
        if (!f.canRead()) throw Error("Failed to open MD3 file $file.")

        // Check whether the md3 file is large enough to contain the header
        fileSize = f.length().i
        if (fileSize < MD3.Header.size) throw Error("MD3 File is too small.")

        // Allocate storage and copy the contents of the file to a memory buffer
        val fileChannel = RandomAccessFile(f, "r").channel
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        header = MD3.Header(buffer)
        // Validate the file header
        header.validateOffsets(buffer.size, configFrameID)
        // Navigate to the list of surfaces
        var pSurfaces = header.ofsSurfaces
        // Navigate to the list of tags
        val tags = MD3.Tag(buffer.apply { position(header.ofsTags) })
        // Allocate output storage
        scene.numMeshes = header.numSurfaces
        if (header.numSurfaces == 0) throw Error("MD3: No surfaces")
        else if (header.numSurfaces > AI_MAX_ALLOC(AiMesh.size))
        // We allocate pointers but check against the size of aiMesh since those pointers will eventually have to point to real objects
            throw Error("MD3: Too many surfaces, would run out of memory")

        scene.numMaterials = header.numSurfaces
        // Now read possible skins from .skin file
        val skins = Q3Shader.SkinData()
        readSkin(skins)
        // And check whether we can locate a shader file for this model
        val shadersData = Q3Shader.ShaderData()
        readShader(shadersData)

        // Adjust all texture paths in the shader
        val headerName = header.name
        shadersData.blocks.forEach {
            it.name = convertPath(it.name, headerName)
            it.maps.forEach { it.name = convertPath(it.name, headerName) }
        }

        // Read all surfaces from the file
        var iNum = header.numSurfaces
        var iNumMaterials = 0
        val meshes = ArrayList<AiMesh>()
        while (iNum-- > 0) {
            var surfaces = MD3.Surface(buffer.apply { position(pSurfaces) })
            // Validate the surface header
            surfaces.validateOffsets(pSurfaces, fileSize)
            // Navigate to the vertex list of the surface
            val pVertices = pSurfaces + surfaces.ofsXyzNormal
            // Navigate to the triangle list of the surface
            var pTriangles = pSurfaces + surfaces.ofsTriangles
            // Navigate to the texture coordinate list of the surface
            val pUVs = pSurfaces + surfaces.ofsSt//
            // Navigate to the shader list of the surface
            val shaders = MD3.Shader(buffer.apply { position(pSurfaces + surfaces.ofsShaders) })
            // If the submesh is empty ignore it
            if (0 == surfaces.numVertices || 0 == surfaces.numTriangles) {
                surfaces = MD3.Surface(buffer.apply { position(pSurfaces + surfaces.ofsEnd) })
                scene.numMeshes--
                continue
            }

            // Allocate output mesh
            val mesh = AiMesh().also { meshes.add(it) }

            // Check whether we have a texture record for this surface in the .skin file
            var textureName = skins.textures.find { it.first == surfaces.name }?.let {
                logger.debug { "MD3: Assigning skin texture ${it.second} to surface ${surfaces.name}" }
                it.resolved = true // mark entry as resolved
                it.second
            } ?: ""

            // Get the first shader (= texture?) assigned to the surface
            if (textureName.isEmpty() && surfaces.numShader != 0)
                textureName = shaders.name

            val convertedPath = when (textureName) {
                "" -> ""
                else -> convertPath(textureName.trim(), headerName.trim())
            }

            var shader: Q3Shader.ShaderDataBlock? = null

            // Now search the current shader for a record with this name (
            // excluding texture file extension)
            if (shadersData.blocks.isNotEmpty()) {
                val s = convertedPath.lastIndexOf('.').takeIf { it != -1 } ?: convertedPath.length

                val withoutExt = convertedPath.substring(0, s)
                shader = shadersData.blocks.find { it.name == withoutExt }
                if (shader != null) // Hurra, wir haben einen. Tolle Sache.
                    logger.info { "Found shader record for $withoutExt" }
                else logger.warn { "Unable to find shader record for $withoutExt" }
            }

            val helper = AiMaterial().apply {
                shadingModel = AiShadingMode.gouraud
                // Add a small ambient color value - Quake 3 seems to have one
                color = AiMaterial.Color().apply {
                    ambient = AiVector3D(0.05f)
                    diffuse = AiVector3D(1f)
                    specular = AiVector3D(1f)
                }
                // use surface name + skin_name as material name
                name = "MD3_[$configSkinFile][${surfaces.name}]"
            }
            if (shader == null)
                helper.textures.add(AiMaterial.Texture().apply {
                    // Setup dummy texture file name to ensure UV coordinates are kept during postprocessing
                    this.file = convertedPath.takeIf { it.isNotEmpty() } ?: run {
                        logger.warn { "Texture file name has zero length. Using default name" }
                        "dummy_texture.bmp"
                    }
                    flags = AiTexture.Flags.ignoreAlpha.i // prevent transparency by default
                })
            else Q3Shader.convertShaderToMaterial(helper, shader)

            scene.materials.add(helper)
            mesh.materialIndex = iNumMaterials++

            // Fill mesh information
            with(mesh) {
                primitiveTypes = AiPrimitiveType.TRIANGLE.i
                numVertices = surfaces.numTriangles * 3
                numFaces = surfaces.numTriangles
                textureCoords.add(mutableListOf())
            }

            // Fill in all triangles
            var iCurrent = 0
            for (i in 0 until surfaces.numTriangles) {
                mesh.faces.add(MutableList(3, { 0 }))

                //unsigned int iTemp = iCurrent;
                for (c in 0..2) {
                    mesh.faces[i][c] = iCurrent++
                    // Read vertices
                    val index = buffer.getInt(pTriangles + c * Int.BYTES)
                    if (index >= surfaces.numVertices) throw Error("MD3: Invalid vertex index")
                    buffer.position(pVertices + index * MD3.Vertex.size)
                    mesh.vertices.add(AiVector3D(buffer.short, buffer.short, buffer.short).apply { this *= MD3.XYZ_SCALE })
                    // Convert the normal vector to uncompressed float3 format
                    mesh.normals.add(MD3.latLngNormalToVec3(buffer.short))

                    // Read texture coordinates
                    buffer.position(pUVs + index * MD3.TexCoord.size)
                    mesh.textureCoords[0].add(floatArrayOf(buffer.float, 1f - buffer.float))
                }
                // Flip face order if necessary
                if (shader == null || shader.cull == Q3Shader.ShaderCullMode.CW) {
                    val t = mesh.faces[i][2]
                    mesh.faces[i][2] = mesh.faces[i][1]
                    mesh.faces[i][1] = t
                }
                pTriangles += MD3.Triangle.size
            }
            // Go to the next surface
            pSurfaces += surfaces.ofsEnd
            pTriangles += surfaces.ofsEnd
        }
        // meshes are inserted from the end
        for (i in meshes.lastIndex downTo 0) scene.meshes.add(meshes[i])

        // For debugging purposes: check whether we found matches for all entries in the skins file
        skins.textures.filter { !it.resolved }.forEach {
            logger.error { "MD3: Failed to match skin ${it.first} to surface ${it.second}" }
        }

        if (scene.numMeshes == 0) throw Error("MD3: File contains no valid mesh")
        scene.numMaterials = iNumMaterials

        // Now we need to generate an empty node graph
        scene.rootNode = AiNode("<MD3Root>").apply { numMeshes = scene.numMeshes }

        // Attach tiny children for all tags
        if (header.numTags != 0) {
            scene.rootNode.numChildren = header.numTags

            for (i in 0 until header.numTags) {

                val nd = AiNode()
                TODO()
//                aiNode * nd = pScene->mRootNode->mChildren[i] = new aiNode()
//                nd->mName.Set((const char*)pcTags->NAME)
//                nd->mParent = pScene->mRootNode
//
//                AI_SWAP4(pcTags->origin.x)
//                AI_SWAP4(pcTags->origin.y)
//                AI_SWAP4(pcTags->origin.z)
//
//                // Copy local origin, again flip z,y
//                nd->mTransformation.a4 = pcTags->origin.x
//                nd->mTransformation.b4 = pcTags->origin.y
//                nd->mTransformation.c4 = pcTags->origin.z
//
//                // Copy rest of transformation (need to transpose to match row-order matrix)
//                for (unsigned int a = 0; a < 3;++a) {
//                    for (unsigned int m = 0; m < 3;++m) { nd ->
//                    mTransformation[m][a] = pcTags->orientation[a][m]
//                    AI_SWAP4(nd->mTransformation[m][a])
//                }
//                }
//                ++pTags
            }
        }

        scene.rootNode.meshes = IntArray(scene.numMeshes, { it })

        // Now rotate the whole scene 90 degrees around the x axis to convert to internal coordinate system
        scene.rootNode.transformation.put(
                1f, 0f, 0f, 0f,
                0f, 0f, -1f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f)
    }

    /** Read a Q3 multipart file
     *  @return true if multi part has been processed     */
    fun readMultipartFile(): Boolean {
        // check whether the file name contains a common postfix, e.g lower_2.md3
        var s = filename.lastIndexOf('_')
        var t = filename.lastIndexOf('.')

        if (t == -1) t = filename.length
        if (s == -1) s = t

        val modFilename = filename.substring(0, s)
        val suffix = filename.substring(s, t)

        if (modFilename == "lower" || modFilename == "upper" || modFilename == "head") {
            val lower = "${path}lower$suffix.md3"
            val upper = "${path}upper$suffix.md3"
            val head = "${path}head$suffix.md3"

            var failure = ""

            logger.info { "Multi part MD3 player model: lower, upper and head parts are joined" }

            // ensure we won't try to load ourselves recursively
            val props = mutableMapOf<Int, Any>(superFastHash(AiConfig.Import.Md3.HANDLE_MULTIPART) to false)

            // now read these three files
            val batch = BatchLoader()
            val _lower = batch.addLoadRequest(lower, 0, props)
            val _upper = batch.addLoadRequest(upper, 0, props)
            val _head = batch.addLoadRequest(head, 0, props)
            batch.loadAll()

            // now construct a dummy scene to place these three parts in
            val master = AiScene().apply { rootNode = AiNode() }
            val nd = master.rootNode
            nd.name = "<MD3_Player>"

            fun error() {
                if (failure == modFilename) throw Error("MD3: failure to read multipart host file")
            }

            // ... and get them. We need all of them.
            val sceneLower = batch.getImport(_lower)
            if (sceneLower == null) {
                logger.error { "M3D: Failed to read multi part model, lower.md3 fails to load" }
                failure = "lower"
                error()
            }

            val sceneUpper = batch.getImport(_upper)
            if (sceneUpper == null) {
                logger.error { "M3D: Failed to read multi part model, upper.md3 fails to load" }
                failure = "upper"
                error()
            }

            val sceneHead = batch.getImport(_head)
            if (sceneHead == null) {
                logger.error { "M3D: Failed to read multi part model, head.md3 fails to load" }
                failure = "head"
                error()
            }

            // build attachment infos. search for typical Q3 tags

            // original root
            sceneLower!!.rootNode.name = "lower"
            val attach = arrayListOf(AttachmentInfo(sceneLower, nd))

            // tagTorso
            val tagTorso = sceneLower.rootNode.findNode("tagTorso")
            if (tagTorso == null) {
                logger.error { "M3D: Failed to find attachment tag for multi part model: tagTorso expected" }
                error()
            }
            sceneUpper!!.rootNode.name = "upper"
            attach.add(AttachmentInfo(sceneUpper, tagTorso))

            // tag_head
            val tagHead = sceneUpper.rootNode.findNode("tag_head")
            if (tagHead == null) {
                logger.error { "M3D: Failed to find attachment tag for multi part model: tag_head expected" }
                error()
            }
            sceneHead!!.rootNode.name = "head"
            attach.add(AttachmentInfo(sceneHead, tagHead))

            // Remove tag_head and tagTorso from all other model parts ...
            // this ensures (together with AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY)
            // that tagTorso/tag_head is also the name of the (unique) output node
            removeSingleNodeFromList(sceneUpper.rootNode.findNode("tagTorso"))
            removeSingleNodeFromList(sceneHead.rootNode.findNode("tag_head"))

            // Undo the rotations which we applied to the coordinate systems. We're working in global Quake space here
            sceneHead.rootNode.transformation put 1f
            sceneLower.rootNode.transformation put 1f
            sceneUpper.rootNode.transformation put 1f

            // and merge the scenes
            scene = SceneCombiner.mergeScenes(master, attach, Ms.GEN.UNIQUE_NAMES or Ms.GEN.UNIQUE_MATNAMES or
                    Ms.RESOLVE_CROSS_ATTACHMENTS or if (configSpeedFlag) Ms.GEN.UNIQUE_NAMES_IF_NECESSARY else 0)

            // Now rotate the whole scene 90 degrees around the x axis to convert to internal coordinate system
            scene!!.rootNode.transformation = AiMatrix4x4(
                    1f, 0f, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1f)
            return true
        }
        return false
    }

    /** Tiny helper to remove a single node from its parent' list */
    fun removeSingleNodeFromList(nd: AiNode?) {
        if (nd == null || nd.numChildren != 0 || nd.parent == null) return
        val par = nd.parent!!
        var i = 0
        if (par.children.remove(nd)) --par.numChildren
    }

    /** Try to read the skin for a MD3 file
     *  @param fill Receives output information     */
    fun readSkin(fill: Q3Shader.SkinData) {
        // skip any postfixes (e.g. lower_1.md3)
        var s = filename.lastIndexOf('_')
        if (s == -1) {
            s = filename.lastIndexOf('.')
            if (s == -1) s = filename.length
        }
        assert(s != -1)
        val skinFile = path + filename.substring(0, s) + "_$configSkinFile.skin"
        Q3Shader.loadSkin(fill, skinFile)
    }

    /** Try to read the shader for a MD3 file
     *  @param fill Receives output information     */
    fun readShader(fill: Q3Shader.ShaderData) {
        // Determine Q3 model name from given path
        val last = path.substring(0, path.length - 2).lastIndexOf(File.separatorChar)
        val modelFile = path.substring(last + 1, path.length - 1)

        // If no specific dir or file is given, use our default search behaviour
        if (configShaderFile.isEmpty()) {
            if (!Q3Shader.loadShader(fill, "$path../../../scripts/$modelFile.shader"))
                Q3Shader.loadShader(fill, "$path../../../scripts/$filename.shader")
        } else {
            TODO()
//            // If the given string specifies a file, load this file.
//            // Otherwise it's a directory.
//            const std::string::size_type st = configShaderFile.find_last_of('.');
//            if (st == std::string::npos) {
//
//                if(!Q3Shader::LoadShader(fill,configShaderFile + modelFile + ".shader",mIOHandler)) {
//                    Q3Shader::LoadShader(fill,configShaderFile + filename + ".shader",mIOHandler);
//                }
//            }
//            else {
//                Q3Shader::LoadShader(fill,configShaderFile,mIOHandler);
//            }
        }
    }

    /** Convert a texture path in a MD3 file to a proper value
     *  @param[in] texture_name Path to be converted
     *  @param[in] header_path Base path specified in MD3 header
     *  @param[out] out Receives the converted output string
     */
    fun convertPath(textureName: String, headerName: String): String {
        /*  If the MD3's internal path itself and the given path are using the same directory, remove it completely
            to get right output paths.         */
        val end1 = with(headerName) { if (contains('\\')) lastIndexOf('\\') else lastIndexOf('/') }
        with(textureName) {
            when {
                contains('\\') -> lastIndexOf('\\')
                contains('/') -> lastIndexOf('/')
                else -> null
            }
        }?.let { end2 ->
            /*  HACK: If the paths starts with "models", ignore the next two hierarchy levels, it specifies just the
                model name.
                Ignored by Q3, it might be not equal to the real model location. */
            var len2 = 6
            val len1 = end1
            if (textureName.startsWith("models") && (textureName[6] == '/' || textureName[6] == '\\')) {
                len2 = 6 // ignore the seventh - could be slash or backslash
                if (headerName[0] == NUL)
                    return textureName.substring(end2 + 1) // Use the file name only
            } else len2 = min(len1, end2)
            if (textureName.startsWith(headerName.substring(0, len2)))
                return textureName.substring(end2 + 1) // Use the file name only
        }
        return textureName // Use the full path
    }
}