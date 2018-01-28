package assimp

import glm_.glm
import glm_.mat3x3.Mat3
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import org.w3c.dom.Element
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.cos
import kotlin.math.sin

        /**
         * Created by elect on 14/11/2016.
         */

typealias ai_real = Float

typealias AiVector3D = Vec3

typealias AiColor3D = Vec3

typealias AiColor4D = Vec4

typealias AiMatrix3x3 = Mat3
typealias AiMatrix4x4 = Mat4

typealias AiVector2D = Vec2

typealias AiQuaternion = Quat

/* To avoid running out of memory
 * This can be adjusted for specific use cases
 * It's NOT a total limit, just a limit for individual allocations
 */
fun AI_MAX_ALLOC(size: Int) = (256 * 1024 * 1024) / size

// TODO file operators overloading, https://youtrack.jetbrains.com/issue/KT-15009
infix operator fun File.plus(another: String) = File(this, another)

fun URL.exists() = Files.exists(Paths.get(toURI()))
fun URI.exists() = Files.exists(Paths.get(this))
val URI.extension
    get() =
        if (path.contains(".")) path.substringAfterLast('.', "").toLowerCase()
        else ""
val URI.s get() = toString()

fun Element.elementChildren(): ArrayList<Element> {

    val res = ArrayList<Element>()

    repeat(childNodes.length) {

        val element = childNodes.item(it)

        if (element is Element)
            res.add(element)
    }
    return res
}

operator fun Element.get(attribute: String) = if (hasAttribute(attribute)) getAttribute(attribute) else null

val String.words get() = trim().split("\\s+".toRegex())

//////////////////////////////////////////////////////////////////////////
/* Useful constants */
//////////////////////////////////////////////////////////////////////////

/* This is PI. Hi PI. */
val AI_MATH_TWO_PI = glm.PI * 2   // TODO glm?
val AI_MATH_TWO_PIf = glm.PIf * 2   // TODO glm?
val AI_MATH_HALF_PI = glm.PIf * 0.5

val AiVector3D.squareLength get() = x * x + y * y + z * z

fun rotationX(a: Float) = AiMatrix4x4().apply {
    /*
         |  1  0       0       0 |
     M = |  0  cos(A)  sin(A)  0 |
         |  0 -sin(A)  cos(A)  0 |
         |  0  0       0       1 |        */
    b1 = cos(a)
    c2 = b1
    c1 = sin(a)
    b2 = -c1
}

fun rotationY(a: Float) = AiMatrix4x4().apply {
    /*
         |  cos(A)  0  -sin(A)  0 |
     M = |  0       1   0       0 |
         | sin(A)   0   cos(A)  0 |
         |  0       0   0       1 |        */

    a0 = cos(a)
    c2 = a0
    a2 = sin(a)
    c0 = -a2
}

fun rotationZ(a: Float) = AiMatrix4x4().apply{
    /*
         |  cos(A)   sin(A)   0   0 |
     M = | -sin(A)   cos(A)   0   0 |
         |  0        0        1   0 |
         |  0        0        0   1 |       */
    a0 = cos(a)
    b1 = a0
    b0 = sin(a)
    a1 = -b0
}

fun translation( v:Vec3) = AiMatrix4x4().apply {
    d0 = v.x
    d1 = v.y
    d2 = v.z
}

fun scaling( v:Vec3) = AiMatrix4x4().apply {
    a0 = v.x
    b1 = v.y
    c2 = v.z
}