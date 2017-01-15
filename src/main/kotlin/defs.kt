import mat.Mat4
import vec._2.Vec2
import vec._2.Vec2d
import vec._3.Vec3
import vec._3.Vec3d
import vec._4.Vec4
import vec._4.Vec4d
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KClass

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
infix operator fun File.plus(another: String) = File(this, another)

fun URL.exists() = Files.exists(Paths.get(toURI()))
fun URI.exists() = Files.exists(Paths.get(this))
val URI.s
    get() = toString()

val String.URI
    get() = javaClass.getResource(this).toURI()