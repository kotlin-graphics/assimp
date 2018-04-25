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

/** @file PolyTools.h, various utilities for our dealings with arbitrary polygons */

package assimp

import glm_.vec2.Vec2
import glm_.vec3.Vec3


/** Compute the signed area of a triangle. */
inline fun getArea2D(v1: Vec3, v2: Vec3, v3: Vec3): Double {
    return 0.5 * (v1.x * (v3.y - v2.y) + v2.x * (v1.y - v3.y) + v3.x * (v2.y - v1.y))
}

/** Compute the signed area of a triangle. */
inline fun getArea2D(v1: Vec2, v2: Vec2, v3: Vec2): Double {
    return 0.5 * (v1.x * (v3.y - v2.y) + v2.x * (v1.y - v3.y) + v3.x * (v2.y - v1.y))
}

/** Test if a given point p2 is on the left side of the line formed by p0-p1. */
inline fun onLeftSideOfLine2D(p0: Vec3, p1: Vec3, p2: Vec3): Boolean {
    return getArea2D(p0, p2, p1) > 0
}

/** Test if a given point p2 is on the left side of the line formed by p0-p1. */
inline fun onLeftSideOfLine2D(p0: Vec2, p1: Vec2, p2: Vec2): Boolean {
    return getArea2D(p0, p2, p1) > 0
}

/** Test if a given point is inside a given triangle in R2. */
inline fun pointInTriangle2D(p0: Vec3, p1: Vec3, p2: Vec3, pp: Vec3): Boolean {
    // Point in triangle test using baryzentric coordinates
    val v0 = p1 - p0
    val v1 = p2 - p0
    val v2 = pp - p0

    var dot00 = v0 dot v0
    val dot01 = v0 dot v1
    val dot02 = v0 dot v2
    var dot11 = v1 dot v1
    val dot12 = v1 dot v2

    val invDenom = 1 / (dot00 * dot11 - dot01 * dot01)
    dot11 = (dot11 * dot02 - dot01 * dot12) * invDenom
    dot00 = (dot00 * dot12 - dot01 * dot02) * invDenom

    return dot11 > 0 && dot00 > 0 && (dot11 + dot00 < 1)
}

/** Test if a given point is inside a given triangle in R2. */
inline fun pointInTriangle2D(p0: Vec2, p1: Vec2, p2: Vec2, pp: Vec2): Boolean {
    // Point in triangle test using baryzentric coordinates
    val v0 = p1 - p0
    val v1 = p2 - p0
    val v2 = pp - p0

    var dot00 = v0 dot v0
    val dot01 = v0 dot v1
    val dot02 = v0 dot v2
    var dot11 = v1 dot v1
    val dot12 = v1 dot v2

    val invDenom = 1 / (dot00 * dot11 - dot01 * dot01)
    dot11 = (dot11 * dot02 - dot01 * dot12) * invDenom
    dot00 = (dot00 * dot12 - dot01 * dot02) * invDenom

    return dot11 > 0 && dot00 > 0 && (dot11 + dot00 < 1)
}


/** Check whether the winding order of a given polygon is counter-clockwise.
 *  The function accepts an unconstrained template parameter, but is intended
 *  to be used only with aiVector2D and aiVector3D (z axis is ignored, only
 *  x and y are taken into account).
 *  @note Code taken from http://cgm.cs.mcgill.ca/~godfried/teaching/cg-projects/97/Ian/applet1.html and translated to C++ */
//template <typename T>
//inline bool IsCCW(T* in , size_t npoints) {
//    double aa, bb, cc, b, c, theta
//    double convex_turn
//    double convex_sum = 0
//
//    ai_assert(npoints >= 3)
//
//    for (size_t i = 0; i < npoints - 2; i++) {
//        aa = (( in [i + 2].x - in[i].x) * ( in [i + 2].x - in[i].x))+
//        ((-in[i + 2].y + in[i].y) * (-in[i + 2].y + in[i].y))
//
//        bb = (( in [i + 1].x - in[i].x) * ( in [i + 1].x - in[i].x))+
//        ((-in[i + 1].y + in[i].y) * (-in[i + 1].y + in[i].y))
//
//        cc = (( in [i + 2].x - in[i + 1].x) *
//                ( in [i + 2].x - in[i + 1].x))+
//        ((-in[i + 2].y + in[i + 1].y) *
//                (-in[i + 2].y + in[i + 1].y))
//
//        b = std::sqrt(bb)
//        c = std::sqrt(cc)
//        theta = std::acos((bb + cc - aa) / (2 * b * c))
//
//        if (OnLeftSideOfLine2D(in[i], in [i+2], in [i+1])) {
//        //  if (convex(in[i].x, in[i].y,
//        //      in[i+1].x, in[i+1].y,
//        //      in[i+2].x, in[i+2].y)) {
//        convex_turn = AI_MATH_PI_F - theta
//        convex_sum += convex_turn
//    }
//        else {
//        convex_sum -= AI_MATH_PI_F - theta
//    }
//    }
//    aa = (( in [1].x - in[npoints - 2].x) *
//            ( in [1].x - in[npoints - 2].x))+
//    ((-in[1].y + in[npoints - 2].y) *
//            (-in[1].y + in[npoints - 2].y))
//
//    bb = (( in [0].x - in[npoints - 2].x) *
//            ( in [0].x - in[npoints - 2].x))+
//    ((-in[0].y + in[npoints - 2].y) *
//            (-in[0].y + in[npoints - 2].y))
//
//    cc = (( in [1].x - in[0].x) * ( in [1].x - in[0].x))+
//    ((-in[1].y + in[0].y) * (-in[1].y + in[0].y))
//
//    b = std::sqrt(bb)
//    c = std::sqrt(cc)
//    theta = std::acos((bb + cc - aa) / (2 * b * c))
//
//    //if (convex(in[npoints-2].x, in[npoints-2].y,
//    //  in[0].x, in[0].y,
//    //  in[1].x, in[1].y)) {
//    if (OnLeftSideOfLine2D(in[npoints - 2], in [1], in [0])) {
//        convex_turn = AI_MATH_PI_F - theta
//        convex_sum += convex_turn
//    }
//    else {
//        convex_sum -= AI_MATH_PI_F - theta
//    }
//
//    return convex_sum >= (2 * AI_MATH_PI_F)
//}


/** Compute the normal of an arbitrary polygon in R3.
 *
 *  The code is based on Newell's formula, that is a polygons normal is the ratio
 *  of its area when projected onto the three coordinate axes.
 *
 *  @param out Receives the output normal
 *  @param num Number of input vertices
 *  @param x X data source. x[ofs_x*n] is the n'th element.
 *  @param y Y data source. y[ofs_y*n] is the y'th element
 *  @param z Z data source. z[ofs_z*n] is the z'th element
 *
 *  @note The data arrays must have storage for at least num+2 elements. Using
 *  this method is much faster than the 'other' NewellNormal()
 */
fun newellNormal(out: Vec3, num: Int, vecs: Array<Vec3>) {
    // Duplicate the first two vertices at the end
    vecs[num + 0](vecs[0])
    vecs[num + 1](vecs[1])

    var sumXy = 0f
    var sumYz = 0f
    var sumZx = 0f

    var ptr = 1
    var low = 0
    var high = 2

    repeat(num) {
        sumXy += vecs[ptr].x * (vecs[high].y-vecs[low].y)
        sumYz += vecs[ptr].y * (vecs[high].z-vecs[low].z)
        sumZx += vecs[ptr].z * (vecs[high].x-vecs[low].x)

        ptr++
        low++
        high++
    }
    out(sumYz, sumZx, sumXy)
}