package X

import assimp.*
import io.kotlintest.matchers.shouldBe

val x = models + "/X/"
val x_ass = models + "/models-assbin-db/X/"

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
    aiScene1.numMeshes shouldBe aiScene2.numMeshes
    for(i in 0 until aiScene1.meshes.size) {
        compareMeshes(aiScene1.meshes[i], aiScene2.meshes[i])
    }
}

fun compareMeshes(aiMesh1 : AiMesh, aiMesh2 : AiMesh) {
    aiMesh1.numFaces shouldBe aiMesh2.numFaces
    for(i in 0 until aiMesh1.faces.size) {
        compareFaces(aiMesh1.faces[i], aiMesh2.faces[i])
    }

    aiMesh1.numVertices shouldBe aiMesh2.numVertices
    for(i in 0 until aiMesh1.vertices.size) {
        compareVertices(aiMesh1.vertices[i], aiMesh2.vertices[i])
    }
}

fun compareVertices(vec1: AiVector3D, vec2 : AiVector3D) {
    vec1.x shouldBe vec2.x
    vec1.y shouldBe vec2.y
    vec1.z shouldBe vec2.z
}

fun compareFaces(aiFace1 : AiFace, aiFace2 : AiFace) {
    aiFace1.size shouldBe aiFace2.size
    for(i in 0 until aiFace1.size) {
        aiFace1[i] shouldBe aiFace2[i]
    }
}