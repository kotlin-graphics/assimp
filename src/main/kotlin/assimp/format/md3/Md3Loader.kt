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
import assimp.*
import assimp.format.AiConfig
import glm_.i
import glm_.size
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import assimp.AI_INT_MERGE_SCENE as Ms


object Q3Shader {

    /** @brief Tiny utility data structure to hold the data of a .skin file */
    class SkinData {
        /** A single entryin texture list   */
        class TextureEntry(val first: String, val second: String) {
            /** did we resolve this texture entry?  */
            var resolved = false
//            // for std::find()
//            bool operator ==(const std ::string& f) const {
//            return f == first;
//        }
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
        var blendSrc = BlendFunc.NONE
        var blendDest = BlendFunc.NONE
        var alphaTest = AlphaTestFunc.NONE

//        //! For std::find()
//        bool operator ==(const std ::string& o) const {
//        return !ASSIMP_stricmp(o, name);
//    }
    }

    /** @brief Tiny utility data structure to hold a .shader data block */
    class ShaderDataBlock {
        /** Name of referenced data element */
        var name = ""
        /** Cull mode for the element   */
        var cull = ShaderCullMode.CW
        /** Maps defined in the shader  */
        val maps = ArrayList<ShaderMapBlock>()
//        //! For std::find()
//        bool operator ==(const std ::string& o) const {
//        return !ASSIMP_stricmp(o, name);
//    }
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
//    bool LoadShader(ShaderData& fill, const std::string& file,IOSystem* io)


    /** @brief Convert a Q3Shader to an aiMaterial
     *
     *  @param[out] out Material structure to be filled.
     *  @param[in] shader Input shader
     */
//    void ConvertShaderToMaterial(aiMaterial* out , const ShaderDataBlock& shader)

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
        return true;
    }
}

class Prova(override val info: AiImporterDesc) : BaseImporter() {
    override fun canRead(file: URI, checkSig: Boolean): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}


/** @brief Importer class to load MD3 files */
class MD3Importer : BaseImporter() {
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
    override fun canRead(file: URI, checkSig: Boolean): Boolean {
        val extension = file.extension
        if (extension == "md3") return true
        // if check for extension is not enough, check for the magic tokens
        if (extension.isNotEmpty() || checkSig) {
            TODO()
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
    override fun internReadFile(file: URI, scene: AiScene) {

        this.file = file.path
        this.scene = scene

        // get base path and file name
        // todo ... move to PathConverter
        filename = this.file.substringAfterLast(File.separator).toLowerCase()

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
        val surfaces = MD3.Surface(buffer.apply { position(header.ofsSurfaces) })
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
        val shaders = Q3Shader.ShaderData()
        readShader(shaders)

        // Adjust all texture paths in the shader
        shaders.blocks.forEach {
            TODO()
//            ConvertPath(( * dit).name.c_str(), header_name, (*dit).name)
//
//            for (std:: list < Q3Shader::ShaderMapBlock > ::iterator mit =(*dit).maps.begin(); mit != ( * dit).maps.end(); ++mit) {
//            ConvertPath(( * mit).name.c_str(), header_name, (*mit).name)
//        }
        }

        // Read all surfaces from the file
        var iNum = header.numSurfaces
        var iNumMaterials = 0
        while (iNum-- > 0) {
            // Validate the surface header
            surfaces.validateOffsets(buffer)
            // Navigate to the vertex list of the surface
            val vertices = MD3.Vertex(buffer.apply { header.ofsSurfaces + surfaces.ofsXyzNormal })
            // Navigate to the triangle list of the surface
//            val triangles = MD3 .Triangle *)
//            (((uint8_t *) pcSurfaces) + pcSurfaces->OFS_TRIANGLES)
//
//            // Navigate to the texture coordinate list of the surface
//            BE_NCONST MD3 ::TexCoord * pcUVs = (BE_NCONST MD3 ::TexCoord *)
//            (((uint8_t *) pcSurfaces) + pcSurfaces->OFS_ST)
//
//            // Navigate to the shader list of the surface
//            BE_NCONST MD3 ::Shader * pcShaders = (BE_NCONST MD3 ::Shader *)
//            (((uint8_t *) pcSurfaces) + pcSurfaces->OFS_SHADERS)
//
//            // If the submesh is empty ignore it
//            if (0 == pcSurfaces->NUM_VERTICES || 0 == pcSurfaces->NUM_TRIANGLES)
//            {
//                pcSurfaces = (BE_NCONST MD3 ::Surface *)(((uint8_t *) pcSurfaces) + pcSurfaces->OFS_END)
//                pScene->mNumMeshes--
//                continue
//            }
//
//            // Allocate output mesh
//            pScene->mMeshes[iNum] = new aiMesh()
//            aiMesh * pcMesh = pScene->mMeshes[iNum]
//
//            std::string _texture_name
//                    const char * texture_name = NULL
//
//                    // Check whether we have a texture record for this surface in the .skin file
//                    std::list < Q3Shader::SkinData::TextureEntry > ::iterator it = std ::find(
//                    skins.textures.begin(), skins.textures.end(), pcSurfaces->NAME)
//
//            if (it != skins.textures.end()) {
//                texture_name = & * (_texture_name = ( * it).second).begin()
//                DefaultLogger::get()->debug("MD3: Assigning skin texture "+(*it).second+" to surface "+pcSurfaces->NAME)
//                ( * it).resolved = true // mark entry as resolved
//            }
//
//            // Get the first shader (= texture?) assigned to the surface
//            if (!texture_name && pcSurfaces->NUM_SHADER)    {
//                texture_name = pcShaders->NAME
//            }
//
//            std::string convertedPath
//                    if (texture_name) {
//                        ConvertPath(texture_name, headerName, convertedPath)
//                    }
//
//            const Q3Shader ::ShaderDataBlock * shader = NULL
//
//            // Now search the current shader for a record with this name (
//            // excluding texture file extension)
//            if (!shaders.blocks.empty()) {
//
//                std::string::size_type s = convertedPath . find_last_of ('.')
//                if (s == std::string::npos)
//                    s = convertedPath.length()
//
//                const std ::string without_ext = convertedPath . substr (0, s)
//                std::list < Q3Shader::ShaderDataBlock > ::const_iterator dit = std ::find(shaders.blocks.begin(), shaders.blocks.end(), without_ext)
//                if (dit != shaders.blocks.end()) {
//                    // Hurra, wir haben einen. Tolle Sache.
//                    shader = & * dit
//                    DefaultLogger::get()->info("Found shader record for "+without_ext)
//                } else DefaultLogger::get()->warn("Unable to find shader record for "+without_ext)
//            }
//
//            aiMaterial * pcHelper = new aiMaterial ()
//
//            const int iMode = (int) aiShadingMode_Gouraud
//                    pcHelper->AddProperty<int>(&iMode, 1, AI_MATKEY_SHADING_MODEL)
//
//            // Add a small ambient color value - Quake 3 seems to have one
//            aiColor3D clr
//                    clr.b = clr.g = clr.r = 0.05f
//            pcHelper->AddProperty<aiColor3D>(&clr, 1, AI_MATKEY_COLOR_AMBIENT)
//
//            clr.b = clr.g = clr.r = 1.0f
//            pcHelper->AddProperty<aiColor3D>(&clr, 1, AI_MATKEY_COLOR_DIFFUSE)
//            pcHelper->AddProperty<aiColor3D>(&clr, 1, AI_MATKEY_COLOR_SPECULAR)
//
//            // use surface name + skin_name as material name
//            aiString name
//                    name.Set("MD3_[" + configSkinFile + "][" + pcSurfaces->NAME+"]")
//            pcHelper->AddProperty(&name, AI_MATKEY_NAME)
//
//            if (!shader) {
//                // Setup dummy texture file name to ensure UV coordinates are kept during postprocessing
//                aiString szString
//                        if (convertedPath.length()) {
//                            szString.Set(convertedPath)
//                        } else {
//                            DefaultLogger::get()->warn("Texture file name has zero length. Using default name")
//                            szString.Set("dummy_texture.bmp")
//                        }
//                pcHelper->AddProperty(&szString, AI_MATKEY_TEXTURE_DIFFUSE(0))
//
//                // prevent transparency by default
//                int no_alpha = aiTextureFlags_IgnoreAlpha
//                        pcHelper->AddProperty(&no_alpha, 1, AI_MATKEY_TEXFLAGS_DIFFUSE(0))
//            } else {
//                Q3Shader::ConvertShaderToMaterial(pcHelper, *shader)
//            }
//
//            pScene->mMaterials[iNumMaterials] = (aiMaterial*)pcHelper
//            pcMesh->mMaterialIndex = iNumMaterials++
//
//            // Ensure correct endianness
//            #ifdef AI_BUILD_BIG_ENDIAN
//
//                    for (uint32_t i = 0; i < pcSurfaces->NUM_VERTICES;++i)  {
//                AI_SWAP2(pcVertices[i].NORMAL)
//                AI_SWAP2(pcVertices[i].X)
//                AI_SWAP2(pcVertices[i].Y)
//                AI_SWAP2(pcVertices[i].Z)
//
//                AI_SWAP4(pcUVs[i].U)
//                AI_SWAP4(pcUVs[i].U)
//            }
//            for (uint32_t i = 0; i < pcSurfaces->NUM_TRIANGLES;++i) {
//                AI_SWAP4(pcTriangles[i].INDEXES[0])
//                AI_SWAP4(pcTriangles[i].INDEXES[1])
//                AI_SWAP4(pcTriangles[i].INDEXES[2])
//            }
//
//            #endif
//
//            // Fill mesh information
//            pcMesh->mPrimitiveTypes = aiPrimitiveType_TRIANGLE
//
//            pcMesh->mNumVertices = pcSurfaces->NUM_TRIANGLES*3
//            pcMesh->mNumFaces = pcSurfaces->NUM_TRIANGLES
//            pcMesh->mFaces = new aiFace[pcSurfaces->NUM_TRIANGLES]
//            pcMesh->mNormals = new aiVector3D[pcMesh->mNumVertices]
//            pcMesh->mVertices = new aiVector3D[pcMesh->mNumVertices]
//            pcMesh->mTextureCoords[0] = new aiVector3D[pcMesh->mNumVertices]
//            pcMesh->mNumUVComponents[0] = 2
//
//            // Fill in all triangles
//            unsigned int iCurrent = 0
//            for (unsigned int i = 0; i < (unsigned int) pcSurfaces->NUM_TRIANGLES;++i)   { pcMesh ->
//                mFaces[i].mIndices = new unsigned int[3]
//                pcMesh->mFaces[i].mNumIndices = 3
//
//                //unsigned int iTemp = iCurrent;
//                for (unsigned int c = 0; c < 3;++c, ++iCurrent)  { pcMesh ->
//                mFaces[i].mIndices[c] = iCurrent
//
//                // Read vertices
//                aiVector3D& vec = pcMesh->mVertices[iCurrent]
//                uint32_t index = pcTriangles->INDEXES[c]
//                if (index >= pcSurfaces->NUM_VERTICES) {
//                throw DeadlyImportError("MD3: Invalid vertex index")
//            }
//                vec.x = pcVertices[index].X * AI_MD3_XYZ_SCALE
//                vec.y = pcVertices[index].Y * AI_MD3_XYZ_SCALE
//                vec.z = pcVertices[index].Z * AI_MD3_XYZ_SCALE
//
//                // Convert the normal vector to uncompressed float3 format
//                aiVector3D& nor = pcMesh->mNormals[iCurrent]
//                LatLngNormalToVec3(pcVertices[pcTriangles->INDEXES[c]].NORMAL, (ai_real*)&nor)
//
//                // Read texture coordinates
//                pcMesh->mTextureCoords[0][iCurrent].x = pcUVs[pcTriangles->INDEXES[c]].U
//                pcMesh->mTextureCoords[0][iCurrent].y = 1.0f-pcUVs[pcTriangles->INDEXES[c]].V
//            }
//                // Flip face order if necessary
//                if (!shader || shader->cull == Q3Shader::CULL_CW) {
//                std::swap(pcMesh->mFaces[i].mIndices[2], pcMesh->mFaces[i].mIndices[1])
//            }
//                pcTriangles++
//            }
//
//            // Go to the next surface
//            pcSurfaces = (BE_NCONST MD3 ::Surface *)(((unsigned char *) pcSurfaces) + pcSurfaces->OFS_END)
        }

        // For debugging purposes: check whether we found matches for all entries in the skins file
//        if (!DefaultLogger::isNullLogger()) {
//            for (std:: list < Q3Shader::SkinData::TextureEntry > ::const_iterator it = skins . textures . begin ();it != skins.textures.end(); ++it) {
//                if (!( * it).resolved) {
//                DefaultLogger::get()->error("MD3: Failed to match skin "+(*it).first+" to surface "+(*it).second)
//            }
//            }
//        }
//
//        if (!pScene->mNumMeshes)
//        throw DeadlyImportError("MD3: File contains no valid mesh")
//        pScene->mNumMaterials = iNumMaterials
//
//        // Now we need to generate an empty node graph
//        pScene->mRootNode = new aiNode("<MD3Root>")
//        pScene->mRootNode->mNumMeshes = pScene->mNumMeshes
//        pScene->mRootNode->mMeshes = new unsigned int[pScene->mNumMeshes]
//
//        // Attach tiny children for all tags
//        if (pcHeader->NUM_TAGS) { pScene ->
//            mRootNode->mNumChildren = pcHeader->NUM_TAGS
//            pScene->mRootNode->mChildren = new aiNode*[pcHeader->NUM_TAGS]
//
//            for (unsigned int i = 0; i < pcHeader->NUM_TAGS; ++i, ++pcTags) {
//
//            aiNode * nd = pScene->mRootNode->mChildren[i] = new aiNode()
//            nd->mName.Set((const char*)pcTags->NAME)
//            nd->mParent = pScene->mRootNode
//
//            AI_SWAP4(pcTags->origin.x)
//            AI_SWAP4(pcTags->origin.y)
//            AI_SWAP4(pcTags->origin.z)
//
//            // Copy local origin, again flip z,y
//            nd->mTransformation.a4 = pcTags->origin.x
//            nd->mTransformation.b4 = pcTags->origin.y
//            nd->mTransformation.c4 = pcTags->origin.z
//
//            // Copy rest of transformation (need to transpose to match row-order matrix)
//            for (unsigned int a = 0; a < 3;++a) {
//            for (unsigned int m = 0; m < 3;++m) { nd ->
//            mTransformation[m][a] = pcTags->orientation[a][m]
//            AI_SWAP4(nd->mTransformation[m][a])
//        }
//        }
//        }
//        }
//
//        for (unsigned int i = 0; i < pScene->mNumMeshes;++i)
//        pScene->mRootNode->mMeshes[i] = i
//
//        // Now rotate the whole scene 90 degrees around the x axis to convert to internal coordinate system
//        pScene->mRootNode->mTransformation = aiMatrix4x4(1.f, 0.f, 0.f, 0.f,
//        0.f, 0.f, 1.f, 0.f, 0.f, -1.f, 0.f, 0.f, 0.f, 0.f, 0.f, 1.f)
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
        val suffix = filename.substring(s, t - s)

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
        TODO()
//        const std::string::size_type s = path.find_last_of("\\/",path.length()-2);
//        const std::string model_file = path.substr(s+1,path.length()-(s+2));
//
//        // If no specific dir or file is given, use our default search behaviour
//        if (!configShaderFile.length()) {
//            if(!Q3Shader::LoadShader(fill,path + "..\\..\\..\\scripts\\" + model_file + ".shader",mIOHandler)) {
//                Q3Shader::LoadShader(fill,path + "..\\..\\..\\scripts\\" + filename + ".shader",mIOHandler);
//            }
//        }
//        else {
//            // If the given string specifies a file, load this file.
//            // Otherwise it's a directory.
//            const std::string::size_type st = configShaderFile.find_last_of('.');
//            if (st == std::string::npos) {
//
//                if(!Q3Shader::LoadShader(fill,configShaderFile + model_file + ".shader",mIOHandler)) {
//                    Q3Shader::LoadShader(fill,configShaderFile + filename + ".shader",mIOHandler);
//                }
//            }
//            else {
//                Q3Shader::LoadShader(fill,configShaderFile,mIOHandler);
//            }
//        }
    }

    // -------------------------------------------------------------------
    /** Convert a texture path in a MD3 file to a proper value
     *  @param[in] texture_name Path to be converted
     *  @param[in] header_path Base path specified in MD3 header
     *  @param[out] out Receives the converted output string
     */
//    void ConvertPath (const char * texture_name, const char* header_path,
//    std::string& out ) const
}