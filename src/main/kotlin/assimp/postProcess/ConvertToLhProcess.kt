/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2018, assimp team


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

package assimp.postProcess

import assimp.*
import glm_.mat4x4.Mat4

/** @file  MakeLeftHandedProcess.h
 *  @brief Defines a bunch of post-processing steps to handle coordinate system conversions.
 *
 *  - LH to RH
 *  - UV origin upper-left to lower-left
 *  - face order cw to ccw
 */

// -----------------------------------------------------------------------------------
/** @brief The MakeLeftHandedProcess converts all imported data to a left-handed
 *   coordinate system.
 *
 * This implies a mirroring of the Z axis of the coordinate system. But to keep
 * transformation matrices free from reflections we shift the reflection to other
 * places. We mirror the meshes and adapt the rotations.
 *
 * @note RH-LH and LH-RH is the same, so this class can be used for both
 */
object MakeLeftHandedProcess : BaseProcess() {

    /** Returns whether the processing step is present in the given flag field. */
    override fun isActive(flags: AiPostProcessStepsFlags): Boolean = flags has AiPostProcessStep.MakeLeftHanded

    /** Executes the post processing step on the given imported data. */
    override fun execute(scene: AiScene) {

        // Check for an existent root node to proceed
        logger.debug("MakeLeftHandedProcess begin")

        // recursively convert all the nodes
        scene.rootNode process Mat4()

        // process the meshes accordingly
        for (a in 0 until scene.numMeshes)
            scene.meshes[a].process()

        // process the materials accordingly
        for (a in 0 until scene.numMaterials)
            scene.materials[a].process()

        // transform all animation channels as well
        for (a in 0 until scene.numAnimations) {
            val anim = scene.animations[a]
            for (b in 0 until anim.numChannels)
                anim.channels[b]!!.process()
        }
        logger.debug("MakeLeftHandedProcess finished")
    }

    /** Recursively converts a node, all of its children and all of its meshes     */
    infix fun AiNode.process(parentGlobalRotation: Mat4) {

        transformation.apply {
            // mirror all base vectors at the local Z axis
            a2 = -a2
            b2 = -b2
            c2 = -c2
            d2 = -d2

            // now invert the Z axis again to keep the matrix determinant positive.
            // The local meshes will be inverted accordingly so that the result should look just fine again.
            this[2].negateAssign()
        }
        // continue for all children
        for (a in 0 until numChildren)
            children[a] process parentGlobalRotation * transformation
    }

    /** Converts a single mesh to left handed coordinates.
     *  This means that positions, normals and tangents are mirrored at the local Z axis and the order of all faces are inverted.
     *  @param pMesh The mesh to convert.     */
    fun AiMesh.process() {
        // mirror positions, normals and stuff along the Z axis
        for (a in 0 until numVertices) {
            vertices[a].z *= -1f
            if (hasNormals)
                normals[a].z *= -1f
            if (hasTangentsAndBitangents) {
                tangents[a].z *= -1f
                bitangents[a].z *= -1f
            }
        }

        // mirror offset matrices of all bones
        for (a in 0 until numBones)
            bones[a].offsetMatrix.apply {
                c0 = -c0
                c1 = -c1
                c3 = -c3
                a2 = -a2
                b2 = -b2
                d2 = -d2
            }

        // mirror bitangents as well as they're derived from the texture coords
        if (hasTangentsAndBitangents)
            for (a in 0 until numVertices)
                bitangents[a] timesAssign -1f
    }

    /** Converts a single material to left-handed coordinates
     *  @param pMat Material to convert     */
    fun AiMaterial.process() {
        textures.forEach {
            it.mapAxis?.let { pff ->
                // Mapping axis for UV mappings?
                pff.z *= -1f
            }
        }
    }

    /** Converts the given animation to LH coordinates.
     *  The rotation and translation keys are transformed, the scale keys work in local space and can therefore be left
     *  untouched.
     *  @param pAnim The bone animation to transform     */
    fun AiNodeAnim.process() {
        // position keys
        for (a in 0 until numPositionKeys)
            positionKeys[a].value.z *= -1f

        // rotation keys
        for (a in 0 until numRotationKeys) {
            /*  That's the safe version, but the float errors add up. So we try the short version instead
                aiMatrix3x3 rotmat = pAnim->mRotationKeys[a].mValue.GetMatrix();
                rotmat.a3 = -rotmat.a3; rotmat.b3 = -rotmat.b3;
                rotmat.c1 = -rotmat.c1; rotmat.c2 = -rotmat.c2;
                aiQuaternion rotquat( rotmat);
                pAnim->mRotationKeys[a].mValue = rotquat;            */
            rotationKeys[a].value.x *= -1f
            rotationKeys[a].value.y *= -1f
        }
    }
}


// ---------------------------------------------------------------------------
/** Postprocessing step to flip the face order of the imported data
 */
object FlipWindingOrderProcess : BaseProcess() {

    /** Returns whether the processing step is present in the given flag field. */
    override fun isActive(flags: AiPostProcessStepsFlags): Boolean = flags has AiPostProcessStep.FlipWindingOrder

    /** Executes the post processing step on the given imported data. */
    override fun execute(scene: AiScene) {
        logger.debug("FlipWindingOrderProcess begin")
        for (i in 0 until scene.numMeshes)
            scene.meshes[i].process()
        logger.debug("FlipWindingOrderProcess finished")
    }

    fun AiMesh.process() {
        // invert the order of all faces in this mesh
        for (a in 0 until numFaces) {
            val face = faces[a]
            for (b in 0 until face.size / 2) {
                val tmp = face[b]
                face[b] = face[face.size - 1 - b]
                face[face.size - 1 - b] = tmp
            }
        }
    }
}

/** Postprocessing step to flip the UV coordinate system of the import data */
object FlipUVsProcess : BaseProcess() {

    override fun isActive(flags: AiPostProcessStepsFlags): Boolean = flags has AiPostProcessStep.FlipUVs

    /** Executes the post processing step on the given imported data. */
    override fun execute(scene: AiScene) {
        logger.debug("FlipUVsProcess begin")
        for (i in 0 until scene.numMeshes)
            scene.meshes[i].process()

        for (i in 0 until scene.numMaterials)
            scene.materials[i].process()
        logger.debug("FlipUVsProcess finished")
    }

    fun AiMesh.process() {
        // mirror texture y coordinate
        for (a in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
            if (!hasTextureCoords(a))
                break

            for (b in 0 until numVertices)
                textureCoords[a][b][1] = 1f - textureCoords[a][b][1]
        }
    }

    fun AiMaterial.process() {
        // UV transformation key?
        textures.forEach {
            it.uvTrafo?.let {
                // just flip it, that's everything
                it.translation.y *= -1f
                it.rotation *= -1f
            }
        }
    }
}