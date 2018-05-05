package assimp.postProcess

import assimp.*
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class `PretransformVertices Test` : StringSpec() {

    init {
        "PretransformVertices Test 0"{

            val scene = setUp()
            val process = PretransformVertices()

            process.keepHierarchy = false
            process.execute(scene)

            scene.numMaterials shouldBe 5
            scene.numMeshes shouldBe 10 // every second mesh has normals
        }

        "PretransformVertices Test 1"{

            val scene = setUp()
            val process = PretransformVertices()

            process.keepHierarchy = true
            process.execute(scene)

            scene.numMaterials shouldBe 5
            scene.numMeshes shouldBe 49 // see note on mesh 12 above
        }
    }
}

fun setUp(): AiScene {

    val scene = AiScene()

    // add 5 empty materials
    scene.numMaterials = 5
    scene.materials.addAll(MutableList(5) { AiMaterial() })

    // add 25 test meshes
    scene.numMeshes = 25
    scene.meshes.addAll(MutableList(25) { i ->

        AiMesh().apply {
            primitiveTypes = AiPrimitiveType.POINT.i
            numFaces = 10 + i
            faces = MutableList(numFaces) { mutableListOf(it * 3) }
            numVertices = numFaces
            vertices = MutableList(numVertices) { Vec3(i, it, 0) }
            materialIndex = i % 5

            if ((i % 2) != 0) {
                normals = MutableList(numVertices) { Vec3() }
                for (normalIdx in 0 until numVertices) {
                    normals[normalIdx].x = 1f
                    normals[normalIdx].y = 1f
                    normals[normalIdx].z = 1f
                    normals[normalIdx].normalizeAssign()
                }
            }
        }
    })

    // construct some nodes (1+25)
    scene.rootNode = AiNode().apply { name = "Root" }
    addNodes(0, scene.rootNode, 2)

    return scene
}

fun addNodes(num: Int, father: AiNode, depth: Int) {
    father.numChildren = 5
    father.children = MutableList(5) { i ->
        AiNode().apply {
            // spawn two meshes
            numMeshes = 2
            meshes = intArrayOf(
                    num * 5 + i,
                    24 - (num * 5 + i)) // mesh 12 is special ... it references the same mesh twice

            // setup an unique transformation matrix
            transformation.a0 = num * 5f + i + 1
        }
    }
    if (depth > 1)
        for (i in 0..4)
            addNodes(i, father.children[i], depth - 1)
}