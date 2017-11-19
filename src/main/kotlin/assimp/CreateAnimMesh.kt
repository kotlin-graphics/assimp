package assimp

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

    mesh.colors.forEachIndexed { i, _ ->
        animesh.colors.add(mutableListOf())
        animesh.colors[i] = mesh.colors[i].toMutableList()
    }

    for (a in 0 until mesh.textureCoords.size) {
        animesh.textureCoords.add(mutableListOf())
        for (b in 0 until mesh.textureCoords[a].size) {
            animesh.textureCoords[a][b] = mesh.textureCoords[a][b].clone()
        }
    }
    return animesh
}
