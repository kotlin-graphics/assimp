package main

/**
 * Created by elect on 31/01/2017.
 */

internal fun aiCreateAnimMesh(mesh: AiMesh): AiAnimMesh {

    val animesh = AiAnimMesh(
//            mVertices = mesh.mVertices.toMutableList(),
//            mNormals = mesh.mNormals.toMutableList(),
//            mTangents = mesh.mTangents.toMutableList(),
//            mBitangents = mesh.mBitangents.toMutableList(),
//            mNumVertices = mesh.mNumVertices
    )

    mesh.mColors.forEachIndexed { i, _ ->
        animesh.mColors.add(mutableListOf())
        animesh.mColors[i] = mesh.mColors[i].toMutableList()
    }

    for (a in 0 until mesh.mTextureCoords.size) {
        animesh.mTextureCoords.add(mutableListOf())
        for (b in 0 until mesh.mTextureCoords[a].size) {
            animesh.mTextureCoords[a][b] = mesh.mTextureCoords[a][b].clone()
        }
    }
    return animesh
}
