package assimp.format.obj

import assimp.isByteOrderMark
import assimp.logger
import assimp.words
import glm_.f
import glm_.i

/**
 * Created by elect on 27/11/2016.
 */

/**
 *  @class  ObjFileMtlImporter
 *  @brief  Loads the material description from a mtl file.
 */

class ObjFileMtlImporter(buffer: List<String>, private val model: Model) {

    init {
        if (model.defaultMaterial == null)
            model.defaultMaterial = Material("default")
        load(buffer)
    }

    fun load(buffer: List<String>) {

        for (line in buffer) {

            val trimmedLine = line.trim()
            val words = when  {
                trimmedLine[0].isByteOrderMark -> trimmedLine.drop(1)
                else -> trimmedLine
            }.words

            when (words[0][0]) {
                'k', 'K' -> when (words[0][1]) {
                    // Ambient color
                    'a' -> model.currentMaterial!!.ambient.put(words, 1)
                    // Diffuse color
                    'd' -> model.currentMaterial?.diffuse?.put(words, 1) // TODO fix me
                    's' -> model.currentMaterial!!.specular.put(words, 1)
                    'e' -> model.currentMaterial!!.emissive.put(words, 1)
                }
                'T' -> when (words[0][1]) { // Material transmission
                    'f' -> model.currentMaterial!!.transparent.put(words, 1)
                }
                'd' -> when {
                    words[0] == "disp" // A displacement map
                    -> getTexture(trimmedLine)
                    else -> model.currentMaterial!!.alpha = words[1].f
                }  // Alpha value
                'n', 'N' -> when (words[0][1]) {
                    // Specular exponent
                    's' -> model.currentMaterial!!.shineness = words[1].f
                    // Index Of refraction
                    'i' -> model.currentMaterial!!.ior = words[1].f
                    // New material
                    'e' -> createMaterial(trimmedLine)
                }
                'm', 'b', 'r' -> getTexture(trimmedLine)
                'i' -> model.currentMaterial!!.illumination_model = words[1].i
            }
        }
    }

    // -------------------------------------------------------------------
    //  Gets a texture name from data.
    fun getTexture(line: String) {

        val words = line.substringBefore('#').split("\\s+".toRegex())   // get rid of comment
        val type: Material.Texture.Type?
        var clamped = false

        type = when {
            words[0] == "refl" && TypeOption in words -> reflMap[words[words.indexOf(TypeOption) + 1]]
            else -> tokenMap[words[0]]
        }

        if (type == null) {
            logger.error { "OBJ/MTL: Encountered unknown texture type --> $type" }
            return
        }

        if (ClampOption in words)
            clamped = words[words.indexOf(ClampOption) + 1] == "on"

        model.currentMaterial!!.textures.add(Material.Texture(words.last(), type, clamped))
    }

    // -------------------------------------------------------------------
    //  Creates a material from loaded data.
    fun createMaterial(line: String) {

        // get the name of the material with spaces
        val name = ObjTools.getNameWithSpace(line)

        model.currentMaterial = model.materialMap[name] // Use older material
                ?: Material(name).also {
                    // New Material created
                    model.materialLib.add(name)
                    model.materialMap[name] = it
                    model.m_pCurrentMesh?.m_uiMaterialIndex = model.materialLib.lastIndex
                }
    }
}

// Material specific token map
val tokenMap = mapOf(
        "map_Kd" to Material.Texture.Type.diffuse,
        "map_Ka" to Material.Texture.Type.ambient,
        "map_Ks" to Material.Texture.Type.specular,
        "map_d" to Material.Texture.Type.opacity,
        "map_emissive" to Material.Texture.Type.emissive, "map_Ke" to Material.Texture.Type.emissive,
        "map_bump" to Material.Texture.Type.bump, "map_Bump" to Material.Texture.Type.bump, "bump" to Material.Texture.Type.bump,
        "map_Kn" to Material.Texture.Type.normal,
        "disp" to Material.Texture.Type.disp,
        "map_ns" to Material.Texture.Type.specularity)

val reflMap = mapOf(
        "sphere" to Material.Texture.Type.reflectionSphere,
        "cube_top" to Material.Texture.Type.reflectionCubeTop,
        "cube_bottom" to Material.Texture.Type.reflectionCubeBottom,
        "cube_front" to Material.Texture.Type.reflectionCubeFront,
        "cube_back" to Material.Texture.Type.reflectionCubeBack,
        "cube_left" to Material.Texture.Type.reflectionCubeLeft,
        "cube_right" to Material.Texture.Type.reflectionCubeRight)

// texture option specific token
const val ClampOption = "-clamp"
const val TypeOption = "-Type"