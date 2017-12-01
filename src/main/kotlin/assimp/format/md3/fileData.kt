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
import glm_.*
import glm_.vec3.Vec3
import unsigned.ushr
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import assimp.AI_INT_MERGE_SCENE as Ms

object MD3 {

    // to make it easier for us, we test the magic word against both "endianesses"
    val MAGIC_NUMBER_BE = AI_MAKE_MAGIC("IDP3")
    val MAGIC_NUMBER_LE = AI_MAKE_MAGIC("3PDI")

    // common limitations
    val VERSION = 15
    val MAXQPATH = 64
    val MAXFRAME = 16
    val MAX_FRAMES = 1024
    val MAX_TAGS = 16
    val MAX_SURFACES = 32
    val MAX_SHADERS = 256
    val MAX_VERTS = 4096
    val MAX_TRIANGLES = 8192

    /** master scale factor for all vertices in a MD3 model */
    val XYZ_SCALE = 1f / 64f

    /** @brief Data structure for the MD3 main header     */
    class Header(buffer: ByteBuffer) {
        /** magic number    */
        val ident = buffer.int
        /** file format version */
        val version = buffer.int
        /** original name in .pak archive   */
        val name = String(ByteArray(MAXQPATH, { buffer.get() })).filter { it != NUL }
        /** unknown */
        val flags = buffer.int
        /** number of frames in the file    */
        val numFrames = buffer.int
        /** number of tags in the file  */
        val numTags = buffer.int
        /** number of surfaces in the file  */
        val numSurfaces = buffer.int
        /** number of skins in the file */
        val numSkins = buffer.int
        /** offset of the first frame   */
        val ofsFrames = buffer.int
        /** offset of the first tag */
        val ofsTags = buffer.int
        /** offset of the first surface */
        val ofsSurfaces = buffer.int
        /** end of file */
        val ofsEof = buffer.int

        /** Validate offsets in the header     */
        fun validateOffsets(fileSize: Int, configFrameID: Int) {
            // Check magic number
            if (ident != MAGIC_NUMBER_BE && ident != MAGIC_NUMBER_LE) throw Error("Invalid MD3 file: Magic bytes not found")
            // Check file format version
            if (version > 15) logger.warn { "Unsupported MD3 file version. Continuing happily ..." }
            // Check some offset values whether they are valid
            if (numSurfaces == 0) throw Error("Invalid md3 file: NUM_SURFACES is 0")
            if (ofsFrames >= fileSize || ofsSurfaces >= fileSize || ofsEof > fileSize) throw Error("Invalid MD3 header: some offsets are outside the file")
            if (numSurfaces > AI_MAX_ALLOC(MD3.Surface.size)) throw Error("Invalid MD3 header: too many surfaces, would overflow")
            if (ofsSurfaces + numSurfaces * MD3.Surface.size >= fileSize) throw Error("Invalid MD3 header: some surfaces are outside the file")
            if (numFrames <= configFrameID) throw Error("The requested frame is not existing the file")
        }

        companion object {
            val size = 11 * Int.BYTES + MAXQPATH
        }
    }

    /** @brief Data structure for the frame header */
    class Frame(buffer: ByteBuffer) {
        /** minimum bounds  */
        val min = AiVector3D(buffer).also { buffer.position(buffer.position() + Vec3.size) }
        /** maximum bounds  */
        val max = AiVector3D(buffer).also { buffer.position(buffer.position() + Vec3.size) }
        /** local origin for this frame */
        val origin = AiVector3D(buffer).also { buffer.position(buffer.position() + Vec3.size) }
        /** radius of bounding sphere   */
        val radius = buffer.float
        /** name of frame   */
        val name = String(ByteArray(MAXFRAME, { buffer.get() }))
    }

    /**
     * @brief Data structure for the tag header
     */
    class Tag(buffer: ByteBuffer) {
        /** name of the tag */
        val name = String(ByteArray(MAXQPATH, { buffer.get() })).filter { it != NUL }
        /** Local tag origin and orientation    */
        val origin = AiVector3D(buffer).also { buffer.position(buffer.position() + Vec3.size) }
        val orientation = FloatArray(9, { buffer.float }) // TODO mat3?
    }

    /** @brief Data structure for the surface header */
    class Surface(buffer: ByteBuffer) {
        /** magic number    */
        val indent = buffer.int
        /** original name of the surface    */
        val name = String(ByteArray(MAXQPATH, { buffer.get() })).filter { it != NUL }
        /** unknown */
        val flags = buffer.int
        /** number of frames in the surface */
        val numFrames = buffer.int
        /** number of shaders in the surface    */
        val numShader = buffer.int
        /** number of vertices in the surface   */
        val numVertices = buffer.int
        /** number of triangles in the surface  */
        val numTriangles = buffer.int
        /** offset to the triangle data */
        val ofsTriangles = buffer.int
        /** offset to the shader data   */
        val ofsShaders = buffer.int
        /** offset to the texture coordinate data   */
        val ofsSt = buffer.int
        /** offset to the vertex/normal data    */
        val ofsXyzNormal = buffer.int
        /** offset to the end of the Surface object */
        val ofsEnd = buffer.int

        fun validateOffsets(ofs: Int, fileSize: Int) {
            // Check whether all data chunks are inside the valid range
            if (ofsTriangles + ofs + numTriangles * MD3.Triangle.size > fileSize ||
                    ofsShaders + ofs + numShader * MD3.Shader.size > fileSize ||
                    ofsSt + ofs + numVertices * MD3.TexCoord.size > fileSize ||
                    ofsXyzNormal + ofs + numVertices * MD3.Vertex.size > fileSize)
                throw Error("Invalid MD3 surface header: some offsets are outside the file")
            // Check whether all requirements for Q3 files are met. We don't care, but probably someone does.
            if (numTriangles > MAX_TRIANGLES) logger.warn { "MD3: Quake III triangle limit exceeded" }
            if (numShader > MAX_SHADERS) logger.warn { "MD3: Quake III shader limit exceeded" }
            if (numVertices > MAX_VERTS) logger.warn { "MD3: Quake III vertex limit exceeded" }
            if (numFrames > MAX_FRAMES) logger.warn { "MD3: Quake III frame limit exceeded" }
        }

        companion object {
            val size = 11 * Int.BYTES + MAXQPATH
        }
    }

    /** @brief Data structure for a shader defined in there */
    class Shader(buffer: ByteBuffer) {
        /** filename of the shader  */
        val name = String(ByteArray(MAXQPATH, { buffer.get() })).filter { it != NUL }
        /** index of the shader */
        val shaderIndex = buffer.int

        companion object {
            val size = MAXQPATH + Int.BYTES
        }
    }

    /** @brief Data structure for a triangle */
    class Triangle(buffer: ByteBuffer) {
        /** triangle indices    */
        val indexes = IntArray(3, { buffer.int })

        companion object {
            val size = 3 * Int.BYTES
        }
    }

    /** @brief Data structure for an UV coord */
    class TexCoord(buffer: ByteBuffer) {
        /** UV coordinates  */
        val u = buffer.float
        val v = buffer.float

        companion object {
            val size = 2 * Float.BYTES
        }
    }

    /** @brief Data structure for a vertex */
    class Vertex(buffer: ByteBuffer) {
        /** X/Y/Z coordinates   */
        val x = buffer.short.i
        val y = buffer.short.i
        val z = buffer.short.i
        /** encoded normal vector   */
        val normal = buffer.short.i

        companion object {
            val size = 4 * Short.BYTES
        }
    }

    /** @brief Unpack a Q3 16 bit vector to its full float3 representation
     *
     *  @param p_iNormal Input normal vector in latitude/longitude form
     *  @return Pointer to an array of three floats to receive the result
     *
     *  @note This has been taken from q3 source (misc_model.c)
     */
    fun latLngNormalToVec3(iNormal: Short): AiVector3D    {
        val lat =((iNormal ushr 8) and 0xff) * PI * invVal
        val lng =(iNormal and 0xff).f  * PI * invVal
        return AiVector3D( cos(lat) * sin(lng), sin(lat) * sin(lng), cos(lng))
    }
    private val invVal = 1f / 128f

//
//// -------------------------------------------------------------------------------
//    /** @brief Pack a Q3 normal into 16bit latitute/longitude representation
//     *  @param p_vIn Input vector
//     *  @param p_iOut Output normal
//     *
//     *  @note This has been taken from q3 source (mathlib.c)
//     */
//    inline void Vec3NormalToLatLng( const aiVector3D& p_vIn, uint16_t& p_iOut )
//    {
//        // check for singularities
//        if (0.0f == p_vIn[0] && 0.0f == p_vIn[1]) {
//            if (p_vIn[2] > 0.0f) {
//                ((unsigned char *)&p_iOut)[0] = 0
//                ((unsigned char *)&p_iOut)[1] = 0       // lat = 0, long = 0
//            } else {
//                ((unsigned char *)&p_iOut)[0] = 128
//                ((unsigned char *)&p_iOut)[1] = 0       // lat = 0, long = 128
//            }
//        } else {
//            int a, b
//
//            a = int(57.2957795f * (std::atan2(p_vIn[1], p_vIn[0])) * (255.0f / 360.0f))
//            a & = 0xff
//
//            b = int(57.2957795f * (std::acos(p_vIn[2])) * (255.0f / 360.0f))
//            b & = 0xff
//
//            ((unsigned char *)&p_iOut)[0] = b   // longitude
//            ((unsigned char *)&p_iOut)[1] = a   // latitude
//        }
//    }
}