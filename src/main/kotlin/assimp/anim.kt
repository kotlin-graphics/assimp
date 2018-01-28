/*
---------------------------------------------------------------------------
Open Asset Import Library (assimp)
---------------------------------------------------------------------------

Copyright (c) 2006-2016, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the following
conditions are met:

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
---------------------------------------------------------------------------
*/

package assimp

import glm_.BYTES
import glm_.quat.Quat
import glm_.vec3.Vec3


/**
 * Created by elect on 29/01/2017.
 */

// ---------------------------------------------------------------------------
/** A time-value pair specifying a certain 3D vector for the given time. */
class AiVectorKey(
        /** The time of this key */
        var time: Double = 0.0,

        /** The value of this key */
        var value: AiVector3D = AiVector3D()) {

    companion object {
        val size = Double.BYTES + Vec3.size
    }
}

// ---------------------------------------------------------------------------
/** A time-value pair specifying a rotation for the given time.
 *  Rotations are expressed with quaternions. */
class AiQuatKey(
        /** The time of this key */
        var time: Double = 0.0,

        /** The value of this key */
        var value: AiQuaternion = AiQuaternion()
) {
    companion object {
        val size = Double.BYTES + Quat.size
    }
}

// ---------------------------------------------------------------------------
/** Binds a anim mesh to a specific point in time. */
class AiMeshKey(

        /** The time of this key */
        var mTime: Double = 0.0,

        /** Index into the aiMesh::mAnimMeshes array of the mesh corresponding to the #aiMeshAnim hosting this key frame. The referenced anim mesh is evaluated
         *  according to the rules defined in the docs for #aiAnimMesh.*/
        var mValue: Int = 0)

// ---------------------------------------------------------------------------
/** Binds a morph anim mesh to a specific point in time. */
class AiMeshMorphKey(
        /** The time of this key */
        var time: Double,
        /** The values and weights at the time of this key */
        var values: IntArray,
        var weights: DoubleArray,
        /** The number of values and weights */
        var numValuesAndWeights: Int)

// ---------------------------------------------------------------------------
/** Defines how an animation channel behaves outside the defined time
 *  range. This corresponds to aiNodeAnim::preState and
 *  aiNodeAnim::postState.*/
enum class AiAnimBehaviour(val i: Int) {
    /** The value from the default node transformation is taken*/
    DEFAULT(0x0),

    /** The nearest key value is used without interpolation */
    CONSTANT(0x1),

    /** The value of the nearest two keys is linearly
     *  extrapolated for the current time value.*/
    LINEAR(0x2),

    /** The animation is repeated.
     *
     *  If the animation key go from n to m and the current
     *  time is t, use the value at (t-n) % (|m-n|).*/
    REPEAT(0x3);

    companion object {
        fun of(i: Int) = values().first { it.i == i }
    }
}

// ---------------------------------------------------------------------------
/** Describes the animation of a single node. The name specifies the
 *  bone/node which is affected by this animation channel. The keyframes
 *  are given in three separate series of values, one each for position,
 *  rotation and scaling. The transformation matrix computed from these
 *  values replaces the node's original transformation matrix at a
 *  specific time.
 *  This means all keys are absolute and not relative to the bone default pose.
 *  The order in which the transformations are applied is
 *  - as usual - scaling, rotation, translation.
 *
 *  @note All keys are returned in their correct, chronological order.
 *  Duplicate keys don't pass the validation step. Most likely there
 *  will be no negative time values, but they are not forbidden also ( so
 *  implementations need to cope with them! ) */
class AiNodeAnim(
        /** The name of the node affected by this animation. The node
         *  must exist and it must be unique.*/
        var nodeName: String = "",

        /** The number of position keys */
        var numPositionKeys: Int = 0,

        /** The position keys of this animation channel. Positions are
         * specified as 3D vector. The array is numPositionKeys in size.
         *
         * If there are position keys, there will also be at least one
         * scaling and one rotation key.*/
        var positionKeys: ArrayList<AiVectorKey> = arrayListOf(),

        /** The number of rotation keys */
        var numRotationKeys: Int = 0,

        /** The rotation keys of this animation channel. Rotations are
         *  given as quaternions,  which are 4D vectors. The array is
         *  numRotationKeys in size.
         *
         * If there are rotation keys, there will also be at least one
         * scaling and one position key. */
        var rotationKeys: ArrayList<AiQuatKey> = arrayListOf(),

        /** The number of scaling keys */
        var numScalingKeys: Int = 0,

        /** The scaling keys of this animation channel. Scalings are
         *  specified as 3D vector. The array is numScalingKeys in size.
         *
         * If there are scaling keys, there will also be at least one
         * position and one rotation key.*/
        var scalingKeys: ArrayList<AiVectorKey> = arrayListOf(),

        /** Defines how the animation behaves before the first
         *  key is encountered.
         *
         *  The default value is aiAnimBehaviour_DEFAULT (the original
         *  transformation matrix of the affected node is used).*/
        var preState: AiAnimBehaviour = AiAnimBehaviour.DEFAULT,

        /** Defines how the animation behaves after the last
         *  key was processed.
         *
         *  The default value is aiAnimBehaviour_DEFAULT (the original
         *  transformation matrix of the affected node is taken).*/
        var postState: AiAnimBehaviour = AiAnimBehaviour.DEFAULT) {

    companion object {
        val size = 3 * Int.BYTES
    }
}

// ---------------------------------------------------------------------------
/** Describes vertex-based animations for a single mesh or a group of
 *  meshes. Meshes carry the animation data for each frame in their
 *  aiMesh::mAnimMeshes array. The purpose of aiMeshAnim is to
 *  define keyframes linking each mesh attachment to a particular
 *  point in time. */
class AiMeshAnim(
        /** Name of the mesh to be animated. An empty string is not allowed,
         *  animated meshes need to be named (not necessarily uniquely,
         *  the name can basically serve as wild-card to select a group
         *  of meshes with similar animation setup)*/
        var mName: String = "",

        /** Size of the #keys array. Must be 1, at least. */
        var mNumKeys: Int = 0,

        /** Key frames of the animation. May not be NULL. */
        var mKeys: List<assimp.AiMeshKey> = ArrayList())

// ---------------------------------------------------------------------------
/** Describes a morphing animation of a given mesh. */
class AiMeshMorphAnim(
        /** Name of the mesh to be animated. An empty string is not allowed, animated meshes need to be named
         *  (not necessarily uniquely, the name can basically serve as wildcard to select a group of meshes
         *  with similar animation setup)*/
        var name: String = "",
        /** Size of the #keys array. Must be 1, at least. */
        var numKeys: Int = 0,
        /** Key frames of the animation. May not be NULL. */
        var keys: Array<AiMeshMorphKey> = arrayOf())

// ---------------------------------------------------------------------------
/** An animation consists of key-frame data for a number of nodes. For
 *  each node affected by the animation a separate series of data is given.*/
class AiAnimation(
        /** The name of the animation. If the modeling package this data was exported from does support only
         *  a single animation channel, this name is usually empty (length is zero). */
        var name: String = "",
        /** Duration of the animation in ticks.  */
        var duration: Double = -1.0,
        /** Ticks per second. 0 if not specified in the imported file */
        var ticksPerSecond: Double = 0.0,
        /** The number of bone animation channels. Each channel affects a single node. */
        var numChannels: Int = 0,
        /** The node animation channels. Each channel affects a single node. The array is numChannels in size. */
        var channels: MutableList<AiNodeAnim?> = mutableListOf(),
        /** The number of mesh animation channels. Each channel affects a single mesh and defines vertex-based animation. */
        var mNumMeshChannels: Int = 0,
        /** The mesh animation channels. Each channel affects a single mesh. The array is mNumMeshChannels in size. */
        var mMeshChannels: MutableList<List<AiMeshAnim>> = mutableListOf(),
        /** The number of mesh animation channels. Each channel affects a single mesh and defines morphing animation. */
        var numMorphMeshChannels: Int = 0,
        /** The morph mesh animation channels. Each channel affects a single mesh. The array is numMorphMeshChannels in size. */
        var morphMeshChannels: MutableList<AiMeshMorphAnim> = mutableListOf()
) {
    constructor(other: AiAnimation) : this(other.name, other.duration, other.ticksPerSecond, other.numChannels,
            MutableList(other.channels.size, { other.channels[it] }), other.mNumMeshChannels,
            MutableList(other.mMeshChannels.size, { other.mMeshChannels[it] }), other.numMorphMeshChannels,
            MutableList(other.morphMeshChannels.size, { other.morphMeshChannels[it] }))

    companion object {
        val size = 2 * Double.BYTES + 3 * Int.BYTES
    }
}