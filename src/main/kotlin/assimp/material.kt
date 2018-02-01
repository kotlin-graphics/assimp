package assimp

import glm_.BYTES
import glm_.vec3.Vec3
import java.nio.ByteBuffer

/**
 * Created by elect on 17/11/2016.
 */

// Name for default materials (2nd is used if meshes have UV coords)
const val AI_DEFAULT_MATERIAL_NAME = "DefaultMaterial"

class AiTexture {

    /** Width of the texture, in pixels
     *
     * If height is zero the texture is compressed in a format like JPEG. In this case width specifies the size of the memory area pcData is pointing to, in bytes.     */
    var width = 0  // ColladaParser.findFilenameForEffectTexture lies on this to be 0 at start, if you have to change it, check it

    /** Height of the texture, in pixels
     *
     * If this value is zero, pcData points to an compressed texture in any format (e.g. JPEG).      */
    var height = 0

    /** A hint from the loader to make it easier for applications to determine the type of embedded textures.
     *
     * If height != 0 this member is show how data is packed. Hint will consist of two parts: channel order and channel bitness (count of the bits for every color
     * channel). For simple parsing by the viewer it's better to not omit absent color channel and just use 0 for bitness. For example:
     * 1. Image contain RGBA and 8 bit per channel, achFormatHint == "rgba8888";
     * 2. Image contain ARGB and 8 bit per channel, achFormatHint == "argb8888";
     * 3. Image contain RGB and 5 bit for R and B channels and 6 bit for G channel, achFormatHint == "rgba5650";
     * 4. One color image with B channel and 1 bit for it, achFormatHint == "rgba0010";
     * If height == 0 then achFormatHint is set set to '\\0\\0\\0\\0' if the loader has no additional information about the texture file format used OR the file
     * extension of the format without a trailing dot. If there are multiple file extensions for a format, the shortest extension is chosen (JPEG maps to 'jpg',
     * not to 'jpeg').
     * E.g. 'dds\\0', 'pcx\\0', 'jpg\\0'.  All characters are lower-case.
     * The fourth character will always be '\\0'.        */
    var achFormatHint = ""// 8 for string + 1 for terminator.

    /** Data of the texture.
     *
     * Points to an array of width * height aiTexel's.
     * The format of the texture data is always ARGB8888 to
     * make the implementation for user of the library as easy
     * as possible. If height = 0 this is a pointer to a memory
     * buffer of size width containing the compressed texture
     * data. Good luck, have fun!
     */
    var pcData = byteArrayOf()

    // ---------------------------------------------------------------------------
    /** @brief Defines how the Nth texture of a specific Type is combined with the result of all previous layers.
     *
     *  Example (left: key, right: value): <br>
     *  @code
     *  DiffColor0     - gray
     *  DiffTextureOp0 - aiTextureOpMultiply
     *  DiffTexture0   - tex1.png
     *  DiffTextureOp0 - aiTextureOpAdd
     *  DiffTexture1   - tex2.png
     *  @endcode
     *  Written as equation, the final diffuse term for a specific pixel would be:
     *  @code
     *  diffFinal = DiffColor0 * sampleTex(DiffTexture0,UV0) + sampleTex(DiffTexture1,UV0) * diffContrib;
     *  @endcode
     *  where 'diffContrib' is the intensity of the incoming light for that pixel.
     */
    enum class Op(val i: Int) {

        /** T = T1 * T2 */
        multiply(0x0),

        /** T ( T1 + T2 */
        add(0x1),

        /** T ( T1 - T2 */
        subtract(0x2),

        /** T ( T1 / T2 */
        divide(0x3),

        /** T ( (T1 + T2) - (T1 * T2) */
        smoothAdd(0x4),

        /** T ( T1 + (T2-0.5) */
        signedAdd(0x5);

        companion object {
            fun of(i: Int) = values().first { it.i == i }
        }
    }

    // ---------------------------------------------------------------------------
    /** @brief Defines how UV coordinates outside the [0...1] range are handled.
     *
     *  Commonly referred to as 'wrapping mode'.
     */
    enum class MapMode(val i: Int) {

        /** A texture coordinate u|v is translated to u%1|v%1     */
        wrap(0x0),

        /** Texture coordinates outside [0...1]
         *  are clamped to the nearest valid value.     */
        clamp(0x1),

        /** If the texture coordinates for a pixel are outside [0...1]
         *  the texture is not applied to that pixel     */
        decal(0x3),

        /** A texture coordinate u|v becomes u%1|v%1 if (u-(u%1))%2 is zero and
         *  1-(u%1)|1-(v%1) otherwise     */
        mirror(0x2);

        companion object {
            fun of(i: Int) = values().first { it.i == i }
        }
    }

    // ---------------------------------------------------------------------------
    /** @brief Defines how the mapping coords for a texture are generated.
     *
     *  Real-time applications typically require full UV coordinates, so the use of the aiProcess_GenUVCoords step is highly
     *  recommended. It generates proper UV channels for non-UV mapped objects, as long as an accurate description how the
     *  mapping should look like (e.g spherical) is given.
     *  See the #AI_MATKEY_MAPPING property for more details.
     */
    enum class Mapping(val i: Int) {

        /** The mapping coordinates are taken from an UV channel.
         *
         *  The #AI_MATKEY_UVWSRC key specifies from which UV channel the texture coordinates are to be taken from
         *  (remember, meshes can have more than one UV channel).
         */
        uv(0x0),

        /** Spherical mapping */
        sphere(0x1),

        /** Cylindrical mapping */
        cylinder(0x2),

        /** Cubic mapping */
        box(0x3),

        /** Planar mapping */
        plane(0x4),

        /** Undefined mapping. Have fun. */
        other(0x5);

        companion object {
            fun of(i: Int) = values().first { it.i == i }
        }
    }

    // ---------------------------------------------------------------------------
    /** @brief Defines the purpose of a texture
     *
     *  This is a very difficult topic. Different 3D packages support different kinds of textures. For very common texture
     *  types, such as bumpmaps, the rendering results depend on implementation details in the rendering pipelines of these
     *  applications. Assimp loads all texture references from the model file and tries to determine which of the predefined
     *  texture types below is the best choice to match the original use of the texture as closely as possible.<br>
     *
     *  In content pipelines you'll usually define how textures have to be handled, and the artists working on models have
     *  to conform to this specification, regardless which 3D tool they're using. */
    enum class Type(val i: Int) {

        /** Dummy value.
         *
         *  No texture, but the value to be used as 'texture semantic' (#aiMaterialProperty::mSemantic) for all material
         *  properties *not* related to textures.     */
        none(0x0),


        /** The texture is combined with the result of the diffuse lighting equation.     */
        diffuse(0x1),

        /** The texture is combined with the result of the specular lighting equation.     */
        specular(0x2),

        /** The texture is combined with the result of the ambient lighting equation.     */
        ambient(0x3),

        /** The texture is added to the result of the lighting calculation. It isn't influenced by incoming light.     */
        emissive(0x4),

        /** The texture is a height map.
         *
         *  By convention, higher gray-scale values stand for higher elevations from the base height.     */
        height(0x5),

        /** The texture is a (tangent space) normal-map.
         *
         *  Again, there are several conventions for tangent-space normal maps. Assimp does (intentionally) not distinguish
         *  here.     */
        normals(0x6),

        /** The texture defines the glossiness of the material.
         *
         *  The glossiness is in fact the exponent of the specular (phong) lighting equation. Usually there is a conversion
         *  function defined to map the linear color values in the texture to a suitable exponent. Have fun.     */
        shininess(0x7),

        /** The texture defines per-pixel opacity.
         *
         *  Usually 'white' means opaque and 'black' means 'transparency'. Or quite the opposite. Have fun.     */
        opacity(0x8),

        /** Displacement texture
         *
         *  The exact purpose and format is application-dependent.
         *  Higher color values stand for higher vertex displacements.     */
        displacement(0x9),

        /** Lightmap texture (aka Ambient Occlusion)
         *
         *  Both 'Lightmaps' and dedicated 'ambient occlusion maps' are covered by this material property. The texture
         *  contains a scaling value for the final color value of a pixel. Its intensity is not affected by incoming light.     */
        lightmap(0xA),

        /** Reflection texture
         *
         * Contains the color of a perfect mirror reflection.
         * Rarely used, almost never for real-time applications.     */
        reflection(0xB),

        /** Unknown texture
         *
         *  A texture reference that does not match any of the definitions above is considered to be 'unknown'. It is still
         *  imported, but is excluded from any further postprocessing.     */
        unknown(0xC)
    }

    // ---------------------------------------------------------------------------
    /** @brief Defines some mixed flags for a particular texture.
     *
     *  Usually you'll instruct your cg artists how textures have to look like ... and how they will be processed in your
     *  application. However, if you use Assimp for completely generic loading purposes you might also need to process these
     *  flags in order to display as many 'unknown' 3D models as possible correctly.
     *
     *  This corresponds to the #AI_MATKEY_TEXFLAGS property. */
    enum class Flags(val i: Int) {

        /** The texture's color values have to be inverted (componentwise 1-n)     */
        invert(0x1),

        /** Explicit request to the application to process the alpha channel of the texture.
         *
         *  Mutually exclusive with #aiTextureFlags_IgnoreAlpha. These flags are set if the library can say for sure that
         *  the alpha channel is used/is not used. If the model format does not define this, it is left to the application
         *  to decide whether the texture alpha channel - if any - is evaluated or not.     */
        useAlpha(0x2),

        /** Explicit request to the application to ignore the alpha channel of the texture.
         *
         *  Mutually exclusive with #aiTextureFlags_UseAlpha.     */
        ignoreAlpha(0x4)
    }

    companion object {
        val size = 2 * Int.BYTES
    }
}


// ---------------------------------------------------------------------------
/** @brief Defines all shading models supported by the library
 *
 *  The list of shading modes has been taken from Blender.
 *  See Blender documentation for more information. The API does not distinguish between "specular" and "diffuse"
 *  shaders (thus the specular term for diffuse shading models like Oren-Nayar remains undefined). <br>
 *  Again, this value is just a hint. Assimp tries to select the shader whose most common implementation matches the
 *  original rendering results of the 3D modeller which wrote a particular model as closely as possible.     */
enum class AiShadingMode(val i: Int) {

    /** Flat shading. Shading is done on per-face base, diffuse only. Also known as 'faceted shading'.     */
    flat(0x1),

    /** Simple Gouraud shading.     */
    gouraud(0x2),

    /** Phong-Shading -     */
    phong(0x3),

    /** Phong-Blinn-Shading     */
    blinn(0x4),

    /** Toon-Shading per pixel
     *
     *  Also known as 'comic' shader.     */
    toon(0x5),

    /** OrenNayar-Shading per pixel
     *
     *  Extension to standard Lambertian shading, taking the roughness of the material into account     */
    orenNayar(0x6),

    /** Minnaert-Shading per pixel
     *
     *  Extension to standard Lambertian shading, taking the "darkness" of the material into account     */
    minnaert(0x7),

    /** CookTorrance-Shading per pixel
     *
     *  Special shader for metallic surfaces.     */
    cookTorrance(0x8),

    /** No shading at all. Constant light influence of 1.0.     */
    noShading(0x9),

    /** Fresnel shading     */
    fresnel(0xa);

    companion object {
        fun of(i: Int) = values().first { it.i == i }
    }
}


// ---------------------------------------------------------------------------
/** @brief Defines alpha-blend flags.
 *
 *  If you're familiar with OpenGL or D3D, these flags aren't new to you.
 *  They define *how* the final color value of a pixel is computed, basing on the previous color at that pixel and the
 *  new color value from the material.
 *  The blend formula is:
 *  @code
 *    SourceColor * SourceBlend + DestColor * DestBlend
 *  @endcode
 *  where DestColor is the previous color in the framebuffer at this position and SourceColor is the material color
 *  before the transparency calculation.<br>
 *  This corresponds to the #AI_MATKEY_BLEND_FUNC property. */
enum class AiBlendMode(val i: Int) {

    /**
     *  Formula:
     *  @code
     *  SourceColor*SourceAlpha + DestColor*(1-SourceAlpha)
     *  @endcode     */
    default(0x0),

    /** Additive blending
     *
     *  Formula:
     *  @code
     *  SourceColor*1 + DestColor*1
     *  @endcode     */
    additive(0x1);

    companion object {
        fun of(i: Int) = values().first { it.i == i }
    }

    // we don't need more for the moment, but we might need them in future versions ...
}

// ---------------------------------------------------------------------------
/** @brief Defines how an UV channel is transformed.
 *
 *  This is just a helper structure for the #AI_MATKEY_UVTRANSFORM key.
 *  See its documentation for more details.
 *
 *  Typically you'll want to build a matrix of this information. However, we keep separate scaling/translation/rotation
 *  values to make it easier to process and optimize UV transformations internally.
 */
data class AiUVTransform(

        /** Translation on the u and v axes.
         *
         *  The default value is (0|0).         */
        var translation: AiVector2D = AiVector2D(),

        /** Scaling on the u and v axes.
         *
         *  The default value is (1|1).         */
        var scaling: AiVector2D = AiVector2D(),

        /** Rotation - in counter-clockwise direction.
         *
         *  The rotation angle is specified in radians. The rotation center is 0.5f|0.5f. The default value 0.f.         */
        var rotation: Float = 0f
)

data class AiMaterial(

        var name: String? = null,

        var twoSided: Boolean? = null,

        var shadingModel: AiShadingMode? = null,

        var wireframe: Boolean? = null,

        var blendFunc: AiBlendMode? = null,

        var opacity: Float? = null,

        var bumpScaling: Float? = null, // TODO unsure

        var shininess: Float? = null,

        var reflectivity: Float? = null, // TODO unsure

        var shininessStrength: Float? = null,

        var refracti: Float? = null,

        var color: AiMaterial.Color? = null,

        var displacementScaling: Float? = null,

        var textures: MutableList<AiMaterial.Texture> = mutableListOf()

        // TODO const AI_MATKEY_GLOBAL_BACKGROUND_IMAGE = '?bg.global';
) {
    constructor(other: AiMaterial) : this(other.name, other.twoSided, other.shadingModel, other.wireframe,
            other.blendFunc, other.opacity, other.bumpScaling, other.shininess, other.reflectivity,
            other.shininessStrength, other.refracti, if (other.color == null) null else Color(other.color!!),
            other.displacementScaling, MutableList(other.textures.size, { Texture(other.textures[it]) }))

    data class Color(

            var diffuse: AiColor3D? = null,

            var ambient: AiColor3D? = null,

            var specular: AiColor3D? = null,

            var emissive: AiColor3D? = null,

            var transparent: AiColor3D? = null,

            var reflective: AiColor3D? = null // TODO unsure
    ) {
        constructor(other: Color) : this(
                if (other.diffuse == null) null else AiColor3D(other.diffuse!!),
                if (other.ambient == null) null else AiColor3D(other.ambient!!),
                if (other.specular == null) null else AiColor3D(other.specular!!),
                if (other.emissive == null) null else AiColor3D(other.emissive!!),
                if (other.transparent == null) null else AiColor3D(other.transparent!!),
                if (other.reflective == null) null else AiColor3D(other.reflective!!))

        companion object {
            val size = 6 * Vec3.size
        }
    }

    data class Texture(

            var type: AiTexture.Type? = null,

            var file: String? = null,

            var blend: Float? = null,

            var op: AiTexture.Op? = null,

            var mapping: AiTexture.Mapping? = null,

            var uvwsrc: Int? = null,

            var mapModeU: AiTexture.MapMode? = null,

            var mapModeV: AiTexture.MapMode? = null,

            var mapAxis: AiVector3D? = null,

            var flags: Int? = null,

            var uvTrafo: AiUVTransform? = null
    ) {
        constructor(other: Texture) : this(other.type, other.file, other.blend, other.op, other.mapping, other.uvwsrc,
                other.mapModeU, other.mapModeV, other.mapAxis, other.flags, other.uvTrafo)

        companion object {
            val size = 8 * Int.BYTES + Float.BYTES + Vec3.size
        }
    }

    companion object {
        val size = 4 * Int.BYTES + 6 * Float.BYTES + Color.size + Texture.size
    }
}