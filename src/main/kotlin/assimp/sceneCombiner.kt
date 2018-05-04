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

package assimp

import gli_.Texture
import gli_.has
import kotlin.reflect.KMutableProperty0
import assimp.AI_INT_MERGE_SCENE as Ms

/** @brief Helper data structure for SceneCombiner.
 *
 *  Describes to which node a scene must be attached to.
 */
class AttachmentInfo(var scene: AiScene? = null, var attachToNode: AiNode? = null)

class NodeAttachmentInfo(var node: AiNode? = null, var attachToNode: AiNode? = null, var srcIdx: Int = Int.MAX_VALUE) {
    var resolved = false
}


object AI_INT_MERGE_SCENE {
    object GEN {
        /** @def AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES
         *  Generate unique names for all named scene items     */
        val UNIQUE_NAMES = 0x1

        /** @def AI_INT_MERGE_SCENE_GEN_UNIQUE_MATNAMES
         *  Generate unique names for materials, too.
         *  This is not absolutely required to pass the validation. */
        val UNIQUE_MATNAMES = 0x2

        /** @def AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY
         * Can be combined with AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES.
         * Unique names are generated, but only if this is absolutely required to avoid name conflicts. */
        val UNIQUE_NAMES_IF_NECESSARY = 0x10
    }

    /** @def AI_INT_MERGE_SCENE_DUPLICATES_DEEP_CPY
     * Use deep copies of duplicate scenes */
    val DUPLICATES_DEEP_CPY = 0x4

    /** @def AI_INT_MERGE_SCENE_RESOLVE_CROSS_ATTACHMENTS
     * If attachment nodes are not found in the given master scene, search the other imported scenes for them in an any order. */
    val RESOLVE_CROSS_ATTACHMENTS = 0x8
}

//    typedef std::pair<aiBone*,unsigned int> BoneSrcIndex;

/** @brief Helper data structure for SceneCombiner::MergeBones.     */
class BoneWithHash(var first: Int = 0, var second: String = "") {
    val srcBones = ArrayList<Pair<AiBone, Int>>()
}

/** @brief Utility for SceneCombiner */
class SceneHelper(
        /** scene we're working on  */
        var scene: AiScene? = null) {
    /** prefix to be added to all identifiers in the scene ...  */
    var id = ""
    /** hash table to quickly check whether a name is contained in the scene    */
    val hashes = hashSetOf<Int>()

    operator fun invoke() = scene!!
}

/** @brief Static helper class providing various utilities to merge two scenes. It is intended as internal utility and
 *      NOT for use by applications.
 *
 *  The class is currently being used by various postprocessing steps and loaders (ie. LWS).
 */
object SceneCombiner {

    /** Merges two or more scenes.
     *
     *  @return Receives a pointer to the destination scene. If the pointer doesn't point to null when the function
     *      is called, the existing scene is cleared and refilled.
     *  @param src Non-empty list of scenes to be merged. The function deletes the input scenes afterwards. There may be
     *      duplicate scenes.
     *  @param flags Combination of the AI_INT_MERGE_SCENE flags defined above
     */
    fun mergeScenes(src: ArrayList<AiScene>, flags: Int = 0): AiScene {
        val dest = AiScene()
        // Create a dummy scene to serve as master for the others
        val master = AiScene().apply { rootNode = AiNode().apply { name = "<MergeRoot>" } }
        val srcList = List(src.size, { AttachmentInfo(src[it], master.rootNode) })
        // 'master' will be deleted afterwards
        return mergeScenes(master, srcList, flags)
    }

    /** Merges two or more scenes and attaches all scenes to a specific position in the node graph of the master scene.
     *
     *  @return Receives a pointer to the destination scene. If the pointer doesn't point to NULL when the function
     *      is called, the existing scene is cleared and refilled.
     *  @param master Master scene. It will be deleted afterwards. All other scenes will be inserted in its node graph.
     *  @param src Non-empty list of scenes to be merged along with their corresponding attachment points in the master
     *      scene. The function deletes the input scenes afterwards. There may be duplicate scenes.
     *  @param flags Combination of the AI_INT_MERGE_SCENE flags defined above
     */
    fun mergeScenes(master: AiScene, srcList: List<AttachmentInfo>, flags: Int = 0): AiScene {

        val dest = AiScene()
        val src = Array(srcList.size + 1, { SceneHelper(if (it == 0) master else srcList[it - 1].scene) })
        // this helper array specifies which scenes are duplicates of others
        val duplicates = IntArray(src.size, { Int.MAX_VALUE })
        // this helper array is used as lookup table several times
        val offset = IntArray(src.size)
        // Find duplicate scenes
        for (i in 0 until src.size) {
            if (duplicates[i] != i && duplicates[i] != Int.MAX_VALUE) continue
            duplicates[i] = i
            for (a in i + 1 until src.size)
                if (src[i]() === src[a]())
                    duplicates[a] = i
        }
        // Generate unique names for all named stuff?
        if (flags has Ms.GEN.UNIQUE_NAMES)
            for (i in 1 until src.size) {
                src[i].id = "$%.6X\$_".format(i)
                if (flags has Ms.GEN.UNIQUE_NAMES_IF_NECESSARY) {
                    /*  Compute hashes for all identifiers in this scene and store them in a sorted table. We hash just
                        the node and animation channel names, all identifiers except the material names should be caught
                        by doing this.  */
                    addNodeHashes(src[i]().rootNode, src[i].hashes)
                    for (a in 0 until src[i]().numAnimations)
                        src[i].hashes.add(superFastHash(src[i]().animations[a].name))
                }
            }
        var cnt = 0
        // First find out how large the respective output arrays must be
        for (n in 0 until src.size) {
            val cur = src[n]
            if (n == duplicates[n] || flags has Ms.DUPLICATES_DEEP_CPY) {
                dest.numTextures += cur().numTextures
                dest.numMaterials += cur().numMaterials
                dest.numMeshes += cur().numMeshes
            }
            dest.numLights += cur().numLights
            dest.numCameras += cur().numCameras
            dest.numAnimations += cur().numAnimations
            // Combine the flags of all scenes
            // We need to process them flag-by-flag here to get correct results
            // dest->mFlags ; //|= (*cur)->mFlags;
            if (cur().flags has AI_SCENE_FLAGS_NON_VERBOSE_FORMAT)
                dest.flags = dest.flags or AI_SCENE_FLAGS_NON_VERBOSE_FORMAT
        }
        // generate the output texture list + an offset table for all texture indices
        if (dest.numTextures != 0) {
            cnt = 0
            for (n in 0 until src.size) {
                val cur = src[n]
                for (entry in cur().textures)
                    if (n != duplicates[n]) {
                        if (flags has Ms.DUPLICATES_DEEP_CPY)
                            dest.textures[entry.key] = Texture(entry.value)
                        else continue
                    } else dest.textures[entry.key] = entry.value
                offset[n] = cnt
                cnt = dest.textures.size
            }
        }
        // generate the output material list + an offset table for all material indices
        if (dest.numMaterials != 0) {
            cnt = 0
            for (n in 0 until src.size) {
                val cur = src[n]
                for (i in 0 until cur().numMaterials) {
                    if (n != duplicates[n]) {
                        if (flags has Ms.DUPLICATES_DEEP_CPY)
                            dest.materials.add(AiMaterial(cur().materials[i]))
                        else continue
                    } else dest.materials.add(cur().materials[i])
                    // JVM, we dont need that because of our texture saved in hashMap with "tex.file" property..
//                    if (cur().numTextures != dest.numTextures)     {
//                        // We need to update all texture indices of the mesh. So we need to search for a material property called '$tex.file'
//
//                        for (unsigned int a = 0; a < ( * pip)->mNumProperties;++a)
//                        {
//                            aiMaterialProperty * prop = ( * pip)->mProperties[a]
//                            if (!strncmp(prop->mKey.data, "$tex.file", 9))
//                            {
//                                // Check whether this texture is an embedded texture.
//                                // In this case the property looks like this: *<n>,
//                                // where n is the index of the texture.
//                                aiString& s = *((aiString*)prop->mData)
//                                if ('*' == s.data[0]) {
//                                    // Offset the index and write it back ..
//                                    const unsigned int idx = strtoul10 (&s.data[1])+offset[n]
//                                    ASSIMP_itoa10(& s . data [1], sizeof(s.data)-1, idx)
//                                }
//                            }
//
//                            // Need to generate new, unique material names?
//                            else if (!::strcmp(prop->mKey.data, "$mat.name") && flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_MATNAMES)
//                            {
//                                aiString * pcSrc = (aiString *) prop->mData
//                                PrefixString(*pcSrc, ( * cur).id, (*cur).idlen)
//                            }
//                        }
//                    }
//                    ++pip
                }
                offset[n] = cnt
                cnt = dest.materials.size
            }
        }
        // generate the output mesh list + again an offset table for all mesh indices
        if (dest.numMeshes != 0) {
            cnt = 0
            for (n in 0 until src.size) {
                val cur = src[n]
                for (i in 0 until cur().numMeshes) {
                    if (n != duplicates[n]) {
                        if (flags has Ms.DUPLICATES_DEEP_CPY)
                            dest.meshes.add(AiMesh(cur().meshes[i]))
                        else continue
                    } else dest.meshes.add(cur().meshes[i])
                    // update the material index of the mesh
                    dest.meshes.last().materialIndex += offset[n]
                }
                // reuse the offset array - store now the mesh offset in it
                offset[n] = cnt
                cnt = dest.meshes.size
            }
        }

        val nodes = ArrayList<NodeAttachmentInfo>(srcList.size)

        /*  ----------------------------------------------------------------------------
            Now generate the output node graph. We need to make those names in the graph that are referenced by anims or
            lights or cameras unique. So we add a prefix to them ... $<rand>_
            We could also use a counter, but using a random value allows us to use just one prefix if we are joining
            multiple scene hierarchies recursively.
            Chances are quite good we don't collide, so we try that ...
            ----------------------------------------------------------------------------    */

        var n = src.lastIndex
        while (n >= 0) { /* !!! important !!! */
            val cur = src[n]
            var node: AiNode? = null

            // To offset or not to offset, this is the question
            if (n != duplicates[n]) {
                // Get full scene-graph copy
                node = AiNode(cur().rootNode)
                offsetNodeMeshIndices(node, offset[duplicates[n]])
                if (flags has Ms.DUPLICATES_DEEP_CPY)
                // (note:) they are already 'offseted' by offset[duplicates[n]]
                    offsetNodeMeshIndices(node, offset[n] - offset[duplicates[n]])
            } else { // if (n == duplicates[n])
                node = cur().rootNode
                offsetNodeMeshIndices(node, offset[n])
            }
            if (n != 0) // src[0] is the master node
                nodes.add(NodeAttachmentInfo(node, srcList[n - 1].attachToNode, n))

            // add name prefixes?
            if (flags has Ms.GEN.UNIQUE_NAMES) {
                // or the whole scenegraph
                if (flags has Ms.GEN.UNIQUE_NAMES_IF_NECESSARY)
                    addNodePrefixesChecked(node, cur.id, src, n)
                else addNodePrefixes(node, cur.id)
                // meshes
                for (i in 0 until cur().numMeshes) {
                    val mesh = cur().meshes[i]
                    // rename all bones
                    for (a in 0 until mesh.numBones) {
                        if (flags has Ms.GEN.UNIQUE_NAMES_IF_NECESSARY)
                            if (!findNameMatch(mesh.bones[a].name, src, n))
                                continue
                        prefixString(mesh.bones[a]::name, cur.id)
                    }
                }
            }
            // --------------------------------------------------------------------
            // Copy light sources
            for (i in 0 until cur().numLights) {
                if (n != duplicates[n]) // duplicate scene?
                    dest.lights.add(AiLight(cur().lights[i]))
                else dest.lights[i] = cur().lights[i]
                // Add name prefixes?
                if (flags has Ms.GEN.UNIQUE_NAMES) {
                    if (flags has Ms.GEN.UNIQUE_NAMES_IF_NECESSARY)
                        if (!findNameMatch(dest.lights.last().name, src, n))
                            continue
                    prefixString(dest.lights.last()::name, cur.id)
                }
            }
            // --------------------------------------------------------------------
            // Copy cameras
            for (i in 0 until cur().numCameras) {
                if (n != duplicates[n]) // duplicate scene?
                    dest.cameras.add(AiCamera(cur().cameras[i]))
                else dest.cameras.add(cur().cameras[i])
                // Add name prefixes?
                if (flags has Ms.GEN.UNIQUE_NAMES) {
                    if (flags has Ms.GEN.UNIQUE_NAMES_IF_NECESSARY)
                        if (!findNameMatch(dest.cameras.last().name, src, n))
                            continue
                    prefixString(dest.cameras.last()::name, cur.id)
                }
            }
            // --------------------------------------------------------------------
            // Copy animations
            for (i in 0 until cur().numAnimations) {
                if (n != duplicates[n]) // duplicate scene?
                    dest.animations.add(AiAnimation(cur().animations[i]))
                else dest.animations.add(cur().animations[i])
                // Add name prefixes?
                if (flags has Ms.GEN.UNIQUE_NAMES) {
                    val last = dest.animations.last()
                    if (flags has Ms.GEN.UNIQUE_NAMES_IF_NECESSARY)
                        if (!findNameMatch(last.name, src, n))
                            continue
                    prefixString(last::name, cur.id)
                    // don't forget to update all node animation channels
                    for (a in 0 until last.numChannels) {
                        if (flags has Ms.GEN.UNIQUE_NAMES_IF_NECESSARY)
                            if (!findNameMatch(last.channels[a]!!.nodeName, src, n))
                                continue
                        prefixString(last.channels[a]!!::nodeName, cur.id)
                    }
                }
            }
            --n
        }
        // Now build the output graph
        attachToGraph(master, nodes)
        dest.rootNode = master.rootNode
        // Check whether we succeeded at building the output graph
        nodes.filter { !it.resolved }.forEach {
            if (flags has Ms.RESOLVE_CROSS_ATTACHMENTS) {
                // search for this attachment point in all other imported scenes, too.
                for (n in 0 until src.size) {
                    if (n != it.srcIdx) {
                        attachToGraph(src[n].scene!!, nodes)
                        if (it.resolved) break
                    }
                }
            }
            if (!it.resolved)
                logger.error { "SceneCombiner: Failed to resolve attachment ${it.node!!.name} ${it.attachToNode!!.name}" }
        }
        // now delete all input scenes. JVM -> GC

        // Check flags
        if (dest.numMeshes == 0 || dest.numMaterials == 0)
            dest.flags = dest.flags or AI_SCENE_FLAGS_INCOMPLETE

        // We're finished
        return dest
    }

    /** Merges two or more meshes
     *
     *  The meshes should have equal vertex formats. Only components that are provided by ALL meshes will be present in
     *  the output mesh.
     *  An exception is made for VColors - they are set to black. The meshes should have the same material indices, too.
     *  The output material index is always the material index of the first mesh.
     *
     *  @param dest Destination mesh. Must be empty.
     *  @param flags Currently no parameters
     *  @param begin First mesh to be processed
     *  @param end Points to the mesh after the last mesh to be processed
     */
    fun mergeMeshes(dest: ArrayList<AiMesh>, flags: Int, meshes: ArrayList<AiMesh>, begin: Int, end: Int) {

        if (begin == end) return // no meshes ...

        // Allocate the output mesh
        val out = AiMesh()
        dest.add(out)
        out.materialIndex = meshes[begin].materialIndex

        // Find out how much output storage we'll need
        for (i in begin until end) {
            val it = meshes[i]
            out.numVertices += it.numVertices
            out.numFaces += it.numFaces
            out.numBones += it.numBones
            // combine primitive type flags
            out.primitiveTypes = out.primitiveTypes or it.primitiveTypes
        }

        if (out.numVertices != 0) {
            // copy vertex positions
            if (meshes[begin].hasPositions)
                for (i in begin until end) {
                    val it = meshes[i]
                    if (it.vertices.isNotEmpty())
                        for (j in 0 until it.numVertices)
                            out.vertices.add(it.vertices[j])
                    else logger.warn { "JoinMeshes: Positions expected but input mesh contains no positions" }
                }
            // copy normals
            if (meshes[begin].hasNormals)
                for (i in begin until end) {
                    val it = meshes[i]
                    if (it.normals.isNotEmpty())
                        for (j in 0 until it.numVertices)
                            out.normals.add(it.normals[j])
                    else logger.warn { "JoinMeshes: Normals expected but input mesh contains no normals" }
                }
            // copy tangents and bitangents
            if (meshes[begin].hasTangentsAndBitangents)
                for (i in begin until end) {
                    val it = meshes[i]
                    if (it.tangents.isNotEmpty())
                        for (j in 0 until it.numVertices) {
                            out.tangents.add(it.tangents[j])
                            out.bitangents.add(it.bitangents[j])
                        }
                    else logger.warn { "JoinMeshes: Tangents expected but input mesh contains no tangents" }
                }
            // copy texture coordinates
            var n = 0
            while (meshes[begin].hasTextureCoords(n)) {
                for (i in begin until end) {
                    val it = meshes[i]
                    if (it.textureCoords[n].isNotEmpty())
                        for (j in 0 until it.textureCoords[n].size)
                            out.textureCoords[n][j] = it.textureCoords[n][j].clone()
                    else logger.warn { "JoinMeshes: UVs expected but input mesh contains no UVs" }
                }
                ++n
            }
            // copy vertex colors
            n = 0
            while (meshes[begin].hasVertexColors(n)) {
                for (i in begin until end) {
                    val it = meshes[i]
                    if (it.colors[n].isNotEmpty())
                        for (j in 0 until it.colors[n].size)
                            out.colors[n].add(it.colors[n][j])
                    else logger.warn { "JoinMeshes: VCs expected but input mesh contains no VCs" }
                }
                ++n
            }
        }

        if (out.numFaces != 0) { // just for safety
            // copy faces
            var ofs = 0
            for (i in begin until end) {
                val it = meshes[i]
                for (m in 0 until it.numFaces) {
                    val face = it.faces[m]
                    out.faces.add(face)
                    if (ofs != 0) // add the offset to the vertex
                        for (q in 0 until face.size)
                            face[q] += ofs
                }
                ofs += it.numVertices
            }
        }
        // bones - as this is quite lengthy, I moved the code to a separate function
        if (out.numBones != 0)
            mergeBones(out, meshes, begin, end)
    }

    /** Merges two or more bones
     *
     *  @param out Mesh to receive the output bone list
     *  @param flags Currently no parameters
     *  @param begin First mesh to be processed
     *  @param end Points to the mesh after the last mesh to be processed
     */
    fun mergeBones(out: AiMesh, meshes: ArrayList<AiMesh>, begin: Int, end: Int) {
        assert(out.numBones == 0)
        /*  find we need to build an unique list of all bones.
            we work with hashes to make the comparisons MUCH faster, at least if we have many bones.         */
        val asBones = ArrayList<BoneWithHash>()
        buildUniqueBoneList(asBones, meshes, begin, end)

        // now create the output bones
        out.numBones = 0

        asBones.forEach {
            // Allocate a bone and setup it's name
            out.bones.add(AiBone())
            out.numBones++
            val pc = out.bones.last().apply { name = it.second }

            val wend = it.srcBones.size

            // Loop through all bones to be joined for this bone
            var wmit = 0
            while (wmit != wend) {
                pc.numWeights += it.srcBones[wmit].first.numWeights
                // NOTE: different offset matrices for bones with equal names are - at the moment - not handled correctly. TODO jvm?
                if (wmit != 0 && pc.offsetMatrix != it.srcBones[wmit].first.offsetMatrix) {
                    logger.warn { "Bones with equal names but different offset matrices can't be joined at the moment" }
                    continue
                }
                pc.offsetMatrix = it.srcBones[wmit].first.offsetMatrix
                ++wmit
            }
            var avw = 0
            // And copy the final weights - adjust the vertex IDs by the face index offset of the corresponding mesh.
            wmit = 0
            while (wmit != wend) {
                val pip = it.srcBones[wmit].first
                for (mp in 0 until pip.numWeights) {
                    val vfi = pip.weights[mp]
                    with(pc.weights[avw]) {
                        weight = vfi.weight
                        vertexId = vfi.vertexId + it.srcBones[wmit].second
                    }
                    ++avw
                }
                ++wmit
            }
        }
    }

    /** Merges two or more materials
     *
     *  The materials should be complementary as much as possible. In case
     *  of a property present in different materials, the first occurrence
     *  is used.
     *
     *  @param dest Destination material. Must be empty.
     *  @param begin First material to be processed
     *  @param end Points to the material after the last material to be processed
     */
    fun mergeMaterials(dest: AiMaterial, materials: ArrayList<AiMaterial>, begin: Int, end: Int) {
        if (begin == end) return // no materials ...
        // Get the maximal number of properties
        for (i in begin until end) {
            val m = materials[i]
            with(dest) {
                // Test if we already have a matching property and if not, we add it to the new material
                if (name == null) m.name?.let { name = m.name }
                if (shadingModel == null) m.shadingModel?.let { shadingModel = m.shadingModel }
                if (wireframe == null) m.wireframe?.let { wireframe = m.wireframe }
                if (blendFunc == null) m.blendFunc?.let { blendFunc = m.blendFunc }
                if (opacity == null) m.opacity?.let { opacity = m.opacity }
                if (bumpScaling == null) m.bumpScaling?.let { bumpScaling = m.bumpScaling }
                if (shininess == null) m.shininess?.let { shininess = m.shininess }
                if (reflectivity == null) m.reflectivity?.let { reflectivity = m.reflectivity }
                if (shininessStrength == null) m.shininessStrength?.let { shininessStrength = m.shininessStrength }
                if (color == null) m.color?.let { color = AiMaterial.Color(it) }
                m.textures.filter { t -> !textures.any { it.file == t.file } }.toCollection(textures)
            }
        }
    }

    /** Builds a list of uniquely named bones in a mesh list
     *
     *  @param asBones Receives the output list
     *  @param it First mesh to be processed
     *  @param end Last mesh to be processed
     */
    fun buildUniqueBoneList(asBones: ArrayList<BoneWithHash>, meshes: ArrayList<AiMesh>, begin: Int, end: Int) {
        var iOffset = 0
        var i = begin
        while (i != end) {
            val it = meshes[i]
            for (l in 0 until it.numBones) {
                val p = it.bones[l]
                val itml = superFastHash(p.name)
                val end2 = asBones.size
                var j = 0
                while (j != end2) {
                    val it2 = asBones[j]
                    if (it2.first == itml) {
                        it2.srcBones.add(p to iOffset)
                        break
                    }
                    j++
                }
                if (end2 == j) {
                    // need to begin a new bone entry
                    asBones.add(BoneWithHash())
                    val btz = asBones.last()
                    // setup members
                    btz.first = itml
                    btz.second = p.name
                    btz.srcBones.add(p to iOffset)
                }
            }
            iOffset += it.numVertices
            i++
        }
    }

    /** Add a name prefix to all nodes in a scene.
     *
     *  @param Current node. This function is called recursively.
     *  @param prefix Prefix to be added to all nodes
     */
    fun addNodePrefixes(node: AiNode, prefix: String) {
        assert(prefix.isNotEmpty())
        prefixString(node::name, prefix)
        // Process all children recursively
        for (i in 0 until node.numChildren)
            addNodePrefixes(node.children[i], prefix)
    }

    /** Add an offset to all mesh indices in a node graph
     *
     *  @param Current node. This function is called recursively.
     *  @param offset Offset to be added to all mesh indices
     */
    fun offsetNodeMeshIndices(node: AiNode, offset: Int) {
        for (i in 0 until node.numMeshes)
            node.meshes[i] += offset
        for (i in 0 until node.numChildren)
            offsetNodeMeshIndices(node.children[i], offset)
    }

    /** Attach a list of node graphs to well-defined nodes in a master graph. This is a helper for MergeScenes()
     *
     *  @param master Master scene
     *  @param srcList List of source scenes along with their attachment points. If an attachment point is null (or does
     *      not exist in the master graph), a scene is attached to the root of the master graph (as an additional child
     *      node)
     *  @duplicates List of duplicates. If elem[n] == n the scene is not a duplicate. Otherwise elem[n] links scene n to
     *      its first occurrence.
     */
    fun attachToGraph(master: AiScene, srcList: ArrayList<NodeAttachmentInfo>) = attachToGraph(master.rootNode, srcList)

    fun attachToGraph(attach: AiNode, srcList: ArrayList<NodeAttachmentInfo>) {
        for (cnt in 0 until attach.numChildren)
            attachToGraph(attach.children[cnt], srcList)
        var cnt = 0
        srcList.filter { it.attachToNode === attach && !it.resolved }.map { ++cnt }
        if (cnt != 0) {
            val n = ArrayList<AiNode>(cnt + attach.numChildren)
            if (attach.numChildren != 0)
                for (i in 0 until attach.numChildren)
                    n.add(attach.children[i])   // TODO addAll?
            attach.children = n
            attach.numChildren += cnt
            for (att in srcList) {
                if (att.attachToNode === attach && !att.resolved) {
                    n.add(att.node!!.apply { parent = attach })
                    att.resolved = true // mark this attachment as resolved
                }
            }
        }
    }


// -------------------------------------------------------------------
    /** Get a deep copy of a scene
     *
     *  @param dest Receives a pointer to the destination scene
     *  @param src Source scene - remains unmodified.
     */
//    static void CopyScene(aiScene * * dest, const aiScene * source, bool allocate = true)
//
//
//// -------------------------------------------------------------------
//    /** Get a flat copy of a scene
//     *
//     *  Only the first hierarchy layer is copied. All pointer members of
//     *  aiScene are shared by source and destination scene.  If the
//     *    pointer doesn't point to NULL when the function is called, the
//     *    existing scene is cleared and refilled.
//     *  @param dest Receives a pointer to the destination scene
//     *  @param src Source scene - remains unmodified.
//     */
//    static void CopySceneFlat(aiScene * * dest, const aiScene * source)
//
//

    // use Mesh constructor
    /** Get a deep copy of a mesh
     *
     *  @param dest Receives a pointer to the destination mesh
     *  @param src Source mesh - remains unmodified.     */
//    fun copy(dest: AiMesh, src: AiMesh?) {
//
//        if (null == src) return
//
//        // get a flat copy
//        ::memcpy(dest, src, sizeof(aiMesh));
//
//        // and reallocate all arrays
//        GetArrayCopy(dest->mVertices, dest->mNumVertices);
//        GetArrayCopy(dest->mNormals, dest->mNumVertices);
//        GetArrayCopy(dest->mTangents, dest->mNumVertices);
//        GetArrayCopy(dest->mBitangents, dest->mNumVertices);
//
//        unsigned int n = 0;
//        while (dest->HasTextureCoords(n))
//        GetArrayCopy(dest->mTextureCoords[n++], dest->mNumVertices);
//
//        n = 0;
//        while (dest->HasVertexColors(n))
//        GetArrayCopy(dest->mColors[n++], dest->mNumVertices);
//
//        // make a deep copy of all bones
//        CopyPtrArray(dest->mBones, dest->mBones, dest->mNumBones);
//
//        // make a deep copy of all faces
//        GetArrayCopy(dest->mFaces, dest->mNumFaces);
//        for (unsigned int i = 0; i < dest->mNumFaces;++i) {
//            aiFace& f = dest->mFaces[i];
//            GetArrayCopy(f.mIndices, f.mNumIndices);
//        }
//    }

//// similar to Copy():
//    static void Copy(aiMaterial * * dest, const aiMaterial * src)
//    static void Copy(aiTexture * * dest, const aiTexture * src)
//    static void Copy(aiAnimation * * dest, const aiAnimation * src)
//    static void Copy(aiCamera * * dest, const aiCamera * src)
//    static void Copy(aiBone * * dest, const aiBone * src)
//    static void Copy(aiLight * * dest, const aiLight * src)
//    static void Copy(aiNodeAnim * * dest, const aiNodeAnim * src)
//    static void Copy(aiMetadata * * dest, const aiMetadata * src)
//
//// recursive, of course
//    static void Copy(aiNode * * dest, const aiNode * src)

    /** Same as AddNodePrefixes, but with an additional check
     *  Add a name prefix to all nodes in a hierarchy if a hash match is found       */
    fun addNodePrefixesChecked(node: AiNode, prefix: String, input: Array<SceneHelper>, cur: Int) {
        assert(prefix.isNotEmpty())
        val hash = superFastHash(node.name)
        // Check whether we find a positive match in one of the given sets
        for (i in 0 until input.size)
            if (cur != i && input[i].hashes.contains(hash)) {
                prefixString(node::name, prefix)
                break
            }
        // Process all children recursively
        for (i in 0 until node.numChildren)
            addNodePrefixesChecked(node.children[i], prefix, input, cur)
    }

    /** Add node identifiers to a hashing set   */
    fun addNodeHashes(node: AiNode, hashes: HashSet<Int>) {
        /*  Add node name to hashing set if it is non-empty - empty nodes are allowed and they can't have any anims
            assigned so its absolutely safe to duplicate them.         */
        if (node.name.isNotEmpty()) hashes.add(superFastHash(node.name))
        // Process all children recursively
        for (i in 0 until node.numChildren) addNodeHashes(node.children[i], hashes)
    }

    /** Search for duplicate names */
    fun findNameMatch(name: String, input: Array<SceneHelper>, cur: Int): Boolean {
        val hash = superFastHash(name)
        // Check whether we find a positive match in one of the given sets
        for (i in 0 until input.size)
            if (cur != i && input[i].hashes.contains(hash))
                return true
        return false
    }

    /** Add a prefix to a string    */
    fun prefixString(string: KMutableProperty0<String>, prefix: String) {
        // If the string is already prefixed, we won't prefix it a second time
        if (string().isNotEmpty() && string()[0] == '$') return
        if (prefix.length + string().length >= MAXLEN - 1) {
            logger.debug { "Can't add an unique prefix because the string is too long" }
            assert(false)
            return
        }
        // Add the prefix
        string.set("$prefix${string()}")
    }
}