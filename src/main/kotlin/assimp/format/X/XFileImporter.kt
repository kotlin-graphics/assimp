package assimp.format.X

import assimp.*
import assimp.AiFace
import java.io.File

class XFileImporter : BaseImporter() {

        override val info = AiImporterDesc(
                name = "Direct3D XFile Importer",
                comments = "Binary not supported",
                flags = AiImporterFlags.SupportTextFlavour.i, // or AiImporterFlags.SupportBinaryFlavour.i or AiImporterFlags.SupportCompressedFlavour.i,
                fileExtensions = arrayListOf("x","X")
        )


    var mBuffer: Pointer<Char> = Pointer<Char>(arrayOf())

    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {
        if (!checkSig)   //Check File Extension
            return file.substring(file.lastIndexOf('.') + 1).toLowerCase() == "x"
        else //Check file Header
            return false
    }

    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {
        // Read file into memory
        val file_ = File(file)
        if (!file_.canRead()) throw FileSystemException(file_, null, "Failed to open file \$pFile.")

        // Get the file-size and validate it, throwing an exception when fails
        val fileSize = file_.length()

        if (fileSize < 16) throw Error("XFile is too small.")

        val bytes = file_.readBytes()
        mBuffer = Pointer<Char>(Array<Char>(bytes.size, { i -> bytes[i].toChar() })) //Assuming every byte is a char.
        // parse the file into a temporary representation
        val parser = XFileParser(mBuffer)

        // And create the proper return structures out of it
        CreateDataRepresentationFromImport(scene, parser.mScene)

        if (scene.rootNode == null)
            throw Error("XFile is ill-formatted - no content imported.")
    }

    fun CreateDataRepresentationFromImport(pScene: AiScene, pData: Scene) {
        // Read the global materials first so that meshes referring to them can find them later
        ConvertMaterials(pScene, pData.mGlobalMaterials)

        // copy nodes, extracting meshes and materials on the way
        pScene.rootNode = CreateNodes(pScene, AiNode(), pData.mRootNode!!)

        // extract animations
        CreateAnimations(pScene, pData)

        // read the global meshes that were stored outside of any node
        if (pData.mGlobalMeshes.size() > 0) {
            // create a root node to hold them if there isn't any, yet
            if (pScene.rootNode == null) {
                pScene.rootNode = AiNode()
                pScene.rootNode.name = "\$dummy_node"
            }

            // convert all global meshes and store them in the root node.
            // If there was one before, the global meshes now suddenly have its transformation matrix...
            // Don't know what to do there, I don't want to insert another node under the present root node
            // just to avoid this.
            CreateMeshes(pScene, pScene.rootNode, pData.mGlobalMeshes)
        }

        if (pScene.rootNode == null) {
            throw Error("No root node")
        }

        // Convert everything to OpenGL space... it's the same operation as the conversion back, so we can reuse the step directly
        //var convertProcess : MakeLeftHandedProcess = ();
        MakeLeftHandedProcess.Execute(pScene)

        //var flipper : ;
        FlipWindingOrderProcess.Execute(pScene)

        // finally: create a dummy material if not material was imported
        if (pScene.numMaterials == 0) {
            debug("Dummy material!")
            pScene.numMaterials = 1
            // create the Material
            var mat = AiMaterial()
            var shadeMode = assimp.AiShadingMode.gouraud
            mat.shadingModel = shadeMode
            // material colours
            var specExp = 1

            var clr = AiColor3D(0, 0, 0)
            mat.color = AiMaterial.Color()
            mat.color!!.emissive = clr
            mat.color!!.specular = clr

            clr = AiColor3D(0.5f, 0.5f, 0.5f)
            mat.color!!.diffuse = clr
            mat.shininess = specExp.toFloat()

            pScene.materials = arrayListOf(AiMaterial())
            pScene.materials[0] = mat
        }
    }

    fun CreateAnimations(pScene: AiScene, pData: Scene) {
        var newAnims: MutableList<AiAnimation> = mutableListOf()

        for (a in 0 until pData.mAnims.size()) {
            var anim = pData.mAnims[a]
            // some exporters mock me with empty animation tags.
            if (anim.mAnims.size() == 0)
                continue

            // create a new animation to hold the data
            var nanim = AiAnimation()
            newAnims.push_back(nanim)
            nanim.name = if (anim.mName == null) "" else anim.mName!!
            // duration will be determined by the maximum length
            nanim.duration = 0.0
            nanim.ticksPerSecond = pData.mAnimTicksPerSecond.toDouble()
            nanim.numChannels = anim.mAnims.size()
            nanim.channels = ArrayList<AiNodeAnim?>(0).reserve(nanim.numChannels, {AiNodeAnim()})

            for (b in 0 until anim.mAnims.size()) {
                var bone = anim.mAnims[b]
                var nbone = AiNodeAnim()
                nbone.nodeName = if (bone.mBoneName == null) "" else bone.mBoneName!!
                nanim.channels[b] = nbone

                // keyframes are given as combined transformation matrix keys
                if (bone.mTrafoKeys.size() > 0) {
                    nbone.numPositionKeys = bone.mTrafoKeys.size()
                    nbone.positionKeys = ArrayList<AiVectorKey>(0).reserve(nbone.numPositionKeys, {AiVectorKey()})
                    nbone.numRotationKeys = bone.mTrafoKeys.size()
                    nbone.rotationKeys = ArrayList<AiQuatKey>(0).reserve(nbone.numRotationKeys, {AiQuatKey()})
                    nbone.numScalingKeys = bone.mTrafoKeys.size()
                    nbone.scalingKeys = ArrayList<AiVectorKey>(0).reserve(nbone.numScalingKeys, {AiVectorKey()})

                    for (c in 0 until bone.mTrafoKeys.size()) {
                        // deconstruct each matrix into separate position, rotation and scaling
                        val time: Double = bone.mTrafoKeys[c].mTime
                        val trafo = bone.mTrafoKeys[c].mMatrix

                        // extract position
                        val pos = AiVector3D(trafo.d0, trafo.d1, trafo.d2)

                        nbone.positionKeys[c].time = time
                        nbone.positionKeys[c].value = pos

                        // extract scaling
                        val scale = AiVector3D()
                        scale.x = AiVector3D(trafo.a0, trafo.a1, trafo.a2).length()
                        scale.y = AiVector3D(trafo.b0, trafo.b1, trafo.b2).length()
                        scale.z = AiVector3D(trafo.c0, trafo.c1, trafo.c2).length()
                        nbone.scalingKeys[c].time = time
                        nbone.scalingKeys[c].value = scale

                        // reconstruct rotation matrix without scaling
                        val rotmat = AiMatrix3x3(
                                trafo.a0 / scale.x, trafo.b0 / scale.y, trafo.c0 / scale.z,
                                trafo.a1 / scale.x, trafo.b1 / scale.y, trafo.c1 / scale.z,
                                trafo.a2 / scale.x, trafo.b2 / scale.y, trafo.c2 / scale.z)

                        // and convert it into a quaternion
                        nbone.rotationKeys[c].time = time
                        nbone.rotationKeys[c].value = rotmat.toQuat()
                    }

                    // longest lasting key sequence determines duration
                    nanim.duration = Math.max(nanim.duration, bone.mTrafoKeys.back().mTime)
                } else {
                    // separate key sequences for position, rotation, scaling
                    nbone.numPositionKeys = bone.mPosKeys.size()
                    nbone.positionKeys = ArrayList<AiVectorKey>(0).reserve(nbone.numPositionKeys, {AiVectorKey()})
                    for (c in 0 until nbone.numPositionKeys) {
                        var pos = bone.mPosKeys[c].value

                        nbone.positionKeys[c].time = bone.mPosKeys[c].time
                        nbone.positionKeys[c].value = pos
                    }

                    // rotation
                    nbone.numRotationKeys = bone.mRotKeys.size()
                    nbone.rotationKeys = ArrayList<AiQuatKey>(0).reserve(nbone.numRotationKeys, {AiQuatKey()})
                    for (c in 0 until nbone.numRotationKeys) {
                        var rotmat: AiMatrix3x3 = bone.mRotKeys[c].value.toMat3()

                        nbone.rotationKeys[c].time = bone.mRotKeys[c].time
                        nbone.rotationKeys[c].value = rotmat.toQuat()
                        nbone.rotationKeys[c].value.w = nbone.rotationKeys[c].value.w * -1.0f // needs quat inversion
                    }

                    // scaling
                    nbone.numScalingKeys = bone.mScaleKeys.size()
                    nbone.scalingKeys = ArrayList<AiVectorKey>(MutableList<AiVectorKey>(nbone.numScalingKeys, { c -> bone.mScaleKeys[c] }))
                    for (c in 0 until nbone.numScalingKeys) {
                        // nbone.scalingKeys[c] = bone.mScaleKeys[c]
                    }

                    // longest lasting key sequence determines duration
                    if (bone.mPosKeys.size() > 0)
                        nanim.duration = Math.max(nanim.duration, bone.mPosKeys.back().time)
                    if (bone.mRotKeys.size() > 0)
                        nanim.duration = Math.max(nanim.duration, bone.mRotKeys.back().time)
                    if (bone.mScaleKeys.size() > 0)
                        nanim.duration = Math.max(nanim.duration, bone.mScaleKeys.back().time)
                }
            }
        }

        // store all converted animations in the scene
        if (newAnims.size() > 0) {
            pScene.numAnimations = newAnims.size()
            pScene.animations = ArrayList<AiAnimation>(MutableList(pScene.numAnimations, {AiAnimation()}))
            for (a in 0 until newAnims.size())
                pScene.animations[a] = newAnims[a]
        }
    }

    fun ConvertMaterials(pScene: AiScene, pMaterials: MutableList<Material>) {
        // count the non-referrer materials in the array
        var numNewMaterials = 0
        for (a in 0 until pMaterials.size())
            if (!pMaterials[a].mIsReference)
                numNewMaterials++

        // resize the scene's material list to offer enough space for the new materials
        if (numNewMaterials > 0) {
            pScene.materials.reserve(pScene.numMaterials + numNewMaterials)
//            var prevMats = pScene.materials;
//            pScene.materials = ArrayList<AiMaterial>(pScene.numMaterials + numNewMaterials);
//            if( prevMats!=null)
//            {
//                memcpy( pScene.mMaterials, prevMats, pScene.mNumMaterials * sizeof( aiMaterial*));
//                delete [] prevMats;
//            }
        }

        // convert all the materials given in the array
        for (a in 0 until pMaterials.size()) {
            var oldMat = pMaterials[a]
            if (oldMat.mIsReference) {
                // find the material it refers to by name, and store its index
                for (a in 0 until pScene.numMaterials) {
                    var name: String
                    name = pScene.materials[a].name!!
                    if ((name == oldMat.mName)) {
                        oldMat.sceneIndex = a
                        break
                    }
                }

                if (oldMat.sceneIndex == Int.MAX_VALUE) {
                    warn("Could not resolve global material reference \"" + oldMat.mName + "\"")
                    oldMat.sceneIndex = 0
                }

                continue
            }

            var mat = AiMaterial()
            var name: String?
            name = (oldMat.mName)
            mat.name = name

            // Shading model: hardcoded to PHONG, there is no such information in an XFile
            // FIX (aramis): If the specular exponent is 0, use gouraud shading. This is a bugfix
            // for some models in the SDK (e.g. good old tiny.x)
            var shadeMode = if (oldMat.mSpecularExponent == 0.0f)
                AiShadingMode.gouraud else AiShadingMode.phong

            mat.shadingModel = shadeMode
            // material colours
            // Unclear: there's no ambient colour, but emissive. What to put for ambient?
            // Probably nothing at all, let the user select a suitable default.
            mat.color = AiMaterial.Color()
            mat.color!!.emissive = oldMat.mEmissive
            mat.color!!.diffuse = AiColor3D(oldMat.mDiffuse) //Why conversion to 3D?
            mat.color!!.specular = oldMat.mSpecular
            mat.shininess = oldMat.mSpecularExponent


            // texture, if there is one
            if (1 == oldMat.mTextures.size()) {
                var otex = oldMat.mTextures.back()
                if (otex.mName != null && otex.mName!!.length() != 0) {
                    // if there is only one texture assume it contains the diffuse color
                    var tex = (otex.mName)
                    if (otex.mIsNormalMap)
                        mat.textures.add(AiMaterial.Texture(type = AiTexture.Type.normals, file = tex, uvwsrc = 0))
                    else
                        mat.textures.add(AiMaterial.Texture(type = AiTexture.Type.diffuse, file = tex, uvwsrc = 0))
                }
            } else {
                // Otherwise ... try to search for typical strings in the
                // texture's file name like 'bump' or 'diffuse'
                var iHM = 0
                var iNM = 0
                var iDM = 0
                var iSM = 0
                var iAM = 0
                var iEM = 0
                for (b in 0 until oldMat.mTextures.size()) {
                    var otex = oldMat.mTextures[b]
                    var sz = otex.mName
                    if (sz == null || sz.length() == 0) continue


                    // find the file name
                    //const size_t iLen = sz.length();
                    var s = sz.lastIndexOfAny(mutableListOf("\\", "/"))
                    if (-1 == s)
                        s = 0

                    // cut off the file extension
                    var sExt = sz.lastIndexOfAny(mutableListOf("."))
                    if (-1 != sExt) {
                        sz = sz.substring(0, sExt)
                    }

                    // convert to lower case for easier comparison
                    sz = sz.toLowerCase()
//                    for(c in 0..sz.length()-1)
//                    if( isalpha( sz[c]))
//                        sz[c] = tolower( sz[c])


                    // Place texture filename property under the corresponding name
                    var tex = (oldMat.mTextures[b].mName)

                    // bump map
                    if (-1 != sz.indexOf("bump", s) || -1 != sz.indexOf("height", s)) {
                        mat.textures.add(AiMaterial.Texture(file = tex, type = AiTexture.Type.height, uvwsrc = iHM++))
                    } else if (otex.mIsNormalMap || -1 != sz.indexOf("normal", s) || -1 != sz.indexOf("nm", s)) {
                        mat.textures.add(AiMaterial.Texture(file = tex, type = AiTexture.Type.normals, uvwsrc = (iNM++)))
                    } else if (-1 != sz.indexOf("spec", s) || -1 != sz.indexOf("glanz", s)) {
                        mat.textures.add(AiMaterial.Texture(file = tex, type = AiTexture.Type.specular, uvwsrc = (iSM++)))
                    } else if (-1 != sz.indexOf("ambi", s) || -1 != sz.indexOf("env", s)) {
                        mat.textures.add(AiMaterial.Texture(file = tex, type = AiTexture.Type.ambient, uvwsrc = (iAM++)))
                    } else if (-1 != sz.indexOf("emissive", s) || -1 != sz.indexOf("self", s)) {
                        mat.textures.add(AiMaterial.Texture(file = tex, type = AiTexture.Type.emissive, uvwsrc = (iEM++)))
                    } else {
                        // Assume it is a diffuse texture
                        mat.textures.add(AiMaterial.Texture(file = tex, type = AiTexture.Type.diffuse, uvwsrc = (iDM++)))
                    }
                }
            }

            pScene.materials.add(mat)
            oldMat.sceneIndex = pScene.numMaterials
            pScene.numMaterials++
        }
    }

    fun CreateNodes(pScene: AiScene, pParent: AiNode, pNode: Node): AiNode {
//        if(pNode==null)
//            return null;

        // create node
        var node = AiNode()
        node.name = pNode.mName
        node.parent = pParent
        node.transformation = pNode.mTrafoMatrix

        // convert meshes from the source node
        CreateMeshes(pScene, node, pNode.mMeshes)

        // handle childs
        if (pNode.mChildren.size() > 0) {
            node.numChildren = pNode.mChildren.size()
            node.children = ArrayList<AiNode>(MutableList(node.numChildren, {AiNode()}))

            for (a in 0 until pNode.mChildren.size())
                node.children[a] = CreateNodes(pScene, node, pNode.mChildren[a])
        }

        return node
    }

    fun CreateMeshes(pScene: AiScene, pNode: AiNode, pMeshes: MutableList<Mesh>) {
        if (pMeshes.isEmpty()) {
            return
        }

        // create a mesh for each mesh-material combination in the source node
        var meshes: MutableList<AiMesh> = mutableListOf()
        for (a in 0 until pMeshes.size()) {
            var sourceMesh = pMeshes[a]
            // first convert its materials so that we can find them with their index afterwards
            ConvertMaterials(pScene, sourceMesh.mMaterials)

            var numMaterials = Math.max(sourceMesh.mMaterials.size(), 1)
            for (b in 0 until numMaterials) {
                // collect the faces belonging to this material
                var faces: MutableList<Int> = mutableListOf()
                var numVertices = 0
                if (sourceMesh.mFaceMaterials.size() > 0) {
                    // if there is a per-face material defined, select the faces with the corresponding material
                    for (c in 0 until sourceMesh.mFaceMaterials.size()) {
                        if (sourceMesh.mFaceMaterials[c] == b) {
                            faces.push_back(c)
                            numVertices += sourceMesh.mPosFaces[c].mIndices.size()
                        }
                    }
                } else {
                    // if there is no per-face material, place everything into one mesh
                    for (c in 0 until sourceMesh.mPosFaces.size()) {
                        faces.push_back(c)
                        numVertices += sourceMesh.mPosFaces[c].mIndices.size()
                    }
                }

                // no faces/vertices using this material? strange...
                if (numVertices == 0)
                    continue

                // create a submesh using this material
                var mesh = AiMesh()
                meshes.push_back(mesh)

                // find the material in the scene's material list. Either own material
                // or referenced material, it should already have a valid index
                if (sourceMesh.mFaceMaterials.size() > 0) {
                    mesh.materialIndex = (sourceMesh.mMaterials[b].sceneIndex)
                } else {
                    mesh.materialIndex = 0
                }

                // Create properly sized data arrays in the mesh. We store unique vertices per face,
                // as specified
                mesh.numVertices = numVertices
                mesh.vertices = MutableList<AiVector3D>(numVertices, { AiVector3D() })
                mesh.numFaces = faces.size()
                mesh.faces = MutableList<AiFace>(mesh.numFaces, { mutableListOf() })

                // name
                mesh.name = (sourceMesh.mName)

                // normals?
                if (sourceMesh.mNormals.size() > 0)
                    mesh.normals = MutableList<AiVector3D>(numVertices, { AiVector3D() })
                // texture coords
                for (c in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                    if (c < sourceMesh.mTexCoords.size && sourceMesh.mTexCoords[c].size() > 0)
                        mesh.textureCoords.add(MutableList(numVertices, { floatArrayOf(0f, 0f) }))
                }
                // vertex colors
                for (c in 0 until AI_MAX_NUMBER_OF_COLOR_SETS) {
                    if (c < sourceMesh.mColors.size && sourceMesh.mColors[c].size() > 0)
                        mesh.colors.add(MutableList<AiColor4D>(numVertices, { AiColor4D() }))
                }

                // now collect the vertex data of all data streams present in the imported mesh
                var newIndex = 0
                var orgPoints: MutableList<Int> = mutableListOf() // from which original point each new vertex stems
                orgPoints.resize(numVertices, { 0 })

                for (c in 0 until faces.size()) {
                    var f = faces[c] // index of the source face
                    var pf = sourceMesh.mPosFaces[f] // position source face

                    // create face. either triangle or triangle fan depending on the index count
                    var df = mesh.faces[c] // destination face
                    //df.numIndices = pf.mIndices.size(); //3
                    df = MutableList<Int>(pf.mIndices.size(), { 0 }); mesh.faces[c]=df

                    // collect vertex data for indices of this face
                    for (d in 0 until df.size()) {
                        df[d] = newIndex
                        orgPoints[newIndex] = pf.mIndices[d]

                        // Position
                        mesh.vertices[newIndex] = sourceMesh.mPositions[pf.mIndices[d]]
                        // Normal, if present
                        if (mesh.hasNormals)
                            mesh.normals[newIndex] = sourceMesh.mNormals[sourceMesh.mNormFaces[f].mIndices[d]]

                        // texture coord sets
                        for (e in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                            if (e < mesh.textureCoords.size && mesh.hasTextureCoords(e)) {
                                var tex = sourceMesh.mTexCoords[e][pf.mIndices[d]]
                                mesh.textureCoords[e][newIndex] = arrayOf(tex.x, 1.0f - tex.y
                                        //, 0.0f //Not sure why this is in original C++ code, but not in kotlin port.
                                ).toFloatArray()
                            }
                        }
                        // vertex color sets
                        for (e in 0 until AI_MAX_NUMBER_OF_COLOR_SETS)
                            if (e < mesh.colors.size && mesh.hasVertexColors(e))
                                mesh.colors[e][newIndex] = sourceMesh.mColors[e][pf.mIndices[d]]

                        newIndex++
                    }
                }

                // there should be as much new vertices as we calculated before
                assert(newIndex == numVertices)

                // convert all bones of the source mesh which influence vertices in this newly created mesh
                var bones = sourceMesh.mBones
                var newBones: MutableList<AiBone> = mutableListOf()
                for (c in 0 until bones.size()) {
                    var obone = bones[c]
                    // set up a vertex-linear array of the weights for quick searching if a bone influences a vertex
                    var oldWeights = MutableList(sourceMesh.mPositions.size(), {0.0.a})
                    for (d in 0 until obone.mWeights.size())
                        oldWeights[obone.mWeights[d].mVertex] = obone.mWeights[d].mWeight

                    // collect all vertex weights that influence a vertex in the new mesh
                    var newWeights: ArrayList<AiVertexWeight> = arrayListOf()
                    newWeights.reserve(numVertices) //TODO: Unnecessary?
                    for (d in 0 until orgPoints.size()) {
                        // does the new vertex stem from an old vertex which was influenced by this bone?
                        var w = oldWeights[orgPoints[d]]
                        if (w > 0.0.a)
                            newWeights.push_back(AiVertexWeight(d, w))
                    }

                    // if the bone has no weights in the newly created mesh, ignore it
                    if (newWeights.size() == 0)
                        continue

                    // create
                    var nbone = AiBone()
                    newBones.push_back(nbone)
                    // copy name and matrix
                    nbone.name = (if (obone.mName == null) "" else obone.mName!!)
                    nbone.offsetMatrix = obone.mOffsetMatrix
                    nbone.numWeights = newWeights.size()
                    nbone.weights = MutableList<AiVertexWeight>(nbone.numWeights, { AiVertexWeight() })
                    for (d in 0 until newWeights.size())
                        nbone.weights[d] = newWeights[d]

                }

                // store the bones in the mesh
                mesh.numBones = newBones.size()
                if (newBones.size() > 0) {
                    mesh.bones = ArrayList<AiBone>(mesh.numBones)
                    mesh.bones.addAll(newBones)
                }
            }
        }

        // reallocate scene mesh array to be large enough
        var prevArray = pScene.meshes
        pScene.meshes.reserve(pScene.numMeshes + meshes.size(), {AiMesh()})
//        pScene.mMeshes = ArrayList<AiMesh>(pScene.mNumMeshes + meshes.size());
//        if( prevArray!=null)
//        {
//
//            memcpy( pScene.mMeshes, prevArray, pScene.mNumMeshes * sizeof( aiMesh*));
//            delete [] prevArray;
        //}

        // allocate mesh index array in the node
        pNode.numMeshes = meshes.size()
        pNode.meshes = IntArray(pNode.numMeshes)

        // store all meshes in the mesh library of the scene and store their indices in the node
        for (a in 0 until meshes.size()) {
            pScene.meshes[pScene.numMeshes] = meshes[a]
            pNode.meshes[a] = pScene.numMeshes
            pScene.numMeshes++
        }
    }

}