package assimp

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

fun Mat4.decompose(pScaling: AiVector3D, pRotation: AiQuaternion, pPosition: AiVector3D) {

    /* extract translation */
    pPosition.x = this[0][3]
    pPosition.y = this[1][3]
    pPosition.z = this[2][3]

    /* extract the columns of the matrix. */
    val vCols = arrayOf(
            AiVector3D(this[0][0], this[1][0], this[2][0]),
            AiVector3D(this[0][1], this[1][1], this[2][1]),
            AiVector3D(this[0][2], this[1][2], this[2][2]))

    /* extract the scaling factors */
    pScaling.x = vCols[0].length()
    pScaling.y = vCols[1].length()
    pScaling.z = vCols[2].length()

    /* and the sign of the scaling */
    if (det() < 0) pScaling.negate_()

    /* and remove all scaling from the matrix */
    if (pScaling.x != 0f) vCols[0] div_ pScaling.x
    if (pScaling.y != 0f) vCols[1] div_ pScaling.y
    if (pScaling.z != 0f) vCols[2] div_ pScaling.z

    // build a 3x3 rotation matrix
    val m = AiMatrix3x3(
            vCols[0].x, vCols[1].x, vCols[2].x,
            vCols[0].y, vCols[1].y, vCols[2].y,
            vCols[0].z, vCols[1].z, vCols[2].z)

    // and generate the rotation quaternion from it
    pRotation put m.toQuat()
}

/** Transformation of a vector by a 4x4 matrix */
operator fun AiMatrix4x4.times(vector: AiVector3D) = AiVector3D(
        a0 * vector.x + a1 * vector.y + a2 * vector.z + a3,
        b0 * vector.x + b1 * vector.y + b2 * vector.z + b3,
        c0 * vector.x + c1 * vector.y + c2 * vector.z + c3)


var ASSIMP_BUILD_NO_VALIDATEDS_PROCESS = false

var ASSIMP_BUILD_DEBUG = true