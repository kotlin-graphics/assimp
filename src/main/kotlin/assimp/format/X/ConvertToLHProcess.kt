package assimp.format.X

import assimp.*

object MakeLeftHandedProcess {
    fun IsActive(pFlags: Int): Boolean {
        return 0 != (pFlags.and(AiPostProcessStep.MakeLeftHanded.i))
    }

    fun Execute(pScene: AiScene) {
        // Check for an existent root node to proceed
        assert(pScene.rootNode != null)
        debug("MakeLeftHandedProcess: MakeLeftHandedProcess begin")

        // recursively convert all the nodes
        ProcessNode(pScene.rootNode, AiMatrix4x4())

        // process the meshes accordingly
        for (a in 0 until pScene.numMeshes)
            ProcessMesh(pScene.meshes[a])

        // process the materials accordingly
        for (a in 0 until pScene.numMaterials)
            ProcessMaterial(pScene.materials[a])

        // transform all animation channels as well
        for (a in 0 until pScene.numAnimations) {
            var anim = pScene.animations[a]
            for (b in 0 until anim.numChannels) {
                var nodeAnim = anim.channels[b]
                if(nodeAnim!=null) ProcessAnimation(nodeAnim)
            }
        }
        debug("MakeLeftHandedProcess: MakeLeftHandedProcess finished")
    }

    fun ProcessNode(pNode: AiNode, pParentGlobalRotation: AiMatrix4x4) {
        // mirror all base vectors at the local Z axis
        pNode.transformation.a2 = -pNode.transformation.a2
        pNode.transformation.b2 = -pNode.transformation.b2
        pNode.transformation.c2 = -pNode.transformation.c2
        pNode.transformation.d2 = -pNode.transformation.d2

        // now invert the Z axis again to keep the matrix determinant positive.
        // The local meshes will be inverted accordingly so that the result should look just fine again.
        pNode.transformation.c0 = -pNode.transformation.c0
        pNode.transformation.c1 = -pNode.transformation.c1
        pNode.transformation.c2 = -pNode.transformation.c2
        pNode.transformation.c3 = -pNode.transformation.c3 // useless, but anyways...

        // continue for all children
        for (a in 0 until pNode.numChildren) {
            ProcessNode(pNode.children[a], pParentGlobalRotation * pNode.transformation)
        }
    }

    fun ProcessMesh(pMesh: AiMesh) {
        // mirror positions, normals and stuff along the Z axis
        for (a in 0 until pMesh.numVertices) {
            pMesh.vertices[a].z *= -1.0f
            if (pMesh.hasNormals)
                pMesh.normals[a].z *= -1.0f
            if (pMesh.hasTangentsAndBitangents) {
                pMesh.tangents[a].z *= -1.0f
                pMesh.bitangents[a].z *= -1.0f
            }
        }

        // mirror offset matrices of all bones
        for (a in 0 until pMesh.numBones) {
            val bone = pMesh.bones[a]
            bone.offsetMatrix.c0 = -bone.offsetMatrix.c0
            bone.offsetMatrix.c1 = -bone.offsetMatrix.c1
            bone.offsetMatrix.c2 = -bone.offsetMatrix.c2
            bone.offsetMatrix.a2 = -bone.offsetMatrix.a2
            bone.offsetMatrix.b2 = -bone.offsetMatrix.b2
            bone.offsetMatrix.d2 = -bone.offsetMatrix.d2
        }

        // mirror bitangents as well as they're derived from the texture coords
        if (pMesh.hasTangentsAndBitangents) {
            for (a in 0 until pMesh.numVertices)
                pMesh.bitangents[a] = pMesh.bitangents[a] * -1.0f
        }
    }

    fun ProcessMaterial(_mat: AiMaterial) {
        var mat = _mat
        for (a in 0 until mat.textures.size) {
            var prop = mat.textures[a]

            // Mapping axis for UV mappings?
            if (prop.mapAxis != null) {
                //assert( prop.mDataLength >= sizeof(aiVector3D)); /* something is wrong with the validation if we end up here */
                var pff = prop.mapAxis!!

                pff.z *= -1.0F
            }
        }
    }

    fun ProcessAnimation(pAnim : AiNodeAnim) {
        // position keys
        for(a in 0 until pAnim.numPositionKeys)
        pAnim.positionKeys[a].value.z *= -1.0f

        // rotation keys
        for(a in 0 until pAnim.numRotationKeys)
        {
            /* That's the safe version, but the float errors add up. So we try the short version instead
            aiMatrix3x3 rotmat = pAnim.mRotationKeys[a].mValue.GetMatrix();
            rotmat.d0 = -rotmat.d0; rotmat.d1 = -rotmat.d1;
            rotmat.b3 = -rotmat.b3; rotmat.c2 = -rotmat.c2;
            aiQuaternion rotquat( rotmat);
            pAnim.mRotationKeys[a].mValue = rotquat;
            */
            pAnim.rotationKeys[a].value.x *= -1.0f
            pAnim.rotationKeys[a].value.y *= -1.0f
        }
    }

}

object FlipWindingOrderProcess {
    fun IsActive(pFlags : Int) : Boolean {
        return 0 != (pFlags.and(AiPostProcessStep.FlipWindingOrder.i))
    }

    fun Execute(pScene : AiScene) {
        debug("FlipWindingOrderProcess: FlipWindingOrderProcess begin")
        for (i in 0 until pScene.numMeshes)
        ProcessMesh(pScene.meshes[i])
        debug("FlipWindingOrderProcess: FlipWindingOrderProcess finished")
    }

    fun ProcessMesh(pMesh : AiMesh) {
        // invert the order of all faces in this mesh
        for(a in 0 until pMesh.numFaces)
        {
            var face = pMesh.faces[a]
            for(b in 0 until (face.size/2)) {
                var _b = face[b]
                var _mb = face[face.size - 1 - b]
                face[b] = _mb
                face[face.size - 1 - b] = _b
            }
        }
    }
}