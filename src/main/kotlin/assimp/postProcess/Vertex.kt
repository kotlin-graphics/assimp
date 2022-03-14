package assimp.postProcess

import assimp.AI_MAX_NUMBER_OF_COLOR_SETS
import assimp.AI_MAX_NUMBER_OF_TEXTURECOORDS
import glm_.vec3.Vec3
import glm_.vec4.Vec4

class Vertex {
    //properties on top
    val position = Vec3()
    val normal = Vec3()
    val tangent = Vec3()
    val bitangent = Vec3()

    val texcoords = List<Vec3>(AI_MAX_NUMBER_OF_TEXTURECOORDS) { _ -> Vec3() }
    val colors = List<Vec4>(AI_MAX_NUMBER_OF_COLOR_SETS) { _ -> Vec4() }

}