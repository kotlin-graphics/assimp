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
public class AiShadingMode {

    /** Flat shading. Shading is done on per-face base,
     *  diffuse only. Also known as 'faceted shading'.
     */
    public static int Flat = 0x1;
    /** Simple Gouraud shading.
     */
    public static int Gouraud = 0x2;
    /** Phong-Shading -
     */
    public static int Phong = 0x3;
    /** Phong-Blinn-Shading
     */
    public static int Blinn = 0x4;
    /** Toon-Shading per pixel
     *
     *  Also known as 'comic' shader.
     */
    public static int Toon = 0x5;
    /** OrenNayar-Shading per pixel
     *
     *  Extension to standard Lambertian shading, taking the
     *  roughness of the material into account
     */
    public static int OrenNayar = 0x6;
    /** Minnaert-Shading per pixel
     *
     *  Extension to standard Lambertian shading, taking the
     *  "darkness" of the material into account
     */
    public static int Minnaert = 0x7;
    /** CookTorrance-Shading per pixel
     *
     *  Special shader for metallic surfaces.
     */
    public static int CookTorrance = 0x8;
    /** No shading at all. Constant light influence of 1.0.
     */
    public static int NoShading = 0x9;
    /** Fresnel shading
     */
    public static int Fresnel = 0xa;
}
