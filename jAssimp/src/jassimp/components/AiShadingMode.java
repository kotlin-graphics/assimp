/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components;

/**
 *
 * @author gbarbieri
 */
public enum AiShadingMode {

    /**
     * Flat shading. Shading is done on per-face base, diffuse only. Also known
     * as 'faceted shading'.
     */
    Flat(0x1),
    /**
     * Simple Gouraud shading.
     */
    Gouraud(0x2),
    /**
     * Phong-Shading -
     */
    Phong(0x3),
    /**
     * Phong-Blinn-Shading
     */
    Blinn(0x4),
    /**
     * Toon-Shading per pixel
     *
     * Also known as 'comic' shader.
     */
    Toon(0x5),
    /**
     * OrenNayar-Shading per pixel
     *
     * Extension to standard Lambertian shading, taking the roughness of the
     * material into account
     */
    OrenNayar(0x6),
    /**
     * Minnaert-Shading per pixel
     *
     * Extension to standard Lambertian shading, taking the "darkness" of the
     * material into account
     */
    Minnaert(0x7),
    /**
     * CookTorrance-Shading per pixel
     *
     * Special shader for metallic surfaces.
     */
    CookTorrance(0x8),
    /**
     * No shading at all. Constant light influence of 1.0.
     */
    NoShading(0x9),
    /**
     * Fresnel shading
     */
    Fresnel(0xa);

    public final int value;

    private AiShadingMode(int value) {
        this.value = value;
    }
}
