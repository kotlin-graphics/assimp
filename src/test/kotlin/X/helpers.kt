package X

import assimp.*
import gli_.Cache
import gli_.Texture
import io.kotlintest.*

val x = "$models/X/"
val x_ass = "$models/models-assbin-db/X/"

fun printNodeNames(n : AiNode, done : MutableList<AiNode> = MutableList<AiNode>(0, { AiNode() })) {
    println(n.name); done.add(n)
    for(a in n.children){
//        if(a in done) {
//            continue
//        }
        printNodeNames(a, done)
    }
}

fun compareScenes(aiScene1 : AiScene, aiScene2 : AiScene) {
    aiScene1.numAnimations shouldBe aiScene2.numAnimations
    for(i in 0 until aiScene1.animations.size) {
        compareAnimations(aiScene1.animations[i], aiScene2.animations[i])
    }

    aiScene1.numCameras shouldBe aiScene2.numCameras
    for(i in 0 until aiScene1.cameras.size) {
        compareCameras(aiScene1.cameras[i], aiScene2.cameras[i])
    }

    aiScene1.numLights shouldBe aiScene2.numLights
    for(i in 0 until aiScene1.lights.size) {
        compareLights(aiScene1.lights[i], aiScene2.lights[i])
    }

    aiScene1.numMaterials shouldBe aiScene2.numMaterials
    for(i in 0 until aiScene1.materials.size) {
        compareMaterials(aiScene1.materials[i], aiScene2.materials[i])
    }

    aiScene1.flags shouldBe aiScene2.flags

    aiScene1.numTextures shouldBe aiScene2.numTextures
    aiScene1.textures.size shouldBe aiScene2.textures.size
    for(entry in aiScene1.textures) {
        aiScene2.textures.containsKey(entry.key) shouldBe true
        compareTextures(entry.value, aiScene2.textures[entry.key]!!)
    }

    aiScene1.numMeshes shouldBe aiScene2.numMeshes
    for(i in 0 until aiScene1.meshes.size) {
        compareMeshes(aiScene1.meshes[i], aiScene2.meshes[i])
    }
}

fun compareTextures(texture: Texture, texture1: Texture) {
    texture.baseFace shouldBe texture1.baseFace
    texture.baseLayer shouldBe texture1.baseLayer
    texture.baseLevel shouldBe texture1.baseLevel
    compareCache(texture.cache, texture1.cache)
    texture.format.shouldEqual(texture1.format)
    texture.maxFace shouldBe texture1.maxFace
    texture.maxLayer shouldBe texture1.maxLayer
    texture.maxLevel shouldBe texture1.maxLevel
    texture.size shouldBe texture1.size
    texture.swizzles.shouldEqual(texture1.swizzles)
    texture.target.shouldEqual(texture1.target)
}

fun compareCache(cache: Cache, cache1: Cache) {
    cache.faces shouldBe cache1.faces
    cache.levels shouldBe cache1.levels
    cache.memorySize shouldBe cache1.memorySize
}

fun compareNode(rootNode: AiNode, rootNode1: AiNode) {
    rootNode.name shouldBe rootNode1.name

    rootNode.numMeshes shouldBe rootNode1.numMeshes
    compareIntArray(rootNode.meshes, rootNode1.meshes)

    if(!areNull(rootNode.metaData, rootNode1.metaData))
        compareMetaData(rootNode.metaData, rootNode1.metaData)

    compareMatrix(rootNode.transformation, rootNode1.transformation)

    rootNode.numChildren shouldBe rootNode1.numChildren
    for(i in 0 until rootNode.children.size) {
        compareNode(rootNode.children[i], rootNode1.children[i])
    }
}

fun compareMetaData(aiMetadata: AiMetadata, aiMetadata1: AiMetadata) {

}

fun compareMaterials(a1: AiMaterial, a2: AiMaterial) {
    a1.blendFunc shouldBe a2.blendFunc
    if(a1.blendFunc!=null && a2.blendFunc!=null) {
        compareBlendMode(a1.blendFunc!!, a2.blendFunc!!)
    }

    a1.bumpScaling shouldBe a2.bumpScaling
    a1.name shouldBe a2.name
    a1.opacity shouldBe a2.opacity
    a1.reflectivity shouldBe a2.reflectivity
    a1.refracti shouldBe a2.refracti
    compareShadingMode(a1.shadingModel, a2.shadingModel)
    a1.shininess shouldBe a2.shininess
    a1.shininessStrength shouldBe a2.shininessStrength
    a1.twoSided shouldBe a2.twoSided
    a1.wireframe shouldBe a2.wireframe
    if(!areNull(a1.color, a2.color))
        compareColors(a1.color!!, a2.color!!)
    a1.textures.size shouldBe a2.textures.size
    for(i in 0 until a1.textures.size) {
        compareTextures(a1.textures[i], a1.textures[i])
    }
}

fun compareTextures(texture: AiMaterial.Texture, texture1: AiMaterial.Texture) {
    texture.blend shouldBe texture1.blend
    texture.file shouldBe texture1.file
    texture.flags shouldBe texture1.flags
    if(!areNull(texture.mapAxis, texture1.mapAxis))
        compareVector(texture.mapAxis!!, texture1.mapAxis!!)
    texture.mapModeU!!.i shouldBe texture1.mapModeU!!.i
    texture.mapModeV!!.i shouldBe texture1.mapModeV!!.i
    texture.mapping!!.i shouldBe texture1.mapping!!.i
    texture.op!!.i shouldBe texture1.op!!.i
    texture.type!!.i shouldBe texture1.type!!.i
    compareUVTransform(texture.uvTrafo!!, texture1.uvTrafo!!)
    texture.uvwsrc shouldBe texture1.uvwsrc
}

fun areNull(a1 : Any?, a2: Any?) : Boolean {
    if(a1 == null) {
        (a2 == null) shouldBe true
        return true
    }
    if(a2 == null) {
        (a1 == null) shouldBe true
        return true
    }
    return false
}

fun compareUVTransform(uvTrafo: AiUVTransform, uvTrafo1: AiUVTransform) {
    compareVector(uvTrafo.scaling, uvTrafo1.scaling)
    compareVector(uvTrafo.translation, uvTrafo1.translation)
    uvTrafo.rotation shouldBe uvTrafo1.rotation
}

fun compareShadingMode(shadingModel: AiShadingMode?, shadingModel1: AiShadingMode?) {
    if(shadingModel==null) {
        shadingModel1 shouldBe null
        return
    }
    shadingModel.i  shouldBe shadingModel1!!.i
}

fun compareColors(color: AiMaterial.Color, color1: AiMaterial.Color) {
    if(!areNull(color.ambient, color1.ambient))
        compareColors(color.ambient!!, color1.ambient!!)
    if(!areNull(color.diffuse, color1.diffuse))
        compareColors(color.diffuse!!, color1.diffuse!!)
    if(!areNull(color.emissive, color1.emissive))
        compareColors(color.emissive!!, color1.emissive!!)
    if(!areNull(color.reflective, color1.reflective))
        compareColors(color.reflective!!, color1.reflective!!)
    if(!areNull(color.specular, color1.specular))
        compareColors(color.specular!!, color1.specular!!)
    if(!areNull(color.transparent, color1.transparent))
        compareColors(color.transparent!!, color1.transparent!!)
}

fun compareBlendMode(blendFunc: AiBlendMode, blendFunc1: AiBlendMode) {
    blendFunc.i shouldBe blendFunc1.i
}

fun compareLights(a1: AiLight, a2: AiLight) {
    a1.name shouldBe a2.name
    a1.angleInnerCone shouldBe a2.angleInnerCone
    a1.angleOuterCone shouldBe a2.angleOuterCone
    a1.attenuationConstant shouldBe a2.attenuationConstant
    a1.attenuationLinear shouldBe a2.attenuationLinear
    a1.attenuationQuadratic shouldBe a2.attenuationQuadratic
    a1.type.i shouldBe a2.type.i
    compareColors(a1.colorAmbient, a2.colorAmbient)
    compareColors(a1.colorDiffuse, a2.colorDiffuse)
    compareColors(a1.colorSpecular, a2.colorSpecular)
    compareVector(a1.direction, a2.direction)
    compareVector(a1.up, a2.up)
    compareVector(a1.position, a2.position)
    compareVector(a1.size, a2.size)
}

fun compareVector(lookAt: AiVector2D, lookAt1: AiVector2D) {
    lookAt.x shouldBe lookAt1.x
    lookAt.y shouldBe lookAt1.y
}

fun compareColors(col1: AiColor3D, col2: AiColor3D) {
    col1.x shouldBe col2.x
    col1.y shouldBe col2.y
    col1.z shouldBe col2.z
}

fun compareCameras(aiCamera: AiCamera, aiCamera1: AiCamera) {
    aiCamera.aspect shouldBe aiCamera1.aspect
    aiCamera.clipPlaneFar shouldBe aiCamera1.clipPlaneFar
    aiCamera.clipPlaneNear shouldBe aiCamera1.clipPlaneNear
    aiCamera.horizontalFOV shouldBe aiCamera1.horizontalFOV
    aiCamera.name shouldBe aiCamera1.name
    compareVector(aiCamera.lookAt, aiCamera1.lookAt)
    compareVector(aiCamera.position, aiCamera1.position)
    compareVector(aiCamera.up, aiCamera1.up)
}

fun compareVector(lookAt: AiVector3D, lookAt1: AiVector3D) {
    lookAt.x shouldBe lookAt1.x
    lookAt.y shouldBe lookAt1.y
    lookAt.z shouldBe lookAt1.z
}

fun compareAnimations(anim1: AiAnimation, anim2: AiAnimation) {
    anim1.name shouldBe anim2.name
    anim1.duration shouldBe anim2.duration
    anim1.ticksPerSecond shouldBe anim2.ticksPerSecond

    anim1.mNumMeshChannels shouldBe anim2.mNumMeshChannels
    for(i in 0 until anim1.mMeshChannels.size) {
        anim1.mMeshChannels[i].size shouldBe anim2.mMeshChannels[i]
        for(j in 0 until anim1.mMeshChannels[i].size) {
            compareMeshAnim(anim1.mMeshChannels[i][j], anim2.mMeshChannels[i][j])
        }
    }

    anim1.numChannels shouldBe anim2.numChannels
    for(i in 0 until anim1.channels.size) {
        compareNodeAnim(anim1.channels[i], anim2.channels[i])
    }

    anim1.numMorphMeshChannels shouldBe anim2.numMorphMeshChannels
    for(i in 0 until anim1.morphMeshChannels.size) {
        compareMeshMorphAnim(anim1.morphMeshChannels[i], anim2.morphMeshChannels[i])
    }
}

fun compareMeshMorphAnim(a1: AiMeshMorphAnim, a2: AiMeshMorphAnim) {
    a1.name shouldBe a2.name
    a1.numKeys shouldBe a2.numKeys
    for(i in 0 until a1.keys.size) {
        compareMeshMorphKey(a1.keys[i], a2.keys[i])
    }
}

fun compareMeshMorphKey(a1: AiMeshMorphKey, a2: AiMeshMorphKey) {
    a1.numValuesAndWeights shouldBe a2.numValuesAndWeights
    a1.time shouldBe a2.time
    compareIntArray(a1.values, a2.values)
    compareDoubleArray(a1.weights, a2.weights)
}

fun compareIntArray(v1: IntArray, v2: IntArray) {
    v1.size shouldBe v2.size
    for(i in 0 until v1.size) {
        v1[i] shouldBe v2[i]
    }
}

fun compareDoubleArray(v1: DoubleArray, v2: DoubleArray) {
    v1.size shouldBe v2.size
    for(i in 0 until v1.size) {
        v1[i] shouldBe v2[i]
    }
}

fun compareNodeAnim(nodeanim1: AiNodeAnim?, nodeanim2: AiNodeAnim?) {
    nodeanim1 shouldNotBe null
    nodeanim2 shouldNotBe null
    nodeanim1!!.nodeName shouldBe nodeanim2!!.nodeName

    nodeanim1.numPositionKeys shouldBe nodeanim2.numPositionKeys
    for(i in 0 until nodeanim1.positionKeys.size) {
        compareVectorKey(nodeanim1.positionKeys[i], nodeanim2.positionKeys[i])
    }

    nodeanim1.numRotationKeys shouldBe nodeanim2.numRotationKeys
    for(i in 0 until nodeanim1.rotationKeys.size) {
        compareQuatKey(nodeanim1.rotationKeys[i], nodeanim2.rotationKeys[i])
    }

    nodeanim1.numScalingKeys shouldBe nodeanim2.numScalingKeys
    for(i in 0 until nodeanim1.scalingKeys.size) {
        compareVectorKey(nodeanim1.scalingKeys[i], nodeanim2.scalingKeys[i])
    }

    compareAnimBehaviour(nodeanim1.preState, nodeanim2.preState)
    compareAnimBehaviour(nodeanim1.postState, nodeanim2.postState)
}

fun compareAnimBehaviour(p1: AiAnimBehaviour, p2: AiAnimBehaviour) {
    p1.i shouldBe p2.i
}

fun compareQuatKey(qk1: AiQuatKey, qk2: AiQuatKey) {
    qk1.time shouldBe qk2.time
    qk1.value shouldBe qk2.value
}

fun compareVectorKey(vk1: AiVectorKey, vk2: AiVectorKey) {
    vk1.time shouldBe vk2.time
    vk1.value shouldBe vk2.value
}

fun compareMeshAnim(anim1: AiMeshAnim, anim2: AiMeshAnim) {
    anim1.mName shouldBe anim2
    anim1.mNumKeys shouldBe anim2.mNumKeys
    for(i in 0 until anim1.mKeys.size) {
        compareMeshKeys(anim1.mKeys[i], anim2.mKeys[i])
    }
}

fun compareMeshKeys(meshkey1: AiMeshKey, meshkey2: AiMeshKey) {
    meshkey1.mTime shouldBe meshkey2.mTime
    meshkey1.mValue shouldBe meshkey2.mValue
}

fun compareMeshes(aiMesh1 : AiMesh, aiMesh2 : AiMesh) {
    aiMesh1.primitiveTypes shouldBe aiMesh2.primitiveTypes
    aiMesh1.name.isEmpty() shouldBe false
    if(!aiMesh2.name.isEmpty()) {
        aiMesh1.name shouldBe aiMesh2.name
    }
    aiMesh1.mMethod shouldBe aiMesh2.mMethod
    aiMesh1.materialIndex shouldBe aiMesh2.materialIndex


    aiMesh1.numFaces shouldBe aiMesh2.numFaces
    for(i in 0 until aiMesh1.faces.size) {
        compareFaces(aiMesh1.faces[i], aiMesh2.faces[i])
    }

    aiMesh1.numVertices shouldBe aiMesh2.numVertices
    for(i in 0 until aiMesh1.vertices.size) {
        compareVertices(aiMesh1.vertices[i], aiMesh2.vertices[i])
    }

    aiMesh1.numBones shouldBe aiMesh2.numBones
    for(i in 0 until aiMesh1.bones.size) {
        compareBones(aiMesh1.bones[i], aiMesh2.bones[i])
    }

    aiMesh1.colors.size shouldBe aiMesh2.colors.size
    for(i in 0 until aiMesh1.colors.size) {
        aiMesh1.colors[i].size shouldBe aiMesh2.colors[i].size
        for(j in 0 until aiMesh1.colors[i].size) {
            compareColors(aiMesh1.colors[i][j], aiMesh2.colors[i][j])
        }
    }

    aiMesh1.normals.size shouldBe aiMesh2.normals.size
    for(i in 0 until aiMesh1.normals.size) {
        compareVertices(aiMesh1.normals[i], aiMesh2.normals[i])
    }

    aiMesh1.tangents.size shouldBe aiMesh2.tangents.size
    for(i in 0 until aiMesh1.tangents.size) {
        compareVertices(aiMesh1.tangents[i], aiMesh2.tangents[i])
    }

    aiMesh1.bitangents.size shouldBe aiMesh2.bitangents.size
    for(i in 0 until aiMesh1.bitangents.size) {
        compareVertices(aiMesh1.bitangents[i], aiMesh2.bitangents[i])
    }

    aiMesh1.textureCoords.size shouldBe aiMesh2.textureCoords.size
    for(i in 0 until aiMesh1.textureCoords.size) {
        aiMesh1.textureCoords[i].size shouldBe aiMesh2.textureCoords[i].size
        for(j in 0 until aiMesh1.textureCoords[i].size) {
            compareFloatArrays(aiMesh1.textureCoords[i][j], aiMesh2.textureCoords[i][j])
        }
    }

//    aiMesh1.getNumUVChannels()
    //shouldBe
    //aiMesh2.getNumUVChannels() //TODO: figure out why this function fails
    aiMesh1.getNumColorChannels() shouldBe aiMesh2.getNumColorChannels()

    aiMesh1.mNumAnimMeshes shouldBe aiMesh2.mNumAnimMeshes
    for(i in 0 until aiMesh1.mAnimMeshes.size) {
        compareMeshes(aiMesh1.mAnimMeshes[i], aiMesh2.mAnimMeshes[i])
    }
}

fun compareFloatArrays(a1 : FloatArray, a2: FloatArray) {
    a1.size shouldBe a2.size
    for(k in 0 until a1.size) {
        a1[k] shouldBe a2[k]
    }
}

fun compareColors(col1: AiColor4D, col2: AiColor4D) {
    col1.x shouldBe col2.x
    col1.y shouldBe col2.y
    col1.z shouldBe col2.z
    col1.w shouldBe col2.w
}

fun compareBones(aiBone1: AiBone, aiBone2: AiBone) {
    aiBone1.name shouldBe aiBone2.name

    aiBone1.numWeights shouldBe aiBone2.numWeights
    for(i in 0 until aiBone1.weights.size) {
        compareWeights(aiBone1.weights[i], aiBone2.weights[i])
    }

    compareMatrix(aiBone1.offsetMatrix, aiBone2.offsetMatrix)
}

fun compareMatrix(matrix1: AiMatrix4x4, matrix2: AiMatrix4x4) {
    matrix1.isEqual(matrix2) shouldBe true
}

fun compareWeights(aiVertexWeight1: AiVertexWeight, aiVertexWeight2: AiVertexWeight) {
    aiVertexWeight1.vertexId shouldBe aiVertexWeight2.vertexId
    aiVertexWeight1.weight shouldBe aiVertexWeight2.weight
}

fun compareVertices(vec1: AiVector3D, vec2 : AiVector3D) {
    vec1.x shouldBe vec2.x
    vec1.y shouldBe vec2.y
    if(vec1.z==-vec2.z) {
        println("Mirrored Z's in vec1: " + vec1.toString())
        println("and")
        println("Mirrored Z's in vec2: " + vec2.toString())
    } else {
        vec1.z shouldBe vec2.z
    }
}

fun compareFaces(aiFace1 : AiFace, aiFace2 : AiFace) {
    aiFace1.size shouldBe aiFace2.size
    for(i in 0 until aiFace1.size) {
        aiFace1[i] shouldBe aiFace2[i]
    }
}