/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/

package assimp.format.fbx

import assimp.AiMatrix4x4
import assimp.AiVector2D
import assimp.AiVector3D
import assimp.strncmp
import glm_.bool
import glm_.c
import glm_.i

/** @file  FBXDocument.h
 *  @brief FBX DOM  */

//#define _AI_CONCAT(a,b)  a ## b
//#define  AI_CONCAT(a,b)  _AI_CONCAT(a,b)

/** Represents a delay-parsed FBX objects. Many objects in the scene
 *  are not needed by assimp, so it makes no sense to parse them
 *  upfront. */
class LazyObject(val id: Long, val element: Element, val doc: Document) {

    var object_: Object? = null
    var flags = Flags.NULL.ordinal

    enum class Flags { NULL, BEING_CONSTRUCTED, FAILED_TO_CONSTRUCT }

    infix fun Int.or(f: LazyObject.Flags) = or(f.ordinal)

    fun get(dieOnError: Boolean = false): Object? {

        if (isBeingConstructed || failedToConstruct) return null

        if (object_ != null) return object_

        /*  if this is the root object, we return a dummy since there is no root object int he fbx file - it is just
            referenced with id 0. */
        if (id == 0L) {
            object_ = Object(id, element, "Model::RootNode")
            return object_
        }

        val key = element.keyToken
        val tokens = element.tokens

        if (tokens.size < 3) domError("expected at least 3 tokens: id, name and class tag", element)

        var name = tokens[1].parseAsString

        /*  small fix for binary reading: binary fbx files don't use prefixes such as Model:: in front of their names.
            The loading code expects this at many places, though!
            so convert the binary representation (a 0x0001) to the double colon notation. */
        if (tokens[1].isBinary)
            for (i in name.indices) {
                if (name[i].i == 0x0 && name[i + 1].i == 0x1)
                    name = name.substring(i + 2) + "::" + name.substring(0, i - 1) // TODO check
            }

        val classtag = tokens[2].parseAsString

        // prevent recursive calls
        flags = flags or Flags.BEING_CONSTRUCTED

        try {
            // this needs to be relatively fast since it happens a lot,
            // so avoid constructing strings all the time.
            val obType = key.begin
            val length = key.end - key.begin

            // For debugging
            //dumpObjectClassInfo( objtype, classtag )

            object_ =
                    when {
                        buffer.strncmp("Geometry", obType, length) -> MeshGeometry(id, element, name, doc).takeIf { classtag == "Mesh" }
                        buffer.strncmp("NodeAttribute", obType, length) -> when (classtag) {
                            "Camera" -> Camera(id, element, doc, name)
                            "CameraSwitcher" -> CameraSwitcher(id, element, doc, name)
                            "Light" -> Light(id, element, doc, name)
                            "Null" -> Null(id, element, doc, name)
                            "LimbNode" -> LimbNode(id, element, doc, name)
                            else -> null
                        }
                        buffer.strncmp("Deformer", obType, length) -> when (classtag) {
                            "Cluster" -> Cluster(id, element, doc, name)
                            "Skin" -> Skin(id, element, doc, name)
                            else -> null
                        }
                        buffer.strncmp("Model", obType, length) -> when (classtag) {
                            "IKEffector", "FKEffector" -> object_ // FK and IK effectors are not supported
                            else -> Model(id, element, doc, name)
                        }
                        buffer.strncmp("Material", obType, length) -> Material(id, element, doc, name)
                        buffer.strncmp("Texture", obType, length) -> Texture(id, element, doc, name)
                        buffer.strncmp("LayeredTexture", obType, length) -> LayeredTexture(id, element, doc, name)
                        buffer.strncmp("Video", obType, length) -> Video(id, element, doc, name)
                        buffer.strncmp("AnimationStack", obType, length) -> AnimationStack(id, element, name, doc)
                        buffer.strncmp("AnimationLayer", obType, length) -> AnimationLayer(id, element, name, doc)
                    // note: order matters for these two
                        buffer.strncmp("AnimationCurve", obType, length) -> AnimationCurve(id, element, name, doc)
                        buffer.strncmp("AnimationCurveNode", obType, length) -> AnimationCurveNode(id, element, name, doc)
                        else -> object_
                    }
        } catch (ex: Exception) {
            //            flags &= ~BEING_CONSTRUCTED;
//            flags |= FAILED_TO_CONSTRUCT;
//
//            if(dieOnError || doc.Settings().strictMode) {
//                throw;
//            }
//
//            // note: the error message is already formatted, so raw logging is ok
//            if(!DefaultLogger::isNullLogger()) {
//                DefaultLogger::get()->error(ex.what());
//            }
//            return NULL;
        }
//
//        if (!object.get()) {
//            //DOMError("failed to convert element to DOM object, class: " + classtag + ", name: " + name,&element);
//        }
//
//        flags &= ~BEING_CONSTRUCTED;
//        return object.get();
        return null
    }

//    inline fun <reified T> get(dieOnError: Boolean = false): T {
//        const Object * const ob = Get(dieOnError)
//        return ob ? dynamic_cast<const T*>(ob) : NULL
//    }

    val isBeingConstructed get() = flags == Flags.BEING_CONSTRUCTED
    val failedToConstruct get() = flags == Flags.FAILED_TO_CONSTRUCT
}


/** Base class for in-memory (DOM) representations of FBX objects */
open class Object(val id: Long, val element: Element, val name: String)


/** DOM class for generic FBX NoteAttribute blocks. NoteAttribute's just hold a property table,
 *  fixed members are added by deriving classes. */
open class NodeAttribute(id: Long, element: Element, doc: Document, name: String) : Object(id, element, name) {

    val props: PropertyTable

    init {
        val sc = element.scope

        val classname = element[2].parseAsString

        /*  hack on the deriving type but Null/LimbNode attributes are the only case in which the property table is by
            design absent and no warning should be generated for it. */
        val isNullOrLimb = classname == "Null" || classname == "LimbNode"
        props = getPropertyTable(doc, "NodeAttribute.Fbx$classname", element, sc, isNullOrLimb)
    }
}


/** DOM base class for FBX camera settings attached to a node */
class CameraSwitcher(id: Long, element: Element, doc: Document, name: String) : NodeAttribute(id, element, doc, name) {

    val cameraId = element.scope["CameraId"]?.let { it[0].parseAsInt } ?: 0
    val cameraName = element.scope["CameraName"]?.let { it[0].stringContents } ?: ""
    val cameraIndexName = element.scope["CameraIndex"]?.let { it[0].stringContents } ?: ""
}
//
//
//#define fbx_stringize(a) #a
//
//#define fbx_simple_property(name, type, default_value) \
//type name() const {
//    \
//    return PropertyGet<type>(Props(), fbx_stringize(name), (default_value)); \
//}
//
//// XXX improve logging
//#define fbx_simple_enum_property(name, type, default_value) \
//type name() const {
//    \
//    const int ival = PropertyGet<int>(Props(), fbx_stringize(name), static_cast<int>(default_value)); \
//    if (ival < 0 || ival >= AI_CONCAT(type, _MAX)) {
//        \
//        ai_assert(static_cast<int>(default_value) >= 0 && static_cast<int>(default_value) < AI_CONCAT(type, _MAX)); \
//        return static_cast<type>(default_value); \
//    } \
//    return static_cast<type>(ival); \
//}
//

/** DOM base class for FBX cameras attached to a node */
class Camera(id: Long, element: Element, doc: Document, name: String) : NodeAttribute(id, element, doc, name) {

    val position = props["Position"] ?: AiVector3D()
    val upVector = props["UpVector"] ?: AiVector3D(0, 1, 0)
    val interestPosition = props["InterestPosition"] ?: AiVector3D()

    val aspectWidth = props["AspectWidth"] ?: 1f
    val aspectHeight = props["AspectHeight"] ?: 1f
    val filmWidth = props["FilmWidth"] ?: 1f
    val filmHeight = props["FilmHeight"] ?: 1f

    val nearPlane = props["NearPlane"] ?: 0.1f
    val farPlane = props["FarPlane"] ?: 100f

    val filmAspectRatio = props["FilmAspectRatio"] ?: 1f
    val apertureMode = props["ApertureMode"] ?: 0

    val fieldOfView = props["FieldOfView"] ?: 1f
    val focalLength = props["FocalLength"] ?: 1f
}

/** DOM base class for FBX null markers attached to a node */
class Null(id: Long, element: Element, doc: Document, name: String) : NodeAttribute(id, element, doc, name)

/** DOM base class for FBX limb node markers attached to a node */
class LimbNode(id: Long, element: Element, doc: Document, name: String) : NodeAttribute(id, element, doc, name)

/** DOM base class for FBX lights attached to a node */
class Light(id: Long, element: Element, doc: Document, name: String) : NodeAttribute(id, element, doc, name) {

    enum class Type { Point, Directional, Spot, Area, Volume }

    enum class Decay { None, Linear, Quadratic, Cubic }

    val color = props["Color"] ?: AiVector3D(1)
    val type = props["Type"] ?: Type.Point
    val castLightOnObject = props["CastLightOnObject"] ?: false
    val drawVolumetricLight = props["DrawVolumetricLight"] ?: true
    val drawGroundProjection = props["DrawGroundProjection"] ?: true
    val drawFrontFacingVolumetricLight = props["DrawFrontFacingVolumetricLight"] ?: false
    val intensity = props["Intensity"] ?: 100f
    val innerAngle = props["InnerAngle"] ?: 0f
    val outerAngle = props["OuterAngle"] ?: 45f
    val fog = props["Fog"] ?: 50
    val decayType = props["DecayType"] ?: Decay.Quadratic
    val decayStart = props["DecayStart"] ?: 1f
    val fileName = props["FileName"] ?: ""

    val enableNearAttenuation = props["EnableNearAttenuation"] ?: false
    val nearAttenuationStart = props["NearAttenuationStart"] ?: 0f
    val nearAttenuationEnd = props["NearAttenuationEnd"] ?: 0f
    val enableFarAttenuation = props["EnableFarAttenuation"] ?: false
    val farAttenuationStart = props["FarAttenuationStart"] ?: 0f
    val farAttenuationEnd = props["FarAttenuationEnd"] ?: 0f

    val castShadows = props["CastShadows"] ?: true
    val shadowColor = props["ShadowColor"] ?: AiVector3D()

    val areaLightShape = props["AreaLightShape"] ?: 0

    val LeftBarnDoor = props["LeftBarnDoor"] ?: 20f
    val RightBarnDoor = props["RightBarnDoor"] ?: 20f
    val TopBarnDoor = props["TopBarnDoor"] ?: 20f
    val BottomBarnDoor = props["BottomBarnDoor"] ?: 20f
    val EnableBarnDoor = props["EnableBarnDoor"] ?: true
}

//
/** DOM base class for FBX models (even though its semantics are more "node" than "model" */
class Model(id: Long, element: Element, doc: Document, name: String) : Object(id, element, name) {

    val materials = ArrayList<Material>()
    val geometry = ArrayList<Geometry>()
    val attributes = ArrayList<NodeAttribute>()

    var shading = element.scope["Shading"]?.get(0)?.stringContents ?: ""
    var culling = element.scope["Culling"]?.get(0)?.stringContents ?: ""
    val props = getPropertyTable(doc, "Model.FbxNode", element, element.scope)

    init {
        resolveLinks(element, doc)
    }

    enum class RotOrder { EulerXYZ, EulerXZY, EulerYZX, EulerYXZ, EulerZXY, EulerZYX, SphericXYZ }

    enum class TransformInheritance { RrSs, RSrs, Rrs }

    val quaternionInterpolate get() = props["QuaternionInterpolate"] ?: 0

    val rotationOffset get() = props["RotationOffset"] ?: AiVector3D()
    val rotationPivot get() = props["RotationPivot"] ?: AiVector3D()
    val scalingOffset get() = props["ScalingOffset"] ?: AiVector3D()
    val scalingPivot get() = props["ScalingPivot"] ?: AiVector3D()
    val translationActive get() = props["TranslationActive"] ?: false

    val translationMin get() = props["TranslationMin"] ?: AiVector3D()
    val translationMax get() = props["TranslationMax"] ?: AiVector3D()

    val translationMinX get() = props["TranslationMinX"] ?: false
    val translationMaxX get() = props["TranslationMaxX"] ?: false
    val translationMinY get() = props["TranslationMinY"] ?: false
    val translationMaxY get() = props["TranslationMaxY"] ?: false
    val translationMinZ get() = props["TranslationMinZ"] ?: false
    val translationMaxZ get() = props["TranslationMaxZ"] ?: false

    val rotationOrder get() = props["RotationOrder"] ?: RotOrder.EulerXYZ
    val rotationSpaceForLimitOnly get() = props["RotationSpaceForLimitOnlyMaxZ"] ?: false
    val rotationStiffnessX get() = props["RotationStiffnessX"] ?: 0f
    val rotationStiffnessY get() = props["RotationStiffnessY"] ?: 0f
    val rotationStiffnessZ get() = props["RotationStiffnessZ"] ?: 0f
    val axisLen get() = props["AxisLen"] ?: 0f

    val preRotation get() = props["PreRotation"] ?: AiVector3D()
    val postRotation get() = props["PostRotation"] ?: AiVector3D()
    val rotationActive get() = props["RotationActive"] ?: false

    val rotationMin get() = props["RotationMin"] ?: AiVector3D()
    val rotationMax get() = props["RotationMax"] ?: AiVector3D()

    val rotationMinX get() = props["RotationMinX"] ?: false
    val rotationMaxX get() = props["RotationMaxX"] ?: false
    val rotationMinY get() = props["RotationMinY"] ?: false
    val rotationMaxY get() = props["RotationMaxY"] ?: false
    val rotationMinZ get() = props["RotationMinZ"] ?: false
    val rotationMaxZ get() = props["RotationMaxZ"] ?: false
    val inheritType get() = props["InheritType"] ?: TransformInheritance.RrSs

    val scalingActive get() = props["ScalingActive"] ?: false
    val scalingMin get() = props["ScalingMin"] ?: AiVector3D()
    val scalingMax get() = props["ScalingMax"] ?: AiVector3D(1f)
    val scalingMinX get() = props["ScalingMinX"] ?: false
    val scalingMaxX get() = props["ScalingMaxX"] ?: false
    val scalingMinY get() = props["ScalingMinY"] ?: false
    val scalingMaxY get() = props["ScalingMaxY"] ?: false
    val scalingMinZ get() = props["ScalingMinZ"] ?: false
    val scalingMaxZ get() = props["ScalingMaxZ"] ?: false

    val geometricTranslation get() = props["GeometricTranslation"] ?: AiVector3D()
    val geometricRotation get() = props["GeometricRotation"] ?: AiVector3D()
    val geometricScaling get() = props["GeometricScaling"] ?: AiVector3D(1f)

    val minDampRangeX get() = props["MinDampRangeX"] ?: 0f
    val minDampRangeY get() = props["MinDampRangeY"] ?: 0f
    val minDampRangeZ get() = props["MinDampRangeZ"] ?: 0f
    val maxDampRangeX get() = props["MaxDampRangeX"] ?: 0f
    val maxDampRangeY get() = props["MaxDampRangeY"] ?: 0f
    val maxDampRangeZ get() = props["MaxDampRangeZ"] ?: 0f

    val minDampStrengthX get() = props["MinDampStrengthX"] ?: 0f
    val minDampStrengthY get() = props["MinDampStrengthY"] ?: 0f
    val minDampStrengthZ get() = props["MinDampStrengthZ"] ?: 0f
    val maxDampStrengthX get() = props["MaxDampStrengthX"] ?: 0f
    val maxDampStrengthY get() = props["MaxDampStrengthY"] ?: 0f
    val maxDampStrengthZ get() = props["MaxDampStrengthZ"] ?: 0f

    val preferredAngleX get() = props["PreferredAngleX"] ?: 0f
    val preferredAngleY get() = props["PreferredAngleY"] ?: 0f
    val preferredAngleZ get() = props["PreferredAngleZ"] ?: 0f

    val show get() = props["Show"] ?: true
    val lodBox get() = props["LODBox"] ?: false
    val freeze get() = props["Freeze"] ?: false

    /** convenience method to check if the node has a Null node marker */
//    bool IsNull () const

    fun resolveLinks(element: Element, doc: Document) {

        val arr = arrayOf("Geometry", "Material", "NodeAttribute")

        // resolve material
        val conns = doc.getConnectionsByDestinationSequenced(id, arr)

        materials.ensureCapacity(conns.size)
        geometry.ensureCapacity(conns.size)
        attributes.ensureCapacity(conns.size)
        for (con in conns) {

            // material and geometry links should be Object-Object connections
            if (con.prop.isEmpty()) continue

            val ob = con.sourceObject
            if (ob == null) {
                domWarning("failed to read source object for incoming Model link, ignoring", element)
                continue
            }

            when (ob) {
                is Material -> materials += ob
                is Geometry -> geometry += ob
                is NodeAttribute -> attributes += ob
            }

            domWarning("source object for model link is neither Material, NodeAttribute nor Geometry, ignoring", element)
            continue
        }
    }
}

/** DOM class for generic FBX textures */
class Texture(id: Long, element: Element, doc: Document, name: String) : Object(id, element, name) {

    val uvTrans = element.scope["ModelUVTranslation"]?.let { AiVector2D(it[0].parseAsFloat, it[1].parseAsFloat) }
            ?: AiVector2D()
    val uvScaling = element.scope["ModelUVScaling"]?.let { AiVector2D(it[0].parseAsFloat, it[1].parseAsFloat) }
            ?: AiVector2D(1)

    val type = element.scope["Type"]?.get(0)?.parseAsString ?: ""
    val relativeFileName = element.scope["RelativeFileName"]?.get(0)?.parseAsString ?: ""
    val fileName = element.scope["FileName"]?.get(0)?.parseAsString ?: ""
    val alphaSource = element.scope["Texture_Alpha_Source"]?.get(0)?.parseAsString ?: ""
    val props = getPropertyTable(doc, "Texture.FbxFileTexture", element, element.scope)

    val crop = element.scope["Cropping"].let { sc -> IntArray(4, { sc?.get(it)?.parseAsInt ?: 0 }) }

    var media: Video? = null

    init {
        // resolve video links
        if (doc.settings.readTextures) {
            val conns = doc.getConnectionsByDestinationSequenced(id)
            for (con in conns) {
                val ob = con.sourceObject
                if (ob == null) {
                    domWarning("failed to read source object for texture link, ignoring", element)
                    continue
                }
                media = ob as? Video
            }
        }
    }
}

/** DOM class for layered FBX textures */
class LayeredTexture(id: Long, element: Element, doc: Document, name: String) : Object(id, element, name) {

    val textures = ArrayList<Texture?>()
    val blendMode = element.scope["BlendModes"]?.let { BlendMode.values()[it[0].parseAsInt] }
            ?: BlendMode.Translucent
    val alpha = element.scope["Alphas"]?.get(0)?.parseAsFloat ?: 0f

    //Can only be called after construction of the layered texture object due to construction flag.
    fun fillTexture(doc: Document) {
        val conns = doc.getConnectionsByDestinationSequenced(id)
        for (i in 0 until conns.size) {
            val con = conns[i]

            val ob = con.sourceObject
            if (ob == null) {
                domWarning("failed to read source object for texture link, ignoring", element)
                continue
            }
            val tex = ob as? Texture
            textures += tex
        }
    }

    enum class BlendMode { Translucent, Additive, Modulate, Modulate2, Over, Normal, Dissolve, Darken, ColorBurn,
        LinearBurn, DarkerColor, Lighten, Screen, ColorDodge, LinearDodge, LighterColor, SoftLight, HardLight,
        VividLight, LinearLight, PinLight, HardMix, Difference, Exclusion, Subtract, Divide, Hue, Saturation, Color,
        Luminosity, Overlay, BlendModeCount
    }

}

/** DOM class for generic FBX videos */
class Video(id: Long, element: Element, doc: Document, name: String) : Object(id, element, name) {

    val type = element.scope["Type"]?.get(0)?.parseAsString ?: ""
    val relativeFileName = element.scope["RelativeFilename"]?.get(0)?.parseAsString ?: ""
    val fileName = element.scope["FileName"]?.get(0)?.parseAsString ?: ""

    var contentLength = 0
    var content: ByteArray? = null

    init {

        element.scope["Content"]?.let {
            //this field is ommited when the embedded texture is already loaded, let's ignore if it´s not found
            try {
                val token = it[0]
                val data = token.begin
                when {
                    !token.isBinary -> domWarning("video content is not binary data, ignoring", element)
                    token.end - data < 5 -> domError("binary data array is too short, need five (5) bytes for type signature and element count", element)
                    buffer[data].c != 'R' -> domWarning("video content is not raw binary data, ignoring", element)
                    else -> {
                        // read number of elements
                        val len = buffer.getInt(data + 1)

                        contentLength = len

                        content = ByteArray(len, { buffer.get(data + 5 + it) })
                    }
                }
            } catch (runtimeError: Exception) {
                //we don´t need the content data for contents that has already been loaded
            }
        }
    }

    val props = getPropertyTable(doc, "Video.FbxVideo", element, element.scope)
}

/** DOM class for generic FBX materials */
class Material(id: Long, element: Element, doc: Document, name: String) : Object(id, element, name) {

    val shading = element.scope["ShadingModel"]?.get(0)?.parseAsString ?: "phong".also {
        domWarning("shading mode not specified, assuming phong", element)
    }
    val multilayer = element.scope["MultiLayer"]?.get(0)?.parseAsInt?.bool ?: false
    val props = getPropertyTable(doc,
            when (shading) {
                "phong" -> "Material.FbxSurfacePhong"
                "lambert" -> "Material.FbxSurfaceLambert"
                else -> "".also { domWarning("shading mode not recognized: $shading", element) }
            },
            element, element.scope)

    val textures = mutableMapOf<String, Texture>()
    val layeredTextures = mutableMapOf<String, ArrayList<LayeredTexture>>()

    init {

        // resolve texture links
        val conns = doc.getConnectionsByDestinationSequenced(id)
        for (con in conns) {

            // texture link to properties, not objects
            if (con.prop.isEmpty()) continue

            val ob = con.sourceObject
            if (ob == null) {
                domWarning("failed to read source object for texture link, ignoring", element)
                continue
            }

            val tex = ob as? Texture
            if (tex == null) {
                val layeredTexture = ob as? LayeredTexture
                if (layeredTexture == null) {
                    domWarning("source object for texture link is not a texture or layered texture, ignoring", element)
                    continue
                }
                val prop = con.prop
                if (layeredTextures.contains(prop)) domWarning("duplicate layered texture link: $prop", element)

                layeredTextures[prop] = arrayListOf(layeredTexture)
                layeredTexture.fillTexture(doc)
            } else {
                val prop = con.prop
                if (textures.contains(prop)) domWarning("duplicate texture link: $prop", element)

                textures[prop] = tex
            }

        }
    }
}

/** Represents a FBX animation curve (i.e. a 1-dimensional set of keyframes and values therefor) */
class AnimationCurve(id: Long, element: Element, name: String, doc: Document) : Object(id, element, name) {

    val keys = ArrayList<Long>()
    val values = ArrayList<Float>()
    val attributes = ArrayList<Float>()
    val flags = ArrayList<Int>()

    init{
        val sc = element.scope
        val keyTime = sc["KeyTime"]!!
        val keyValueFloat = sc["KeyValueFloat"]!!

        keyTime.parseLongsDataArray(keys, KeyTime)
        keyValueFloat.parseFloatsDataArray(values, KeyValueFloat)

        if(keys.size() != values.size()) {
            DOMError("the number of key times does not match the number of keyframe values",&KeyTime)
        }

        // check if the key times are well-ordered
        if(!std::equal(keys.begin(), keys.end() - 1, keys.begin() + 1, std::less<KeyTimeList::value_type>())) {
            DOMError("the keyframes are not in ascending order",&KeyTime)
        }

        const Element* KeyAttrDataFloat = sc["KeyAttrDataFloat"]
        if(KeyAttrDataFloat) {
            ParseVectorDataArray(attributes, *KeyAttrDataFloat)
        }

        const Element* KeyAttrFlags = sc["KeyAttrFlags"]
        if(KeyAttrFlags) {
            ParseVectorDataArray(flags, *KeyAttrFlags)
        }
    }

    /** get list of keyframe positions (time).
     *  Invariant: |GetKeys()| > 0 */
    const KeyTimeList & GetKeys ()
    const {
        return keys
    }


    /** get list of keyframe values.
     * Invariant: |GetKeys()| == |GetValues()| && |GetKeys()| > 0*/
    const KeyValueList & GetValues ()
    const {
        return values
    }


    const std ::vector<float>& GetAttributes()
    const {
        return attributes
    }

    const std ::vector < unsigned int >& GetFlags()
    const {
        return flags
    }
}
//
//// property-name -> animation curve
//typedef std::map<std::string, const AnimationCurve*> AnimationCurveMap
//
//
///** Represents a FBX animation curve (i.e. a mapping from single animation curves to nodes) */
//class AnimationCurveNode : public Object
//{
//    public:
//    /* the optional white list specifies a list of property names for which the caller
//    wants animations for. If the curve node does not match one of these, std::range_error
//    will be thrown. */
//    AnimationCurveNode(uint64_t id, const Element & element, const std ::string& name, const Document& doc,
//    const char * const * target_prop_whitelist = NULL, size_t whitelist_size = 0)
//
//    virtual ~AnimationCurveNode()
//
//    const PropertyTable & Props () const {
//        ai_assert(props.get())
//        return * props.get()
//    }
//
//
//    const AnimationCurveMap & Curves () const
//
//            /** Object the curve is assigned to, this can be NULL if the
//             *  target object has no DOM representation or could not
//             *  be read for other reasons.*/
//            const Object * Target () const {
//        return target
//    }
//
//    const Model * TargetAsModel () const {
//        return dynamic_cast < const Model * > (target)
//    }
//
//    const NodeAttribute * TargetAsNodeAttribute () const {
//        return dynamic_cast < const NodeAttribute * > (target)
//    }
//
//    /** Property of Target() that is being animated*/
//    const std ::string& TargetProperty() const {
//    return prop
//}
//
//    private:
//    const Object * target
//            std::shared_ptr < const PropertyTable > props
//            mutable AnimationCurveMap curves
//
//    std::string prop
//            const Document & doc
//}
//
//typedef std::vector<const AnimationCurveNode*> AnimationCurveNodeList

/** Represents a FBX animation layer (i.e. a list of node animations) */
class AnimationLayer(id: Long, element: Element, name: String, val doc: Document) : Object(id, element, name) {

    // note: the props table here bears little importance and is usually absent
    val props = getPropertyTable(doc, "AnimationLayer.FbxAnimLayer", element, element.scope, true)

    /* the optional white list specifies a list of property names for which the caller
    wants animations for. Curves not matching this list will not be added to the
    animation layer. */
    AnimationCurveNodeList Nodes (const char * const * target_prop_whitelist = NULL, size_t whitelist_size = 0) const
}

/** Represents a FBX animation stack (i.e. a list of animation layers) */
class AnimationStack(id: Long, element: Element, name: String, doc: Document) : Object(id, element, name) {

    // note: we don't currently use any of these properties so we shouldn't bother if it is missing
    val props = getPropertyTable(doc, "AnimationStack.FbxAnimStack", element, element.scope, true)
    val layers = ArrayList<AnimationLayer>()

    init {

        // resolve attached animation layers
        val conns = doc.getConnectionsByDestinationSequenced(id, "AnimationLayer")
        layers.ensureCapacity(conns.size)

        for (con in conns) {
            // link should not go to a property
            if (con.prop.isEmpty()) continue

            val ob = con.sourceObject
            if (ob == null) {
                domWarning("failed to read source object for AnimationLayer->AnimationStack link, ignoring", element)
                continue
            }

            val anim = ob as? AnimationLayer
            if (anim == null) {
                domWarning("source object for ->AnimationStack link is not an AnimationLayer", element)
                continue
            }
            layers += anim
        }
    }

    val localStart = props["LocalStart"] ?: 0L
    val localStop = props["LocalStop"] ?: 0L
    val referenceStart = props["ReferenceStart"] ?: 0L
    val referenceStop = props["ReferenceStop"] ?: 0L
}


/** DOM class for deformers */
open class Deformer(id: Long, element: Element, doc: Document, name: String) : Object(id, element, name) {

    val props = getPropertyTable(doc, "Deformer.Fbx" + element[2].parseAsString, element, element.scope, true)
}

/** DOM class for skin deformer clusters (aka subdeformers) */
class Cluster(id: Long, element: Element, doc: Document, name: String) : Deformer(id, element, doc, name) {

    /** get the list of deformer weights associated with this cluster. Use #GetIndices() to get the associated vertices.
     *  Both arrays have the same size (and may also be empty). */
    val weights = ArrayList<Float>()
    /** get indices into the vertex data of the geometry associated with this cluster. Use #GetWeights() to get the
     *  associated weights. Both arrays have the same size (and may also be empty). */
    val indices = ArrayList<Int>()

    val transform: AiMatrix4x4
    val transformLink: AiMatrix4x4

    var node: Model? = null

    init {
        val sc = element.scope

        val indexes = sc.getArray("Indexes")
        val weights = sc.getArray("Weights")

        val transform_ = getRequiredElement(sc, "Transform", element)
        val transformLink_ = getRequiredElement(sc, "TransformLink", element)

        transform = transform_.readMatrix()
        transformLink = transformLink_.readMatrix()

        // it is actually possible that there be Deformer's with no weights
        if (indexes.isNotEmpty() != weights.isNotEmpty()) domError("either Indexes or Weights are missing from Cluster", element)

        if (indexes.isNotEmpty()) {
            indexes[0].parseIntsDataArray(indices)
            weights[0].parseFloatsDataArray(this.weights)
        }

        if (indices.size != weights.size) domError("sizes of index and weight array don't match up", element)

        // read assigned node
        val conns = doc.getConnectionsByDestinationSequenced(id, "Model")
        for (con in conns) {
            val mod = processSimpleConnection<Model>(con, false, "Model -> Cluster", element)
            if (mod != null) {
                node = mod
                break
            }
        }
        if (node == null) domError("failed to read target Node for Cluster", element)
    }
}

/** DOM class for skin deformers */
class Skin(id: Long, element: Element, doc: Document, name: String) : Deformer(id, element, doc, name) {

    val accuracy = element.scope["Link_DeformAcuracy"]?.get(0)?.parseAsFloat ?: 0f
    val clusters = ArrayList<Cluster>()

    init {
        // resolve assigned clusters
        val conns = doc.getConnectionsByDestinationSequenced(id, "Deformer")

        clusters.ensureCapacity(conns.size)
        for (con in conns) {
            val cluster = processSimpleConnection<Cluster>(con, false, "Cluster -> Skin", element)
            if (cluster != null) {
                clusters += cluster
                continue
            }
        }
    }
}

/** Represents a link between two FBX objects. */
class Connection(val insertionOrder: Long, val src: Long, val dest: Long, val prop: String, val doc: Document) : Comparable<Connection> {

    init {
        assert(doc.objects.contains(src))
        // dest may be 0 (root node)
        assert(dest != 0L || doc.objects.contains(dest))
    }

    /** note: a connection ensures that the source and dest objects exist, but not that they have DOM representations,
     *  so the return value of one of these functions can still be NULL. */
    val sourceObject get() = doc[src]!!.get()
    val destinationObject get() = doc[dest]!!.get()

    // these, however, are always guaranteed to be valid
    val lazySourceObject get() = doc[src]!!
    val lazyDestinationObject get() = doc[dest]!!

    override fun compareTo(other: Connection) = insertionOrder.compareTo(other.insertionOrder)

//    bool Compare(const Connection* c)
//    const {
//        ai_assert(NULL != c)
//
//        return InsertionOrder() < c->InsertionOrder()
//    }
}
//
//// XXX again, unique_ptr would be useful. shared_ptr is too
//// bloated since the objects have a well-defined single owner
//// during their entire lifetime (Document). FBX files have
//// up to many thousands of objects (most of which we never use),
//// so the memory overhead for them should be kept at a minimum.
//typedef std::map<uint64_t, LazyObject*> ObjectMap
//typedef std::fbx_unordered_map<std::string, std::shared_ptr<const PropertyTable> > PropertyTemplateMap
//
//typedef std::multimap<uint64_t, const Connection*> ConnectionMap

/** DOM class for global document settings, a single instance per document can
 *  be accessed via Document.Globals(). */
class FileGlobalSettings(val doc: Document, val props: PropertyTable) {

    val upAxis get() = props.get<Int>("UpAxis") ?: 1
    val upAxisSign get() = props.get<Int>("UpAxisSign") ?: 1
    val frontAxis get() = props.get<Int>("FrontAxis") ?: 2
    val frontAxisSign get() = props.get<Int>("FrontAxisSign") ?: 1
    val coordAxis get() = props.get<Int>("CoordAxis") ?: 1
    val coordAxisSign get() = props.get<Int>("CoordAxisSign") ?: 1
    val originalUpAxis get() = props.get<Int>("OriginalUpAxis") ?: 1
    val originalUpAxisSign get() = props.get<Int>("OriginalUpAxisSign") ?: 1
    val unitScaleFactor get() = props.get<Double>("UnitScaleFactor") ?: 1.0
    val originalUnitScaleFactor get() = props.get<Double>("OriginalUnitScaleFactor") ?: 1.0
    val ambientColor get() = props.get<AiVector3D>("AmbientColor") ?: AiVector3D()
    val defaultCamera get() = props.get<String>("DefaultCamera") ?: ""

    enum class FrameRate { DEFAULT, _120, _100, _60, _50, _48, _30, _30_DROP, NTSC_DROP_FRAME, NTSC_FULL_FRAME, PAL,
        CINEMA, _1000, CINEMA_ND, CUSTOM // end-of-enum sentinel
    }

    val timeMode get() = FrameRate.values()[props["TimeMode"] ?: FrameRate.DEFAULT.ordinal]

    val timeSpanStart get() = props.get<Long>("TimeSpanStart") ?: 0L
    val timeSpanStop get() = props.get<Long>("TimeSpanStop") ?: 0L
    val customFrameRate get() = props.get<Float>("CustomFrameRate") ?: -1f
}

/** DOM root for a FBX file */
class Document(val parser: Parser, val settings: ImportSettings) {

    val objects = mutableMapOf<Long, LazyObject>()

    val templates = HashMap<String, PropertyTable>()
    val srcConnections = mutableMapOf<Long, ArrayList<Connection>>()
    val destConnections = mutableMapOf<Long, ArrayList<Connection>>()

    var fbxVersion = 0
    var creator = ""
    val creationTimeStamp = IntArray(7)

    private val animationStacks = ArrayList<Long>()
//    mutable std::vector<const AnimationStack*> animationStacksResolved

    var globals: FileGlobalSettings? = null

    init {
        readHeader()
        readPropertyTemplates()

        readGlobalSettings()

        /*  This order is important, connections need parsed objects to check whether connections are ok or not.
            Objects may not be evaluated yet, though, since this may require valid connections. */
        readObjects()
        readConnections()
    }


    operator fun get(id: Long) = objects[id]
    //
//    bool IsBinary()
//    const {
//        return parser.IsBinary()
//    }
//
//    unsigned int FBXVersion()
//    const {
//        return fbxVersion
//    }
//
//    const std::string& Creator()
//    const {
//        return creator
//    }
//
//    // elements (in this order): Year, Month, Day, Hour, Second, Millisecond
//    const unsigned int* CreationTimeStamp()
//    const {
//        return creationTimeStamp
//    }
//
//    const FileGlobalSettings& GlobalSettings()
//    const {
//        ai_assert(globals.get())
//        return * globals.get()
//    }
//
//    const PropertyTemplateMap& Templates()
//    const {
//        return templates
//    }
//
//    const ObjectMap& Objects()
//    const {
//        return objects
//    }
//
//    const ImportSettings& Settings()
//    const {
//        return settings
//    }
//
//    const ConnectionMap& ConnectionsBySource()
//    const {
//        return src_connections
//    }
//
//    const ConnectionMap& ConnectionsByDestination()
//    const {
//        return dest_connections
//    }
//
    /*  note: the implicit rule in all DOM classes is to always resolve from destination to source (since the FBX object
        hierarchy is, with very few exceptions, a DAG, this avoids cycles). In all cases that may involve back-facing
        edges in the object graph, use LazyObject::IsBeingConstructed() to check. */

    fun getConnectionsBySourceSequenced(source: Long) = getConnectionsSequenced(source, srcConnections)
    fun getConnectionsByDestinationSequenced(dest: Long) = getConnectionsSequenced(dest, destConnections)

    fun getConnectionsBySourceSequenced(source: Long, classname: String) = getConnectionsBySourceSequenced(source, arrayOf(classname))
    fun getConnectionsByDestinationSequenced(dest: Long, classname: String) = getConnectionsByDestinationSequenced(dest, arrayOf(classname))

    fun getConnectionsBySourceSequenced(source: Long, classnames: Array<String>) = getConnectionsSequenced(source, true, srcConnections, classnames)
    fun getConnectionsByDestinationSequenced(dest: Long, classnames: Array<String>) = getConnectionsSequenced(dest, false, destConnections, classnames)

    fun animationStacks(): ArrayList<AnimationStack> {

    }

    fun getConnectionsSequenced(id: Long, conns: MutableMap<Long, ArrayList<Connection>>): ArrayList<Connection> {

        val range = conns[id]!!

        return ArrayList<Connection>(range.size).apply {
            addAll(range)
            sort()
        } // NRVO should handle this
    }

    val MAX_CLASSNAMES = 6
    fun getConnectionsSequenced(id: Long, isSrc: Boolean, conns: MutableMap<Long, ArrayList<Connection>>, classnames: Array<String>): ArrayList<Connection> {
        assert(classnames.size in 1..MAX_CLASSNAMES)

        val temp = ArrayList<Connection>()
        val range = conns[id]!!

        temp.ensureCapacity(range.size)
        for (it in range) {
            val key = (if (isSrc) it.lazyDestinationObject else it.lazySourceObject).element.keyToken

            var obType = key.begin

            for (name in classnames) {
                assert(name.isNotEmpty())
                if (key.end - key.begin == name.length && buffer.strncmp(name, obType)) {
                    obType = 0
                    break
                }
            }
            if (obType == 0) continue
            temp += it
        }
        temp.sort()
        return temp // NRVO should handle this
    }

    fun readHeader() {
        // Read ID objects from "Objects" section
        val sc = parser.root
        val eHead = sc["FBXHeaderExtension"]
        if (eHead?.compound == null) domError("no FBXHeaderExtension dictionary found")
        val sHead = eHead.compound!!
        fbxVersion = getRequiredElement(sHead, "FBXVersion", eHead)[0].parseAsInt

        // While we may have some success with newer files, we don't support the older 6.n fbx format
        if (fbxVersion < lowerSupportedVersion) domError("unsupported, old format version, supported are only FBX 2011, FBX 2012 and FBX 2013")
        if (fbxVersion > upperSupportedVersion)
            if (settings.strictMode) domError("unsupported, newer format version, supported are only FBX 2011, FBX 2012 and FBX 2013 (turn off strict mode to try anyhow) ")
            else domWarning("unsupported, newer format version, supported are only FBX 2011, FBX 2012 and FBX 2013, trying to read it nevertheless")

        sHead["Creator"]?.let { creator = it[0].parseAsString }

        sHead["CreationTimeStamp"]?.let {
            it.compound?.let {
                creationTimeStamp[0] = it["Year"]!![0].parseAsInt
                creationTimeStamp[1] = it["Month"]!![0].parseAsInt
                creationTimeStamp[2] = it["Day"]!![0].parseAsInt
                creationTimeStamp[3] = it["Hour"]!![0].parseAsInt
                creationTimeStamp[4] = it["Minute"]!![0].parseAsInt
                creationTimeStamp[5] = it["Second"]!![0].parseAsInt
                creationTimeStamp[6] = it["Millisecond"]!![0].parseAsInt
            }
        }
    }

    fun readObjects() {
        // read ID objects from "Objects" section
        val sc = parser.root
        val eObjects = sc["Objects"]
        if (eObjects?.compound == null) domError("no Objects dictionary found")

        // add a dummy entry to represent the Model::RootNode object (id 0), which is only indirectly defined in the input file
        objects[0L] = LazyObject(0L, eObjects, this)

        val sObjects = eObjects.compound!!
        for (el in sObjects.elements.flatMap { e -> List(e.value.size, { Pair(e.key, e.value[it]) }) }) {

            // extract ID
            val tok = el.second.tokens

            if (tok.isEmpty()) domError("expected ID after object key", el.second)

            val id = tok[0].parseAsId

            // id=0 is normally implicit
            if (id == 0L) domError("encountered object with implicitly defined id 0", el.second)

            if (objects.contains(id)) domWarning("encountered duplicate object id, ignoring first occurrence", el.second)

            objects[id] = LazyObject(id, el.second, this)

            // grab all animation stacks upfront since there is no listing of them
            if (el.first == "AnimationStack") animationStacks += id
        }
    }

    fun readPropertyTemplates() {
        // read property templates from "Definitions" section
        val eDefs = parser.root["Definitions"]
        if (eDefs?.compound == null) {
            domWarning("no Definitions dictionary found")
            return
        }

        val sDefs = eDefs.compound!!
        val oTypes = sDefs.getCollection("ObjectType")
        for (el in oTypes) {
            val sc = el.compound
            if (sc == null) {
                domWarning("expected nested scope in ObjectType, ignoring", el)
                continue
            }
            val tok = el.tokens
            if (tok.isEmpty()) {
                domWarning("expected name for ObjectType element, ignoring", el)
                continue
            }
            val oName = tok[0].parseAsString
            val templs = sc.getCollection("PropertyTemplate")
            for (el in templs) {
                val sc = el.compound
                if (sc == null) {
                    domWarning("expected nested scope in PropertyTemplate, ignoring", el)
                    continue
                }
                val tok = el.tokens
                if (tok.isEmpty()) {
                    domWarning("expected name for PropertyTemplate element, ignoring", el)
                    continue
                }
                val pName = tok[0].parseAsString
                sc["Properties70"]?.let {
                    val props = PropertyTable(it, null)
                    templates[oName + "." + pName] = props
                }
            }
        }
    }

    fun readConnections() {
        val sc = parser.root
        // read property templates from "Definitions" section
        val eConns = sc["Connections"]
        if (eConns?.compound == null) domError("no Connections dictionary found")

        var insertionOrder = 0L
        val sConns = eConns.compound!!
        val conns = sConns.getCollection("C")
        for (el in conns) {
            val type = el[0].parseAsString

            // PP = property-property connection, ignored for now
            // (tokens: "PP", ID1, "Property1", ID2, "Property2")
            if (type == "PP") continue

            val src = el[1].parseAsId
            val dest = el[2].parseAsId

            // OO = object-object connection
            // OP = object-property connection, in which case the destination property follows the object ID
            val prop = if (type == "OP") el[3].parseAsString else ""

            if (!objects.contains(src)) {
                domWarning("source object for connection does not exist", el)
                continue
            }

            // dest may be 0 (root node) but we added a dummy object before
            if (!objects.contains(dest)) {
                domWarning("destination object for connection does not exist", el)
                continue
            }

            // add new connection
            val c = Connection(insertionOrder++, src, dest, prop, this)
            srcConnections.getOrPut(src, ::arrayListOf) += c
            destConnections.getOrPut(dest, ::arrayListOf) += c
        }
    }

    fun readGlobalSettings() {
        val sc = parser.root
        val eHead = sc["GlobalSettings"]
        if (eHead == null || eHead.compound != null) {
            domWarning("no GlobalSettings dictionary found")
            globals = FileGlobalSettings(this, PropertyTable())
            return
        }

        val props = getPropertyTable(this, "", eHead, eHead.compound!!, true)

//        if (!props) { TODO
//            DOMError("GlobalSettings dictionary contains no property table")
//        }

        globals = FileGlobalSettings(this, props)
    }
//
//    private :
//    const ImportSettings& settings
//
//    ObjectMap objects
//    const Parser& parser
//
//    PropertyTemplateMap templates
//    ConnectionMap src_connections
//    ConnectionMap dest_connections
//
//    unsigned int fbxVersion
//    std::string creator
//    unsigned int creationTimeStamp[7]
//
//    std::vector<uint64_t> animationStacks
//    mutable std::vector<const AnimationStack*> animationStacksResolved
//
//    std::unique_ptr<FileGlobalSettings> globals

    companion object {
        val lowerSupportedVersion = 7100
        val upperSupportedVersion = 7400

    }
}