package assimp

import glm_.vec4.Vec4

object ProcessHelper {

    fun getMeshVFormatUnique(pcMesh: AiMesh): Int {
        //Assimp.ai_assert(pcMesh != 0);

        // FIX: the hash may never be 0. Otherwise a comparison against
        // nullptr could be successful
        var iRet = 1

        // normals
        if (pcMesh.hasNormals)
            iRet = iRet or 0x2
        // tangents and bitangents
        if (pcMesh.hasTangentsAndBitangents)
            iRet = iRet or 0x4

        // texture coordinates
        var p = 0
        while (pcMesh.hasTextureCoords(p)) {
            iRet = iRet or (0x100 shl p)
            //if (3 == pcMesh.mNumUVComponents[p])
            //iRet = iRet or (0x10000 shl p)

            ++p
        }
        // vertex colors
        p = 0
        while (pcMesh.hasVertexColors(p))
            iRet = iRet or (0x1000000 shl p++)
        return iRet
    }

    //(TODO write getColorDifference(pColor1: aiColor4D, pColor2: aiColor4D) (quadratic color difference))
    fun getColorDifference(pColor1: Vec4, pColor2: Vec4): Float {
        return ( (pColor1.r - pColor2.r)*(pColor1.r - pColor2.r) +
                (pColor1.g - pColor2.g)*(pColor1.g - pColor2.g) +
                (pColor1.b - pColor2.b)*(pColor1.b - pColor2.b) +
                (pColor1.a - pColor2.a)*(pColor1.a - pColor2.a) )
    }
}