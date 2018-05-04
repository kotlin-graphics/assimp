package assimp.postProcess

import assimp.AiMesh
import assimp.AiPrimitiveType
import assimp.or
import glm_.glm
import glm_.vec3.Vec3
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlin.math.cos
import kotlin.math.sin

class TriangulateTest : StringSpec() {

    init {

        val count = 1000
        val piProcess = TriangulateProcess()
        val mesh = AiMesh().apply {
            numFaces = count
            faces = MutableList(count) { mutableListOf<Int>() }
            vertices = MutableList(count * 10) { Vec3() }
            primitiveTypes = AiPrimitiveType.POINT or AiPrimitiveType.LINE or AiPrimitiveType.LINE or AiPrimitiveType.POLYGON
        }
        run {
            var m = 0
            var t = 0
            var q = 4
            while (m < count) {
                ++t
                val face = mesh.faces[m]
                var numIndices = t
                if (4 == t) {
                    numIndices = q++
                    t = 0
                    if (10 == q) q = 4
                }
                for (i in 0 until numIndices)
                    face += 0
                for (p in 0 until numIndices) {
                    face[p] = mesh.numVertices

                    // construct fully convex input data in ccw winding, xy plane
                    mesh.vertices[mesh.numVertices++](
                            cos(p * glm.PI2f / numIndices),
                            sin(p * glm.PI2f / numIndices), 0f)
                }
                ++m
            }
        }

        "triangulate process test"        {

            piProcess.triangulateMesh(mesh)

            var m = 0
            var t = 0
            var q = 4
            var max = count
            var idx = 0
            while (m < max) {
                ++t
                val face = mesh.faces[m]
                if (4 == t) {
                    t = 0
                    max += q - 3

                    val ait = BooleanArray(q)

                    var i = 0
                    val tt = q - 2
                    while (i < tt) {
                        val f = mesh.faces[m]
                        f.size shouldBe 3

                        for (qqq in 0 until f.size)
                            ait[f[qqq] - idx] = true
                        ++i
                        ++m
                    }
                    ait.forEach { it shouldBe true }
                    --m
                    idx += q
                    if (++q == 10)
                        q = 4
                } else {
                    face.size shouldBe t

                    for (i in face.indices)
                        face[i] shouldBe idx++
                }
                ++m
            }

            // we should have no valid normal vectors now necause we aren't a pure polygon mesh
            mesh.normals.isEmpty() shouldBe true
        }
    }
}