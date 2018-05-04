/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2018, assimp team


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

package assimp.postProcess

import assimp.*
import glm_.glm
import glm_.max
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import kotlin.math.abs
import kotlin.math.acos

/** @file Defines a post processing step to triangulate all faces
with more than three vertices.
 */

/** The TriangulateProcess splits up all faces with more than three indices
 * into triangles. You usually want this to happen because the graphics cards
 * need their data as triangles.
 */
class TriangulateProcess : BaseProcess() {


    /** Returns whether the processing step is present in the given flag field.
     *  @param flags The processing flags the importer was called with. A bitwise combination of AiPostProcessStep.
     *  @return true if the process is present in this flag fields, false if not.     */
    override fun isActive(flags: AiPostProcessStepsFlags): Boolean = flags has AiPostProcessStep.Triangulate

    /** Executes the post processing step on the given imported data.
     *  At the moment a process is not supposed to fail.
     *  @param scene The imported data to work at.     */
    override fun execute(scene: AiScene) {

        logger.debug("TriangulateProcess begin")

        var has = false
        for (a in 0 until scene.numMeshes)
            if (triangulateMesh(scene.meshes[a]))
                has = true
        if (has)
            logger.info("TriangulateProcess finished. All polygons have been triangulated.")
        else
            logger.debug("TriangulateProcess finished. There was nothing to be done.")
    }

    /** Triangulates the given mesh.
     *  @param pMesh The mesh to triangulate.     */
    fun triangulateMesh(mesh: AiMesh): Boolean { // TODO bug

        // Now we have aiMesh::mPrimitiveTypes, so this is only here for test cases
        if (mesh.primitiveTypes == 0) {

            var need = false

            for (a in 0 until mesh.numFaces)
                if (mesh.faces[a].size != 3)
                    need = true
            if (!need)
                return false
        } else if (mesh.primitiveTypes hasnt AiPrimitiveType.POLYGON)
            return false

        // Find out how many output faces we'll get
        var numOut = 0
        var maxOut = 0
        var getNormals = true
        for (a in 0 until mesh.numFaces) {
            val face = mesh.faces[a]
            if (face.size <= 4)
                getNormals = false
            if (face.size <= 3)
                numOut++
            else {
                numOut += face.size - 2
                maxOut = maxOut max face.size
            }
        }

        // Just another check whether aiMesh::mPrimitiveTypes is correct
        assert(numOut != mesh.numFaces)

        val norOut: Array<Vec3>? = null
//
//        // if we don't have normals yet, but expect them to be a cheap side product of triangulation anyway, allocate storage for them.
//        if (!pMesh->mNormals && get_normals) {
//            // XXX need a mechanism to inform the GenVertexNormals process to treat these normals as preprocessed per-face normals
//            //  nor_out = pMesh->mNormals = new aiVector3D[pMesh->mNumVertices];
//        }

        // the output mesh will contain triangles, but no polys anymore
        mesh.primitiveTypes = mesh.primitiveTypes or AiPrimitiveType.TRIANGLE
        mesh.primitiveTypes = mesh.primitiveTypes wo AiPrimitiveType.POLYGON

        val out: Array<AiFace> = Array(numOut) { mutableListOf<Int>() }
        var curOut = 0
        val tempVerts3d = Array(maxOut + 2) { Vec3() } /* temporary storage for vertices */
        val tempVerts = Array(maxOut + 2) { Vec2() }

        // Apply vertex colors to represent the face winding?
//        #ifdef AI_BUILD_TRIANGULATE_COLOR_FACE_WINDING TODO
//                if (!pMesh->mColors[0])
//        pMesh->mColors[0] = new aiColor4D[pMesh->mNumVertices]
//        else
//        new(pMesh->mColors[0]) aiColor4D[pMesh->mNumVertices]
//
//        aiColor4D * clr = pMesh->mColors[0]
//        #endif
//
//        #ifdef AI_BUILD_TRIANGULATE_DEBUG_POLYS
//                FILE * fout = fopen(POLY_OUTPUT_FILE, "a")
//        #endif

        val verts = mesh.vertices

        // use std::unique_ptr to avoid slow std::vector<bool> specialiations
        val done = BooleanArray(maxOut)
        for (a in 0 until mesh.numFaces) {
            val face = mesh.faces[a]

            val idx = face
            var num = face.size
            var next = 0
            var tmp = 0
            var prev = num - 1
            val max = num

            // Apply vertex colors to represent the face winding?
//            #ifdef AI_BUILD_TRIANGULATE_COLOR_FACE_WINDING
//                for (unsigned int i = 0; i < face.mNumIndices; ++i) {
//            aiColor4D& c = clr[idx[i]]
//            c.r = (i + 1) / (float) max
//                    c.b = 1.f - c.r
//        }
//            #endif

            val lastFace = curOut

            // if it's a simple point,line or triangle: just copy it
            if (face.size <= 3) {
                val nFace = out[curOut++]
                nFace.clear()
                nFace += face
                face.clear()
                continue
            }
            // optimized code for quadrilaterals
            else if (face.size == 4) {

                /*  quads can have at maximum one concave vertex.
                    Determine this vertex (if it exists) and start tri-fanning from it.                 */
                var startVertex = 0
                for (i in 0..3) {
                    val v0 = verts[face[(i + 3) % 4]]
                    val v1 = verts[face[(i + 2) % 4]]
                    val v2 = verts[face[(i + 1) % 4]]

                    val v = verts[face[i]]

                    val left = v0 - v
                    val diag = v1 - v
                    val right = v2 - v

                    left.normalizeAssign()
                    diag.normalizeAssign()
                    right.normalizeAssign()

                    val angle = acos(left dot diag) + acos(right dot diag)
                    if (angle > glm.PIf) {
                        // this is the concave point
                        startVertex = i
                        break
                    }
                }

                val temp = IntArray(4) { face[it] }

                val nFace = out[curOut++]
                nFace.clear()

                nFace += temp[startVertex]
                nFace += temp[(startVertex + 1) % 4]
                nFace += temp[(startVertex + 2) % 4]

                val sFace = out[curOut++]
                sFace.clear()

                sFace += temp[startVertex]
                sFace += temp[(startVertex + 2) % 4]
                sFace += temp[(startVertex + 3) % 4]

                // prevent double deletion of the indices field
                face.clear()
                continue
            }
            else {
                /*  A polygon with more than 3 vertices can be either concave or convex.
                    Usually everything we're getting is convex and we could easily triangulate by tri-fanning.
                    However, LightWave is probably the only modeling suite to make extensive use of highly concave,
                    monster polygons ...
                    so we need to apply the full 'ear cutting' algorithm to get it right.

                    REQUIREMENT: polygon is expected to be simple and *nearly* planar.
                    We project it onto a plane to get a 2d triangle.    */

                // Collect all vertices of of the polygon.
                tmp = 0
                while (tmp < max)
                    tempVerts3d[tmp] = verts[idx[tmp++]]
                // Get newell normal of the polygon. Store it for future use if it's a polygon-only mesh
                val n = Vec3()
                newellNormal(n, max, tempVerts3d)
                norOut?.let {
                    tmp = 0
                    while (tmp < max)
                        it[idx[tmp++]] = n
                }

                // Select largest normal coordinate to ignore for projection
                val aX = if (n.x > 0) n.x else -n.x
                val aY = if (n.y > 0) n.y else -n.y
                val aZ = if (n.z > 0) n.z else -n.z

                var ac = 0
                var bc = 1 /* no z coord. projection to xy */
                var inv = n.z
                if (aX > aY) {
                    if (aX > aZ) { /* no x coord. projection to yz */
                        ac = 1; bc = 2
                        inv = n.x
                    }
                } else if (aY > aZ) { /* no y coord. projection to zy */
                    ac = 2; bc = 0
                    inv = n.y
                }

                // Swap projection axes to take the negated projection vector into account
                if (inv < 0f) {
                    val t = ac
                    ac = bc
                    bc = t
                }

                tmp = 0
                while (tmp < max) {
                    tempVerts[tmp][0] = verts[idx[tmp]][ac]
                    tempVerts[tmp][1] = verts[idx[tmp]][bc]
                    done[tmp++] = false
                }

//                #ifdef AI_BUILD_TRIANGULATE_DEBUG_POLYS
//                        // plot the plane onto which we mapped the polygon to a 2D ASCII pic
//                        aiVector2D bmin, bmax
//                ArrayBounds(& temp_verts [0], max, bmin, bmax)
//
//                char grid [POLY_GRID_Y][POLY_GRID_X + POLY_GRID_XPAD]
//                std::fill_n((char *) grid, POLY_GRID_Y * (POLY_GRID_X + POLY_GRID_XPAD), ' ')
//
//                for (int i = 0; i < max; ++i) {
//                    const aiVector2D & v =(tempVerts[i] - bmin) / (bmax - bmin)
//                    const size_t x = static_cast<size_t>(v.x * (POLY_GRID_X - 1)), y = static_cast<size_t>(v.y*(POLY_GRID_Y-1))
//                    char * loc = grid[y] + x
//                    if (grid[y][x] != ' ') {
//                        for (;* loc != ' '; ++loc)
//                        *loc++ = '_'
//                    }
//                    *(loc + ::ai_snprintf(loc, POLY_GRID_XPAD, "%i", i)) = ' '
//                }
//
//
//                for (size_t y = 0; y < POLY_GRID_Y; ++y) {
//                    grid[y][POLY_GRID_X + POLY_GRID_XPAD - 1] = '\0'
//                    fprintf(fout, "%s\n", grid[y])
//                }
//
//                fprintf(fout, "\ntriangulation sequence: ")
//                #endif

                // FIXME: currently this is the slow O(kn) variant with a worst case
                // complexity of O(n^2) (I think). Can be done in O(n).
                while (num > 3) {

                    // Find the next ear of the polygon
                    var numFound = 0
                    var ear = next
                    while (true) {

                        // break after we looped two times without a positive match
                        next = ear + 1
                        while (done[if (next >= max) 0.also { next = 0 } else next])
                            ++next
                        if (next < ear)
                            if (++numFound == 2)
                                break
                        val pnt1 = tempVerts[ear]
                        val pnt0 = tempVerts[prev]
                        val pnt2 = tempVerts[next]

                        // Must be a convex point. Assuming ccw winding, it must be on the right of the line between p-1 and p+1.
                        if (onLeftSideOfLine2D(pnt0, pnt2, pnt1)) {
                            prev = ear
                            ear = next
                            continue
                        }

                        // and no other point may be contained in this triangle
                        tmp = 0
                        while (tmp < max) {

                            /*  We need to compare the actual values because it's possible that multiple indexes in
                                the polygon are referring to the same position. concave_polygon.obj is a sample

                                FIXME: Use 'epsiloned' comparisons instead? Due to numeric inaccuracies in
                                PointInTriangle() I'm guessing that it's actually possible to construct
                                input data that would cause us to end up with no ears. The problem is,
                                which epsilon? If we chose a too large value, we'd get wrong results    */
                            val vtmp = tempVerts[tmp]
                            if (vtmp !== pnt1 && vtmp !== pnt2 && vtmp !== pnt0 && pointInTriangle2D(pnt0, pnt1, pnt2, vtmp))
                                break
                            ++tmp
                        }
                        if (tmp != max) {
                            prev = ear
                            ear = next
                            continue
                        }
                        // this vertex is an ear
                        break
                    }
                    if (numFound == 2) {

                        /*  Due to the 'two ear theorem', every simple polygon with more than three points must
                            have 2 'ears'. Here's definitely something wrong ... but we don't give up yet.

                            Instead we're continuing with the standard tri-fanning algorithm which we'd
                            use if we had only convex polygons. That's life. */
                        logger.error("Failed to triangulate polygon (no ear found). Probably not a simple polygon?")

//                        #ifdef AI_BUILD_TRIANGULATE_DEBUG_POLYS
//                                fprintf(fout, "critical error here, no ear found! ")
//                        #endif
                        num = 0
                        break
//                      TODO unreachable
//                        curOut -= (max - num) /* undo all previous work */
//                        for (tmp = 0; tmp < max - 2; ++tmp) {
//                            aiFace& nface = *curOut++
//
//                            nface.mNumIndices = 3
//                            if (!nface.mIndices)
//                                nface.mIndices = new unsigned int[3]
//
//                            nface.mIndices[0] = 0
//                            nface.mIndices[1] = tmp + 1
//                            nface.mIndices[2] = tmp + 2
//
//                        }
//                        num = 0
//                        break
                    }

                    val nFace = out[curOut++]

                    if (nFace.isEmpty())
                        for (i in 0..2)
                            nFace += 0

                    // setup indices for the new triangle ...
                    nFace[0] = prev
                    nFace[1] = ear
                    nFace[2] = next

                    // exclude the ear from most further processing
                    done[ear] = true
                    --num
                }
                if (num > 0) {
                    // We have three indices forming the last 'ear' remaining. Collect them.
                    val nFace = out[curOut++]
                    if (nFace.isEmpty())
                        for (i in 0..2)
                            nFace += 0

                    tmp = 0
                    while (done[tmp]) ++tmp
                    nFace[0] = tmp

                    ++tmp
                    while (done[tmp]) ++tmp
                    nFace[1] = tmp

                    ++tmp
                    while (done[tmp]) ++tmp
                    nFace[2] = tmp
                }
            }

//            #ifdef AI_BUILD_TRIANGULATE_DEBUG_POLYS
//
//                    for (aiFace* f = lastFace; f != curOut; ++f) {
//                unsigned int * i = f->mIndices
//                fprintf(fout, " (%i %i %i)", i[0], i[1], i[2])
//            }
//
//            fprintf(fout, "\n*********************************************************************\n")
//            fflush(fout)
//
//            #endif

            var f = lastFace
            while (f != curOut) {
                val i = out[f]

                //  drop dumb 0-area triangles
                val abs = abs(getArea2D(tempVerts[i[0]], tempVerts[i[1]], tempVerts[i[2]]))
                if (abs < 1e-5f) {
                    logger.debug("Dropping triangle with area 0")
                    --curOut

                    out[f].clear()

                    var ff = f
                    while (ff != curOut) {
                        out[ff].clear()
                        out[ff].addAll(out[ff + 1])
                        out[ff + 1].clear()
                        ++ff
                    }
                    continue
                }

                i[0] = idx[i[0]]
                i[1] = idx[i[1]]
                i[2] = idx[i[2]]
                ++f
            }

            face.clear()
        }

//        #ifdef AI_BUILD_TRIANGULATE_DEBUG_POLYS TODO
//                fclose(fout)
//        #endif

        // kill the old faces
        mesh.faces.clear()

        // ... and store the new ones
        mesh.faces.addAll(out)
        mesh.numFaces = curOut /* not necessarily equal to numOut */
        return true
    }
}