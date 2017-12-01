package assimp.format.X

import assimp.ai_real
import assimp.AiMatrix4x4
import assimp.AiVector3D
import assimp.AiVector2D
import assimp.AiColor4D
import assimp.AiColor3D
import assimp.AiQuatKey
import assimp.AiVectorKey
import assimp.AI_MAX_NUMBER_OF_TEXTURECOORDS
import assimp.AI_MAX_NUMBER_OF_COLOR_SETS

class XFileParser() {

    var P : Pointer<Char> = Pointer<Char>(arrayOf())
    /**Should be 1 more than Java length of array, i.e. should point to lastIndex+1, because null-termination does not exist in Java*/
    var End : Pointer<Char> = Pointer<Char>(arrayOf())

    constructor(pBuffer: Pointer<Char>) : this() {
        setup(pBuffer)
        fullRoutine(pBuffer)
    }

    fun fullRoutine(pBuffer: Pointer<Char>) {
        ParseFile()

        // filter the imported hierarchy for some degenerated cases
        if (mScene.mRootNode != null) {
            FilterHierarchy(mScene.mRootNode!!)
        }
    }

    // set up memory pointers



    var mMajorVersion: Int = 0
    var mMinorVersion: Int = 0

    var mIsBinaryFormat: Boolean = false

    var compressed: Boolean = false

    var mBinaryFloatSize: Int = 0
    var mBinaryNumCount: Int = 0

    var mLineNumber: Int = 0 //=0?

    var mScene: Scene = Scene()

    fun setup(pBuffer: Pointer<Char>) {
        P = pBuffer + 0
        End = P + pBuffer.lastIndex + 1

        // check header
        if (!(P[0..3].equals("xof ")))
            ThrowException("Header mismatch, file is not an XFile.")

        // read version. It comes in a four byte format such as "0302"
        mMajorVersion = ((P[4] - 48).toInt() * 10) + (P[5] - 48).toInt()
        mMinorVersion = ((P[6] - 48).toInt() * 10) + (P[7] - 48).toInt()

        // txt - pure ASCII text format
        if (strncmp(P + 8, "txt ", 4) == 0)
            mIsBinaryFormat = false

        // bin - Binary format
        else if (strncmp(P + 8, "bin ", 4) == 0)
            mIsBinaryFormat = true

        // tzip - Inflate compressed text format
        else if (strncmp(P + 8, "tzip", 4) == 0) {
            mIsBinaryFormat = false
            compressed = true
        }
        // bzip - Inflate compressed binary format
        else if (strncmp(P + 8, "bzip", 4) == 0) {
            mIsBinaryFormat = true
            compressed = true
        } else ThrowException("Unsupported xfile format '" + (P + 8)[0..3] + "'")

        // float size
        mBinaryFloatSize = ((P[12] - 48).toInt() * 1000)+((P[13] - 48).toInt() * 100)        +((P[14] - 48).toInt() * 10)        +((P[15] - 48).toInt())

        if (mBinaryFloatSize != 32 && mBinaryFloatSize != 64)
            ThrowException("Unknown float size " + mBinaryFloatSize + " specified in xfile header.")

        // The x format specifies size in bits, but we work in bytes
        mBinaryFloatSize /= 8

        P += 16

        if (compressed || mIsBinaryFormat) {
            ThrowException("Binary format is currently unsupported")
        } else {
            ReadUntilEndOfLine()
        }

        mScene = Scene()
    }

    fun ParseFile() {
        var running: Boolean = true
        while (running) {
            // read name of next object
            var objectName: String = GetNextToken()
            if (objectName.length() == 0)
                break

            //println("objectName: " + objectName)

            // parse specific object
            if (objectName.equals("template"))
                ParseDataObjectTemplate()
            else if (objectName == "Frame")
                ParseDataObjectFrame(null)
            else if (objectName == "Mesh") {
                // some meshes have no frames at all
                var mesh = Mesh()
                ParseDataObjectMesh(mesh)
                mScene.mGlobalMeshes.push_back(mesh)
            } else if (objectName == "AnimTicksPerSecond")
                ParseDataObjectAnimTicksPerSecond()
            else if (objectName == "AnimationSet")
                ParseDataObjectAnimationSet()
            else if (objectName == "Material") {
                // Material outside of a mesh or node
                var material = Material()
                ParseDataObjectMaterial(material)
                mScene.mGlobalMaterials.push_back(material)
            } else if (objectName == "}") {
                // whatever?
                warn("} found in dataObject")
            } else {
                // unknown format
                warn("Unknown data object in animation of .x file")
                ParseUnknownDataObject()
            }
        }
    }


    fun ParseDataObjectTemplate() {
        // parse a template data object. Currently not stored.
        var name: StringBuilder = StringBuilder()
        readHeadOfDataObject(name)

        // read GUID
        var guid: String = GetNextToken()

        // read and ignore data members
        var running = true
        while (running) {
            var s: String = GetNextToken()

            if (s == "}")
                break

            if (s.length() == 0)
                ThrowException("Unexpected end of file reached while parsing template definition")
        }
    }

    fun ParseDataObjectFrame(pParent: Node?) {
        var name: String
        var namebuilder: StringBuilder = StringBuilder()
        readHeadOfDataObject(namebuilder); name = namebuilder.toString()

        var node: Node = Node(mParent = pParent)
        node.mName = name
        if (pParent != null) {
            pParent.mChildren.push_back(node)
        } else {
            if (mScene.mRootNode != null) {
                if (!mScene.mRootNode!!.mName.equals("\$dummy_root", false)) {
                    var exroot: Node = mScene.mRootNode!!
                    mScene.mRootNode = Node(mParent = null)
                    mScene.mRootNode!!.mName = "\$dummy_root"
                    mScene.mRootNode!!.mChildren.push_back(exroot)
                    exroot.mParent = mScene.mRootNode
                }
                mScene.mRootNode!!.mChildren.push_back(node)
                node.mParent = mScene.mRootNode
            } else {
                mScene.mRootNode = node
            }
        }

        var running: Boolean = true
        while (running) {
            var objectName: String = GetNextToken()
            if (objectName.length == 0)
                throw Error("Unexpected end of file reached while parsing frame")

            if (objectName.equals("}"))
                break // frame finished
            else if (objectName.equals("Frame"))
                ParseDataObjectFrame(node) // child frame
            else if (objectName.equals("FrameTransformMatrix"))
                ParseDataObjectTransformationMatrix(node.mTrafoMatrix)
            else if (objectName.equals("Mesh")) {
                var mesh: Mesh = Mesh(name)
                node.mMeshes.push_back(mesh)
                ParseDataObjectMesh(mesh)
            } else {
                //TODO: ("Unknown data object in frame in x file");
                ParseUnknownDataObject()
            }
        }
    }


    fun ParseDataObjectTransformationMatrix(pMatrix: AiMatrix4x4) {
        readHeadOfDataObject()

        // read its components
        pMatrix.a0 = ReadFloat(); pMatrix.a1 = ReadFloat()
        pMatrix.a2 = ReadFloat(); pMatrix.a3 = ReadFloat()
        pMatrix.b0 = ReadFloat(); pMatrix.b1 = ReadFloat()
        pMatrix.b2 = ReadFloat(); pMatrix.b3 = ReadFloat()
        pMatrix.c0 = ReadFloat(); pMatrix.c1 = ReadFloat()
        pMatrix.c2 = ReadFloat(); pMatrix.c3 = ReadFloat()
        pMatrix.d0 = ReadFloat(); pMatrix.d1 = ReadFloat()
        pMatrix.d2 = ReadFloat(); pMatrix.d3 = ReadFloat()

        // trailing symbols
        CheckForSemicolon()
        CheckForClosingBrace()
    }

    fun ParseDataObjectMesh(pMesh: Mesh) {
        var name: String
        var builder: StringBuilder = StringBuilder()
        readHeadOfDataObject(builder)

        var numVertices: Int = ReadInt()
        pMesh.mPositions.resize(numVertices, { AiVector3D() })

        for (a in IntRange(0, numVertices - 1))
            pMesh.mPositions.set(a, ReadVector3())

        var numPosFaces: Int = ReadInt()
        pMesh.mPosFaces.resize(numPosFaces, { Face() })
        for (a in 0 until numPosFaces) {
            var numIndices: Int = ReadInt()
            var face: Face = pMesh.mPosFaces.get(a)
            for (b in IntRange(0, numIndices - 1)) {
                face.mIndices.push_back(ReadInt())
            }
            TestForSeparator()
        }

        var running = true
        while (running) {
            var objectName: String = GetNextToken()

            if (objectName.length == 0)
                throw Error("Unexpected end of file while parsing mesh structure")
            else if (objectName.equals("}"))
                break // mesh finished
            else if (objectName.equals("MeshNormals"))
                ParseDataObjectMeshNormals(pMesh)
            else if (objectName.equals("MeshTextureCoords"))
                ParseDataObjectMeshTextureCoords(pMesh)
            else if (objectName.equals("MeshVertexColors"))
                ParseDataObjectMeshVertexColors(pMesh)
            else if (objectName.equals("MeshMaterialList"))
                ParseDataObjectMeshMaterialList(pMesh)
            else if (objectName.equals("VertexDuplicationIndices"))
                ParseUnknownDataObject() // we'll ignore vertex duplication indices
            else if (objectName.equals("XSkinMeshHeader"))
                ParseDataObjectSkinMeshHeader(pMesh)
            else if (objectName.equals("SkinWeights"))
                ParseDataObjectSkinWeights(pMesh)
            else {
                //TODO ("Unknown data object in mesh in x file");
                ParseUnknownDataObject()
            }
        }
    }

    fun ParseDataObjectSkinWeights(pMesh: Mesh) {
        readHeadOfDataObject()

        var transformNodeName = StringBuilder()
        GetNextTokenAsString(transformNodeName)

        pMesh.mBones.push_back(Bone())
        var bone = pMesh.mBones.back()
        bone.mName = transformNodeName.toString()

        // read vertex weights
        var numWeights = ReadInt()
        bone.mWeights.reserve(numWeights, { BoneWeight() })

        for (a in 0 until numWeights) {
            var weight = BoneWeight()
            weight.mVertex = ReadInt()
            bone.mWeights[a] = (weight)
        }

        // read vertex weights
        for (a in 0 until numWeights)
            bone.mWeights[a].mWeight = ReadFloat()

        // read matrix offset
        bone.mOffsetMatrix.a0 = ReadFloat(); bone.mOffsetMatrix.a1 = ReadFloat()
        bone.mOffsetMatrix.a2 = ReadFloat(); bone.mOffsetMatrix.a3 = ReadFloat()
        bone.mOffsetMatrix.b0 = ReadFloat(); bone.mOffsetMatrix.b1 = ReadFloat()
        bone.mOffsetMatrix.b2 = ReadFloat(); bone.mOffsetMatrix.b3 = ReadFloat()
        bone.mOffsetMatrix.c0 = ReadFloat(); bone.mOffsetMatrix.c1 = ReadFloat()
        bone.mOffsetMatrix.c2 = ReadFloat(); bone.mOffsetMatrix.c3 = ReadFloat()
        bone.mOffsetMatrix.d0 = ReadFloat(); bone.mOffsetMatrix.d1 = ReadFloat()
        bone.mOffsetMatrix.d2 = ReadFloat(); bone.mOffsetMatrix.d3 = ReadFloat()

        CheckForSemicolon()
        CheckForClosingBrace()
    }

    fun ParseDataObjectSkinMeshHeader(pMesh: Mesh) {
        readHeadOfDataObject()

        /*var maxSkinWeightsPerVertex =*/ ReadInt()
        /*var maxSkinWeightsPerFace =*/ ReadInt()
        /*var numBonesInMesh = */ReadInt()

        CheckForClosingBrace()
    }

    fun ParseDataObjectMeshNormals(pMesh: Mesh) {
        readHeadOfDataObject()

        // read count
        var numNormals = ReadInt()
        pMesh.mNormals.resize(numNormals, { AiVector3D() })

        // read normal vectors
        for (a in 0 until numNormals)
            pMesh.mNormals[a] = ReadVector3()

        // read normal indices
        var numFaces = ReadInt()
        if (numFaces != pMesh.mPosFaces.size())
            ThrowException("Normal face count does not match vertex face count.")

        for (a in 0 until numFaces) {
            var numIndices: Int = ReadInt()
            pMesh.mNormFaces.push_back(Face())
            var face = pMesh.mNormFaces.last()

            for (b in 0 until numIndices)
                face.mIndices.push_back(ReadInt())

            TestForSeparator()
        }

        CheckForClosingBrace()
    }

    fun ParseDataObjectMeshTextureCoords(pMesh: Mesh) {
        readHeadOfDataObject()
        if (pMesh.mNumTextures + 1 > AI_MAX_NUMBER_OF_TEXTURECOORDS)
            ThrowException("Too many sets of texture coordinates")

        pMesh.mTexCoords.push_back(MutableList<AiVector2D>(0, { AiVector2D() }))
        var coords: MutableList<AiVector2D> = pMesh.mTexCoords[pMesh.mNumTextures++]

        var numCoords = ReadInt()
        if (numCoords != pMesh.mPositions.size())
            ThrowException("Texture coord count does not match vertex count")

        coords.resize(numCoords, { AiVector2D() })
        for (a in 0 until numCoords)
            coords[a] = ReadVector2()

        CheckForClosingBrace()
    }

    fun ParseDataObjectMeshVertexColors(pMesh: Mesh) {
        readHeadOfDataObject()
        if (pMesh.mNumColorSets + 1 > AI_MAX_NUMBER_OF_COLOR_SETS)
            ThrowException("Too many colorsets")
        pMesh.mColors.push_back(MutableList<AiColor4D>(0, { AiColor4D() }))
        var colors = pMesh.mColors[pMesh.mNumColorSets++]

        var numColors = ReadInt()
        if (numColors != pMesh.mPositions.size())
            ThrowException("Vertex color count does not match vertex count")

        colors.resize(numColors, { AiColor4D(0, 0, 0, 1) })
        for (a in 0 until numColors) {
            var index = ReadInt()
            if (index >= pMesh.mPositions.size())
                ThrowException("Vertex color index out of bounds")

            colors[index] = ReadRGBA()
            // HACK: (thom) Maxon Cinema XPort plugin puts a third separator here, kwxPort puts a comma.
            // Ignore gracefully.
            if (!mIsBinaryFormat) {
                FindNextNoneWhiteSpace()
                if (P.value == ';' || P.value == ',')
                    P++
            }
        }

        CheckForClosingBrace()
    }

    fun ParseDataObjectMeshMaterialList(pMesh: Mesh) {
        readHeadOfDataObject()

        // read material count
        /*var numMaterials =*/ ReadInt()
        // read non triangulated face material index count
        var numMatIndices = ReadInt()

        // some models have a material index count of 1... to be able to read them we
        // replicate this single material index on every face
        if (numMatIndices != pMesh.mPosFaces.size() && numMatIndices != 1)
            ThrowException("Per-Face material index count does not match face count.")

        // read per-face material indices
        for (a in 0 until numMatIndices)
            pMesh.mFaceMaterials.push_back(ReadInt())

        // in version 03.02, the face indices end with two semicolons.
        // commented out version check, as version 03.03 exported from blender also has 2 semicolons
        if (!mIsBinaryFormat) // && MajorVersion == 3 && MinorVersion <= 2)
        {
            if (P < End && P.value == ';')
                ++P
        }

        // if there was only a single material index, replicate it on all faces
        while (pMesh.mFaceMaterials.size() < pMesh.mPosFaces.size())
            pMesh.mFaceMaterials.push_back(pMesh.mFaceMaterials.front())

        // read following data objects
        var running = true
        while (running) {
            var objectName = GetNextToken()
            if (objectName.size() == 0)
                ThrowException("Unexpected end of file while parsing mesh material list.")
            else if (objectName == "}")
                break // material list finished
            else if (objectName == "{") {
                // template materials
                var matName = GetNextToken()
                var material: Material = Material()
                material.mIsReference = true
                material.mName = matName
                pMesh.mMaterials.push_back(material)

                CheckForClosingBrace() // skip }
            } else if (objectName == "Material") {
                pMesh.mMaterials.push_back(Material())
                ParseDataObjectMaterial(pMesh.mMaterials.back())
            } else if (objectName == ";") {
                // ignore
            } else {
                warn("Unknown data object in material list in x file")
                ParseUnknownDataObject()
            }
        }
    }

    fun ParseDataObjectMaterial(pMaterial: Material) {
        var matName: StringBuilder = StringBuilder()
        readHeadOfDataObject(matName)
        if (matName.isEmpty())
            matName.append("material" + mLineNumber)
        pMaterial.mName = matName.toString()
        pMaterial.mIsReference = false

        // read material values
        pMaterial.mDiffuse = ReadRGBA()
        pMaterial.mSpecularExponent = ReadFloat()
        pMaterial.mSpecular = ReadRGB()
        pMaterial.mEmissive = ReadRGB()

        // read other data objects
        var running = true
        while (running) {
            var objectName = GetNextToken()
            if (objectName.size() == 0)
                ThrowException("Unexpected end of file while parsing mesh material")
            else if (objectName == "}")
                break // material finished
            else if (objectName == "TextureFilename" || objectName == "TextureFileName") {
                // some exporters write "TextureFileName" instead.
                var texname: StringBuilder = StringBuilder()
                ParseDataObjectTextureFilename(texname)
                pMaterial.mTextures.push_back(TexEntry(texname.toString()))
            } else if (objectName == "NormalmapFilename" || objectName == "NormalmapFileName") {
                // one exporter writes out the normal map in a separate filename tag
                var texname: StringBuilder = StringBuilder()
                ParseDataObjectTextureFilename(texname)
                pMaterial.mTextures.push_back(TexEntry(texname.toString(), true))
            } else {
                warn("Unknown data object in material in x file")
                ParseUnknownDataObject()
            }
        }
    }

    fun ParseDataObjectAnimTicksPerSecond() {
        readHeadOfDataObject()
        mScene.mAnimTicksPerSecond = ReadInt()
        CheckForClosingBrace()
    }

    fun ParseDataObjectAnimationSet() {
        var animName: StringBuilder = StringBuilder()
        readHeadOfDataObject(animName)

        var anim = Animation()
        mScene.mAnims.push_back(anim)
        anim.mName = animName.toString()

        var running = true
        while (running) {if(P.datas.size==343) {println(P.pointer); if(P.pointer==342){println(P[0..P.lastIndex])}}
            var objectName = GetNextToken()
            if (objectName.length() == 0)
                ThrowException("ParseDataObjectAnimationSet: Unexpected end of file while parsing animation set: pointer =" + P.pointer + " total length =" + P.datas.size)
            else if (objectName == "}")
                break // animation set finished
            else if (objectName == "Animation")
                ParseDataObjectAnimation(anim)
            else {
                warn("ParseDataObjectAnimationSet: Unknown data object in animation set in x file")
                ParseUnknownDataObject()
            }
        }
    }

    fun ParseDataObjectAnimation(pAnim: Animation) {
        readHeadOfDataObject()
        var banim = AnimBone()
        pAnim.mAnims.push_back(banim)

        var running = true
        while (running) {
            var objectName = GetNextToken()

            if (objectName.length() == 0)
                ThrowException("ParseDataObjectAnimation: Unexpected end of file while parsing animation.")
            else if (objectName == "}")
                break // animation finished
            else if (objectName == "AnimationKey")
                ParseDataObjectAnimationKey(banim)
            else if (objectName == "AnimationOptions")
                ParseUnknownDataObject() // not interested
            else if (objectName == "{") {
                // read frame name
                banim.mBoneName = GetNextToken()
                CheckForClosingBrace()
            } else {
                warn("ParseDataObjectAnimation: Unknown data object in animation in x file")
                ParseUnknownDataObject()
            }
        }
    }

    fun ParseDataObjectAnimationKey(pAnimBone: AnimBone) {
        readHeadOfDataObject()

        // read key type
        var keyType = ReadInt()

        // read number of keys
        var numKeys = ReadInt()

        for (a in 0 until numKeys) {
            // read time
            var time = ReadInt()

            // read keys
            when (keyType) {
                0 -> // rotation quaternion
                {
                    // read count
                    if (ReadInt() != 4)
                        ThrowException("Invalid number of arguments for quaternion key in animation")

                    var key = AiQuatKey()
                    key.time = time.toDouble()
                    key.value.w = ReadFloat()
                    key.value.x = ReadFloat()
                    key.value.y = ReadFloat()
                    key.value.z = ReadFloat()
                    pAnimBone.mRotKeys.push_back(key)

                    CheckForSemicolon()
                }

                1, // scale vector
                2 ->  // position vector
                {
                    // read count
                    if (ReadInt() != 3)
                        ThrowException("Invalid number of arguments for vector key in animation")

                    var key = AiVectorKey()
                    key.time = time.toDouble()
                    key.value = ReadVector3()

                    if (keyType == 2)
                        pAnimBone.mPosKeys.push_back(key)
                    else
                        pAnimBone.mScaleKeys.push_back(key)

                }

                3, // combined transformation matrix
                4 -> // denoted both as 3 or as 4
                {
                    // read count
                    if (ReadInt() != 16)
                        ThrowException("Invalid number of arguments for matrix key in animation")

                    // read matrix
                    var key = MatrixKey()
                    key.mTime = time.toDouble()
                    key.mMatrix.a0 = ReadFloat(); key.mMatrix.a1 = ReadFloat()
                    key.mMatrix.a2 = ReadFloat(); key.mMatrix.a3 = ReadFloat()
                    key.mMatrix.b0 = ReadFloat(); key.mMatrix.b1 = ReadFloat()
                    key.mMatrix.b2 = ReadFloat(); key.mMatrix.b3 = ReadFloat()
                    key.mMatrix.c0 = ReadFloat(); key.mMatrix.c1 = ReadFloat()
                    key.mMatrix.c2 = ReadFloat(); key.mMatrix.c2 = ReadFloat()
                    key.mMatrix.d0 = ReadFloat(); key.mMatrix.d1 = ReadFloat()
                    key.mMatrix.d2 = ReadFloat(); key.mMatrix.d3 = ReadFloat()
                    pAnimBone.mTrafoKeys.push_back(key)

                    CheckForSemicolon()
                }

                else -> {
                    ThrowException("Unknown key type " + keyType + " in animation.")
                }
            } // end switch

            // key separator
            CheckForSeparator()
        }

        CheckForClosingBrace()
    }

    fun ParseDataObjectTextureFilename(pName: StringBuilder) {
        readHeadOfDataObject()
        GetNextTokenAsString(pName)
        CheckForClosingBrace()

        // FIX: some files (e.g. AnimationTest.x) have "" as texture file name
        if (pName.length() == 0) {
            warn("Length of texture file name is zero. Skipping this texture.")
        }

        // some exporters write double backslash paths out. We simply replace them if we find them
        while (pName.indexOf("\\\\") != -1)
            pName.replace(pName.indexOf("\\\\"), pName.indexOf("\\\\")+2, "\\")
    }

    fun ParseUnknownDataObject() {
        // find opening delimiter
        var running = true
        while (running) {
            var t = GetNextToken()
            if (t.length() == 0)
                ThrowException("Unexpected end of file while parsing unknown segment: " + t)

            if (t.equals("{"))
                break
        }

        var counter = 1

        // parse until closing delimiter
        while (counter > 0) {
            var t = GetNextToken()

            if (t.length() == 0)
                ThrowException("Unexpected end of file while parsing unknown segment.")

            if (t == "{")
                ++counter
            else
                if (t == "}")
                    --counter
        }
    }

    fun CheckForClosingBrace() {

        if (GetNextToken() != "}")
            ThrowException("Closing brace expected.")
    }

    fun CheckForSemicolon() {
        if (mIsBinaryFormat)
            return

        if (GetNextToken() != ";")
            ThrowException("Semicolon expected.")
    }

    fun CheckForSeparator() {
        if (mIsBinaryFormat)
            return

        var token = GetNextToken()
        if (token != "," && token != ";")
            ThrowException("Separator character (';' or ',') expected: " + token)
    }

    fun TestForSeparator() {
        if (mIsBinaryFormat)
            return

        FindNextNoneWhiteSpace()
        if (P >= End)
            return

        // test and skip
        if (P.value == ';' || P.value == ',')
            P++
    }

    fun readHeadOfDataObject(poName: StringBuilder = StringBuilder()) {
        var nameOrBrace = GetNextToken()
        if (nameOrBrace != "{") {
            //if( poName)
            poName.append(nameOrBrace)

            if (GetNextToken() != "{")
                ThrowException("Opening brace expected.")
        }
    }

    fun GetNextToken(): String {
        var s: StringBuilder = StringBuilder()
        if (mIsBinaryFormat) {

        } else {
            FindNextNoneWhiteSpace()
            if (P >= End)
                return s.toString()

            while ((P < End) && !isspace(P.value)) {
                // either keep token delimiters when already holding a token, or return if first valid char
                if (P.value == ';' || P.value == '}' || P.value == '{' || P.value == ',') {
                    if (s.length == 0) {
                        s.append(P.value); P++
                    }
                    break // stop for delimiter
                }
                s.append(P.value); P++
            }
        }
        return s.toString()
    }

    fun FindNextNoneWhiteSpace() {
        if (mIsBinaryFormat)
            return

        var running = true
        while (running) {
            while (P < End && isspace(P.value)) {
                if (P.value == '\n') {
                    mLineNumber++
                }
                ++P
            }

            if (P >= End)
                return

            // check if this is a comment
            if ((P[0] == '/' && P[1] == '/') || P[0] == '#')
                ReadUntilEndOfLine()
            else
                break
        }
    }

    fun GetNextTokenAsString(poString: StringBuilder) {
        if (mIsBinaryFormat) {
            poString.append(GetNextToken())
            return
        }

        FindNextNoneWhiteSpace()
        if (P >= End)
            ThrowException("Unexpected end of file while parsing string")

        if (P.value != '"')
            ThrowException("Expected quotation mark.")
        ++P

        while (P < End && P.value != '"') {
            poString.append(P.value); P++ //Is stuf bugged now?
        }

        if (P >= End - 1)
            ThrowException("Unexpected end of file while parsing string")

        if (P[1] != ';' || P[0] != '"')
            ThrowException("Expected quotation mark and semicolon at the end of a string.")
        P += 2
    }

    fun ReadUntilEndOfLine() {
        if (mIsBinaryFormat)
            return

        while (P < End) {
            if (P.value == '\n' || P.value == '\r') {
                ++P; mLineNumber++
                return
            }

            ++P
        }
    }

    fun ReadInt(): Int {
        if (mIsBinaryFormat) {
//        if( mBinaryNumCount == 0 && End - P >= 2)
//        {
//            unsigned short tmp = ReadBinWord(); // 0x06 or 0x03
//            if( tmp == 0x06 && End - P >= 4) // array of ints follows
//                mBinaryNumCount = ReadBinDWord();
//            else // single int follows
//                mBinaryNumCount = 1;
//        }
//
//        --mBinaryNumCount;
//        if ( End - P >= 4) {
//            return ReadBinDWord();
//        } else {
//            P = End;
//            return 0;
//        }
            throw RuntimeException("BinaryFormat is unsupported")
        } else {
            FindNextNoneWhiteSpace()

            // TODO: consider using strtol10 instead???

            // check preceding minus sign
            var isNegative = false
            if (P.value == '-') {
                isNegative = true
                P++
            }

            // at least one digit expected
            if (!isdigit(P.value))
                ThrowException("Number expected.")

            // read digits
            var number = 0
            while (P < End) {
                if (!isdigit(P.value))
                    break
                number = number * 10 + (P.value - 48).toInt()
                P++
            }

            CheckForSeparator()
            return if (isNegative) (-(number)) else number
        }
    }

    fun ReadFloat(): ai_real {
        if (mIsBinaryFormat) {
//			if (mBinaryNumCount == 0 && End - P >= 2) {
//				var tmp = ReadBinWord(); // 0x07 or 0x42
//				if (tmp == 0x07 && End - P >= 4) // array of floats following
//					mBinaryNumCount = ReadBinDWord();
//				else // single float following
//					mBinaryNumCount = 1;
//			}
//
//			--mBinaryNumCount;
//			if (mBinaryFloatSize == 8) {
//				if (End - P >= 8) {
//					ai_real result =(ai_real)(*(double *) P);
//					P += 8;
//					return result;
//				} else {
//					P = End;
//					return 0;
//				}
//			} else {
//				if (End - P >= 4) {
//					ai_real result = * (ai_real *) P;
//					P += 4;
//					return result;
//				} else {
//					P = End;
//					return 0;
//				}
//			}
        }

        // text version
        FindNextNoneWhiteSpace()
        // check for various special strings to allow reading files from faulty exporters
        // I mean you, Blender!
        // Reading is safe because of the terminating zero
        if (strncmp(P, "-1.#IND00", 9) == 0 || strncmp(P, "1.#IND00", 8) == 0) {
            P += 9
            CheckForSeparator()
            return 0.0.a
        } else
            if (strncmp(P, "1.#QNAN0", 8) == 0) {
                P += 8
                CheckForSeparator()
                return 0.0.a
            }

        var p_result: Pointer<ai_real> = Pointer<ai_real>(arrayOf(0.0.a))
        P = fast_atoreal_move(P, p_result)

        CheckForSeparator()

        return p_result.value
    }

    fun ReadVector2(): AiVector2D {
        var vector = AiVector2D()
        vector.x = ReadFloat()
        vector.y = ReadFloat()
        TestForSeparator()

        return vector
    }

    fun ReadVector3(): AiVector3D {
        var vector: AiVector3D = AiVector3D()
        vector.x = ReadFloat()
        vector.y = ReadFloat()
        vector.z = ReadFloat()
        TestForSeparator()

        return vector
    }

    fun ReadRGBA(): AiColor4D {
        var color = AiColor4D()
        color.r = ReadFloat()
        color.g = ReadFloat()
        color.b = ReadFloat()
        color.a = ReadFloat()
        TestForSeparator()

        return color
    }

    fun ReadRGB(): AiColor3D {
        var color = AiColor3D()
        color.r = ReadFloat()
        color.g = ReadFloat()
        color.b = ReadFloat()
        TestForSeparator()

        return color
    }

    fun ThrowException(s: String) {
        throw RuntimeException("Line" + mLineNumber + ": " + s)
    }

    fun FilterHierarchy(pNode: Node) {
        // if the node has just a single unnamed child containing a mesh, remove
        // the anonymous node between. The 3DSMax kwXport plugin seems to produce this
        // mess in some cases
        if (pNode.mChildren.size() == 1 && pNode.mMeshes.isEmpty()) {
            var child = pNode.mChildren.front()
            if (child.mName.length() == 0 && child.mMeshes.size() > 0) {
                // transfer its meshes to us
                for (a in 0 until child.mMeshes.size())
                    pNode.mMeshes.push_back(child.mMeshes[a])
                child.mMeshes.clear()

                // transfer the transform as well
                pNode.mTrafoMatrix = pNode.mTrafoMatrix * child.mTrafoMatrix

                // then kill it
                //delete child;
                pNode.mChildren.clear()
            }
        }

        // recurse
        for (a in 0 until pNode.mChildren.size())
            FilterHierarchy(pNode.mChildren[a])
    }

}



