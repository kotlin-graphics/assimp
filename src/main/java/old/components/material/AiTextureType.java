/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assimp.components.material;

/**
 *
 * Defines the purpose of a texture
 *
 * This is a very difficult topic. Different 3D packages support different kinds
 * of textures. For very common texture types, such as bumpmaps, the rendering
 * results depend on implementation details in the rendering pipelines of these
 * applications. Assimp loads all texture references from the model file and
 * tries to determine which of the predefined texture types below is the best
 * choice to match the original use of the texture as closely as possible.<br>
 *
 * In content pipelines you'll usually define how textures have to be handled,
 * and the artists working on models have to conform to this specification,
 * regardless which 3D tool they're using.
 *
 * @author elect
 */
public enum AiTextureType {

    /**
     * Dummy value.
     *
     * No texture, but the value to be used as 'texture semantic'
     * (#aiMaterialProperty::mSemantic) for all material properties *not*
     * related to textures.
     */
    NONE(0x0),
    /**
     * The texture is combined with the result of the diffuse lighting equation.
     */
    DIFFUSE(0x1),
    /**
     * The texture is combined with the result of the specular lighting
     * equation.
     */
    SPECULAR(0x2),
    /**
     * The texture is combined with the result of the ambient lighting equation.
     */
    AMBIENT(0x3),
    /**
     * The texture is added to the result of the lighting calculation. It isn't
     * influenced by incoming light.
     */
    EMISSIVE(0x4),
    /**
     * The texture is a height map.
     *
     * By convention, higher gray-scale values stand for higher elevations from
     * the base height.
     */
    HEIGHT(0x5),
    /**
     * The texture is a (tangent space) normal-map.
     *
     * Again, there are several conventions for tangent-space normal maps.
     * Assimp does (intentionally) not distinguish here.
     */
    NORMALS(0x6),
    /**
     * The texture defines the glossiness of the material.
     *
     * The glossiness is in fact the exponent of the specular (phong) lighting
     * equation. Usually there is a conversion function defined to map the
     * linear color values in the texture to a suitable exponent. Have fun.
     */
    SHININESS(0x7),
    /**
     * The texture defines per-pixel opacity.
     *
     * Usually 'white' means opaque and 'black' means 'transparency'. Or quite
     * the opposite. Have fun.
     */
    OPACITY(0x8),
    /**
     * Displacement texture
     *
     * The exact purpose and format is application-dependent. Higher color
     * values stand for higher vertex displacements.
     */
    DISPLACEMENT(0x9),
    /**
     * Lightmap texture (aka Ambient Occlusion)
     *
     * Both 'Lightmaps' and dedicated 'ambient occlusion maps' are covered by
     * this material property. The texture contains a scaling value for the
     * final color value of a pixel. Its intensity is not affected by incoming
     * light.
     */
    LIGHTMAP(0xA),
    /**
     * Reflection texture
     *
     * Contains the color of a perfect mirror reflection. Rarely used, almost
     * never for real-time applications.
     */
    REFLECTION(0xB),
    /**
     * Unknown texture
     *
     * A texture reference that does not match any of the definitions above is
     * considered to be 'unknown'. It is still imported, but is excluded from
     * any further postprocessing.
     */
    UNKNOWN(0xC);

    public int value;

    private AiTextureType(int value) {
        this.value = value;
    }
}
