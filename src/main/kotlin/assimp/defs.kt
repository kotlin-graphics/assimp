package assimp

import glm.glm
import glm.vec2.Vec2
import glm.vec3.Vec3
import glm.vec4.Vec4
import glm.mat4x4.Mat4

/**
 * Created by elect on 14/11/2016.
 */

typealias ai_real = Float

typealias AiVector3D = Vec3

typealias AiColor3D = Vec3

typealias AiColor4D = Vec4

typealias AiMatrix4x4 = Mat4

typealias AiVector2D = Vec2

/* To avoid running out of memory
 * This can be adjusted for specific use cases
 * It's NOT a total limit, just a limit for individual allocations
 */
fun AI_MAX_ALLOC(size: Int) = (256 * 1024 * 1024) / size

// TODO file operators overloading, https://youtrack.jetbrains.com/issue/KT-15009
infix operator fun java.io.File.plus(another: String) = java.io.File(this, another)

fun java.net.URL.exists() = java.nio.file.Files.exists(java.nio.file.Paths.get(toURI()))
fun java.net.URI.exists() = java.nio.file.Files.exists(java.nio.file.Paths.get(this))
val java.net.URI.s
    get() = toString()

val String.URI: java.net.URI
    get() = javaClass.getResource(this).toURI()

fun org.w3c.dom.Element.elementChildren(): ArrayList<org.w3c.dom.Element> {

    val res = ArrayList<org.w3c.dom.Element>()

    repeat(childNodes.length) {

        val element = childNodes.item(it)

        if (element is org.w3c.dom.Element)
            res.add(element)
    }
    return res
}

operator fun org.w3c.dom.Element.get(attribute: String) = if (hasAttribute(attribute)) getAttribute(attribute) else null

val String.words
    get() = trim().split("\\s+".toRegex())

//////////////////////////////////////////////////////////////////////////
/* Useful constants */
//////////////////////////////////////////////////////////////////////////

/* This is PI. Hi PI. */
val AI_MATH_TWO_PI      = glm.PI * 2   // TODO glm?
val AI_MATH_TWO_PIf      = glm.PIf * 2   // TODO glm?
val AI_MATH_HALF_PI    = glm.PIf * 0.5