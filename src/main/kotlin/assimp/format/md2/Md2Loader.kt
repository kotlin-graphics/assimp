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

/** @file  MD2Loader.h
 *  @brief Declaration of the .MD2 importer class.
 */

package assimp.format.md2

import assimp.*
import assimp.format.AiConfig
import glm_.b
import glm_.f
import glm_.i
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** Importer class for MD2 */
class Md2Importer : BaseImporter() {

    /** Configuration option: frame to be loaded */
    var configFrameID = 0

    /** Header of the MD2 file */
    lateinit var header: MD2.Header

    /** Buffer to hold the loaded file */
//    BE_NCONST uint8_t * mBuffer;

    /** Size of the file, in bytes */
    var fileSize = 0

    /** Returns whether the class can handle the format of the given file.
     *  See BaseImporter.canRead() for details. */
    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {
        val extension = getExtension(file)
        if (extension == "md2") return true

        // if check for extension is not enough, check for the magic tokens
        if (extension.isEmpty() || checkSig) {
            TODO()
//            uint32_t tokens[1];
//            tokens[0] = AI_MD2_MAGIC_NUMBER_LE;
//            return CheckMagicToken(pIOHandler,pFile,tokens,1);
        }
        return false
    }

    /** Called prior to readFile().
     *  The function is a request to the importer to update its configuration basing on the Importer's configuration
     *  property list.  */
    override fun setupProperties(imp: Importer) {
        //  The AI_CONFIG_IMPORT_MD2_KEYFRAME option overrides the AI_CONFIG_IMPORT_GLOBAL_KEYFRAME option.
        configFrameID = imp[AiConfig.Import.MD2_KEYFRAME] ?: imp[AiConfig.Import.GLOBAL_KEYFRAME] ?: 0
    }

    /** Return importer meta information.
     *  See BaseImporter.info for the details
     */

    override val info
        get() = AiImporterDesc(
                name = "Quake II Mesh Importer",
                flags = AiImporterFlags.SupportBinaryFlavour.i,
                fileExtensions = listOf("md2"))

    /** Imports the given file into the given scene structure.
     *  See BaseImporter.internReadFile() for details     */
    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        val file = File(file)

        // Check whether we can read from the file
        if (!file.canRead())
            throw Error("Failed to open MD2 file $file")

        // check whether the md3 file is large enough to contain at least the file header
        fileSize = file.length().i
        if (fileSize < MD2.Header.size) throw Error("MD2 File is too small")

        val fileChannel = RandomAccessFile(file, "r").channel
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()).order(ByteOrder.nativeOrder())

        header = MD2.Header(buffer).apply { validate(fileSize, configFrameID) }

        // there won't be more than one mesh inside the file
        scene.numMaterials = 1
        scene.rootNode = AiNode().apply {
            numMeshes = 1
            meshes = intArrayOf(0)
        }
        scene.materials.add(AiMaterial())
        scene.numMeshes = 1
        val mesh = AiMesh(primitiveTypes = AiPrimitiveType.TRIANGLE.i).also { scene.meshes.add(it) }

        // navigate to the begin of the frame data
        val framePos = header.offsetFrames + MD2.Frame.size(header.numVertices) * configFrameID
        val frame = MD2.Frame(buffer.apply { position(framePos) }, header.numVertices)

        // navigate to the begin of the triangle data
        buffer.position(header.offsetTriangles)
        val triangles = Array(header.numTriangles, { MD2.Triangle(buffer) })

        // navigate to the begin of the tex coords data
        buffer.position(header.offsetTexCoords)
        val texCoords = Array(header.numTexCoords, { MD2.TexCoord(buffer) })

        // navigate to the begin of the vertex data
        buffer.position(framePos + MD2.Frame.verticesOffset)
        val verts = frame.vertices

        mesh.apply { numFaces = header.numTriangles; numVertices = numFaces * 3; }

        /*  Not sure whether there are MD2 files without texture coordinates
            NOTE: texture coordinates can be there without a texture, but a texture can't be there without a valid UV channel   */
        val helper = scene.materials[0].apply { shadingModel = AiShadingMode.gouraud }

        if (header.numTexCoords != 0 && header.numSkins != 0) {
            // navigate to the first texture associated with the mesh
            buffer.position(MD2.Header.size + header.offsetSkins)
            val skins = MD2.Skin(buffer)

            helper.color = AiMaterial.Color(diffuse = AiVector3D(1f), specular = AiVector3D(1f), ambient = AiVector3D(0.05f))

            if (skins.name.isNotEmpty())
                helper.textures.add(AiMaterial.Texture(file = skins.name))
            else
                logger.warn { "Texture file name has zero length. It will be skipped." }
        } else {
            // apply a default material
            helper.color = AiMaterial.Color(diffuse = AiColor3D(0.6f), specular = AiColor3D(0.6f), ambient = AiColor3D(0.05f))
            helper.name = AI_DEFAULT_MATERIAL_NAME
            val fileName = file.name.substringAfterLast('\\').substringBeforeLast('.')
            helper.textures.add(AiMaterial.Texture(file = "$fileName.bmp", type = AiTexture.Type.diffuse))
        }

        // now read all triangles of the first frame, apply scaling and translation
        var current = 0

        var divisorU = 1f
        var divisorV = 1f
        if (header.numTexCoords != 0) {
            // allocate storage for texture coordinates, too
//            mesh.textureCoords[0] = new aiVector3D [pcMesh->mNumVertices]
//            pcMesh->mNumUVComponents[0] = 2

            // check whether the skin width or height are zero (this would cause a division through zero)
            if (header.skinWidth == 0) logger.error { "MD2: No valid skin width given" }
            else divisorU = header.skinWidth.f
            if (header.skinHeight == 0) logger.error { "MD2: No valid skin height given" }
            else divisorV = header.skinHeight.f
        }
        for (i in 0 until header.numTriangles) {
            // Allocate the face
            scene.meshes[0].faces.add(MutableList(3, { 0 }))

            /*  copy texture coordinates
                check whether they are different from the previous value at this index.
                In this case, create a full separate set of vertices/normals/texcoords  */
            for (c in 0..2) {
                // validate vertex indices
                var index = triangles[i].vertexIndices[c]
                if (index >= header.numVertices) {
                    logger.error { "MD2: Vertex index is outside the allowed range" }
                    index = header.numVertices - 1
                }
                // read x,y, and z component of the vertex, flip z and y to become right-handed
                mesh.vertices.add(AiVector3D(
                        verts[index].vertex[0] * frame.scale[0] + frame.translate[0],
                        verts[index].vertex[2] * frame.scale[2] + frame.translate[2],
                        verts[index].vertex[1] * frame.scale[1] + frame.translate[1]))

                // read the normal vector from the precalculated normal table
                mesh.normals.add(AiVector3D(MD2.lookupNormalIndex(verts[index].lightNormalIndex)))

                if (header.numTexCoords != 0) {
                    // validate texture coordinates
                    index = triangles[i].textureIndices[c]
                    if (index >= header.numTexCoords) {
                        logger.error { "MD2: UV index is outside the allowed range" }
                        index = header.numTexCoords - 1
                    }

                    if (mesh.textureCoords.isEmpty()) mesh.textureCoords.add(mutableListOf())
                    // the texture coordinates are absolute values but we need relative values between 0 and 1
                    mesh.textureCoords[0].add(
                            floatArrayOf(texCoords[index].s / divisorU, 1f - texCoords[index].t / divisorV))
                }
                scene.meshes[0].faces[i][c] = current++
            }
        }
    }
}
