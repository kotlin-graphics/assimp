package assimp

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import mu.KotlinLogging
import kotlin.math.abs

val logger = KotlinLogging.logger {}

fun Mat4.decompose(pScaling: AiVector3D, pRotation: AiQuaternion, pPosition: AiVector3D) {

    /* extract translation */
    pPosition[0] = this[0][3]
    pPosition[1] = this[1][3]
    pPosition[2] = this[2][3]

    /* extract the columns of the matrix. */
    val vCols = listOf(
            AiVector3D(this[0, 0], this[1, 0], this[2, 0]),
            AiVector3D(this[0, 1], this[1, 1], this[2, 1]),
            AiVector3D(this[0 ,2], this[1, 2], this[2, 2]))

    /* extract the scaling factors */
    pScaling.x = vCols[0].length()
    pScaling.y = vCols[1].length()
    pScaling.z = vCols[2].length()

    /* and the sign of the scaling */
    if (det < 0) pScaling.negateAssign()

    /* and remove all scaling from the matrix */
    if (pScaling.x != 0f) vCols[0] /= pScaling.x
    if (pScaling.y != 0f) vCols[1] /= pScaling.y
    if (pScaling.z != 0f) vCols[2] /= pScaling.z

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
        a0 * vector.x + b0 * vector.y + c0 * vector.z + d0,
        a1 * vector.x + b1 * vector.y + c1 * vector.z + d1,
        a2 * vector.x + b2 * vector.y + c2 * vector.z + d2)

val epsilon = 10e-3f
val Vec3.isBlack get() = abs(r) < epsilon && abs(g) < epsilon && abs(b) < epsilon

object ASSIMP {

    val defaultIOSystem = DefaultIOSystem()

    var DEBUG = true

    var BLENDER_DEBUG = true

    var BLENDER_NO_STATS = false

    object NO {

        var VALIDATEDS_PROCESS = true

        var X_IMPORTER = false
        var OBJ_IMPORTER = false
        var AMF_IMPORTER = false
        var _3DS_IMPORTER = false
        var MD3_IMPORTER = false
        var MD2_IMPORTER = false
        var PLY_IMPORTER = false
        var MDL_IMPORTER = false
        var ASE_IMPORTER = false
        var HMP_IMPORTER = false
        var SMD_IMPORTER = false
        var MDC_IMPORTER = false
        var MD5_IMPORTER = false
        var STL_IMPORTER = false
        var LWO_IMPORTER = false
        var DXF_IMPORTER = false
        var NFF_IMPORTER = false
        var RAW_IMPORTER = false
        var SIB_IMPORTER = false
        var OFF_IMPORTER = false
        var AC_IMPORTER = false
        var BVH_IMPORTER = false
        var IRRMESH_IMPORTER = false
        var IRR_IMPORTER = false
        var Q3D_IMPORTER = false
        var B3D_IMPORTER = false
        var COLLADA_IMPORTER = false
        var TERRAGEN_IMPORTER = false
        var CSM_IMPORTER = false
        var _3D_IMPORTER = false
        var LWS_IMPORTER = false
        var OGRE_IMPORTER = false
        var OPENGEX_IMPORTER = false
        var MS3D_IMPORTER = false
        var COB_IMPORTER = false
        var BLEND_IMPORTER = false
        var Q3BSP_IMPORTER = false
        var NDO_IMPORTER = false
        var IFC_IMPORTER = false
        var XGL_IMPORTER = false
        var FBX_IMPORTER = false
        var ASSBIN_IMPORTER = false
        var GLTF_IMPORTER = false
        var C4D_IMPORTER = false
        var _3MF_IMPORTER = false
        var X3D_IMPORTER = false

        object PROCESS {
            var MAKELEFTHANDED = false
            var FLIPUVS = false
            var FLIPWINDINGORDER = false
            var REMOVEVC = false
            var REMOVE_REDUNDANTMATERIALS = false
            var EMBEDTEXTURES = false
            var FINDINSTANCES = false
            var OPTIMIZEGRAPH = false
            var FINDDEGENERATES = false
            var GENUVCOORDS = false
            var TRANSFORMTEXCOORDS = false
            var PRETRANSFORMVERTICES = false
            var TRIANGULATE = false
            var SORTBYPTYPE = false
            var FINDINVALIDDATA = false
            var OPTIMIZEMESHES = false
            var FIXINFACINGNORMALS = false
            var SPLITBYBONECOUNT = false
            var SPLITLARGEMESHES = false
            var GENFACENORMALS = false
        }
    }
}