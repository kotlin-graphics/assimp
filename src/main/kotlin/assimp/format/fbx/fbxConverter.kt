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

package assimp.format.fbx

import assimp.*
import assimp.AiTexture.Type as Tt
import assimp.format.md5.mat
import gli_.has
import glm_.d
import glm_.f
import glm_.func.deg
import glm_.func.rad
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import assimp.format.fbx.Converter.TransformationComp as Tc
import assimp.format.fbx.FileGlobalSettings.FrameRate as Fr

val MAGIC_NODE_TAG = "_\$AssimpFbx$"
fun CONVERT_FBX_TIME(time: Long) = time / 46186158000.0

/** @file  FBXDConverter.h
 *  @brief FBX DOM to aiScene conversion */

/**
 *  Convert a FBX #Document to #aiScene
 *  @param out Empty scene to be populated
 *  @param doc Parsed FBX document
 */
fun convertToAssimpScene(out: AiScene, doc: Document) {
    Converter(out, doc)
}


// key (time), value, mapto (component index)
typealias KeyFrameList = Triple<ArrayList<Long>, ArrayList<Float>, Int>

typealias KeyTimeList = ArrayList<Long>
typealias KeyValueList = ArrayList<Float>

/** Dummy class to encapsulate the conversion process */
class Converter(val out: AiScene, val doc: Document) {

    // 0: not assigned yet, others: index is value - 1
    var defaultMaterialIndex = 0

    val meshes = ArrayList<AiMesh>()
    val materials = ArrayList<AiMaterial>()
    val animations = ArrayList<AiAnimation>()
    val lights = ArrayList<AiLight>()
    val cameras = ArrayList<AiCamera>()
    val textures = mutableMapOf<String, AiTexture>()

    val materialsConverted = mutableMapOf<Material, Int>()
    val texturesConverted = mutableMapOf<Video, Int>()
    val meshesConverted = mutableMapOf<Geometry, ArrayList<Int>>()
    /** fixed node name -> which trafo chain components have animations? */
    val nodeAnimChainBits = mutableMapOf<String, Int>()
    /** name -> has had its prefix_stripped? */
    val nodeNames = mutableMapOf<String, Boolean>()
    val renamedNodes = mutableMapOf<String, String>()

    var animFps = 0.0

    /** The different parts that make up the final local transformation of a fbx-node     */
    enum class TransformationComp { Translation, RotationOffset, RotationPivot, PreRotation, Rotation, PostRotation,
        RotationPivotInverse, ScalingOffset, ScalingPivot, Scaling, ScalingPivotInverse, GeometricTranslation,
        GeometricRotation, GeometricScaling;

        val i = ordinal

        /** note: this returns the REAL fbx property names  */
        val nameProperty
            get() = when (this) {
                Tc.Translation -> "Lcl Translation"
                Tc.Rotation -> "Lcl Rotation"
                Tc.Scaling -> "Lcl Scaling"
                else -> toString()
            }

        /** XXX a neat way to solve the never-ending special cases for scaling would be to do everything in log space! */
        val defaultValue
            get() = when (this) {
                Scaling -> AiVector3D(1f)
                else -> AiVector3D()
            }

        companion object {
            val MAX = values().size
        }
    }

    operator fun Array<AiMatrix4x4>.get(transf: TransformationComp) = get(transf.i)
    operator fun Array<AiMatrix4x4>.set(transf: TransformationComp, mat: AiMatrix4x4) = set(transf.i, mat)

    init {
        /*  animations need to be converted first since this will populate the node_anim_chain_bits map, which is needed
            to determine which nodes need to be generated.         */
        convertAnimations()
        convertRootNode()

        if (doc.settings.readAllMaterials)
        // unfortunately this means we have to evaluate all objects
            for (v in doc.objects) {

                val ob = v.value.get() ?: continue

                (ob as? Material)?.let {
                    if (materialsConverted.contains(it))
                        convertMaterial(it, null)
                }
            }

        convertGlobalSettings()
        transferDataToScene()

        /*  if we didn't read any meshes set the AI_SCENE_FLAGS_INCOMPLETE to make sure the scene passes assimp's validation.
            FBX files need not contain geometry (i.e. camera animations, raw armatures).    */
        if (out.numMeshes == 0)
            out.flags = out.flags or AI_SCENE_FLAGS_INCOMPLETE
    }

    /** find scene root and trigger recursive scene conversion  */
    fun convertRootNode() {
        out.rootNode = AiNode(name = "RootNode")
        // root has ID 0
        convertNodes(0L, out.rootNode)
    }

    /** collect and assign child nodes */
    fun convertNodes(id: Long, parent: AiNode, parentTransform: AiMatrix4x4 = AiMatrix4x4()) {

        val conns = doc.getConnectionsByDestinationSequenced(id, "Model")

        val nodes = ArrayList<AiNode>(conns.size)

        val nodesChain = ArrayList<AiNode>()

        try {
            for (con in conns) {
                // ignore object-property links
                if (con.prop.isNotEmpty()) continue

                val `object` = con.sourceObject
                if (`object` == null) {
                    logger.warn("failed to convert source object for Model link")
                    continue
                }

                val model = `object` as? Model

                if (model != null) {
                    nodesChain.clear()

                    val newAbsTransform = AiMatrix4x4(parentTransform)

                    /*  even though there is only a single input node, the design of assimp (or rather: the complicated
                        transformation chain that is employed by fbx) means that we may need multiple aiNode's to
                        represent a fbx node's transformation.  */
                    generateTransformationNodeChain(model, nodesChain)

                    assert(nodesChain.isNotEmpty())

                    val originalName = fixNodeName(model.name)

                    /*  check if any of the nodes in the chain has the name the fbx node is supposed to have.
                        If there is none, add another node to preserve the name - people might have scripts etc. that
                        rely on specific node names.    */
                    val nameCarrier = nodesChain.find { it.name == originalName }

                    if (nameCarrier == null) nodesChain += AiNode(originalName)

                    //setup metadata on newest node
                    setupNodeMetadata(model, nodesChain.last())

                    // link all nodes in a row
                    var lastParent = parent
                    for (preNode in nodesChain) {

                        if (lastParent !== parent) {
                            lastParent.numChildren = 1
                            lastParent.children = mutableListOf(preNode)
                        }

                        preNode.parent = lastParent
                        lastParent = preNode

                        newAbsTransform timesAssign preNode.transformation
                    }

                    // attach geometry
                    convertModel(model, nodesChain.last(), newAbsTransform)

                    // attach sub-nodes
                    convertNodes(model.id, nodesChain.last(), newAbsTransform)

                    if (doc.settings.readLights) convertLights(model)

                    if (doc.settings.readCameras) convertCameras(model)

                    nodes += nodesChain.first()
                    nodesChain.clear()
                }
            }

            if (nodes.isNotEmpty()) {
                parent.children.clear()
                parent.children.addAll(nodes)
                parent.numChildren = nodes.size
            }
        } catch (exc: Exception) {
//            Util::delete_fun<aiNode> deleter
//                    std::for_each(nodes.begin(), nodes.end(), deleter)
//            std::for_each(nodes_chain.begin(), nodes_chain.end(), deleter)
            TODO()
        }
    }

    fun convertLights(model: Model) {
        val nodeAttrs = model.attributes
        for (attr in nodeAttrs) {
            (attr as? Light)?.let {
                convertLight(model, it)
            }
        }
    }

    fun convertCameras(model: Model) {
        for (attr in model.attributes)
            (attr as? Camera)?.let { convertCamera(model, it) }
    }

    fun convertLight(model: Model, light: Light) {

        lights += AiLight().apply {

            name = fixNodeName(model.name)

            val intensity = light.intensity / 100f
            val col = light.color

            colorDiffuse = col * intensity

            colorSpecular put colorDiffuse

            //lights are defined along negative y direction
            position put 0f
            direction.put(0f, -1f, 0f)
            up.put(0f, 0f, -1f)

            type = when (light.type) {
                Light.Type.Point -> AiLightSourceType.POINT
                Light.Type.Directional -> AiLightSourceType.DIRECTIONAL
                Light.Type.Spot -> {
                    angleOuterCone = light.outerAngle.rad
                    angleInnerCone = light.innerAngle.rad
                    AiLightSourceType.SPOT
                }
                Light.Type.Area -> {
                    logger.warn("cannot represent area light, set to UNDEFINED")
                    AiLightSourceType.UNDEFINED
                }
                Light.Type.Volume -> {
                    logger.warn("cannot represent volume light, set to UNDEFINED")
                    AiLightSourceType.UNDEFINED
                }
                else -> throw Error()
            }

            val decay = light.decayStart
            when (light.decayType) {
                Light.Decay.None -> {
                    attenuationConstant = decay
                    attenuationLinear = 0f
                    attenuationQuadratic = 0f
                }
                Light.Decay.Linear -> {
                    attenuationConstant = 0f
                    attenuationLinear = 2f / decay
                    attenuationQuadratic = 0f
                }
                Light.Decay.Quadratic -> {
                    attenuationConstant = 0f
                    attenuationLinear = 0f
                    attenuationQuadratic = 2f / (decay * decay)
                }
                Light.Decay.Cubic -> {
                    logger.warn("cannot represent cubic attenuation, set to Quadratic")
                    attenuationQuadratic = 1f
                }
                else -> throw Error()
            }
        }
    }

    fun convertCamera(model: Model, cam: Camera) {
        cameras += AiCamera().apply {

            name = fixNodeName(model.name)

            aspect = cam.aspectWidth / cam.aspectHeight
            //cameras are defined along positive x direction
            position put cam.position
            lookAt = (cam.interestPosition - position).normalizeAssign()
            up put cam.upVector
            horizontalFOV = cam.fieldOfView.rad
            clipPlaneNear = cam.nearPlane
            clipPlaneFar = cam.farPlane
        }
    }


    //    // ------------------------------------------------------------------------------------------------
//    aiVector3D TransformationCompDefaultValue( TransformationComp comp );
//
    fun getRotationMatrix(mode: Model.RotOrder, rotation: AiVector3D): AiMatrix4x4 {
        val out = AiMatrix4x4()
        if (mode == Model.RotOrder.SphericXYZ) {
            logger.error("Unsupported RotationMode: SphericXYZ")
            return out
        }

        val angleEpsilon = 1e-6f

        val isId = BooleanArray(3, { true })

        val temp = Array(3, { AiMatrix4x4() })
        if (abs(rotation.z) > angleEpsilon) {
            temp[2] = rotationZ(rotation.z.rad)
            isId[2] = false
        }
        if (abs(rotation.y) > angleEpsilon) {
            temp[1] = rotationY(rotation.y.rad)
            isId[1] = false
        }
        if (abs(rotation.x) > angleEpsilon) {
            temp[0] = rotationX(rotation.x.deg)
            isId[0] = false
        }

        val order = IntArray(3, { -1 })

        // note: rotation order is inverted since we're left multiplying as is usual in assimp
        when (mode) {
            Model.RotOrder.EulerXYZ -> {
                order[0] = 2
                order[1] = 1
                order[2] = 0
            }
            Model.RotOrder.EulerXZY -> {
                order[0] = 1
                order[1] = 2
                order[2] = 0
            }
            Model.RotOrder.EulerYZX -> {
                order[0] = 0
                order[1] = 2
                order[2] = 1
            }
            Model.RotOrder.EulerYXZ -> {
                order[0] = 2
                order[1] = 0
                order[2] = 1
            }
            Model.RotOrder.EulerZXY -> {
                order[0] = 1
                order[1] = 0
                order[2] = 2
            }
            Model.RotOrder.EulerZYX -> {
                order[0] = 0
                order[1] = 1
                order[2] = 2
            }
            else -> throw Error()
        }

        assert(order[0] in 0..2)
        assert(order[1] in 0..2)
        assert(order[2] in 0..2)

        if (!isId[order[0]]) out put temp[order[0]]
        if (!isId[order[1]]) out put temp[order[1]]
        if (!isId[order[2]]) out put temp[order[2]]

        return out
    }

    /** checks if a node has more than just scaling, rotation and translation components     */
    fun needsComplexTransformationChain(model: Model): Boolean {
        val props = model.props

        val zeroEpsilon = 1e-6f
        Tc.values().filter {
            it != Tc.Rotation && it != Tc.Scaling && it != Tc.Translation && it != Tc.GeometricScaling
                    && it != Tc.GeometricRotation && it != Tc.GeometricTranslation
        }.forEach { comp -> props<AiVector3D>(comp.nameProperty)?.let { if (it.squareLength > zeroEpsilon) return true } }
        return false
    }

    /** note: name must be a fixNodeName() result   */
    fun nameTransformationChainNode(name: String, comp: Tc) = "$name${MAGIC_NODE_TAG}_${comp.name}"

    /** note: memory for output_nodes will be managed by the caller */
    fun generateTransformationNodeChain(model: Model, outputNodes: ArrayList<AiNode>) {

        val props = model.props
        val rot = model.rotationOrder

        val chain = Array(Tc.MAX, { AiMatrix4x4() })

        // generate transformation matrices for all the different transformation components
        val zeroEpsilon = 1e-6f
        var isComplex = false

        props<AiVector3D>("PreRotation")?.let {
            if (it.squareLength > zeroEpsilon) {
                isComplex = true
                chain[Tc.PreRotation] = getRotationMatrix(rot, it)
            }
        }

        props<AiVector3D>("PostRotation")?.let {
            if (it.squareLength > zeroEpsilon) {
                isComplex = true
                chain[Tc.PostRotation] = getRotationMatrix(rot, it)
            }
        }

        props<AiVector3D>("RotationPivot")?.let {
            if (it.squareLength > zeroEpsilon) {
                isComplex = true
                chain[Tc.RotationPivot] = translation(it)
                chain[Tc.RotationPivotInverse] = translation(-it)
            }
        }

        props<AiVector3D>("RotationOffset")?.let {
            if (it.squareLength > zeroEpsilon) {
                isComplex = true
                chain[Tc.RotationOffset] = translation(it)
            }
        }

        props<AiVector3D>("ScalingOffset")?.let {
            if (it.squareLength > zeroEpsilon) {
                isComplex = true
                chain[Tc.ScalingOffset] = translation(it)
            }
        }

        props<AiVector3D>("ScalingPivot")?.let {
            if (it.squareLength > zeroEpsilon) {
                isComplex = true
                chain[Tc.ScalingPivot] = translation(it)
                chain[Tc.ScalingPivotInverse] = translation(-it)
            }
        }

        props<AiVector3D>("Lcl Translation")?.let {
            if (it.squareLength > zeroEpsilon)
                chain[Tc.Translation] = translation(it)
        }

        props<AiVector3D>("Lcl Scaling")?.let {
            if (abs(it.squareLength - 1f) > zeroEpsilon)
                chain[Tc.Scaling] = scaling(it)
        }

        props<AiVector3D>("Lcl Rotation")?.let {
            if (it.squareLength > zeroEpsilon)
                chain[Tc.Rotation] = getRotationMatrix(rot, it)
        }

        props<AiVector3D>("GeometricScaling")?.let {
            if (abs(it.squareLength - 1f) > zeroEpsilon)
                chain[Tc.GeometricScaling] = scaling(it)
        }

        props<AiVector3D>("GeometricRotation")?.let {
            if (it.squareLength > zeroEpsilon)
                chain[Tc.GeometricRotation] = getRotationMatrix(rot, it)
        }

        props<AiVector3D>("GeometricTranslation")?.let {
            if (it.squareLength > zeroEpsilon)
                chain[Tc.GeometricTranslation] = translation(it)
        }

        /*  isComplex needs to be consistent with needsComplexTransformationChain() or the interplay between this code
            and the animation converter would not be guaranteed.    */
        assert(needsComplexTransformationChain(model) == isComplex)

        val name = fixNodeName(model.name)

        /*  now, if we have more than just Translation, Scaling and Rotation, we need to generate a full node chain
            to accommodate for assimp's lack to express pivots and offsets. */
        if (isComplex && doc.settings.preservePivots) {
            logger.info("generating full transformation chain for node: $name")

            /*  query the anim_chain_bits dictionary to find out which chain elements have associated node animation
                channels. These can not be dropped even if they have identity transform in bind pose.   */
            val animChainBitmask = nodeAnimChainBits[name] ?: 0

            var bit = 0x1
            for (comp in Tc.values()) {

                if (!chain[comp].isIdentity || animChainBitmask has bit) {

                    if (comp == Tc.PostRotation) chain[comp].inverseAssign()

                    outputNodes += AiNode().apply {
                        this.name = nameTransformationChainNode(name, comp)
                        transformation put chain[comp]
                    }
                }
                bit = bit shl 1
            }

            assert(outputNodes.isNotEmpty())
            return
        }

        // else, we can just multiply the matrices together
        outputNodes += AiNode().apply {
            this.name = name
            for (transform in chain)
                transformation timesAssign transform
        }
    }

    fun setupNodeMetadata(model: Model, nd: AiNode) {
        val props = model.props
        val unparsedProperties = props.getUnparsedProperties()

        // create metadata on node
        val data = AiMetadata()
        nd.metaData = data

        // find user defined properties (3ds Max)
        data["UserProperties"] = props("UDP3DSMAX") ?: ""
        // preserve the info that a node was marked as Null node in the original file.
        data["IsNull"] = model.isNull

        // add unparsed properties to the node's metadata
        for (entry in unparsedProperties)
            data[entry.key] = (entry.value as TypedProperty<*>).value
    }

    fun convertModel(model: Model, nd: AiNode, nodeGlobalTransform: AiMatrix4x4) {

        val geos = model.geometry

        val meshes = IntArray(geos.size)

        for (geo in geos) {

            val mesh = geo as? MeshGeometry
            if (mesh != null) {
                val indices = convertMesh(mesh, model, nodeGlobalTransform)
                for (i in indices.indices)
                    meshes[i] = indices[i]
            } else
                logger.warn("ignoring unrecognized geometry: ${geo.name}")
        }

        if (meshes.isNotEmpty()) {
            nd.meshes = meshes
            nd.numMeshes = meshes.size
        }
    }

    /** MeshGeometry -> AiMesh, return mesh index + 1 or 0 if the conversion failed */
    fun convertMesh(mesh: MeshGeometry, model: Model, nodeGlobalTransform: AiMatrix4x4): ArrayList<Int> {

        val temp = ArrayList<Int>()

        meshesConverted[mesh]?.let {
            temp.addAll(it)
            return temp
        }

        val vertices = mesh.vertices
        val faces = mesh.faces
        if (vertices.isEmpty() || faces.isEmpty()) {
            logger.warn("ignoring empty geometry: ${mesh.name}")
            return temp
        }

        // one material per mesh maps easily to AiMesh. Multiple material meshes need to be split.
        val mIndices = mesh.materials
        if (doc.settings.readMaterials && mIndices.isNotEmpty()) {
            val base = mIndices[0]
            for (index in mIndices)
                if (index != base)
                    return convertMeshMultiMaterial(mesh, model, nodeGlobalTransform)
        }

        // faster code-path, just copy the data
        temp += convertMeshSingleMaterial(mesh, model, nodeGlobalTransform)
        return temp
    }

    fun setupEmptyMesh(mesh: MeshGeometry): AiMesh {
        val outMesh = AiMesh()
        meshes += outMesh
        meshesConverted[mesh] = arrayListOf(meshes.lastIndex)

        // set name
        val name = if (mesh.name.startsWith("Geometry::")) mesh.name.substring(10) else mesh.name

        if (name.isNotEmpty()) outMesh.name = name

        return outMesh
    }

    fun convertMeshSingleMaterial(mesh: MeshGeometry, model: Model, nodeGlobalTransform: AiMatrix4x4): Int {

        val mIndices = mesh.materials
        val outMesh = setupEmptyMesh(mesh)

        val vertices = mesh.vertices
        val faces = mesh.faces

        // copy vertices
        outMesh.numVertices = vertices.size
        outMesh.vertices = MutableList(vertices.size) { AiVector3D(vertices[it]) }

        // generate dummy faces
        outMesh.numFaces = faces.size
        outMesh.faces = MutableList(faces.size) { mutableListOf<Int>() }
        var fac = 0

        var cursor = 0
        for (count in faces) {
            val f = outMesh.faces[fac++]
            for (i in 0 until count)
                f += 0
            outMesh.primitiveTypes = outMesh.primitiveTypes or when (count) {
                1 -> AiPrimitiveType.POINT
                2 -> AiPrimitiveType.LINE
                3 -> AiPrimitiveType.TRIANGLE
                else -> AiPrimitiveType.POLYGON
            }
            for (i in 0 until count)
                f[i] = cursor++
        }

        // copy normals
        val normals = mesh.normals
        if (normals.isNotEmpty()) {
            assert(normals.size == vertices.size)

            outMesh.normals = MutableList(vertices.size) { AiVector3D(normals[it]) }
        }

        /*  copy tangents - assimp requires both tangents and bitangents (binormals) to be present, or neither of them.
            Compute binormals from normals and tangents if needed.  */
        val tangents = mesh.tangents
        val binormals = mesh.binormals

        if (tangents.isNotEmpty()) {
            val tempBinormals = ArrayList<AiVector3D>()
            if (binormals.isEmpty()) {
                if (normals.isNotEmpty()) {
                    for (i in 0 until tangents.size)
                        tempBinormals += normals[i] cross tangents[i]
                    binormals += tempBinormals
                } else
                    binormals.clear()
            }
            if (binormals.isNotEmpty()) {
                assert(tangents.size == vertices.size)
                assert(binormals.size == vertices.size)

                outMesh.tangents = MutableList(vertices.size) { AiVector3D(tangents[it]) }
                outMesh.bitangents = MutableList(vertices.size) { AiVector3D(binormals[it]) }
            }
        }

        // copy texture coords
        for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
            val uvs = mesh.getTextureCoords(i)
            if (uvs.isEmpty()) break

            outMesh.textureCoords[i] = MutableList(vertices.size) { uvs[it].to(FloatArray(2)) }
        }

        // copy vertex colors
        for (i in 0 until AI_MAX_NUMBER_OF_COLOR_SETS) {
            val colors = mesh.getVertexColors(i)
            if (colors.isEmpty()) break

            outMesh.colors[i] = MutableList(vertices.size) { AiColor4D(colors[it]) }
        }

        if (!doc.settings.readMaterials || mIndices.isEmpty()) {
            logger.error("no material assigned to mesh, setting default material")
            outMesh.materialIndex = getDefaultMaterial()
        } else
            convertMaterialForMesh(outMesh, model, mesh, mIndices[0])

        if (doc.settings.readWeights && mesh.skin != null)
            convertWeights(outMesh, model, mesh, nodeGlobalTransform, NO_MATERIAL_SEPARATION)

        return meshes.lastIndex
    }

    fun convertMeshMultiMaterial(mesh: MeshGeometry, model: Model, nodeGlobalTransform: AiMatrix4x4): ArrayList<Int> {

        val mIndices = mesh.materials
        assert(mIndices.isNotEmpty())

        val had = HashSet<Int>()
        val indices = ArrayList<Int>()

        for (index in mIndices)
            if (had.contains(index)) {
                indices += convertMeshMultiMaterial(mesh, model, index, nodeGlobalTransform)
                had += index
            }

        return indices
    }

    fun convertMeshMultiMaterial(mesh: MeshGeometry, model: Model, index: Int, nodeGlobalTransform: AiMatrix4x4): Int {

        val outMesh = setupEmptyMesh(mesh)

        val mIndices = mesh.materials
        val vertices = mesh.vertices
        val faces = mesh.faces

        val processWeights = doc.settings.readWeights && mesh.skin != null

        var countFaces = 0
        var countVertices = 0

        // count faces
        var itf = 0
        for (it in mIndices) {
            if (it != index) continue

            ++countFaces
            countVertices += itf
            ++itf
        }

        assert(countFaces != 0)
        assert(countVertices != 0)

        // mapping from output indices to DOM indexing, needed to resolve weights
        val reverseMapping = ArrayList<Int>()

        if (processWeights) reverseMapping.ensureCapacity(countVertices)

        // allocate output data arrays, but don't fill them yet
        outMesh.numVertices = countVertices
        outMesh.vertices = MutableList(countVertices, { AiVector3D() })

        outMesh.numFaces = countFaces
        outMesh.faces = MutableList(countFaces, { mutableListOf<Int>() })
        val fac = outMesh.faces

        // allocate normals
        val normals = mesh.normals
        if (normals.isNotEmpty()) {
            assert(normals.size == vertices.size)
            outMesh.normals = MutableList(vertices.size, { AiVector3D() })
        }

        // allocate tangents, binormals.
        val tangents = mesh.tangents
        val binormals = mesh.binormals
        val tempBinormals = ArrayList<AiVector3D>()

        if (tangents.isNotEmpty()) {
            if (binormals.isEmpty()) {
                if (normals.isNotEmpty()) {
                    // XXX this computes the binormals for the entire mesh, not only the part for which we need them.
                    for (i in 0 until tangents.size)
                        tempBinormals[i] = normals[i] cross tangents[i]

                    binormals.clear()
                    binormals.addAll(tempBinormals)
                } else
                    binormals.clear()
            }

            if (binormals.isNotEmpty()) {
                assert(tangents.size == vertices.size && binormals.size == vertices.size)

                outMesh.tangents = MutableList(vertices.size, { AiVector3D() })
                outMesh.bitangents = MutableList(vertices.size, { AiVector3D() })
            }
        }

        // allocate texture coords
        var numUvs = 0
        for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
            val uvs = mesh.getTextureCoords(i)
            if (uvs.isEmpty()) break

            outMesh.textureCoords[i] = MutableList(vertices.size, { FloatArray(2) })
            ++numUvs
        }

        // allocate vertex colors
        var numVcs = 0
        for (i in 0 until AI_MAX_NUMBER_OF_COLOR_SETS) {
            val colors = mesh.getVertexColors(i)
            if (colors.isEmpty()) break

            outMesh.colors[i] = MutableList(vertices.size, { AiColor4D() })
            ++numVcs
        }

        var cursor = 0
        var inCursor = 0

        itf = -1
        var facIdx = 0
        for (it in mIndices) {
            val pCount = itf++
            if (it != index) {
                inCursor += pCount
                continue
            }

            val f = fac[facIdx++]

            for (i in 0 until pCount)
                f += 0
            when (pCount) {
                1 -> outMesh.primitiveTypes = outMesh.primitiveTypes or AiPrimitiveType.POINT
                2 -> outMesh.primitiveTypes = outMesh.primitiveTypes or AiPrimitiveType.LINE
                3 -> outMesh.primitiveTypes = outMesh.primitiveTypes or AiPrimitiveType.TRIANGLE
                else -> outMesh.primitiveTypes = outMesh.primitiveTypes or AiPrimitiveType.POLYGON
            }
            for (i in 0 until pCount) {
                f[i] = cursor

                if (reverseMapping.isNotEmpty())
                    reverseMapping[cursor] = inCursor

                outMesh.vertices[cursor] = vertices[inCursor]

                if (outMesh.normals.isNotEmpty())
                    outMesh.normals[cursor] = normals[inCursor]

                if (outMesh.tangents.isNotEmpty()) {
                    outMesh.tangents[cursor] = tangents[inCursor]
                    outMesh.bitangents[cursor] = binormals[inCursor]
                }

                for (j in 0 until numUvs) {
                    val uvs = mesh.getTextureCoords(j)
                    outMesh.textureCoords[j][cursor] = floatArrayOf(uvs[inCursor].x, uvs[inCursor].y)
                }

                for (j in 0 until numVcs) {
                    val cols = mesh.getVertexColors(j)
                    outMesh.colors[j][cursor] = cols[inCursor]
                }
                ++cursor
                ++inCursor
            }
        }

        convertMaterialForMesh(outMesh, model, mesh, index)

        if (processWeights)
            convertWeights(outMesh, model, mesh, nodeGlobalTransform, index, reverseMapping)

        return meshes.lastIndex
    }

    val NO_MATERIAL_SEPARATION = /* std::numeric_limits<unsigned int>::max() */ -1

    /** - if materialIndex == NO_MATERIAL_SEPARATION, materials are not taken into
     *    account when determining which weights to include.
     *  - outputVertStartIndices is only used when a material index is specified, it gives for
     *    each output vertex the DOM index it maps to.     */
    fun convertWeights(out: AiMesh, model: Model, geo: MeshGeometry, nodeGlobalTransform: AiMatrix4x4 = AiMatrix4x4(),
                       materialIndex: Int, outputVertStartIndices: ArrayList<Int> = arrayListOf()) {

        val outIndices = ArrayList<Int>()
        val indexOutIndices = ArrayList<Int>()
        val countOutIndices = ArrayList<Int>()

        val sk = geo.skin!!

        val bones = ArrayList<AiBone>(sk.clusters.size)

        val noMatCheck = materialIndex == NO_MATERIAL_SEPARATION
        assert(noMatCheck || outputVertStartIndices.isNotEmpty())

        try {
            for (cluster in sk.clusters) {

                val indices = cluster.indices

                if (indices.isEmpty()) continue

                val mats = geo.materials

                var ok = false

                val noIndexSentinel = Int.MAX_VALUE

                countOutIndices.clear()
                indexOutIndices.clear()
                outIndices.clear()

                // now check if *any* of these weights is contained in the output mesh, taking notes so we don't need to do it twice.
                for (index in indices) {

                    // ToOutputVertexIndex only returns NULL if index is out of bounds which should never happen
                    val outIdx = geo.toOutputVertexIndex(index, ::_i)!!
                    val count = _i

                    indexOutIndices += noIndexSentinel
                    countOutIndices += 0

                    for (i in 0 until count) {
                        if (noMatCheck || mats[geo.faceForVertexIndex(geo.mappings[outIdx + i])] == materialIndex) {

                            if (indexOutIndices.last() == noIndexSentinel)
                                indexOutIndices[indexOutIndices.lastIndex] = outIndices.size

                            if (noMatCheck)
                                outIndices += geo.mappings[outIdx + i]
                            else {
                                // this extra lookup is in O(logn), so the entire algorithm becomes O(nlogn)
                                val it = outputVertStartIndices.indexOfFirst { it >= geo.mappings[outIdx + i] }

                                outIndices += it
                            }

                            countOutIndices[countOutIndices.lastIndex] = countOutIndices.last() + 1
                            ok = true
                        }
                    }
                }
                /*  if we found at least one, generate the output bones
                    XXX this could be heavily simplified by collecting the bone data in a single step.  */
                if (ok)
                    convertCluster(bones, model, cluster, outIndices, indexOutIndices, countOutIndices, nodeGlobalTransform)
            }
        } catch (exc: Exception) {
            TODO()
//            std::for_each(bones.begin(), bones.end(), Util::delete_fun<aiBone>())
//            throw
        }

        if (bones.isEmpty()) return

        out.bones.clear()
        out.bones.addAll(bones)
        out.numBones = bones.size
    }

    fun convertCluster(bones: ArrayList<AiBone>, model: Model, cl: Cluster, outIndices: ArrayList<Int>,
                       indexOutIndices: ArrayList<Int>, countOutIndices: ArrayList<Int>, nodeGlobalTransform: AiMatrix4x4) {

        val bone = AiBone().apply {

            name = fixNodeName(cl.node!!.name)

            cl.transformLink.inverse(offsetMatrix)

            offsetMatrix timesAssign nodeGlobalTransform

            numWeights = outIndices.size
            weights = MutableList(outIndices.size, { AiVertexWeight() })
        }
        bones += bone

        var cursor = 0

        val noIndexSentinel = Int.MAX_VALUE
        val weights = cl.weights

        val c = indexOutIndices.size
        for (i in 0 until c) {
            val indexIndex = indexOutIndices[i]

            if (indexIndex == noIndexSentinel) continue

            val cc = countOutIndices[i]
            for (j in 0 until cc) {
                val outWeight = bone.weights[cursor++]

                outWeight.vertexId = outIndices[indexIndex + j]
                outWeight.weight = weights[i]
            }
        }
    }

    fun convertMaterialForMesh(out: AiMesh, model: Model, geo: MeshGeometry, materialIndex: Int) {
        // locate source materials for this mesh
        val mats = model.materials
        if (materialIndex >= mats.size || materialIndex < 0) {
            logger.error("material index out of bounds, setting default material")
            out.materialIndex = getDefaultMaterial()
            return
        }

        val mat = mats[materialIndex]
        materialsConverted[mat]?.let {
            out.materialIndex = it
            return
        }

        out.materialIndex = convertMaterial(mat, geo)
        materialsConverted[mat] = out.materialIndex
    }

    fun getDefaultMaterial(): Int {
        if (defaultMaterialIndex != 0)
            return defaultMaterialIndex - 1

        val outMat = AiMaterial()
        materials += outMat

        outMat.color = AiMaterial.Color(diffuse = AiColor3D(0.8f))
        outMat.name = AI_DEFAULT_MATERIAL_NAME

        return materials.lastIndex
    }

    /** Material -> AiMaterial   */
    fun convertMaterial(material: Material, mesh: MeshGeometry?): Int {
        val props = material.props

        // generate empty output material
        val outMat = AiMaterial()
        materialsConverted[material] = materials.size

        materials += outMat
        // strip Material:: prefix
        val name = if (material.name.startsWith("Material::")) material.name.substring(10) else material.name

        // set material name if not empty - this could happen and there should be no key for it in this case.
        if (name.isNotEmpty())
            outMat.name = name

        // shading stuff and colors
        setShadingPropertiesCommon(outMat, props)

        // texture assignments
        setTextureProperties(outMat, material.textures, mesh)
        setTextureProperties_(outMat, material.layeredTextures, mesh)

        return materials.lastIndex
    }

    /** Video -> AiTexture  */
    fun convertVideo(video: Video): Int {

        val filename = if (video.fileName.isEmpty()) video.relativeFileName else video.fileName

        // generate empty output texture
        val outTex = AiTexture()
        textures[filename] = outTex

        // assuming the texture is compressed
        outTex.width = video.contentLength // total data size
        outTex.height = 0 // fixed to 0

        // steal the data from the Video to avoid an additional copy
        outTex.pcData = video.relinquishContent()

        // try to extract a hint from the file extension
        val ext = BaseImporter.getExtension(filename)

        if (ext.length <= 3)
            outTex.achFormatHint = if (ext == "jpeg") "jpg" else ext

        return textures.size - 1
    }

    fun trySetTextureProperties(outMat: AiMaterial, textures: MutableMap<String, Texture>, propName: String,
                                target: AiTexture.Type, mesh: MeshGeometry?) {
        val tex = textures[propName] ?: return

        var path = tex.relativeFileName

        tex.media?.let { media ->
            var textureReady = false //tells if our texture is ready (if it was loaded or if it was found)
            var index = 0

            val it = texturesConverted[media]
            if (it != null) {
                index = it
                textureReady = true
            } else {
                if (media.contentLength > 0) {
                    index = convertVideo(media)
                    texturesConverted[media] = index
                    textureReady = true
                } else if (doc.settings.searchEmbeddedTextures) { //try to find the texture on the already-loaded textures by the filename, if the flag is on
                    textureReady = findTextureIndexByFilename(media, ::_i)
                    index = _i
                }
            }
            // setup texture reference string (copied from ColladaLoader::FindFilenameForEffectTexture), if the texture is ready
            if (textureReady)
                path = "*$index"
        }

        outMat.textures[0].apply {
            file = path
            type = target
        }
        // XXX handle all kinds of UV transformations
        outMat.textures[0].uvTrafo = AiUVTransform(scaling = tex.uvScaling, translation = tex.uvTrans)

        val props = tex.props

        var uvIndex = 0


        props<String>("UVSet")?.let { uvSet ->
            // "default" is the name which usually appears in the FbxFileTexture template
            if (uvSet != "default" && uvSet.isNotEmpty()) {
                /*  this is a bit awkward - we need to find a mesh that uses this material and scan its UV channels
                    for the given UV name because assimp references UV channels by index, not by name.

                    XXX: the case that UV channels may appear in different orders in meshes is unhandled.
                    A possible solution would be to sort the UV channels alphabetically, but this would have the side
                    effect that the primary (first) UV channel would sometimes be moved, causing trouble when users read
                    only the first UV channel and ignore UV channel assignments altogether. */

                val matIndex = materials.indexOf(outMat)

                uvIndex = -1
                if (mesh == null) {
                    for (v in meshesConverted) {
                        val mesh = v.key as? MeshGeometry ?: continue

                        if (!mesh.materials.contains(matIndex)) continue

                        var index = -1
                        for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                            if (mesh.getTextureCoords(i).isEmpty()) break
                            val name = mesh.getTextureCoordChannelName(i)
                            if (name == uvSet) {
                                index = i
                                break
                            }
                        }
                        if (index == -1) {
                            logger.warn("did not find UV channel named $uvSet in a mesh using this material")
                            continue
                        }
                        if (uvIndex == -1)
                            uvIndex = index
                        else
                            logger.warn("the UV channel named $uvSet appears at different positions in meshes, results will be wrong")
                    }
                } else {
                    var index = -1
                    for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                        if (mesh.getTextureCoords(i).isEmpty()) break
                        val name = mesh.getTextureCoordChannelName(i)
                        if (name == uvSet) {
                            index = i
                            break
                        }
                    }
                    if (index == -1)
                        logger.warn("did not find UV channel named $uvSet in a mesh using this material")
                    if (uvIndex == -1)
                        uvIndex = index
                }
                if (uvIndex == -1) {
                    logger.warn("failed to resolve UV channel $uvSet, using first UV channel")
                    uvIndex = 0
                }
            }
        }
        outMat.textures[0].uvwsrc = uvIndex
    }

    fun trySetTextureProperties_(outMat: AiMaterial, layeredTextures: MutableMap<String, LayeredTexture>,
                                 propName: String, target: AiTexture.Type, mesh: MeshGeometry?) {

        val it = layeredTextures[propName] ?: return

        val texCount = it.textureCount

        // Set the blend mode for layered textures
        outMat.textures[0].apply {
            op = AiTexture.Op.of(it.blendMode.ordinal)
            type = target
        }

        for (texIndex in 0 until texCount) {

            val tex = it.textures[texIndex]

            val path = tex.relativeFileName

            outMat.textures[texIndex].apply {
                file = path
                // XXX handle all kinds of UV transformations
                uvTrafo = AiUVTransform(scaling = tex.uvScaling, translation = tex.uvTrans)
                type = target
            }

            val props = tex.props

            var uvIndex = 0

            props<String>("UVSet")?.let { uvSet ->
                // "default" is the name which usually appears in the FbxFileTexture template
                if (uvSet != "default" && uvSet.isNotEmpty()) {
                    /*  this is a bit awkward - we need to find a mesh that uses this material and scan its UV channels
                        for the given UV name because assimp references UV channels by index, not by name.

                        XXX: the case that UV channels may appear in different orders in meshes is unhandled.
                        A possible solution would be to sort the UV channels alphabetically, but this would have
                        the side effect that the primary (first) UV channel would sometimes be moved, causing trouble
                        when users read only the first UV channel and ignore UV channel assignments altogether. */

                    val matIndex = materials.indexOf(outMat)

                    uvIndex = -1
                    if (mesh == null) {
                        for (v in meshesConverted) {
                            val mesh = v.key as? MeshGeometry ?: continue

                            if (!mesh.materials.contains(matIndex)) continue

                            var index = -1
                            for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                                if (mesh.getTextureCoords(i).isEmpty()) break
                                val name = mesh.getTextureCoordChannelName(i)
                                if (name == uvSet) {
                                    index = i
                                    break
                                }
                            }
                            if (index == -1) {
                                logger.warn("did not find UV channel named $uvSet in a mesh using this material")
                                continue
                            }
                            if (uvIndex == -1)
                                uvIndex = index
                            else
                                logger.warn("the UV channel named $uvSet appears at different positions in meshes, results will be wrong")
                        }
                    } else {
                        var index = -1
                        for (i in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                            if (mesh.getTextureCoords(i).isEmpty()) break
                            val name = mesh.getTextureCoordChannelName(i)
                            if (name == uvSet) {
                                index = i
                                break
                            }
                        }
                        if (index == -1)
                            logger.warn("did not find UV channel named $uvSet in a mesh using this material")
                        if (uvIndex == -1)
                            uvIndex = index
                    }
                    if (uvIndex == -1) {
                        logger.warn("failed to resolve UV channel $uvSet, using first UV channel")
                        uvIndex = 0
                    }
                }
            }
            outMat.textures[texIndex].uvwsrc = uvIndex
        }
    }

    fun setTextureProperties(outMat: AiMaterial, textures: MutableMap<String, Texture>, mesh: MeshGeometry?) {
        trySetTextureProperties(outMat, textures, "DiffuseColor", Tt.diffuse, mesh)
        trySetTextureProperties(outMat, textures, "AmbientColor", Tt.ambient, mesh)
        trySetTextureProperties(outMat, textures, "EmissiveColor", Tt.emissive, mesh)
        trySetTextureProperties(outMat, textures, "SpecularColor", Tt.specular, mesh)
        trySetTextureProperties(outMat, textures, "SpecularFactor", Tt.specular, mesh)
        trySetTextureProperties(outMat, textures, "TransparentColor", Tt.opacity, mesh)
        trySetTextureProperties(outMat, textures, "ReflectionColor", Tt.reflection, mesh)
        trySetTextureProperties(outMat, textures, "DisplacementColor", Tt.displacement, mesh)
        trySetTextureProperties(outMat, textures, "NormalMap", Tt.normals, mesh)
        trySetTextureProperties(outMat, textures, "Bump", Tt.height, mesh)
        trySetTextureProperties(outMat, textures, "ShininessExponent", Tt.shininess, mesh)
    }

    fun setTextureProperties_(outMat: AiMaterial, layeredTextures: MutableMap<String, LayeredTexture>, mesh: MeshGeometry?) {
        trySetTextureProperties_(outMat, layeredTextures, "DiffuseColor", Tt.diffuse, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "AmbientColor", Tt.ambient, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "EmissiveColor", Tt.emissive, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "SpecularColor", Tt.specular, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "SpecularFactor", Tt.specular, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "TransparentColor", Tt.opacity, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "ReflectionColor", Tt.reflection, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "DisplacementColor", Tt.displacement, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "NormalMap", Tt.normals, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "Bump", Tt.height, mesh)
        trySetTextureProperties_(outMat, layeredTextures, "ShininessExponent", Tt.shininess, mesh)
    }

    fun getColorPropertyFromMaterial(props: PropertyTable, baseName: String) = getColorPropertyFactored(props, "${baseName}Color", "${baseName}Factor", true)

    fun getColorPropertyFactored(props: PropertyTable, colorName: String, factorName: String, useTemplate: Boolean = true): AiColor3D? {

        val baseColor = props<AiVector3D>(colorName, useTemplate) ?: return AiColor3D()

        // if no factor name, return the colour as is
        if (factorName.isEmpty())
            return baseColor

        // otherwise it should be multiplied by the factor, if found.
        props<Float>(factorName, useTemplate)?.let { baseColor *= it }

        return baseColor
    }

    fun getColorProperty(props: PropertyTable, colorName: String, useTemplate: Boolean = true) =
            props<AiVector3D>(colorName, useTemplate) ?: AiColor3D()

    fun setShadingPropertiesCommon(outMat: AiMaterial, props: PropertyTable) {
        /*  Set shading properties.
            Modern FBX Files have two separate systems for defining these, with only the more comprehensive one described
            in the property template.
            Likely the other values are a legacy system, which is still always exported by the official FBX SDK.

            Blender's FBX import and export mostly ignore this legacy system, and as we only support recent versions of
            FBX anyway, we can do the same.    */
        getColorPropertyFromMaterial(props, "Diffuse")?.let { diffuse ->
            outMat.color.let {
                if (it == null) outMat.color = AiMaterial.Color(diffuse = diffuse)
                else it.diffuse = diffuse
            }
        }
        getColorPropertyFromMaterial(props, "Emissive")?.let { emissive ->
            outMat.color.let {
                if (it == null) outMat.color = AiMaterial.Color(emissive = emissive)
                else it.emissive = emissive
            }
        }
        getColorPropertyFromMaterial(props, "Ambient")?.let { ambient ->
            outMat.color.let {
                if (it == null) outMat.color = AiMaterial.Color(ambient = ambient)
                else it.ambient = ambient
            }
        }
        // we store specular factor as SHININESS_STRENGTH, so just get the color
        val specular = getColorProperty(props, "SpecularColor", true)
        outMat.color.let {
            if (it == null) outMat.color = AiMaterial.Color(specular = specular)
            else it.specular = specular
        }
        // and also try to get SHININESS_STRENGTH
        outMat.shininessStrength = props("SpecularFactor", true)
        // and the specular exponent
        outMat.shininess = props("ShininessExponent")
        // TransparentColor / TransparencyFactor... gee thanks FBX :rolleyes:
        var calculatedOpacity = 1f
        getColorPropertyFactored(props, "TransparentColor", "TransparencyFactor")?.let {transparent ->
            outMat.color.let {
                if (it == null) outMat.color = AiMaterial.Color(transparent = transparent)
                else it.transparent = transparent
            }
            // as calculated by FBX SDK 2017:
            calculatedOpacity = 1f - (transparent.r + transparent.g + transparent.b) / 3f
        }
        /*  use of TransparencyFactor is inconsistent.
            Maya always stores it as 1.0, so we can't use it to set AI_MATKEY_OPACITY.
            Blender is more sensible and stores it as the alpha value.
            However both the FBX SDK and Blender always write an additional legacy "Opacity" field, so we can try to use that.

            If we can't find it, we can fall back to the value which the FBX SDK calculates from transparency colour (RGB)
            and factor (F) as:
            1.0 - F*((R+G+B)/3)

            There's no consistent way to interpret this opacity value, so it's up to clients to do the correct thing.   */
        val opacity = props<Float>("Opacity")
        if (opacity != null)
            outMat.opacity = opacity
        else if (calculatedOpacity != 0f)
            outMat.opacity = calculatedOpacity

        // reflection color and factor are stored separately
        val reflective = getColorProperty(props, "ReflectionColor", true)
        outMat.color.let {
            if (it == null) outMat.color = AiMaterial.Color(reflective = reflective)
            else it.reflective = reflective
        }
        outMat.reflectivity = props("ReflectionFactor", true)
        outMat.bumpScaling = props("BumpFactor")
        outMat.displacementScaling = props("DisplacementFactor")
    }

    /** get the number of fps for a FrameRate enumerated value */
    fun frameRateToDouble(fp: FileGlobalSettings.FrameRate, customFPSVal: Double = -1.0) = when (fp) {
        Fr.DEFAULT -> 1.0
        Fr._120 -> 120.0
        Fr._100 -> 100.0
        Fr._60 -> 60.0
        Fr._50 -> 50.0
        Fr._48 -> 48.0
        Fr._30, Fr._30_DROP -> 30.0
        Fr.NTSC_DROP_FRAME, Fr.NTSC_FULL_FRAME -> 29.9700262
        Fr.PAL -> 25.0
        Fr.CINEMA -> 24.0
        Fr._1000 -> 1000.0
        Fr.CINEMA_ND -> 23.976
        Fr.CUSTOM -> customFPSVal
    }

    /** convert animation data to aiAnimation et al */
    fun convertAnimations() {
        // first of all determine framerate
        val fps = doc.globals!!.timeMode
        val custom = doc.globals!!.customFrameRate
        animFps = frameRateToDouble(fps, custom.d)

        for (stack in doc.animationStacks()) convertAnimationStack(stack)
    }

    //
//    // ------------------------------------------------------------------------------------------------
//    // rename a node already partially converted. fixed_name is a string previously returned by
//    // FixNodeName, new_name specifies the string FixNodeName should return on all further invocations
//    // which would previously have returned the old value.
//    //
//    // this also updates names in node animations, cameras and light sources and is thus slow.
//    //
//    // NOTE: the caller is responsible for ensuring that the new name is unique and does
//    // not collide with any other identifiers. The best way to ensure this is to only
//    // append to the old name, which is guaranteed to match these requirements.
//    void RenameNode( const std::string& fixed_name, const std::string& new_name );
//
    /** takes a fbx node name and returns the identifier to be used in the assimp output scene.
     *  the function is guaranteed to provide consistent results over multiple invocations UNLESS renameNode() is called
     *  for a particular node name. */
    fun fixNodeName(name: String): String {
        /*  strip Model prefix, avoiding ambiguities (i.e. don't strip if this causes ambiguities, well possible
            between empty identifiers, such as "Model::" and ""). Make sure the behaviour is consistent across multiple
            calls to fixNodeName(). */
        if (name.substring(0, 7) == "Model::") {
            val temp = name.substring(7)

            nodeNames[temp]?.let { if (!it) return fixNodeName(name + "_") }
            nodeNames[temp] = true

            return renamedNodes[temp] ?: temp
        }

        nodeNames[name]?.let { if (it) return fixNodeName(name + "_") }
        nodeNames[name] = false

        return renamedNodes[name] ?: name
    }

    //
//    typedef std::map<const AnimationCurveNode*, const AnimationLayer*> LayerMap;
//
//    // XXX: better use multi_map ..
//    typedef std::map<std::string, std::vector<const AnimationCurveNode*> > NodeMap;
//
//
    fun convertAnimationStack(st: AnimationStack) {

        val layers = st.layers
        if (layers.isEmpty()) return

        val anim = AiAnimation()
        animations += anim

        // strip AnimationStack:: prefix
        val name = when {
            st.name.substring(0, 16) == "AnimationStack::" -> st.name.substring(16)
            st.name.substring(0, 11) == "AnimStack::" -> st.name.substring(11)
            else -> st.name
        }

        anim.name = name

        // need to find all nodes for which we need to generate node animations - it may happen that we need to merge multiple layers, though.
        val nodeMap = mutableMapOf<String, ArrayList<AnimationCurveNode>>()

        // reverse mapping from curves to layers, much faster than querying the FBX DOM for it.
        val layerMap = mutableMapOf<AnimationCurveNode, AnimationLayer>()

        val propWhitelist = arrayOf("Lcl Scaling", "Lcl Rotation", "Lcl Translation")

        for (layer in layers) {
            val nodes = layer.nodes(propWhitelist)
            for (node in nodes) {

                // this can happen - it could also be a NodeAttribute (i.e. for camera animations)
                val model = node.target as? Model ?: continue

                val name = fixNodeName(model.name)
                nodeMap[name]!!.add(node)

                layerMap[node] = layer
            }
        }

        // generate node animations
        val nodeAnims = ArrayList<AiNodeAnim>()

        val minMaxTime = doubleArrayOf(1e10, -1e10)

        var startTime = st.localStart
        var stopTime = st.localStop
        val hasLocalStartstop = startTime != 0L || stopTime != 0L
        if (!hasLocalStartstop) {
            // no time range given, so accept every keyframe and use the actual min/max time
            // the numbers are INT64_MIN/MAX, the 20000 is for safety because GenerateNodeAnimations uses an epsilon of 10000
            startTime = -9223372036854775807L + 20000
            stopTime = 9223372036854775807L - 20000
        }

        try {
            for (kv in nodeMap)
                generateNodeAnimations(nodeAnims, kv.key, kv.value, layerMap, startTime, stopTime, minMaxTime)
        } catch (exc: Exception) {
//            std::for_each(node_anims.begin(), node_anims.end(), Util::delete_fun<aiNodeAnim>())
//            throw
            TODO()
        }

        if (nodeAnims.isNotEmpty()) {
            anim.channels = Array(nodeAnims.size, { AiNodeAnim() }).toMutableList()
            anim.numChannels = nodeAnims.size

            anim.channels.clear()
            anim.channels.addAll(nodeAnims)
        } else {
            // empty animations would fail validation, so drop them
            animations.removeAt(animations.lastIndex)
            logger.info("ignoring empty AnimationStack (using IK?): $name")
            return
        }

        val startTimeFps = if (hasLocalStartstop) CONVERT_FBX_TIME(startTime) * animFps else minMaxTime[0]
        val stopTimeFps = if (hasLocalStartstop) CONVERT_FBX_TIME(stopTime) * animFps else minMaxTime[1]

        // adjust relative timing for animation
        for (channel in anim.channels) {
            for (i in 0 until channel!!.numPositionKeys)
                channel.positionKeys[i].time -= startTimeFps
            for (i in 0 until channel.numRotationKeys)
                channel.rotationKeys[i].time -= startTimeFps
            for (i in 0 until channel.numScalingKeys)
                channel.scalingKeys[i].time -= startTimeFps
        }

        /*  for some mysterious reason, mDuration is simply the maximum key -- the validator always assumes animations to start at zero. */
        anim.duration = stopTimeFps - startTimeFps
        anim.ticksPerSecond = animFps
    }

    // ------------------------------------------------------------------------------------------------
    fun generateNodeAnimations(nodeAnims: ArrayList<AiNodeAnim>, fixedName: String,
                               curves: ArrayList<AnimationCurveNode>, layerMap: MutableMap<AnimationCurveNode, AnimationLayer>,
                               start: Long, stop: Long, minMaxTime: DoubleArray) {

        val nodePropertyMap = mutableMapOf<String, ArrayList<AnimationCurveNode>>()
        assert(curves.isNotEmpty())

        if (ASSIMP.DEBUG)
            validateAnimCurveNodes(curves, doc.settings.strictMode)

        var curveNode: AnimationCurveNode? = null
        for (node in curves) {
            if (node.prop.isEmpty()) {
                logger.warn("target property for animation curve not set: ${node.name}")
                continue
            }
            curveNode = node
            if (node.curves.isEmpty()) {
                logger.warn("no animation curves assigned to AnimationCurveNode: ${node.name}")
                continue
            }

            nodePropertyMap.getOrPut(node.prop, { arrayListOf() }) += node
        }

        curveNode!!
        val target = curveNode.targetAsModel!!

        // check for all possible transformation components
        val chain = Array(Tc.values().size, { ArrayList<AnimationCurveNode>() })

        var hasAny = false
        var hasComplex = false

        for (i in Tc.values().indices) {

            val comp = Tc.values()[i]

            // inverse pivots don't exist in the input, we just generate them
            if (comp == Tc.RotationPivotInverse || comp == Tc.ScalingPivotInverse) {
                chain[i].clear()
                continue
            }

            val acnl = nodePropertyMap[comp.nameProperty]
            if (acnl != null) {
                chain[i].addAll(acnl)
                // check if this curves contains redundant information by looking up the corresponding node's transformation chain.
                if (doc.settings.optimizeEmptyAnimationCurves && isRedundantAnimationData(target, comp, acnl)) {
                    logger.debug("dropping redundant animation channel for node " + target.name)
                    continue
                }
                hasAny = true
                if (comp != Tc.Rotation && comp != Tc.Scaling && comp != Tc.Translation && comp != Tc.GeometricScaling &&
                        comp != Tc.GeometricRotation && comp != Tc.GeometricTranslation)
                    hasComplex = true
            }
        }

        if (!hasAny) {
            logger.warn("ignoring node animation, did not find any transformation key frames")
            return
        }

        /*  this needs to play nicely with GenerateTransformationNodeChain() which will be invoked _later_
            (animations come first). If this node has only rotation, scaling and translation _and_ there are no animated
            other components either, we can use a single node and also a single node animation channel. */
        if (!hasComplex && !needsComplexTransformationChain(target)) {

            val nd = generateSimpleNodeAnim(fixedName, target, chain, layerMap, start, stop, minMaxTime, true) // input is TRS order, assimp is SRT

            if (nd.numPositionKeys != 0 || nd.numRotationKeys != 0 || nd.numScalingKeys != 0)
                nodeAnims += nd
            return
        }

        /*  otherwise, things get gruesome and we need separate animation channels for each part of the transformation chain.
            Remember which channels we generated and pass this information to the node conversion code to avoid nodes
            that have identity transform, but non-identity animations, being dropped.   */
        var flags = 0
        var bit = 0x1
        for (i in 0 until Tc.values().size) {
            val comp = Tc.values()[i]

            if (chain[i].isNotEmpty()) {
                flags = flags or bit

                assert(comp != Tc.RotationPivotInverse && comp != Tc.ScalingPivotInverse)

                val chainName = nameTransformationChainNode(fixedName, comp)

                var na: AiNodeAnim? = null
                when (comp) {
                    Tc.Rotation, Tc.PreRotation, Tc.PostRotation, Tc.GeometricRotation ->
                        na = generateRotationNodeAnim(chainName, target, chain[i], layerMap, start, stop, minMaxTime)

                    Tc.RotationOffset, Tc.RotationPivot, Tc.ScalingOffset, Tc.ScalingPivot, Tc.Translation, Tc.GeometricTranslation -> {
                        na = generateTranslationNodeAnim(chainName, target, chain[i], layerMap, start, stop, minMaxTime)

                        // pivoting requires us to generate an implicit inverse channel to undo the pivot translation
                        if (comp == Tc.RotationPivot) {
                            val invName = nameTransformationChainNode(fixedName, Tc.RotationPivotInverse)

                            val inv = generateTranslationNodeAnim(invName, target, chain[i], layerMap, start, stop, minMaxTime, true)

                            if (inv.numPositionKeys != 0 || inv.numRotationKeys != 0 || inv.numScalingKeys != 0)
                                nodeAnims += inv

                            assert(Tc.RotationPivotInverse.i > i)
                            flags = flags or (bit shl (Tc.RotationPivotInverse.i - i))

                        } else if (comp == Tc.ScalingPivot) {
                            val invName = nameTransformationChainNode(fixedName, Tc.ScalingPivotInverse)

                            val inv = generateTranslationNodeAnim(invName, target, chain[i], layerMap, start, stop, minMaxTime, true)

                            if (inv.numPositionKeys != 0 || inv.numRotationKeys != 0 || inv.numScalingKeys != 0)
                                nodeAnims += inv

                            assert(Tc.RotationPivotInverse.i > i)
                            flags = flags or (bit shl (Tc.RotationPivotInverse.i - i))
                        }

                    }

                    Tc.Scaling, Tc.GeometricScaling -> na = generateScalingNodeAnim(chainName, target, chain[i], layerMap, start, stop, minMaxTime)

                    else -> throw Error()
                }

                if (na.numPositionKeys != 0 || na.numRotationKeys != 0 || na.numScalingKeys != 0)
                    nodeAnims += na
                continue
            }
            bit = bit shl 1 // TODO check early returns
        }

        nodeAnimChainBits[fixedName] = flags
    }

    /** sanity check whether the input is ok */
    fun validateAnimCurveNodes(curves: ArrayList<AnimationCurveNode>, strictMode: Boolean) {
        var target: Object? = null
        for (node in curves) {
            if (target == null) target = node.target
            if (node.target !== target)
                logger.warn("Node target is nullptr type.")
            if (strictMode) assert(node.target === target)
        }
    }

    fun isRedundantAnimationData(target: Model, comp: Tc, curves: ArrayList<AnimationCurveNode>): Boolean {
        assert(curves.isNotEmpty())

        /*  look for animation nodes with
                - sub channels for all relevant components set
                - one key/value pair per component
                - combined values match up the corresponding value in the bind pose node transformation
            only such nodes are 'redundant' for this function.  */

        if (curves.size > 1) return false

        val nd = curves[0]
        val subCurves = nd.curves

        val dx = subCurves["d|X"] ?: return false
        val dy = subCurves["d|Y"] ?: return false
        val dz = subCurves["d|Z"] ?: return false

        val vx = dx.values
        val vy = dy.values
        val vz = dz.values

        if (vx.size != 1 || vy.size != 1 || vz.size != 1) return false

        val dynVal = AiVector3D(vx[0], vy[0], vz[0])
        val staticVal = target.props(comp.nameProperty, comp.defaultValue)

        val epsilon = 1e-6f
        return (dynVal - staticVal).squareLength < epsilon
    }

    fun generateRotationNodeAnim(name: String, target: Model, curves: ArrayList<AnimationCurveNode>, layerMap: MutableMap<AnimationCurveNode, AnimationLayer>,
                                 start: Long, stop: Long, minMaxTime: DoubleArray): AiNodeAnim {
        val na = AiNodeAnim().apply { nodeName = name }

        convertRotationKeys(na, curves, layerMap, start, stop, minMaxTime, target.rotationOrder)

        // dummy scaling key
        na.scalingKeys = arrayListOf(AiVectorKey())
        na.numScalingKeys = 1

        na.scalingKeys[0].time = 0.0
        na.scalingKeys[0].value = AiVector3D(1f)

        // dummy position key
        na.positionKeys = arrayListOf(AiVectorKey())
        na.numPositionKeys = 1

        na.positionKeys[0].time = 0.0
        na.positionKeys[0].value = AiVector3D()

        return na
    }

    fun generateScalingNodeAnim(name: String, target: Model, curves: ArrayList<AnimationCurveNode>, layerMap: MutableMap<AnimationCurveNode, AnimationLayer>,
                                start: Long, stop: Long, minMaxTime: DoubleArray): AiNodeAnim {
        val na = AiNodeAnim().apply { nodeName = name }

        convertScaleKeys(na, curves, layerMap, start, stop, minMaxTime)

        // dummy rotation key
        na.rotationKeys = arrayListOf(AiQuatKey())
        na.numRotationKeys = 1

        na.rotationKeys[0].time = 0.0
        na.rotationKeys[0].value = AiQuaternion()

        // dummy position key
        na.positionKeys = arrayListOf(AiVectorKey())
        na.numPositionKeys = 1

        na.positionKeys[0].time = 0.0
        na.positionKeys[0].value = AiVector3D()

        return na
    }

    fun generateTranslationNodeAnim(name: String, target: Model, curves: ArrayList<AnimationCurveNode>, layerMap: MutableMap<AnimationCurveNode, AnimationLayer>,
                                    start: Long, stop: Long, minMaxTime: DoubleArray, inverse: Boolean = false): AiNodeAnim {
        val na = AiNodeAnim().apply { nodeName = name }

        convertTranslationKeys(na, curves, layerMap, start, stop, minMaxTime)

        if (inverse)
            for (i in 0 until na.numPositionKeys)
                na.positionKeys[i].value timesAssign -1f

        // dummy scaling key
        na.scalingKeys = arrayListOf(AiVectorKey())
        na.numScalingKeys = 1

        na.scalingKeys[0].time = 0.0
        na.scalingKeys[0].value = AiVector3D(1f)

        // dummy rotation key
        na.rotationKeys = arrayListOf(AiQuatKey())
        na.numRotationKeys = 1

        na.rotationKeys[0].time = 0.0
        na.rotationKeys[0].value = AiQuaternion()

        return na
    }

    /** generate node anim, extracting only Rotation, Scaling and Translation from the given chain */
    fun generateSimpleNodeAnim(name: String, target: Model, chain: Array<ArrayList<AnimationCurveNode>>,
                               layerMap: MutableMap<AnimationCurveNode, AnimationLayer>, start: Long, stop: Long,
                               minMaxTime: DoubleArray, reverseOrder: Boolean): AiNodeAnim {

        val na = AiNodeAnim().apply { nodeName = name }

        val props = target.props

        // need to convert from TRS order to SRT?
        if (reverseOrder) {

            val defScale = props("Lcl Scaling", AiVector3D(1))
            val defTranslate = props("Lcl Translation", AiVector3D())
            val defRot = props("Lcl Rotation", AiVector3D())

            val scaling = ArrayList<KeyFrameList>()
            val translation = ArrayList<KeyFrameList>()
            val rotation = ArrayList<KeyFrameList>()

            chain.getOrNull(Tc.Scaling.i)?.let { scaling += getKeyframeList(it, start, stop) }
            chain.getOrNull(Tc.Translation.i)?.let { translation += getKeyframeList(it, start, stop) }
            chain.getOrNull(Tc.Rotation.i)?.let { rotation += getKeyframeList(it, start, stop) }

            val joined = ArrayList<KeyFrameList>()
            joined += scaling
            joined += translation
            joined += rotation

            val times = getKeyTimeList(joined)

            val outQuat = Array(times.size) { AiQuatKey() }.toCollection(ArrayList())
            val outScale = Array(times.size) { AiVectorKey() }.toCollection(ArrayList())
            val outTranslation = Array(times.size) { AiVectorKey() }.toCollection(ArrayList())

            if (times.isNotEmpty())
                convertTransformOrderTrsToSrt(outQuat, outScale, outTranslation,
                        scaling, translation, rotation,
                        times, minMaxTime, target.rotationOrder,
                        defScale, defTranslate, defRot)

            // XXX remove duplicates / redundant keys which this operation did likely produce if not all three channels were equally dense.

            na.numScalingKeys = times.size
            na.numRotationKeys = na.numScalingKeys
            na.numPositionKeys = na.numScalingKeys

            na.scalingKeys.addAll(outScale)
            na.rotationKeys.addAll(outQuat)
            na.positionKeys.addAll(outTranslation)
        } else {
            /*  if a particular transformation is not given, grab it from the corresponding node to meet the semantics
                of AiNodeAnim, which requires all of rotation, scaling and translation to be set.   */
            if (chain[Tc.Scaling.i].isNotEmpty()) {
                convertScaleKeys(na, chain[Tc.Scaling.i], layerMap, start, stop, minMaxTime)
            } else {
                na.scalingKeys = arrayListOf(AiVectorKey())
                na.numScalingKeys = 1

                na.scalingKeys[0].time = 0.0
                na.scalingKeys[0].value = props("Lcl Scaling", AiVector3D(1f))
            }

            if (chain[Tc.Rotation.i].isNotEmpty())
                convertRotationKeys(na, chain[Tc.Rotation.i], layerMap, start, stop, minMaxTime, target.rotationOrder)
            else {
                na.rotationKeys = arrayListOf(AiQuatKey())
                na.numRotationKeys = 1

                na.rotationKeys[0].time = 0.0
                na.rotationKeys[0].value = eulerToQuaternion(props("Lcl Rotation", AiVector3D()), target.rotationOrder)
            }

            if (chain[Tc.Translation.i].isNotEmpty())
                convertTranslationKeys(na, chain[Tc.Translation.i], layerMap, start, stop, minMaxTime)
            else {
                na.positionKeys = arrayListOf(AiVectorKey())
                na.numPositionKeys = 1

                na.positionKeys[0].time = 0.0
                na.positionKeys[0].value = props("Lcl Translation", AiVector3D(0f))
            }

        }
        return na
    }

    fun getKeyframeList(nodes: ArrayList<AnimationCurveNode>, start: Long, stop: Long): ArrayList<KeyFrameList> {

        val inputs = ArrayList<KeyFrameList>(nodes.size * 3)

        //give some breathing room for rounding errors
        val adjStart = start - 10000
        val adjStop = stop + 10000

        for (node in nodes) {
//            assert(node)

            for (kv in node.curves) {

                val mapTo = if (kv.key == "d|X") 0
                else if (kv.key == "d|Y") 1
                else if (kv.key == "d|Z") 2
                else {
                    logger.warn("ignoring scale animation curve, did not recognize target component")
                    continue
                }

                val curve = kv.value
                assert(curve.keys.size == curve.values.size && curve.keys.isNotEmpty())

                //get values within the start/stop time window
                val count = curve.keys.size
                val keys = KeyTimeList(count)
                val values = KeyValueList(count)
                for (n in 0 until count) {
                    val k = curve.keys[n]
                    if (k in adjStart..adjStop) {
                        keys += k
                        values += curve.values[n]
                    }
                }
                inputs += KeyFrameList(keys, values, mapTo)
            }
        }
        return inputs // pray for NRVO :-)
    }

    fun getKeyTimeList(inputs: ArrayList<KeyFrameList>): KeyTimeList {

        assert(inputs.isNotEmpty())

        /*  reserve some space upfront - it is likely that the keyframe lists have matching time values,
            so max(of all keyframe lists) should be a good estimate.    */
        val keys = KeyTimeList()

        val estimate = inputs.maxBy { it.first.size }?.first?.size ?: 0

        keys.ensureCapacity(estimate)

        val nextPos = IntArray(inputs.size)

        val count = inputs.size
        while (true) {
            var minTick = Long.MAX_VALUE
            for (i in 0 until count) {
                val kfl = inputs[i]
                if (kfl.first.size > nextPos[i] && kfl.first[nextPos[i]] < minTick)
                    minTick = kfl.first[nextPos[i]]
            }

            if (minTick == Long.MAX_VALUE) break

            keys += minTick

            for (i in 0 until count) {
                val kfl = inputs[i]
                while (kfl.first.size > nextPos[i] && kfl.first[nextPos[i]] == minTick)
                    ++nextPos[i]
            }
        }

        return keys
    }

    fun interpolateKeys(valOut: ArrayList<AiVectorKey>, keys: KeyTimeList, inputs: ArrayList<KeyFrameList>,
                        defValue: AiVector3D, minMaxTime: DoubleArray) {
        assert(keys.isNotEmpty())

        val nextPos = IntArray(inputs.size)
        val count = inputs.size

        var valOutIdx = 0
        for (time in keys) {
            val result = FloatArray(3, { defValue[it] })

            for (i in 0 until count) {
                val kfl = inputs[i]
                val kSize = kfl.first.size
                if (kSize > nextPos[i] && kfl.first[nextPos[i]] == time)
                    ++nextPos[i]

                val id0 = if (nextPos[i] > 0) nextPos[i] - 1 else 0
                val id1 = if (nextPos[i] == kSize) kSize - 1 else nextPos[i]

                // use lerp for interpolation
                val valueA = kfl.second[id0]
                val valueB = kfl.second[id1]

                val timeA = kfl.first[id0]
                val timeB = kfl.first[id1]

                val factor = if (timeB == timeA) 0f else (time - timeA) / (timeB - timeA).f
                val interpValue = valueA + (valueB - valueA) * factor

                result[kfl.third] = interpValue
            }

            // magic value to convert fbx times to seconds
            valOut[valOutIdx].time = CONVERT_FBX_TIME(time) * animFps

            minMaxTime[0] = min(minMaxTime[0], valOut[valOutIdx].time)
            minMaxTime[1] = max(minMaxTime[1], valOut[valOutIdx].time)

            valOut[valOutIdx].value put result

            ++valOutIdx
        }
    }

    fun interpolateKeys(valOut: ArrayList<AiQuatKey>, keys: KeyTimeList, inputs: ArrayList<KeyFrameList>,
                        defValue: AiVector3D, minMaxTime: DoubleArray, order: Model.RotOrder) {
        assert(keys.isNotEmpty())
        assert(valOut.isNotEmpty())

        val temp = Array(keys.size) { AiVectorKey() }.toCollection(ArrayList())
        interpolateKeys(temp, keys, inputs, defValue, minMaxTime)

        val lastQ = AiQuaternion()

        for (i in 0 until keys.size) {

            valOut[i].time = temp[i].time

            val m = getRotationMatrix(order, temp[i].value)
            val quat = AiMatrix3x3(m).toQuat()

            // take shortest path by checking the inner product http://www.3dkingdoms.com/weekly/weekly.php?a=36
            if (quat.x * lastQ.x + quat.y * lastQ.y + quat.z * lastQ.z + quat.w * lastQ.w < 0) {
                quat.x = -quat.x
                quat.y = -quat.y
                quat.z = -quat.z
                quat.w = -quat.w
            }
            lastQ put quat

            valOut[i].value = quat
        }
    }

    fun convertTransformOrderTrsToSrt(outQuat: ArrayList<AiQuatKey>, outScale: ArrayList<AiVectorKey>, outTranslation: ArrayList<AiVectorKey>,
                                      scaling: ArrayList<KeyFrameList>, translation: ArrayList<KeyFrameList>, rotation: ArrayList<KeyFrameList>,
                                      times: KeyTimeList, minMaxTime: DoubleArray, order: Model.RotOrder,
                                      defScale: AiVector3D, defTranslate: AiVector3D, defRotation: AiVector3D) {
        if (rotation.isNotEmpty())
            interpolateKeys(outQuat, times, rotation, defRotation, minMaxTime, order)
        else {
            for (i in 0 until times.size) {
                outQuat[i].time = CONVERT_FBX_TIME(times[i]) * animFps
                outQuat[i].value put eulerToQuaternion(defRotation, order)
            }
        }

        if (scaling.isNotEmpty())
            interpolateKeys(outScale, times, scaling, defScale, minMaxTime)
        else {
            for (i in 0 until times.size) {
                outScale[i].time = CONVERT_FBX_TIME(times[i]) * animFps
                outScale[i].value = defScale
            }
        }

        if (translation.isNotEmpty())
            interpolateKeys(outTranslation, times, translation, defTranslate, minMaxTime)
        else {
            for (i in 0 until times.size) {
                outTranslation[i].time = CONVERT_FBX_TIME(times[i]) * animFps
                outTranslation[i].value = defTranslate
            }
        }

        val count = times.size
        for (i in 0 until count) {
            val r = outQuat[i].value
            val s = outScale[i].value
            val t = outTranslation[i].value

            val mat = assimp.translation(t)
            mat *= AiMatrix4x4(r.mat)
            mat *= scaling(s)

            mat.decompose(s, r, t)
        }
    }

    /** euler xyz -> quat   */
    fun eulerToQuaternion(rot: AiVector3D, order: Model.RotOrder) = AiMatrix3x3(getRotationMatrix(order, rot)).toQuat()


    fun convertScaleKeys(na: AiNodeAnim, nodes: ArrayList<AnimationCurveNode>, layers: MutableMap<AnimationCurveNode, AnimationLayer>,
                         start: Long, stop: Long, minMaxTime: DoubleArray) {
        assert(nodes.isNotEmpty())

        /*  XXX for now, assume scale should be blended geometrically (i.e. two layers should be multiplied with each other).
            There is a FBX property in the layer to specify the behaviour, though.  */

        val inputs = getKeyframeList(nodes, start, stop)
        val keys = getKeyTimeList(inputs)

        na.numScalingKeys = keys.size
        na.scalingKeys = Array(keys.size) { AiVectorKey() }.toCollection(ArrayList())
        if (keys.isNotEmpty())
            interpolateKeys(na.scalingKeys, keys, inputs, AiVector3D(1f), minMaxTime)
    }

    fun convertTranslationKeys(na: AiNodeAnim, nodes: ArrayList<AnimationCurveNode>, layers: MutableMap<AnimationCurveNode, AnimationLayer>,
                               start: Long, stop: Long, minMaxTime: DoubleArray) {
        assert(nodes.isNotEmpty())

        // XXX see notes in ConvertScaleKeys()
        val inputs = getKeyframeList(nodes, start, stop)
        val keys = getKeyTimeList(inputs)

        na.numPositionKeys = keys.size
        na.positionKeys = Array(keys.size) { AiVectorKey() }.toCollection(ArrayList())
        if (keys.isNotEmpty())
            interpolateKeys(na.positionKeys, keys, inputs, AiVector3D(), minMaxTime)
    }

    fun convertRotationKeys(na: AiNodeAnim, nodes: ArrayList<AnimationCurveNode>, layers: MutableMap<AnimationCurveNode, AnimationLayer>,
                            start: Long, stop: Long, minMaxTime: DoubleArray, order: Model.RotOrder) {
        assert(nodes.isNotEmpty())

        // XXX see notes in ConvertScaleKeys()
        val inputs = getKeyframeList(nodes, start, stop)
        val keys = getKeyTimeList(inputs)

        na.numRotationKeys = keys.size
        na.rotationKeys = Array(keys.size) { AiQuatKey() }.toCollection(ArrayList())
        if (keys.isNotEmpty())
            interpolateKeys(na.rotationKeys, keys, inputs, AiVector3D(), minMaxTime, order)
    }

    fun convertGlobalSettings() {
        out.metaData = AiMetadata().apply {
            val unitScalFactor = doc.globals!!.unitScaleFactor
            map["UnitScaleFactor"] = AiMetadataEntry(unitScalFactor)
        }
    }

    /** copy generated meshes, animations, lights, cameras and textures to the output scene */
    fun transferDataToScene() {
        assert(out.meshes.isEmpty())
        assert(out.numMeshes == 0)

        /*  note: the trailing () ensures initialization with NULL - not many C++ users seem to know this,
            so pointing it out to avoid confusion why this code works.  */

        if (meshes.isNotEmpty()) {
            out.meshes.clear()
            out.numMeshes = meshes.size
            out.meshes.addAll(meshes)
        }

        if (materials.isNotEmpty()) {
            out.materials.clear()
            out.numMaterials = materials.size
            out.materials.addAll(materials)
        }

        if (animations.isNotEmpty()) {
            out.animations.clear()
            out.numAnimations = animations.size
            out.animations.addAll(animations)
        }

        if (lights.isNotEmpty()) {
            out.lights.clear()
            out.numLights = lights.size
            out.lights.addAll(lights)
        }

        if (cameras.isNotEmpty()) {
            out.cameras.clear()
            out.numCameras = cameras.size
            out.cameras.addAll(cameras)
        }

        if (textures.isNotEmpty()) {
            out.textures.clear()
            out.numTextures = textures.size
//            out.textures.putAll(textures) TODO
            TODO()
        }
    }

    fun findTextureIndexByFilename(video: Video, index: KMutableProperty0<Int>): Boolean {
        index.set(0)
        val videoFileName = video.fileName
        for (texture in texturesConverted)
            if (texture.key.fileName == videoFileName) {
                index.set(texture.value)
                return true
            }
        return false
    }

    companion object {
        var _i = 0
    }
}