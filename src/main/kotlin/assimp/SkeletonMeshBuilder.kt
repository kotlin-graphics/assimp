/** Helper class to construct a dummy mesh for file formats containing only motion data */

/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above
copyright notice, this list of conditions and the
following disclaimer.

* Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the
following disclaimer in the documentation and/or other
materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
contributors may be used to endorse or promote products
derived from this software without specific prior
written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/

package assimp

import glm_.glm

/** SkeletonMeshBuilder
 *  Declares SkeletonMeshBuilder, a tiny utility to build dummy meshes for animation skeletons. */


/**
 * This little helper class constructs a dummy mesh for a given scene
 * the resembles the node hierarchy. This is useful for file formats
 * that don't carry any mesh data but only animation data.
 */
class SkeletonMeshBuilder
/** The constructor processes the given scene and adds a mesh there.
 *
 * Does nothing if the scene already has mesh data.
 * @param scene The scene for which a skeleton mesh should be constructed.
 * @param root The node to start with. NULL is the scene root
 * @param knobsOnly Set this to true if you don't want the connectors
 *   between the knobs representing the nodes.
 */
constructor(scene: AiScene, root: AiNode? = null, val knobsOnly: Boolean = false) {

    /** space to assemble the mesh data: points */
    val vertices = ArrayList<AiVector3D>()
    val faces = ArrayList<Face>()
    /** bones */
    val bones = ArrayList<AiBone>()

    init {
        // nothing to do if there's mesh data already present at the scene
        if (scene.numMeshes == 0 && scene.rootNode != null) {
            val root = root ?: scene.rootNode
            // build some faces around each node
            createGeometry(root)
            // create a mesh to hold all the generated faces
            scene.numMeshes = 1
            scene.meshes = arrayListOf(createMesh())
            // and install it at the root node
            root.numMeshes = 1
            root.meshes = IntArray(1)
            // create a dummy material for the mesh
            if (scene.numMaterials == 0){
                scene.numMaterials = 1
                scene.materials = arrayListOf(createMaterial())
            }
        }
    }

    /** Recursively builds a simple mesh representation for the given node and also creates a joint for the node that
     *  affects this part of the mesh.
     * @param pNode The node to build geometry for. */
    fun createGeometry(node: AiNode) {
        // add a joint entry for the node.
        val vertexStartIndex = vertices.size
        // now build the geometry.
        if (node.numChildren > 0 && !knobsOnly)
        // If the node has children, we build little pointers to each of them
            for (a in 0 until node.numChildren) {
                // find a suitable coordinate system
                val childTransform = node.children[a].transformation
                val childpos = AiVector3D(childTransform.a3, childTransform.b3, childTransform.c3)
                val distanceToChild = childpos.length()
                if (distanceToChild < 0.0001f) continue
                val up = AiVector3D(childpos).normalizeAssign()

                val orth = AiVector3D(1f, 0f, 0f)
                if (glm.abs(orth dot up) > 0.99) orth.put(0f, 1f, 0f)

                val front = (up cross orth).normalizeAssign()
                val side = (front cross up).normalizeAssign()

                val localVertexStart = vertices.size
                vertices.add(-front * distanceToChild * 0.1f)
                vertices.add(childpos)
                vertices.add(-side * distanceToChild * 0.1f)
                vertices.add(-side * distanceToChild * 0.1f)
                vertices.add(childpos)
                vertices.add(front * distanceToChild * 0.1f)
                vertices.add(front * distanceToChild * 0.1f)
                vertices.add(childpos)
                vertices.add(side * distanceToChild * 0.1f)
                vertices.add(side * distanceToChild * 0.1f)
                vertices.add(childpos)
                vertices.add(-front * distanceToChild * 0.1f)

                faces.add(Face(localVertexStart + 0, localVertexStart + 1, localVertexStart + 2))
                faces.add(Face(localVertexStart + 3, localVertexStart + 4, localVertexStart + 5))
                faces.add(Face(localVertexStart + 6, localVertexStart + 7, localVertexStart + 8))
                faces.add(Face(localVertexStart + 9, localVertexStart + 10, localVertexStart + 11))
            }
        else {
            // if the node has no children, it's an end node. Put a little knob there instead
            val ownpos = AiVector3D(node.transformation.a3, node.transformation.b3, node.transformation.c3)
            val sizeEstimate = ownpos.length() * 0.18f

            vertices.add(AiVector3D(-sizeEstimate, 0f, 0f))
            vertices.add(AiVector3D(0f, sizeEstimate, 0f))
            vertices.add(AiVector3D(0f, 0f, -sizeEstimate))
            vertices.add(AiVector3D(0f, sizeEstimate, 0f))
            vertices.add(AiVector3D(sizeEstimate, 0f, 0f))
            vertices.add(AiVector3D(0f, 0f, -sizeEstimate))
            vertices.add(AiVector3D(sizeEstimate, 0f, 0f))
            vertices.add(AiVector3D(0f, -sizeEstimate, 0f))
            vertices.add(AiVector3D(0f, 0f, -sizeEstimate))
            vertices.add(AiVector3D(0f, -sizeEstimate, 0f))
            vertices.add(AiVector3D(-sizeEstimate, 0f, 0f))
            vertices.add(AiVector3D(0f, 0f, -sizeEstimate))

            vertices.add(AiVector3D(-sizeEstimate, 0f, 0f))
            vertices.add(AiVector3D(0f, 0f, sizeEstimate))
            vertices.add(AiVector3D(0f, sizeEstimate, 0f))
            vertices.add(AiVector3D(0f, sizeEstimate, 0f))
            vertices.add(AiVector3D(0f, 0f, sizeEstimate))
            vertices.add(AiVector3D(sizeEstimate, 0f, 0f))
            vertices.add(AiVector3D(sizeEstimate, 0f, 0f))
            vertices.add(AiVector3D(0f, 0f, sizeEstimate))
            vertices.add(AiVector3D(0f, -sizeEstimate, 0f))
            vertices.add(AiVector3D(0f, -sizeEstimate, 0f))
            vertices.add(AiVector3D(0f, 0f, sizeEstimate))
            vertices.add(AiVector3D(-sizeEstimate, 0f, 0f))

            faces.add(Face(vertexStartIndex + 0, vertexStartIndex + 1, vertexStartIndex + 2))
            faces.add(Face(vertexStartIndex + 3, vertexStartIndex + 4, vertexStartIndex + 5))
            faces.add(Face(vertexStartIndex + 6, vertexStartIndex + 7, vertexStartIndex + 8))
            faces.add(Face(vertexStartIndex + 9, vertexStartIndex + 10, vertexStartIndex + 11))
            faces.add(Face(vertexStartIndex + 12, vertexStartIndex + 13, vertexStartIndex + 14))
            faces.add(Face(vertexStartIndex + 15, vertexStartIndex + 16, vertexStartIndex + 17))
            faces.add(Face(vertexStartIndex + 18, vertexStartIndex + 19, vertexStartIndex + 20))
            faces.add(Face(vertexStartIndex + 21, vertexStartIndex + 22, vertexStartIndex + 23))
        }

        val numVertices = vertices.size - vertexStartIndex
        if (numVertices > 0) {
            // create a bone affecting all the newly created vertices
            val bone = AiBone()
            bones.add(bone)
            bone.name = node.name
            // calculate the bone offset matrix by concatenating the inverse transformations of all parents
            bone.offsetMatrix = node.transformation.inverse()
            var parent: AiNode? = node.parent
            while (parent != null) {
                bone.offsetMatrix = parent.transformation.inverse() * bone.offsetMatrix
                parent = parent.parent
            }
            // add all the vertices to the bone's influences
            bone.numWeights = numVertices
            bone.weights = MutableList(numVertices, { AiVertexWeight(vertexStartIndex + it, 1f) })
            // HACK: (thom) transform all vertices to the bone's local space. Should be done before adding
            // them to the array, but I'm tired now and I'm annoyed.
            val boneToMeshTransform = bone.offsetMatrix.inverse()
            for (a in vertexStartIndex until vertices.size)
                vertices[a] = boneToMeshTransform * vertices[a]
        }
        // and finally recurse into the children list
        node.children.forEach(this::createGeometry)
    }

    /** Creates the mesh from the internally accumulated stuff and returns it.     */
    fun createMesh(): AiMesh {

        val mesh = AiMesh()
        // add points
        mesh.numVertices = vertices.size
        mesh.vertices = MutableList(mesh.numVertices, { vertices[it] })

        mesh.normals = MutableList(mesh.numVertices, { AiVector3D() })

        // add faces
        mesh.numFaces = faces.size
        mesh.faces = MutableList(mesh.numFaces, { f -> MutableList(3, { faces[f].indices[it] }) })
        faces.forEach {
            // Compute per-face normals ... we don't want the bones to be smoothed ... they're built to visualize
            // the skeleton, so it's good if there's a visual difference to the rest of the geometry
            val nor = (vertices[it.indices[2]] - vertices[it.indices[0]]) cross
                    (vertices[it.indices[1]] - vertices[it.indices[0]])

            if (nor.length() < 1e-5) /* ensure that FindInvalidData won't remove us ...*/
                nor.put(1f, 0f, 0f)

            for (n in 0..2) mesh.normals[it.indices[n]] put nor
        }
        // add the bones
        mesh.numBones = bones.size
        mesh.bones = bones
        // default
        mesh.materialIndex = 0
        return mesh
    }

    /** Creates a dummy material and returns it. */
    fun createMaterial() = AiMaterial().apply {
        name = "SkeletonMaterial"   // Name
        twoSided = true // Prevent backface culling
    }
    /** faces */
    class Face {

        val indices: IntArray

        constructor() {
            indices = IntArray((3))
        }
        constructor(p0: Int, p1: Int, p2: Int) {
            indices = intArrayOf(p0, p1, p2)
        }

    }
}