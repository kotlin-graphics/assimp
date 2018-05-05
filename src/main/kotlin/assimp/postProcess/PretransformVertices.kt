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


*/

package assimp.postProcess

import assimp.*
import assimp.format.AiConfig
import gli_.has
import glm_.glm
import glm_.i
import glm_.mat3x3.Mat3
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import unsigned.Uint

/** @file PretransformVertices.h
 *  @brief Defines a post processing step to pretransform all
 *    vertices in the scenegraph
 */
/** The PretransformVertices pre-transforms all vertices in the node tree
 *  and removes the whole graph. The output is a list of meshes, one for
 *  each material.
 */
class PretransformVertices : BaseProcess() {

    //! Configuration option: keep scene hierarchy as long as possible
    var configKeepHierarchy = false
    var configNormalize = false
    var configTransform = false
    var configTransformation = Mat4()

    val meshVFormats = HashMap<AiMesh, Int>()
    val absTransform = HashMap<AiMesh, Mat4>()

    /** Check whether step is active */
    override fun isActive(flags: AiPostProcessStepsFlags): Boolean = flags has AiPostProcessStep.PreTransformVertices

    /** Executes the post processing step on the given imported data.   */
    override fun execute(scene: AiScene) {

        logger.debug("PretransformVerticesProcess begin")

        // Return immediately if we have no meshes
        if (scene.numMeshes == 0) return

        val iOldMeshes = scene.numMeshes
        val iOldAnimationChannels = scene.numAnimations
        val iOldNodes = scene.rootNode.countNodes()

        if (configTransform)
            scene.rootNode.transformation put configTransformation

        // first compute absolute transformation matrices for all nodes
        computeAbsoluteTransform(scene.rootNode)

        // Delete aiMesh::mBones for all meshes. The bones are removed during this step and we need the pointer as temporary storage
        for (i in 0 until scene.numMeshes)
            absTransform.clear()

        // now build a list of output meshes
        val outMeshes = ArrayList<AiMesh>()

        /*  Keep scene hierarchy? It's an easy job in this case ...
            we go on and transform all meshes, if one is referenced by nodes with different absolute transformations
            a depth copy of the mesh is required. */
        if (configKeepHierarchy) {

            // Hack: store the matrix we're transforming a mesh with in aiMesh::mBones
            buildWCSMeshes(outMeshes, scene.meshes, scene.numMeshes, scene.rootNode)

            // ... if new meshes have been generated, append them to the end of the scene
            if (outMeshes.isNotEmpty()) {
                scene.meshes.addAll(outMeshes)
                scene.numMeshes += outMeshes.size
            }

            // now iterate through all meshes and transform them to worldspace
            for (i in 0 until scene.numMeshes) {
                applyTransform(scene.meshes[i], absTransform[scene.meshes[i]]!!)

                // prevent improper destruction
                absTransform -= scene.meshes[i]
                scene.meshes[i].numBones = 0
            }
        } else {

            outMeshes.ensureCapacity(scene.numMaterials shl 1)
            val aiVFormats = ArrayList<Int>()

            val s = IntArray(scene.numMeshes)
            buildMeshRefCountArray(scene.rootNode, s)

            for (i in 0 until scene.numMaterials) {
                // get the list of all vertex formats for this material
                aiVFormats.clear()
                getVFormatList(scene, i, aiVFormats)
                aiVFormats.sort()
                aiVFormats.distinct().forEach {
                    var (iFaces, iVertices) = countVerticesAndFaces(scene, scene.rootNode, i, it)
                    if (0 != iFaces && 0 != iVertices) {
                        val mesh = AiMesh().apply {
                            outMeshes += this
                            numFaces = iFaces
                            numVertices = iVertices
                            faces = MutableList(iFaces) { mutableListOf<Int>() }
                            vertices = MutableList(iVertices) { Vec3() }
                            materialIndex = i
                            if (it has 0x2)
                                normals = MutableList(iVertices) { Vec3() }
                            if (it has 0x4) {
                                tangents = MutableList(iVertices) { Vec3() }
                                bitangents = MutableList(iVertices) { Vec3() }
                            }
                            iFaces = 0
                            while (it has (0x100 shl iFaces)) {
                                val components = if (it has (0x10000 shl iFaces)) 3 else 2
                                textureCoords.add(MutableList(iVertices) { FloatArray(components) })
                                iFaces++
                            }
                            iFaces = 0
                            while (it has (0x1000000 shl iFaces++))
                                colors.add(MutableList(iVertices) { Vec4() })
                        }
                        // fill the mesh ...
                        val aiTemp = IntArray(2)
                        collectData(scene, scene.rootNode, i, it, mesh, aiTemp, s)
                    }
                }
            }

            // If no meshes are referenced in the node graph it is possible that we get no output meshes.
            if (outMeshes.isEmpty())
                throw Error("No output meshes: all meshes are orphaned and are not referenced by any nodes")
            else {
                // now delete all meshes in the scene and build a new mesh list
                for (i in 0 until scene.numMeshes) {
                    val mesh = scene.meshes[i].apply { numBones = 0 }
                    absTransform -= mesh

                    // we're reusing the face index arrays. avoid destruction. Invalid for JVM
//                    for (a in 0 until mesh.numFaces)
//                        mesh.faces[a].clear()

                    /*  Invalidate the contents of the old mesh array. We will most likely have less output meshes now,
                        so the last entries of the mesh array are not overridden. We set them to NULL to make sure
                        the developer gets notified when his application attempts to access these fields ... */
//                    mesh = NULL
                }

                // It is impossible that we have more output meshes than input meshes, so we can easily reuse the old mesh array
                scene.numMeshes = outMeshes.size
                for (i in 0 until scene.numMeshes)
                    scene.meshes[i] = outMeshes[i]
            }
        }

        // remove all animations from the scene
        scene.animations.clear()
        scene.numAnimations = 0

        // --- we need to keep all cameras and lights
        for (i in 0 until scene.numCameras) {
            val cam = scene.cameras[i]
            val nd = scene.rootNode.findNode(cam.name)!!

            // multiply all properties of the camera with the absolute transformation of the corresponding node
            cam.position = nd.transformation * cam.position
            cam.lookAt = Mat3(nd.transformation) * cam.lookAt
            cam.up = Mat3(nd.transformation) * cam.up
        }

        for (i in 0 until scene.numLights) {
            val l = scene.lights[i]
            val nd = scene.rootNode.findNode(l.name)!!

            // multiply all properties of the camera with the absolute transformation of the corresponding node
            l.position = nd.transformation * l.position
            l.direction = Mat3(nd.transformation) * l.direction
            l.up = Mat3(nd.transformation) * l.up
        }

        if (!configKeepHierarchy) {

            // now delete all nodes in the scene and build a new flat node graph with a root node and some level 1 children
            val newRoot = AiNode()
            newRoot.name = scene.rootNode.name
            scene.rootNode = newRoot

            if (1 == scene.numMeshes && scene.numLights == 0 && scene.numCameras == 0)
                scene.rootNode.apply {
                    numMeshes = 1
                    meshes = IntArray(1)
                }
            else {
                scene.rootNode.numChildren = scene.numMeshes + scene.numLights + scene.numCameras
                val nodes = MutableList(scene.rootNode.numChildren) { AiNode() }
                scene.rootNode.children = nodes

                var nodesPtr = 0
                // generate mesh nodes
                for (i in 0 until scene.numMeshes) {
                    val pcNode = AiNode()
                    nodes[nodesPtr++] = pcNode
                    pcNode.parent = scene.rootNode
                    pcNode.name = scene.meshes[i].name

                    // setup mesh indices
                    pcNode.numMeshes = 1
                    pcNode.meshes = intArrayOf(i)
                }
                // generate light nodes
                for (i in 0 until scene.numLights) {
                    val node = AiNode()
                    nodes[nodesPtr++] = node
                    node.parent = scene.rootNode
                    node.name = "light_$i"
                    scene.lights[i].name = node.name
                }
                // generate camera nodes
                for (i in 0 until scene.numCameras) {
                    val node = AiNode()
                    nodes[nodesPtr++] = node
                    node.parent = scene.rootNode
                    node.name = "cam_$i"
                    scene.cameras[i].name = node.name
                }
            }
        } else
        // ... and finally set the transformation matrix of all nodes to identity
            makeIdentityTransform(scene.rootNode)

        if (configNormalize) {
            // compute the boundary of all meshes
            var min = Vec3(1e10f)
            var max = Vec3(-1e10f)

            for (a in 0 until scene.numMeshes) {
                val m = scene.meshes[a]
                for (i in 0 until m.numVertices) {
                    min = glm.min(m.vertices[i], min)
                    max = glm.max(m.vertices[i], max)
                }
            }

            // find the dominant axis
            var d = max - min
            val div = glm.max(d.x, glm.max(d.y, d.z)) * 0.5f

            d = min + d * 0.5f
            for (a in 0 until scene.numMeshes) {
                val m = scene.meshes[a]
                for (i in 0 until m.numVertices) {
                    m.vertices[i] = (m.vertices[i] - d) / div
                }
            }

            // print statistics
            logger.debug("PretransformVerticesProcess finished")

            logger.info("Removed $iOldNodes nodes and $iOldAnimationChannels animation channels (${scene.rootNode.countNodes()} output nodes)")

            logger.info("Kept ${scene.numLights} lights and ${scene.numCameras} cameras")

            logger.info("Moved $iOldMeshes meshes to WCS (number of output meshes: ${scene.numMeshes})")
        }
    }

    /** Setup import settings/configuration */
    override fun setupProperties(imp: Importer) {
        // Get the current value of AI_CONFIG_PP_PTV_KEEP_HIERARCHY, AI_CONFIG_PP_PTV_NORMALIZE,
        // AI_CONFIG_PP_PTV_ADD_ROOT_TRANSFORMATION and AI_CONFIG_PP_PTV_ROOT_TRANSFORMATION
        configKeepHierarchy = imp[AiConfig.PP.PTV.KEEP_HIERARCHY] ?: false
        configNormalize = imp[AiConfig.PP.PTV.NORMALIZE] ?: false
        configTransform = imp[AiConfig.PP.PTV.ADD_ROOT_TRANSFORMATION] ?: false

        configTransformation = imp[AiConfig.PP.PTV.ROOT_TRANSFORMATION] ?: Mat4()
    }


    /** @brief Toggle the 'keep hierarchy' option
     *  @param d hm ... difficult to guess what this means, hu!?     */
    var keepHierarchy: Boolean
        get() = configKeepHierarchy
        set(value) {
            configKeepHierarchy = value
        }

    /** Count the number of nodes */
    fun AiNode.countNodes(): Int {
        var iRet = 1
        for (i in 0 until numChildren)
            iRet += children[i].countNodes()
        return iRet
    }

    /** Get a bitwise combination identifying the vertex format of a mesh */
    val AiMesh.vertexFormat: Int
            /*  The vertex format is stored in meshVFormats for later retrieval.
                There isn't a good reason to compute it a few hundred times from scratch.
                The pointer is unused as animations are lost during PretransformVertices. */
        get() = meshVFormats.getOrPut(this) {
            // store the value for later use
            ProcessHelper.getMeshVFormatUnique(this)
        }

    /** Count the number of vertices in the whole scene and a given material index */
    fun countVerticesAndFaces(scene: AiScene, node: AiNode, iMat: Int, iVFormat: Int): Pair<Int, Int> {
        var iFaces = 0
        var iVertices = 0

        for (i in 0 until node.numMeshes) {
            val mesh = scene.meshes[node.meshes[i]]
            if (iMat == mesh.materialIndex && iVFormat == mesh.vertexFormat) {
                iVertices += mesh.numVertices
                iFaces += mesh.numFaces
            }
        }
        for (i in 0 until node.numChildren)
            countVerticesAndFaces(scene, node.children[i], iMat, iVFormat).let {
                iFaces += it.first
                iVertices += it.second
            }
        return iFaces to iVertices
    }

    /** Collect vertex/face data */
    fun collectData(scene: AiScene, node: AiNode, iMat: Int, iVFormat: Int, meshOut: AiMesh, aiCurrent: IntArray, numRefs: IntArray) {

        // some array offsets
        val AI_PTVS_VERTEX = 0x0
        val AI_PTVS_FACE = 0x1

        // No need to multiply if there's no transformation
        val identity = node.transformation.isIdentity
        for (i in 0 until node.numMeshes) {
            val mesh = scene.meshes[node.meshes[i]]
            if (iMat == mesh.materialIndex && iVFormat == mesh.vertexFormat) {
                // Decrement mesh reference counter
                var numRef = numRefs[node.meshes[i]]
                assert(0 != numRef)
                --numRef
                // Save the name of the last mesh
                if (numRef == 0)
                    meshOut.name = mesh.name

                if (identity) {
                    // copy positions without modifying them
                    for (j in 0 until mesh.numVertices)
                        meshOut.vertices.add(aiCurrent[AI_PTVS_VERTEX] + j, Vec3(mesh.vertices[j]))

                    if (iVFormat has 0x2)
                    // copy normals without modifying them
                        for (j in 0 until mesh.numVertices)
                            meshOut.normals.add(aiCurrent[AI_PTVS_VERTEX] + j, Vec3(mesh.normals[j]))

                    if (iVFormat has 0x4)
                    // copy tangents and bitangents without modifying them
                        for (j in 0 until mesh.numVertices) {
                            meshOut.tangents.add(aiCurrent[AI_PTVS_VERTEX] + j, Vec3(mesh.tangents[j]))
                            meshOut.bitangents.add(aiCurrent[AI_PTVS_VERTEX] + j, Vec3(mesh.bitangents[j]))
                        }
                } else {
                    // copy positions, transform them to worldspace
                    for (n in 0 until mesh.numVertices)
                        meshOut.vertices[aiCurrent[AI_PTVS_VERTEX] + n] = node.transformation * mesh.vertices[n]

                    val mWorldIT = node.transformation.inverse().transposeAssign()

                    // TODO: implement Inverse() for aiMatrix3x3
                    val m = Mat3(mWorldIT)

                    if (iVFormat has 0x2)
                    // copy normals, transform them to worldspace
                        for (n in 0 until mesh.numVertices)
                            meshOut.normals[aiCurrent[AI_PTVS_VERTEX] + n] = (m * mesh.normals[n]).normalizeAssign()

                    if (iVFormat has 0x4)
                    // copy tangents and bitangents, transform them to worldspace
                        for (n in 0 until mesh.numVertices) {
                            meshOut.tangents[aiCurrent[AI_PTVS_VERTEX] + n] = (m * mesh.tangents[n]).normalizeAssign()
                            meshOut.bitangents[aiCurrent[AI_PTVS_VERTEX] + n] = (m * mesh.bitangents[n]).normalizeAssign()
                        }
                }
                var p = 0
                while (iVFormat has (0x100 shl p)) {
                    // copy texture coordinates
                    for (j in 0 until mesh.numVertices)
                        for (k in meshOut.textureCoords[p][j].indices)
                            meshOut.textureCoords[p][j][k] = mesh.textureCoords[p][j][k]
                    ++p
                }
                p = 0
                while (iVFormat has (0x1000000 shl p)) {
                    // copy vertex colors
                    for (j in 0 until mesh.numVertices)
                        meshOut.colors[p][j] put mesh.colors[p][j]
                    ++p
                }
                /*  now we need to copy all faces. since we will delete the source mesh afterwards,
                    we don't need to reallocate the array of indices except if this mesh is referenced multiple times. */
                for (planck in 0 until mesh.numFaces) {

                    val fSrc = mesh.faces[planck]
                    val fDst = meshOut.faces[aiCurrent[AI_PTVS_FACE] + planck]

                    val numIdx = fSrc.size

                    var pi: AiFace
                    if (numRef == 0) { /* if last time the mesh is referenced -> no reallocation */
//                        fDst = fSrc
                        meshOut.faces[aiCurrent[AI_PTVS_FACE] + planck] = mesh.faces[planck]
                        pi = meshOut.faces[aiCurrent[AI_PTVS_FACE] + planck]

                        // offset all vertex indices
                        for (hahn in 0 until numIdx)
                            pi[hahn] += aiCurrent[AI_PTVS_VERTEX]
                    } else {
                        for (j in 0 until numIdx)
                            fDst.add(0)
                        pi = fDst

                        // copy and offset all vertex indices
                        for (hahn in 0 until numIdx)
                            pi[hahn] = fSrc[hahn] + aiCurrent[AI_PTVS_VERTEX]
                    }

                    // Update the mPrimitiveTypes member of the mesh
                    meshOut.primitiveTypes = meshOut.primitiveTypes or when (mesh.faces[planck].size) {
                        0x1 -> AiPrimitiveType.POINT
                        0x2 -> AiPrimitiveType.LINE
                        0x3 -> AiPrimitiveType.TRIANGLE
                        else -> AiPrimitiveType.POLYGON
                    }
                }
                aiCurrent[AI_PTVS_VERTEX] += mesh.numVertices
                aiCurrent[AI_PTVS_FACE] += mesh.numFaces
            }
        }

        // append all children of us
        for (i in 0 until node.numChildren)
            collectData(scene, node.children[i], iMat, iVFormat, meshOut, aiCurrent, numRefs)
    }

    /** Get a list of all vertex formats that occur for a given material index
     *  The output list contains duplicate elements */
    fun getVFormatList(scene: AiScene, iMat: Int, aiOut: ArrayList<Int>) {
        for (i in 0 until scene.numMeshes) {
            val mesh = scene.meshes[i]
            if (iMat == mesh.materialIndex)
                aiOut += mesh.vertexFormat
        }
    }

    /** Compute the absolute transformation matrices of each node */
    fun computeAbsoluteTransform(node: AiNode) {
        node.parent?.let {
            node.transformation = it.transformation * node.transformation
        }

        for (i in 0 until node.numChildren)
            computeAbsoluteTransform(node.children[i])
    }

    // Simple routine to build meshes in worldspace, no further optimization
    fun buildWCSMeshes(out: ArrayList<AiMesh>, `in`: ArrayList<AiMesh>, numIn: Int, node: AiNode) {

        /*  NOTE:
            AiMesh::numBones store original source mesh, or UINT_MAX if not a copy
            absTransform store reference to abs. transform we multiplied with */

        // process meshes
        for (i in 0 until node.numMeshes) {
            val mesh = `in`[node.meshes[i]]

            // check whether we can operate on this mesh
            if (!absTransform.contains(mesh) || absTransform[mesh] == node.transformation) {
                // yes, we can.
                absTransform[mesh] = node.transformation
                mesh.numBones = Uint.MAX_VALUE.i

            } else {

                // try to find us in the list of newly created meshes
                for (n in 0 until out.size) {
                    val ctz = out[n]
                    if (ctz.numBones == node.meshes[i] && absTransform[ctz] == node.transformation) {

                        // ok, use this one. Update node mesh index
                        node.meshes[i] = numIn + n
                    }
                }
                if (node.meshes[i] < numIn) {
                    // Worst case. Need to operate on a full copy of the mesh
                    logger.info("PretransformVertices: Copying mesh due to mismatching transforms")

                    val tmp = mesh.numBones
                    mesh.numBones = 0
                    val ntz = AiMesh(mesh)
                    mesh.numBones = tmp

                    ntz.numBones = node.meshes[i]
                    absTransform[ntz] = node.transformation

                    out += ntz

                    node.meshes[i] = numIn + out.size - 1
                }
            }
        }

        // call children
        for (i in 0 until node.numChildren)
            buildWCSMeshes(out, `in`, numIn, node.children[i])
    }

    /** Apply the node transformation to a mesh */
    fun applyTransform(mesh: AiMesh, mat: Mat4) {

        // Check whether we need to transform the coordinates at all
        if (!mat.isIdentity) {

            if (mesh.hasPositions)
                for (i in 0 until mesh.numVertices)
                    mesh.vertices[i] = mat * mesh.vertices[i]

            if (mesh.hasNormals || mesh.hasTangentsAndBitangents) {
                val worldIT = Mat4(mat)
                worldIT.inverseAssign().transposeAssign()

                // TODO: implement Inverse() for aiMatrix3x3
                val m = Mat3(worldIT)

                if (mesh.hasNormals)
                    for (i in 0 until mesh.numVertices)
                        mesh.normals[i] = (m * mesh.normals[i]).normalizeAssign()

                if (mesh.hasTangentsAndBitangents)
                    for (i in 0 until mesh.numVertices) {
                        mesh.tangents[i] = (m * mesh.tangents[i]).normalizeAssign()
                        mesh.bitangents[i] = (m * mesh.bitangents[i]).normalizeAssign()
                    }
            }
        }
    }

    /** Reset transformation matrices to identity */
    fun makeIdentityTransform(nd: AiNode) {
        nd.transformation put 1f

        // call children
        for (i in 0 until nd.numChildren)
            makeIdentityTransform(nd.children[i])
    }


    /** Build reference counters for all meshes */
    fun buildMeshRefCountArray(nd: AiNode, refs: IntArray) {
        for (i in 0 until nd.numMeshes)
            refs[nd.meshes[i]]++

        // call children
        for (i in 0 until nd.numChildren)
            buildMeshRefCountArray(nd.children[i], refs)
    }
}