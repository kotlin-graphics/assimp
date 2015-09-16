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
public enum AiPrimitiveType {

    /**
     * A point primitive.
     *
     * This is just a single vertex in the virtual world, #aiFace contains just
     * one index for such a primitive.
     */
    aiPrimitiveType_POINT(0x1),
    /**
     * A line primitive.
     *
     * This is a line defined through a start and an end position. #aiFace
     * contains exactly two indices for such a primitive.
     */
    aiPrimitiveType_LINE(0x2),
    /**
     * A triangular primitive.
     *
     * A triangle consists of three indices.
     */
    aiPrimitiveType_TRIANGLE(0x4),
    /**
     * A higher-level polygon with more than 3 edges.
     *
     * A triangle is a polygon, but polygon in this context means "all polygons
     * that are not triangles". The "Triangulate"-Step is provided for your
     * convenience, it splits all polygons in triangles (which are much easier
     * to handle).
     */
    aiPrimitiveType_POLYGON(0x8);

    public int value;

    private AiPrimitiveType(int value) {
        this.value = value;
    }
}
