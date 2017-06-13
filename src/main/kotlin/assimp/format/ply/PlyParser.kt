package assimp.format.ply

import glm_.d
import glm_.f
import glm_.i
import assimp.*
import glm_.ui
import java.nio.ByteBuffer

/**
 * Created by elect on 10/12/2016.
 */

// ---------------------------------------------------------------------------------
/*
name        type        number of bytes
---------------------------------------
char       character                 1
uchar      unsigned character        1
short      short integer             2
ushort     unsigned short integer    2
int        integer                   4
uint       unsigned integer          4
float      single-precision float    4
double     double-precision float    8

int8
int16
uint8 ... forms are also used
*/
enum class EDataType {
    Char,
    UChar,
    Short,
    UShort,
    Int,
    UInt,
    Float,
    Double,

    // Marks invalid entries
    INVALID;

    companion object {

        fun of(string: String) = when (string) {
            "char", "int8" -> Char
            "uchar", "uint8" -> UChar
            "short", "int16" -> Short
            "ushort", "uint16" -> UShort
            "int32", "int" -> Int
            "uint32", "uint" -> UInt
            "float", "float32" -> Float
            "double64", "double", "float64" -> Double
            else -> {
                println("Found unknown data type in PLY file. This is OK")
                INVALID
            }
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Specifies semantics for PLY element properties
 *
 * Semantics define the usage of a property, e.g. x coordinate
 */
enum class ESemantic {
    //! vertex position x coordinate
    XCoord,
    //! vertex position x coordinate
    YCoord,
    //! vertex position x coordinate
    ZCoord,

    //! vertex normal x coordinate
    XNormal,
    //! vertex normal y coordinate
    YNormal,
    //! vertex normal z coordinate
    ZNormal,

    //! u texture coordinate
    UTextureCoord,
    //! v texture coordinate
    VTextureCoord,

    //! vertex colors, red channel
    Red,
    //! vertex colors, green channel
    Green,
    //! vertex colors, blue channel
    Blue,
    //! vertex colors, alpha channel
    Alpha,

    //! vertex index list
    VertexIndex,

    //! texture index
    TextureIndex,

    //! texture coordinates (stored as element of a face)
    TextureCoordinates,

    //! material index
    MaterialIndex,

    //! ambient color, red channel
    AmbientRed,
    //! ambient color, green channel
    AmbientGreen,
    //! ambient color, blue channel
    AmbientBlue,
    //! ambient color, alpha channel
    AmbientAlpha,

    //! diffuse color, red channel
    DiffuseRed,
    //! diffuse color, green channel
    DiffuseGreen,
    //! diffuse color, blue channel
    DiffuseBlue,
    //! diffuse color, alpha channel
    DiffuseAlpha,

    //! specular color, red channel
    SpecularRed,
    //! specular color, green channel
    SpecularGreen,
    //! specular color, blue channel
    SpecularBlue,
    //! specular color, alpha channel
    SpecularAlpha,

    //! specular power for phong shading
    PhongPower,

    //! opacity between 0 and 1
    Opacity,

    //! Marks invalid entries
    INVALID;

    companion object {

        fun of(string: String) = when (string) {
            "red", "r" -> Red
            "green", "g" -> Green
            "blue", "b" -> Blue
            "alpha" -> Alpha
            "vertex_index", "vertex_indices" -> VertexIndex
            "material_index" -> MaterialIndex
            "ambient_red" -> AmbientRed
            "ambient_green" -> AmbientGreen
            "ambient_blue" -> AmbientBlue
            "ambient_alpha" -> AmbientAlpha
            "diffuse_red" -> DiffuseRed
            "diffuse_green" -> DiffuseGreen
            "diffuse_blue" -> DiffuseBlue
            "diffuse_alpha" -> DiffuseAlpha
            "specular_red" -> SpecularRed
            "specular_green" -> SpecularGreen
            "specular_blue" -> SpecularBlue
            "specular_alpha" -> SpecularAlpha
            "opacity" -> Opacity
            "specular_power" -> PhongPower
        // NOTE: Blender3D exports texture coordinates as s,t tuples
            "u", "s", "tx" -> UTextureCoord
            "v", "t", "ty" -> VTextureCoord
            "x" -> XCoord
            "y" -> YCoord
            "z" -> ZCoord
            "nx" -> XNormal
            "ny" -> YNormal
            "nz" -> ZNormal
            else -> {
                println("Found unknown property semantic in file. This is ok")
                INVALID
            }
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Specifies semantics for PLY elements
 *
 * Semantics define the usage of an element, e.g. vertex or material
 */
enum class EElementSemantic {
    //! The element is a vertex
    Vertex,

    //! The element is a face description (index table)
    Face,

    //! The element is a tristrip description (index table)
    TriStrip,

    //! The element is an edge description (ignored)
    Edge,

    //! The element is a material description
    Material,

    //! Marks invalid entries
    INVALID;

    companion object {

        // -------------------------------------------------------------------
        //! Parse a semantic from a string
        fun of(string: String) = when (string) {
            "vertex" -> Vertex
        // TODO: maybe implement this?
            "face" /*, "range_grid" */ -> Face
            "tristrips" -> TriStrip
            "edge" -> Edge
            "material" -> Material
            else -> INVALID
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Helper class for a property in a PLY file.
 *
 * This can e.g. be a part of the vertex declaration
 */
class Property(

        //! Data type of the property
        var eType: EDataType = EDataType.Int,
        //! Semantical meaning of the property
        var semantic: ESemantic = ESemantic.INVALID,
        //! Of the semantic of the property could not be parsed: Contains the semantic specified in the file
        var szName: String = "",
        //! Specifies whether the data type is a list where the first element specifies the size of the list
        var bIsList: Boolean = false,
        var eFirstType: EDataType = EDataType.UChar
) {

    companion object {

        // -------------------------------------------------------------------
        //! Parse a property from a string. The end of the string is either '\n', '\r' or '\0'. Return value is false
        //! if the input string is NOT a valid property (E.g. does not start with the "property" keyword)
        fun parseProperty(buffer: ByteBuffer, pOut: Property): Boolean {

            // Forms supported:
            // "property float x"
            // "property list uchar int vertex_index"

            // skip leading spaces
            if (!buffer.skipSpaces()) return false

            // skip the "property" string at the beginning
            val nextWord = buffer.nextWord()
            if (nextWord != "property") {
                // seems not to be a valid property entry, reset pointer
                buffer.position(buffer.position() - nextWord.length)
                return false
            }


            // get next word
            if (!buffer.skipSpaces()) return false
            var token = buffer.nextWord()
            if (token == "list") {

                pOut.bIsList = true

                // seems to be a list.
                pOut.eFirstType = EDataType.of(buffer.nextWord())
                if (pOut.eFirstType == EDataType.INVALID) {
                    // unable to parse list size data type
                    buffer.skipLine()
                    return false
                }

                if (!buffer.skipSpaces()) return false
                pOut.eType = EDataType.of(buffer.nextWord())
                if (pOut.eType == EDataType.INVALID) {
                    // unable to parse list data type
                    buffer.skipLine()
                    return false
                }
            } else {
                pOut.eType = EDataType.of(token)
                if (pOut.eType == EDataType.INVALID) {
                    // unable to parse data type. Skip the property
                    buffer.skipLine()
                    return false
                }
            }

            if (!buffer.skipSpaces()) return false

            token = buffer.nextWord()
            pOut.semantic = ESemantic.of(token)
            if (pOut.semantic == ESemantic.INVALID) {

                buffer.skipLine()
                // store the name of the semantic
                pOut.szName = token
            }

            buffer.skipSpacesAndLineEnd()
            return true
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Helper class for an element in a PLY file.
 *
 * This can e.g. be the vertex declaration. Elements contain a well-defined number of properties.
 */
class Element(
        //! List of properties assigned to the element, support operator[]
        var alProperties: MutableList<Property> = ArrayList(),
        //! Semantic of the element
        var eSemantic: EElementSemantic = EElementSemantic.INVALID,
        //! Of the semantic of the element could not be parsed: Contains the semantic specified in the file
        var szName: String? = null,
        //! How many times will the element occur?
        var numOccur: Int = 0
) {

    companion object {
        // -------------------------------------------------------------------
        //! Parse an element from a string.
        //! The function will parse all properties contained in the element, too.
        fun parseElement(buffer: ByteBuffer, pOut: Element): Boolean {

            // skip leading spaces
            if (!buffer.skipSpaces()) return false

            // skip the "element" string at the beginning

            val nextWord = buffer.nextWord()
            if (nextWord != "element") {
                // seems not to be a valid property entry, reset pointer
                buffer.position(buffer.position() - nextWord.length)
                return false
            }

            // get next word
            if (!buffer.skipSpaces()) return false

            // parse the semantic of the element
            val token = buffer.nextWord()
            pOut.eSemantic = EElementSemantic.of(token)
            if (pOut.eSemantic == EElementSemantic.INVALID)
            // if the exact semantic can't be determined, just store the original string identifier
                pOut.szName = token

            if (!buffer.skipSpaces()) return false

            //parse the number of occurrences of this element
            pOut.numOccur = buffer.nextWord().toInt()

            // go to the next line
            buffer.skipSpacesAndLineEnd()

            // now parse all properties of the element
            while (true) {
                // skip all comments
                DOM.skipComments(buffer)

                val prop = Property()
                if (!Property.parseProperty(buffer, prop)) break
                pOut.alProperties.add(prop)
            }
            return true
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Instance of a property in a PLY file
 */
class PropertyInstance(

        // -------------------------------------------------------------------
        //! List of all values parsed. Contains only one value for non-list properties
        val avList: ArrayList<Number> = ArrayList()
) {


    companion object {
        // -------------------------------------------------------------------
        //! Parse a property instance
        fun parseInstance(buffer: ByteBuffer, prop: Property, p_pcOut: MutableList<PropertyInstance>): Boolean {

            // skip spaces at the beginning
            if (!buffer.skipSpaces()) return false

            p_pcOut.add(PropertyInstance())
            if (prop.bIsList) {

                // parse all list elements
                val words = buffer.restOfLine().words
                val iNum = words[0].ui
                repeat(iNum.v) {
                    parseValue(words[it + 1], prop.eType, p_pcOut.last().avList)
                }
            } else
            // parse the property
                parseValue(buffer.nextWord(), prop.eType, p_pcOut.last().avList)

            buffer.skipSpacesAndLineEnd()
            return true
        }

        // -------------------------------------------------------------------
        //! Parse a property instance in binary format
        fun parseInstanceBinary(buffer: ByteBuffer, prop: Property, p_pcOut: MutableList<PropertyInstance>): Boolean {

            p_pcOut.add(PropertyInstance())
            if (prop.bIsList) {

                // parse the number of elements in the list
                val iNum: MutableList<Number> = mutableListOf()
                parseValueBinary(buffer, prop.eType, iNum)
                repeat(iNum[0].ui.v) {
                    parseValueBinary(buffer, prop.eType, p_pcOut.last().avList)
                }
            } else
            // parse the property
                parseValueBinary(buffer, prop.eType, p_pcOut.last().avList)
            return true
        }

        // -------------------------------------------------------------------
        //! Parse a value
        fun parseValue(value: String, eType: EDataType, out: MutableList<Number>): Boolean {

            val v: Number = when (eType) {
                EDataType.UInt, EDataType.UShort, EDataType.UChar -> value.ui
                EDataType.Int, EDataType.Short, EDataType.Char -> value.i
            // technically this should cast to float, but people tend to use float descriptors for double data this is the
            // best way to not risk loosing precision on import and it doesn't hurt to do this
                EDataType.Float -> value.f
                EDataType.Double -> value.d
                else -> return false
            }
            return out.add(v)
        }

        // -------------------------------------------------------------------
        //! Parse a binary value
        fun parseValueBinary(buffer: ByteBuffer, eType: EDataType, out: MutableList<Number>): Boolean {

            val v: Number = when (eType) {
                EDataType.UInt, EDataType.Int -> buffer.int.ui
                EDataType.UShort, EDataType.Short -> buffer.short.ui
                EDataType.UChar, EDataType.Char -> buffer.get().ui
            // technically this should cast to float, but people tend to use float descriptors for double data this is the
            // best way to not risk loosing precision on import and it doesn't hurt to do this
                EDataType.Float -> buffer.float
                EDataType.Double -> buffer.double
                else -> return false
            }
            return out.add(v)
        }

        // -------------------------------------------------------------------
        //! Parse a binary value

        // -------------------------------------------------------------------
        //! Get the default value for a given data type
        fun defaultValue(eType: EDataType): Number = when (eType) {
            EDataType.Float -> 0f
            EDataType.Double -> 0.0
            else -> 0
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Class for an element instance in a PLY file
 */
class ElementInstance(
        //! List of all parsed properties
        val alProperties: ArrayList<PropertyInstance> = ArrayList()
) {

    companion object {
        // -------------------------------------------------------------------
        //! Parse an element instance
        fun parseInstance(buffer: ByteBuffer, pcElement: Element, p_pcOut: MutableList<ElementInstance>): Boolean {

            if (!buffer.skipSpaces()) return false

            p_pcOut.add(ElementInstance())
            pcElement.alProperties.forEach {
                if (!PropertyInstance.parseInstance(buffer, it, p_pcOut.last().alProperties)) {

                    System.err.println("Unable to parse property instance. Skipping this element instance")

                    // skip the rest of the instance
                    buffer.skipLine()

                    val defaultProperty = PropertyInstance()
                    defaultProperty.avList.add(PropertyInstance.defaultValue(it.eType))

                    p_pcOut.last().alProperties.add(defaultProperty)
                }
            }
            return true
        }

        fun parseInstanceBinary(buffer: ByteBuffer, pcElement: Element, p_pcOut: MutableList<ElementInstance>): Boolean {

            p_pcOut.add(ElementInstance())
            pcElement.alProperties.forEach {
                if (!PropertyInstance.parseInstanceBinary(buffer, it, p_pcOut.last().alProperties)) {

                    System.err.println("Unable to parse binary property instance. Skipping this element instance")

                    val defaultProperty = PropertyInstance()
                    defaultProperty.avList.add(PropertyInstance.defaultValue(it.eType))

                    p_pcOut.last().alProperties.add(defaultProperty)
                }
            }
            return true
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Class for an element instance list in a PLY file
 */
class ElementInstanceList(
        //! List of all element instances
        val alInstances: MutableList<ElementInstance> = ArrayList()
) {

    companion object {
        // -------------------------------------------------------------------
        //! Parse an element instance list
        fun parseInstanceList(buffer: ByteBuffer, pcElement: Element, p_pcOut: MutableList<ElementInstanceList>): Boolean {

            if (pcElement.eSemantic == EElementSemantic.INVALID || pcElement.alProperties.isEmpty())
            // if the element has an unknown semantic we can skip all lines
            // However, there could be comments
                repeat(pcElement.numOccur) { DOM.skipComments(buffer); buffer.skipLine() }
            else {
                p_pcOut.add(ElementInstanceList())
                // be sure to have enough storage
                repeat(pcElement.numOccur) {
                    DOM.skipComments(buffer)
                    ElementInstance.parseInstance(buffer, pcElement, p_pcOut.last().alInstances)
                }
            }
            return true
        }

        // -------------------------------------------------------------------
        //! Parse a binary element instance list
        fun parseInstanceListBinary(buffer: ByteBuffer, pcElement: Element, p_pcOut: MutableList<ElementInstanceList>) {
            /**
             * we can add special handling code for unknown element semantics since we can't skip it as a whole block
             * (we don't know its exact size due to the fact that lists could be contained in the property list of the
             * unknown element)
             */
            p_pcOut.add(ElementInstanceList())
            repeat(pcElement.numOccur) {
//                if(it == 29)
//                    println("block")
//                if(buffer.position() != 276 + it * 31) {
//                    println("different, it: $it, position: ${buffer.position()}")
//                }
                ElementInstance.parseInstanceBinary(buffer, pcElement, p_pcOut.last().alInstances)
            }
        }
    }
}

// ---------------------------------------------------------------------------------
/** \brief Class to represent the document object model of an ASCII or binary
 * (both little and big-endian) PLY file
 */
class DOM(
        //! Contains all elements of the file format
        val alElements: MutableList<Element> = ArrayList(),
        //! Contains the real data of each element's instance list
        val alElementData: MutableList<ElementInstanceList> = ArrayList()
) {

    companion object {
        //! Parse the DOM for a PLY file. The input string is assumed
        //! to be terminated with zero
        fun parseInstance(buffer: ByteBuffer, p_pcOut: DOM): Boolean {

            println("PLY::DOM::ParseInstance() begin")

            if (!p_pcOut.parseHeader(buffer, false)) {

                println("PLY::DOM::ParseInstance() failure")
                return false
            }
            if (!p_pcOut.parseElementInstanceLists(buffer)) {
                println("PLY::DOM::ParseInstance() failure")
                return false
            }
            println("PLY::DOM::ParseInstance() succeeded")
            return true
        }

        //! Skip all comment lines after this
        fun skipComments(buffer: ByteBuffer): Boolean {
            val position = buffer.position()
            // skip spaces
            if (buffer.nextWord() == "comment") {
                buffer.skipLine()
                skipComments(buffer)
                return true
            }
            buffer.position(position)
            return false
        }

        fun parseInstanceBinary(buffer: ByteBuffer, p_pcOut: DOM): Boolean {

            println("PLY::DOM::ParseInstanceBinary() failure")

            if (!p_pcOut.parseHeader(buffer, false)) {
                println("PLY::DOM::ParseInstanceBinary() failure")
                return false
            }
            if (!p_pcOut.parseElementInstanceListsBinary(buffer)) {
                println("PLY::DOM::ParseInstanceBinary() failure")
                return false
            }
            println("PLY::DOM::ParseInstanceBinary() succeeded")
            return true
        }
    }


    // -------------------------------------------------------------------
    //! Handle the file header and read all element descriptions
    fun parseHeader(buffer: ByteBuffer, isBinary: Boolean): Boolean {

        println("PLY::DOM::ParseHeader() begin")

        // parse all elements
        while (true) {

            // skip all comments
            skipComments(buffer)

            val out = Element()
            if (Element.parseElement(buffer, out))
            // add the element to the list of elements
                alElements.add(out)
            else if (buffer.nextWord() == "end_header")
            // we have reached the end of the header
                break
            else
            // ignore unknown header elements
                buffer.skipLine()
        }
        if (!isBinary)
        // it would occur an error, if binary data start with values as space or line end.
            buffer.skipSpacesAndLineEnd()

        println("PLY::DOM::ParseHeader() succeeded")
        return true
    }

    // -------------------------------------------------------------------
    //! Read in all element instance lists
    fun parseElementInstanceLists(buffer: ByteBuffer): Boolean {

        println("PLY::DOM::ParseElementInstanceLists() begin")

        // parse all element instances
        alElements.forEach {
            ElementInstanceList.parseInstanceList(buffer, it, alElementData)
        }

        println("PLY::DOM::ParseElementInstanceLists() succeeded")
        return true
    }


    // -------------------------------------------------------------------
    //! Read in all element instance lists for a binary file format
    fun parseElementInstanceListsBinary(buffer: ByteBuffer): Boolean {

        println("PLY::DOM::ParseElementInstanceListsBinary() begin")

        // parse all element instances
        alElements.forEach {
            ElementInstanceList.parseInstanceListBinary(buffer, it, alElementData)
        }

        println("PLY::DOM::ParseElementInstanceListsBinary() succeeded")
        return true
    }
}

/**
 * brief Helper class to represent a loaded PLY face
 */
class Face(
        //! Material index
        var iMaterialIndex: Int = 0xFFFFFFFF.i,
        //! List of vertex indices
        var mIndices: IntArray = IntArray(3, { 0 })
)