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

package assimp.postProcess

import assimp.*
import gli_.hasnt
import kotlin.math.PI
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import assimp.AiPostProcessStep as Pps
import assimp.AiShadingMode as Sm

/** @file Defines a (dummy) post processing step to validate the loader's
 *  output data structure (for debugging)
 *  --------------------------------------------------------------------------------------
 *      Validates the whole ASSIMP scene data structure for correctness.
 *      ImportErrorException is thrown of the scene is corrupt.
 *  --------------------------------------------------------------------------------------  */
class ValidateDSProcess : BaseProcess() {

    lateinit var scene: AiScene

    /** Returns whether the processing step is present in the given flag field. */
    override fun isActive(flags: Int) = flags has Pps.ValidateDataStructure

    /** Executes the post processing step on the given imported data.   */
    override fun execute(scene: AiScene) {
        this.scene = scene
        logger.debug { "ValidateDataStructureProcess begin" }
        // validate the node graph of the scene
        validate(scene.rootNode)
        // validate all meshes
        if (scene.numMeshes != 0)
            doValidation(scene.meshes, scene.numMeshes, "meshes", "numMeshes")
        else if (scene.flags hasnt AI_SCENE_FLAGS_INCOMPLETE)
            reportError("AiScene.numMeshes is 0. At least one mesh must be there")
        else if (scene.meshes.isNotEmpty())
            reportError("AiScene.meshes is not empty although there are no meshes")

        // validate all animations
        if (scene.numAnimations != 0)
            doValidation(scene.animations, scene.numAnimations, "animations", "numAnimations")
        else if (scene.animations.isNotEmpty())
            reportError("AiScene.animations is not empty although there are no animations")

        // validate all cameras
        if (scene.numCameras != 0)
            doValidationWithNameCheck(scene.cameras, scene.numCameras, "cameras", "numCameras")
        else if (scene.cameras.isNotEmpty())
            reportError("AiScene.cameras is not empty although there are no cameras")

        // validate all lights
        if (scene.numLights > 0)
            doValidationWithNameCheck(scene.lights, scene.numLights, "lights", "numLights")
        else if (scene.lights.isNotEmpty())
            reportError("AiScene.lights is not empty although there are no lights")

        // validate all textures
        if (scene.numTextures > 0)
            doValidation(scene.textures.values.toCollection(ArrayList()), scene.numTextures, "textures", "numTextures")
        else if (scene.textures.isNotEmpty())
            reportError("AiScene.textures is not empty although there are no textures")

        // validate all materials
        if (scene.numMaterials > 0)
            doValidation(scene.materials, scene.numMaterials, "materials", "numMaterials")
        else if (scene.materials.isNotEmpty())
            reportError("AiScene.materials is not empty although there are no materials")

        logger.debug { "ValidateDataStructureProcess end" }
    }

    /** Report a validation error. This will throw an exception, control won't return.
     *  @param msg Format string for sprintf().*/
    fun reportError(msg: String, vararg args: Array<out Any?>): Nothing = throw Error("Validation failed: $msg $args")

    /** Report a validation warning. This won't throw an exception, control will return to the caller.
     * @param msg Format string for sprintf().*/
    fun reportWarning(msg: String, vararg args: Array<out Any?>) = logger.warn("Validation warning: $msg $args")

    /** Validates a mesh
     *  @param mesh Input mesh*/
    fun validate(mesh: AiMesh) {
        // validate the material index of the mesh
        if (scene.numMaterials != 0 && mesh.materialIndex >= scene.numMaterials)
            reportError("AiMesh.materialIndex is invalid (value: ${mesh.materialIndex} maximum: ${scene.numMaterials - 1})")

        validate(mesh.name)

        for (i in 0 until mesh.numFaces) {
            val face = mesh.faces[i]
            if (mesh.primitiveTypes != 0)
                when (face.size) {
                    0 -> reportError("AiMesh.faces[$i].numIndices is 0")
                    1 -> {
                        if (mesh.primitiveTypes hasnt AiPrimitiveType.POINT)
                            reportError("AiMesh.faces[$i] is a POINT but AiMesh.primitiveTypes does not report the POINT flag")
                    }
                    2 -> {
                        if (mesh.primitiveTypes hasnt AiPrimitiveType.LINE)
                            reportError("AiMesh.faces[$i] is a LINE but AiMesh.primitiveTypes does not report the LINE flag")
                    }
                    3 -> {
                        if (mesh.primitiveTypes hasnt AiPrimitiveType.TRIANGLE)
                            reportError("AiMesh.faces[$i] is a TRIANGLE but AiMesh.primitiveTypes does not report the TRIANGLE flag")
                    }
                    else -> {
                        if (mesh.primitiveTypes hasnt AiPrimitiveType.POLYGON)
                            reportError("AiMesh.faces[$i] is a POLYGON but AiMesh.primitiveTypes does not report the POLYGON flag")
                    }
                }
            if (face.size == 0) reportError("AiMesh.faces[$i] is empty")
        }
        // positions must always be there ...
        if (mesh.numVertices == 0 || (mesh.vertices.isEmpty() && scene.flags == 0))
            reportError("The mesh contains no vertices")
        if (mesh.numVertices > AI_MAX_VERTICES)
            reportError("Mesh has too many vertices: ${mesh.numVertices}, but the limit is $AI_MAX_VERTICES")
        if (mesh.numFaces > AI_MAX_FACES)
            reportError("Mesh has too many faces: ${mesh.numFaces}, but the limit is $AI_MAX_FACES")
        // if tangents are there there must also be bitangent vectors ...
        if (mesh.tangents.isNotEmpty() != mesh.bitangents.isNotEmpty())
            reportError("If there are tangents, bitangent vectors must be present as well")
        // faces, too
        if (mesh.numFaces == 0 || (mesh.faces.isEmpty() && scene.flags == 0))
            reportError("Mesh contains no faces")
        // now check whether the face indexing layout is correct: unique vertices, pseudo-indexed.
        val abRefList = BooleanArray(mesh.numVertices)
        for (i in 0 until mesh.numFaces) {
            val face = mesh.faces[i]
            if (face.size > AI_MAX_FACE_INDICES)
                reportError("Face $i has too many faces: ${face.size}, but the limit is $AI_MAX_FACE_INDICES")
            for (a in 0 until face.size) {
                if (face[a] >= mesh.numVertices) reportError("AiMesh.faces[$i][$a] is out of range")
                abRefList[face[a]] = true
            }
        }
        // check whether there are vertices that aren't referenced by a face
        for (i in 0 until mesh.numVertices) if (!abRefList[i]) reportWarning("There are unreferenced vertices")

        // texture channel 2 may not be set if channel 1 is zero ...
        run {
            var i = 0
            while (i < AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                if (!mesh.hasTextureCoords(i)) break
                ++i
            }
            while (i < AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                if (mesh.hasTextureCoords(i))
                    reportError("Texture coordinate channel $i exists although the previous channel didn't exist.")
                ++i
            }
        }
        // the same for the vertex colors
        run {
            var i = 0
            while (i < AI_MAX_NUMBER_OF_COLOR_SETS) {
                if (!mesh.hasVertexColors(i)) break
                ++i
            }
            while (i < AI_MAX_NUMBER_OF_COLOR_SETS) {
                if (mesh.hasVertexColors(i))
                    reportError("Vertex color channel $i is exists although the previous channel didn't exist.")
                ++i
            }
        }
        // now validate all bones
        if (mesh.numBones > 0) {
            if (mesh.bones.isEmpty())
                reportError("AiMesh.bones is empty (AiMesh.numBones is ${mesh.numBones})")
            val afSum = FloatArray(mesh.numVertices)
            // check whether there are duplicate bone names
            for (i in 0 until mesh.numBones) {
                val bone = mesh.bones[i]
                if (bone.numWeights > AI_MAX_BONE_WEIGHTS)
                    reportError("Bone $i has too many weights: ${bone.numWeights}, but the limit is $AI_MAX_BONE_WEIGHTS")
                if (i >= mesh.bones.size)
                    reportError("AiMesh.bones[$i] doesn't exist (AiMesh.numBones is ${mesh.numBones})")
                validate(mesh, mesh.bones[i], afSum)
                for (a in i + 1 until mesh.numBones)
                    if (mesh.bones[i].name == mesh.bones[a].name)
                        reportError("AiMesh.bones[$i] has the same name as AiMesh.bones[$a]")
            }
            // check whether all bone weights for a vertex sum to 1.0 ...
            for (i in 0 until mesh.numVertices)
                if (afSum[i] != 0f && (afSum[i] <= 0.94 || afSum[i] >= 1.05))
                    reportWarning("AiMesh.vertices[$i]: bone weight sum != 1f (sum is ${afSum[i]})")
        } else if (mesh.bones.isNotEmpty())
            reportError("AiMesh.bones is no empty although there are no bones")
    }

    /** Validates a bone
     *  @param mesh Input mesh
     *  @param bone Input bone  */
    fun validate(mesh: AiMesh, bone: AiBone, afSum: FloatArray) {
        validate(bone.name)
        if (bone.numWeights == 0) reportError("aiBone::mNumWeights is zero")
        // check whether all vertices affected by this bone are valid
        for (i in 0 until bone.numWeights) {
            if (bone.weights[i].vertexId >= mesh.numVertices)
                reportError("AiBone.weights[$i].vertexId is out of range")
            else if (bone.weights[i].weight == 0f || bone.weights[i].weight > 1f)
                reportWarning("AiBone.weights[$i].weight has an invalid value")
            afSum[bone.weights[i].vertexId] += bone.weights[i].weight
        }
    }

    /** Validates an animation
     *  @param animation Input animation*/
    fun validate(animation: AiAnimation) {
        validate(animation.name)
        // validate all materials
        if (animation.numChannels > 0) {
            if (animation.channels.isEmpty())
                reportError("AiAnimation.channels is empty (AiAnimation.numChannels is ${animation.numChannels})")
            for (i in 0 until animation.numChannels) {
                if (i >= animation.channels.size)
                    reportError("AiAnimation.channels[$i] doesn't exist (AiAnimation.numChannels is ${animation.numChannels})")
                validate(animation, animation.channels[i]!!)
            }
        } else reportError("aiAnimation::mNumChannels is 0. At least one node animation channel must be there.")
    }

    /** Validates a material
     *  @param material Input material*/
    fun validate(material: AiMaterial) {
        // make some more specific tests
        var temp = 0f
        material.shadingModel?.let {
            when (it) {
                Sm.blinn, Sm.cookTorrance, Sm.phong -> {
                    if (material.shininess == null)
                        reportWarning("A specular shading model is specified but there is no Shininess key")
                    material.shininessStrength?.let {
                        if (it == 0f)
                            reportWarning("A specular shading model is specified but the value of the Shininess Strenght key is 0")
                    }
                }
                else -> Unit
            }

            material.opacity?.let {
                if (it == 0f || it > 1.01f)
                    reportWarning("Invalid opacity value (must be 0 < opacity < 1f)")
            }

            // Check whether there are invalid texture keys
            // TODO: that's a relict of the past, where texture type and index were baked
            // into the material string ... we could do that in one single pass.
            searchForInvalidTextures(material)
        }
    }

    /** Search the material data structure for invalid or corrupt texture keys.
     *  @param material Input material  */
    fun searchForInvalidTextures(material: AiMaterial) {
        var index = 0
        // Now check whether all UV indices are valid ...
        var noSpecified = true
        for (texture in material.textures)
            texture.uvwsrc?.let {
                noSpecified = false
                // Ignore UV indices for texture channels that are not there ...
                // Get the value
                index = it
                // Check whether there is a mesh using this material which has not enough UV channels ...
                for (a in 0 until scene.numMeshes) {
                    val mesh = scene.meshes[a]
                    if (mesh.materialIndex == scene.materials.indexOf(material)) {
                        var channels = 0
                        while (mesh.hasTextureCoords(channels)) ++channels
                        if (it >= channels)
                            reportWarning("Invalid UV index: $it (key uvwsrc). Mesh $a has only $channels UV channels")
                    }
                }
            }
        if (noSpecified)
            for (a in 0 until scene.numMeshes) { // Assume that all textures are using the first UV channel
                val mesh = scene.meshes[a]
                if (mesh.materialIndex == index && mesh.textureCoords[0].isEmpty())
                // This is a special case ... it could be that the original mesh format intended the use of a special mapping here.
                    reportWarning("UV-mapped texture, but there are no UV coords")
            }
    }

    /** Validates a texture
     *  @param texture Input texture*/
    fun validate(texture: AiTexture) {
        // the data section may NEVER be NULL
        if (texture.pcData.isEmpty())
            reportError("AiTexture.pcData is empty")
        if (texture.height > 0 && texture.width == 0)
            reportError("AiTexture.width is zero (AiTexture.height is ${texture.height}, uncompressed texture)")
        else {
            if (texture.width == 0)
                reportError("AiTexture.width is zero (compressed texture)")
            else if ('.' == texture.achFormatHint[0])
                reportWarning("AiTexture.achFormatHint should contain a file extension  without a leading dot (format hint: ${texture.achFormatHint}).")
        }
        if (texture.achFormatHint.any { it.isUpperCase() })
            reportError("AiTexture.achFormatHint contains non-lowercase letters")
    }

    /** Validates a light source
     *  @param light Input light
     */
    fun validate(light: AiLight) {
        if (light.type == AiLightSourceType.UNDEFINED)
            reportWarning("AiLight.type is undefined")
        if (light.attenuationConstant == 0f && light.attenuationLinear == 0f && light.attenuationQuadratic == 0f)
            reportWarning("AiLight.attenuation* - all are zero")
        if (light.angleInnerCone > light.angleOuterCone)
            reportError("AiLight.angleInnerCone is larger than AiLight.angleOuterCone")
        if (light.colorDiffuse.isBlack && light.colorAmbient.isBlack && light.colorSpecular.isBlack)
            reportWarning("AiLight.color* - all are black and won't have any influence")
    }

    /** Validates a camera
     *  @param camera Input camera*/
    fun validate(camera: AiCamera) {
        if (camera.clipPlaneFar <= camera.clipPlaneNear)
            reportError("AiCamera.clipPlaneFar must be >= AiCamera.clipPlaneNear")
        // FIX: there are many 3ds files with invalid FOVs. No reason to reject them at all ... a warning is appropriate.
        if (camera.horizontalFOV == 0f || camera.horizontalFOV >= PI)
            reportWarning("${camera.horizontalFOV} is not a valid value for AiCamera.horizontalFOV")
    }

    /** Validates a bone animation channel
     *  @param animation Animation channel.
     *  @param boneAnim Input bone animation */
    fun validate(animation: AiAnimation, boneAnim: AiNodeAnim) {
        validate(boneAnim.nodeName)
        if (boneAnim.numPositionKeys == 0 && boneAnim.scalingKeys.isEmpty() && boneAnim.numRotationKeys == 0)
            reportError("Empty node animation channel")
        // otherwise check whether one of the keys exceeds the total duration of the animation
        if (boneAnim.numPositionKeys > 0) {
            if (boneAnim.positionKeys.isEmpty())
                reportError("AiNodeAnim.positionKeys is empty (AiNodeAnim.numPositionKeys is ${boneAnim.numPositionKeys})")
            var last = -10e10
            for (i in 0 until boneAnim.numPositionKeys) {
                /*  ScenePreprocessor will compute the duration if still the default value
                    (Aramis) Add small epsilon, comparison tended to fail if max_time == duration, seems to be due
                    the compilers register usage/width. */
                if (animation.duration > 0 && boneAnim.positionKeys[i].time > animation.duration + 0.001) {
                    val t = boneAnim.positionKeys[i].time
                    val d = "%.5f".format(animation.duration)
                    reportError("AiNodeAnim.positionKeys[$i].time ($t) is larger than AiAnimation.duration (which is $d)")
                }
                if (i > 0 && boneAnim.positionKeys[i].time <= last) {
                    val t = "%.5f".format(boneAnim.positionKeys[i].time)
                    val l = "%.5f".format(last)
                    reportWarning("AiNodeAnim.positionKeys[$i].time ($t) is smaller than AiAnimation.positionKeys[${i - 1}] (which is $l)")
                }
                last = boneAnim.positionKeys[i].time
            }
        }
        // rotation keys
        if (boneAnim.numRotationKeys > 0) {
            if (boneAnim.rotationKeys.isEmpty())
                reportError("AiNodeAnim.rotationKeys is empty (AiNodeAnim.numRotationKeys is ${boneAnim.numRotationKeys})")
            var last = -10e10
            for (i in 0 until boneAnim.numRotationKeys) {
                if (animation.duration > 0 && boneAnim.rotationKeys[i].time > animation.duration + 0.001) {
                    val t = "%.5f".format(boneAnim.rotationKeys[i].time)
                    val d = "%.5f".format(animation.duration)
                    reportError("aiNodeAnim::mRotationKeys[$i].time ($t) is larger than AiAnimation.duration (which is $d)")
                }
                if (i > 0 && boneAnim.rotationKeys[i].time <= last) {
                    val t = "%.5f".format(boneAnim.rotationKeys[i].time)
                    val l = "%.5f".format(last)
                    reportWarning("AiNodeAnim.rotationKeys[$i].time ($t) is smaller than AiAnimation.rotationKeys[${i - 1}] (which is $last)")
                }
                last = boneAnim.rotationKeys[i].time
            }
        }
        // scaling keys
        if (boneAnim.numScalingKeys > 0) {
            if (boneAnim.scalingKeys.isEmpty())
                reportError("AiNodeAnim.scalingKeys is empty (AiNodeAnim.numScalingKeys is ${boneAnim.numScalingKeys})")
            var last = -10e10
            for (i in 0 until boneAnim.numScalingKeys) {
                if (animation.duration > 0 && boneAnim.scalingKeys[i].time > animation.duration + 0.001) {
                    val t = boneAnim.scalingKeys[i].time
                    val d = animation.duration
                    reportError("AiNodeAnim.scalingKeys[$i].time ($t) is larger than AiAnimation.duration (which is $d)")
                }
                if (i > 0 && boneAnim.scalingKeys[i].time <= last) {
                    val t = "%.5f".format(boneAnim.scalingKeys[i].time)
                    val l = "%.5f".format(last)
                    reportWarning("AiNodeAnim.scalingKeys[$i].time ($t) is smaller than AiAnimation.scalingKeys[${i - 1}] (which is $l)")
                }
                last = boneAnim.scalingKeys[i].time
            }
        }
        if (boneAnim.numScalingKeys == 0 && boneAnim.numRotationKeys == 0 && boneAnim.numPositionKeys == 0)
            reportError("A node animation channel must have at least one subtrack")
    }

    /** Validates a node and all of its subnodes
     *  @param node Input node*/
    fun validate(node: AiNode) {
        if (node != scene.rootNode && node.parent == null)
            reportError("A node has no valid parent (AiNode.parent is null)")
        validate(node.name)
        // validate all meshes
        if (node.numMeshes > 0) {
            if (node.meshes.isEmpty())
                reportError("AiNode.meshes is empty (AiNode.numMeshes is ${node.numMeshes})")
            val abHadMesh = BooleanArray(scene.numMeshes)
            for (i in 0 until node.numMeshes) {
                if (node.meshes[i] >= scene.numMeshes)
                    reportError("AiNode.meshes[${node.meshes[i]}] is out of range (maximum is ${scene.numMeshes - 1})")
                if (abHadMesh[node.meshes[i]])
                    reportError("AiNode.meshes[$i] is already referenced by this node (value: ${node.meshes[i]})")
                abHadMesh[node.meshes[i]] = true
            }
        }
        if (node.numChildren > 0) {
            if (node.children.isEmpty())
                reportError("AiNode.children is empty (AiNode.numChildren is ${node.numChildren})")
            for (i in 0 until node.numChildren)
                validate(node.children[i])
        }
    }

    /** Validates a string
     *  @param string Input string*/
    fun validate(string: String) {
        if (string.length > MAXLEN)
            reportError("String.length is too large (${string.length}, maximum is $MAXLEN)")
        if (string.toCharArray().any { it == '\u0000' })
            reportError("String data is invalid: it contains the terminal zero")
    }

    /** template to validate one of the AiScene::XXX arrays    */
    fun doValidation(array: ArrayList<*>, size: Int, firstName: String, secondName: String) {
        // validate all entries
        if (size > 0) {
            if (array.isEmpty())
                reportError("AiScene.$firstName is empty (AiScene.$secondName is $size)")
            for (i in 0 until size) {
                val element = array[i]
                when (element) {
                    is AiMesh -> validate(element)
                    is AiAnimation -> validate(element)
                    is AiCamera -> validate(element)
                    is AiLight -> validate(element)
                    is AiTexture -> validate(element)
                    is AiMaterial -> validate(element)
                }
            }
        }
    }

    /** extended version: checks whethr T.name occurs twice   */
    fun doValidationEx(array: ArrayList<*>, size: Int, firstName: String, secondName: String) {
        // validate all entries
        if (size > 0) {
            if (array.isEmpty())
                reportError("AiScene.$firstName is empty (AiScene.$secondName is $size)")
            for (i in 0 until size) {
                val element = array[i]
                when (element) {
                    is AiMesh -> validate(element)
                    is AiAnimation -> validate(element)
                    is AiCamera -> validate(element)
                    is AiLight -> validate(element)
                    is AiTexture -> validate(element)
                    is AiMaterial -> validate(element)
                }
                // check whether there are duplicate names
                for (a in i + 1 until size) {
                    val propI = element::class.memberProperties.find { it.name == "name" } as KMutableProperty1<Any?, String>
                    val nameI = propI.get(element)
                    val elementA = array[a]
                    val propA = elementA::class.memberProperties.find { it.name == "name" } as KMutableProperty1<Any?, String>
                    val nameA = propA.get(elementA)
                    if (nameI == nameA)
                        reportError("AiScene.$firstName[$i] has the same name as AiScene.$secondName[$a]")
                }
            }
        }
    }

    /** extension to the first template which does also search the nodegraph for an item with the same name */
    fun doValidationWithNameCheck(array: ArrayList<*>, size: Int, firstName: String, secondName: String) {
        // validate all entries
        doValidationEx(array, size, firstName, secondName)
        for (i in 0 until size) {
            val element = array[i]
            val prop = element::class.memberProperties.find { it.name == "name" } as KMutableProperty1<Any?, String>
            val name = prop.get(element)
            val res = hasNameMatch(name, scene.rootNode)
            if (res == 0)
                reportError("AiScene$firstName[$i] has no corresponding node in the scene graph ($name)")
            else if (1 != res)
                reportError("AiScene.$firstName[$i]: there are more than one nodes with $name as name")
        }
    }

    fun hasNameMatch(sIn: String, node: AiNode): Int =
            (if (node.name == sIn) 1 else 0) + (0 until node.numChildren).sumBy { hasNameMatch(sIn, node.children[it]) }
}
