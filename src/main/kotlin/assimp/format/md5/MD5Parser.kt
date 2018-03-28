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

package assimp.format.md5

import assimp.*
import glm_.f
import glm_.i

/** @file  MD5Parser.h
 *  @brief Definition of the .MD5 parser class.
 *  http://www.modwiki.net/wiki/MD5_(file_format)
 */


/** Represents a single element in a MD5 file
 *
 *  Elements are always contained in sections.
 */
class Element {
    /** Points to the starting point of the element
     *  Whitespace at the beginning and at the end have been removed,
     *  Elements are terminated with \0 */
    var szStart = ""
    /** Original line number (can be used in error messages if a parsing error occurs) */
    var iLineNumber = 0
}

/** Represents a section of a MD5 file (such as the mesh or the joints section)
 *
 *  A section is always enclosed in { and } brackets. */
class Section {
    /** Original line number (can be used in error messages if a parsing error occurs) */
    var iLineNumber = 0
    /** List of all elements which have been parsed in this section.    */
    val elements = ArrayList<Element>()
    /** Name of the section */
    var name = ""
    /** For global elements: the value of the element as string
     *  if isEmpty() the section is not a global element */
    var globalValue = ""
}

/** Basic information about a joint */
open class BaseJointDescription {
    /** Name of the bone */
    var name = ""
    /** Parent index of the bone */
    var parentIndex = 0
}

/** Represents a bone (joint) descriptor in a MD5Mesh file */
class BoneDesc : BaseJointDescription() {
    /** Absolute position of the bone */
    val positionXYZ = AiVector3D()
    /** Absolute rotation of the bone */
    val rotationQuat = AiVector3D()
    val rotationQuatConverted = AiQuaternion()
    /** Absolute transformation of the bone (temporary) */
    val transform = AiMatrix4x4()
    /* Inverse transformation of the bone (temporary) */
    val invTransform = AiMatrix4x4()
    /** Internal */
    var map = 0
}

/** Represents a bone (joint) descriptor in a MD5Anim file */
class AnimBoneDesc : BaseJointDescription() {
    /** Flags (AI_MD5_ANIMATION_FLAG_xxx) */
    var flags = 0
    /** Index of the first key that corresponds to this anim bone */
    var firstKeyIndex = 0
}

/** Represents a base frame descriptor in a MD5Anim file */
open class BaseFrameDesc {
    val positionXYZ = AiVector3D()
    val rotationQuat = AiVector3D()
}

/** Represents a camera animation frame in a MDCamera file */
class CameraAnimFrameDesc : BaseFrameDesc() {
    var fov = 0f
}

/** Represents a frame descriptor in a MD5Anim file */
class FrameDesc {
    /** Index of the frame */
    var index = 0
    /** Animation keyframes - a large blob of data at first */
    val values = ArrayList<Float>()
}

/** Represents a vertex  descriptor in a MD5 file */
class VertexDesc {
    /** UV cordinate of the vertex */
    val uv = AiVector2D()
    /** Index of the first weight of the vertex in the vertex weight list */
    var firstWeight = 0
    /** Number of weights assigned to this vertex */
    var numWeights = 0
}

/** Represents a vertex weight descriptor in a MD5 file */
class WeightDesc {
    /** Index of the bone to which this weight refers */
    var bone = 0
    /** The weight value */
    var weight = 0f
    /** The offset position of this weight (in the coordinate system defined by the parent bone) */
    val offsetPosition = AiVector3D()
}

/** Represents a mesh in a MD5 file */
class MeshDesc {
    /** Weights of the mesh */
    val weights = ArrayList<WeightDesc>()
    /** Vertices of the mesh */
    val vertices = ArrayList<VertexDesc>()
    /** Faces of the mesh */
    val faces = ArrayList<AiFace>()
    /** Name of the shader (=texture) to be assigned to the mesh */
    var shader = ""
}

// Convert a quaternion to its usual representation
//inline void ConvertQuaternion(const aiVector3D & in , aiQuaternion& out ) {
//
//    out.x = in.x
//    out.y = in.y
//    out.z = in.z
//
//    const float t = 1.0f - ( in . x * in . x)-( in .y* in .y)-( in .z* in .z)
//
//    if (t < 0.0f)
//        out.w = 0.0f
//    else out.w = std::sqrt (t)
//
//    // Assimp convention.
//    out.w *= -1.f
//}

/** Parses the data sections of a MD5 mesh file */
class MD5MeshParser
/** Constructs a new MD5MeshParser instance from an existing preparsed list of file sections.
 *
 *  @param mSections List of file sections (output of MD5Parser)     */
constructor(sections: ArrayList<Section>) {
    /** List of all meshes */
    val meshes = ArrayList<MeshDesc>()
    /** List of all joints */
    val joints = ArrayList<BoneDesc>()

    init {
        logger.debug { "MD5MeshParser begin" }
        // now parse all sections
        sections.forEach {
            when (it.name) {
                "numMeshes" -> meshes.ensureCapacity(it.globalValue.i)
                "numJoints" -> joints.ensureCapacity(it.globalValue.i)
                "joints" -> {
                    // "origin" -1 ( -0.000000 0.016430 -0.006044 ) ( 0.707107 0.000000 0.707107 )
                    it.elements.forEach {
                        val desc = BoneDesc()
                        joints.add(desc.apply {
                            val words = it.szStart.words
                            name = words[0].trim('"')
                            // negative values, at least -1, is allowed here
                            parentIndex = words[1].i
                            READ_TRIPLE(positionXYZ, words, 2, it.iLineNumber)
                            READ_TRIPLE(rotationQuat, words, 7, it.iLineNumber) // normalized quaternion, so w is not there
                        })
                    }
                }
                "mesh" -> {
                    val desc = MeshDesc()
                    meshes.add(desc)
                    for (elem in it.elements) {
                        val sz = elem.szStart.words
                        when (sz[0]) {
                        // shader attribute
                            "shader" -> desc.shader = sz[1].trim('"')
                        // numverts attribute
                            "numverts" -> for (i in 0 until sz[1].i) desc.vertices.add(VertexDesc())
                        // numtris attribute
                            "numtris" -> for (i in 0 until sz[1].i) desc.faces.add(mutableListOf())
                        // numweights attribute
                            "numweights" -> for (i in 0 until sz[1].i) desc.weights.add(WeightDesc())
                        // vert attribute, ex: "vert 0 ( 0.394531 0.513672 ) 0 1"
                            "vert" -> with(desc.vertices[sz[1].i]) {
                                if ("(" != sz[2]) MD5Parser.reportWarning("Unexpected token: ( was expected", elem.iLineNumber)
                                uv.put(sz, 3)
                                if (")" != sz[5]) MD5Parser.reportWarning("Unexpected token: ) was expected", elem.iLineNumber)
                                firstWeight = sz[6].i
                                numWeights = sz[7].i
                            }
                        // tri attribute, ex: "tri 0 15 13 12"
                            "tri" -> with(desc.faces[sz[1].i]) { for (i in 0..2) add(sz[2 + i].i) }
                        // weight attribute, ex: "weight 362 5 0.500000 ( -3.553583 11.893474 9.719339 )"
                            "weight" -> with(desc.weights[sz[1].i]) {
                                bone = sz[2].i
                                weight = sz[3].f
                                READ_TRIPLE(offsetPosition, sz, 4, it.iLineNumber)
                            }
                        }
                    }
                }
            }
        }
        logger.debug { "MD5MeshParser end" }
    }
}

/** Parses the data sections of a MD5 animation file */
class MD5AnimParser {

    /** Constructs a new MD5AnimParser instance from an existing preparsed list of file sections.
     *
     *  @param mSections List of file sections (output of MD5Parser)     */
//    explicit MD5AnimParser(SectionList& mSections)

    /** Output frame rate */
    var frameRate = 0f
    /** List of animation bones */
    val animatedBones = ArrayList<AnimBoneDesc>()
    /** List of base frames */
    val baseFrames = ArrayList<BaseFrameDesc>()
    /** List of animation frames */
    val frames = ArrayList<FrameDesc>()
    /** Number of animated components */
    var numAnimatedComponents = 0
}

/** Parses the data sections of a MD5 camera animation file */
class MD5CameraParser {

    /** Constructs a new MD5CameraParser instance from an existing preparsed list of file sections.
     *
     *  @param mSections List of file sections (output of MD5Parser)     */
//    explicit MD5CameraParser(SectionList& mSections)

    /** Output frame rate */
    var frameRate = 0f
    /** List of cuts */
    val cuts = ArrayList<Int>()
    /** Frames */
    val frames = ArrayList<CameraAnimFrameDesc>()
}

/** Parses the block structure of MD5MESH and MD5ANIM files (but does no further processing) */
class MD5Parser(val lines: ArrayList<String>) {

    var lineNumber = 0
    var line = ""
    /** List of all sections which have been read */
    val sections = ArrayList<Section>()

    init {
        assert(lines.isNotEmpty())
        logger.debug { "MD5Parser begin" }
        // parse the file header
        parseHeader()
        // and read all sections until we're finished
        while (true) {
            val sec = Section()
            sections.add(sec)
            if (!sec.parse()) break
        }
        logger.debug("MD5Parser end. Parsed ${sections.size} sections")
    }

    fun reportError(error: String): Nothing = reportError(error, lineNumber)
    fun reportWarning(warn: String) = reportWarning(warn, lineNumber)

    /** Parses a file section. The current file pointer must be outside of a section.
     *  @param out Receives the section data
     *  @return true if the end of the file has been reached
     *  @throws ImportErrorException if an error occurs
     */
    fun Section.parse(): Boolean {
        if (lineNumber >= lines.size) return true
        // store the current line number for use in error messages
        iLineNumber = lineNumber
        line = lines[lineNumber++]
        val words = line.words
        // first parse the name of the section
        name = words[0]

        when (words[1]) {
            "{" -> { // it is a normal section so read all lines
                line = lines[lineNumber++]
                while (line[0] != '}') {
                    elements.add(Element().apply {
                        iLineNumber = lineNumber
                        szStart = line
                    })
                    line = lines[lineNumber++]
                }
            }
            else -> globalValue = words[1] // it is an element at global scope. Parse its value and go on
        }

        return lineNumber < lines.size
    }

    /** Parses the file header
     *  @throws ImportErrorException if an error occurs     */
    fun parseHeader() {
        // parse and validate the file version
        val words = lines[lineNumber++].words
        if (!words[0].startsWith("MD5Version")) reportError("Invalid MD5 file: MD5Version tag has not been found")
        val iVer = words[1].i
        if (10 != iVer) reportError("MD5 version tag is unknown (10 is expected)")
        skipLine()
        // print the command line options to the console
        // FIX: can break the log length limit, so we need to be careful
        logger.info { line }
    }

    fun skipLine() {
        line = lines[lineNumber++]
    }

    companion object {
        /** Report a specific error message and throw an exception
         *  @param error Error message to be reported
         *  @param line Index of the line where the error occurred     */
        fun reportError(error: String, line: Int): Nothing = throw Error("[MD5] Line $line: $error")

        /** Report a specific warning
         *  @param warn Warn message to be reported
         *  @param line Index of the line where the error occurred     */
        fun reportWarning(warn: String, line: Int) = logger.warn { "[MD5] Line $line: $warn" }
    }
}

/** read a triple float in brackets: (1.0 1.0 1.0) */
private fun READ_TRIPLE(vec: AiVector3D, words: List<String>, index: Int, lineNumber: Int) {
    if ("(" != words[index]) MD5Parser.reportWarning("Unexpected token: ( was expected", lineNumber)
    vec.put(words, index + 1)
    if (")" != words[index + 4]) MD5Parser.reportWarning("Unexpected token: ) was expected", lineNumber)
}