package assimp.format.collada

/**
 * Created by elect on 23/01/2017.
 */

import glm_.i
import assimp.*
import org.w3c.dom.Node as XmlNode

/** Collada file versions which evolved during the years ...  */
enum class FormatVersion {
    _1_5_n,
    _1_4_n,
    _1_3_n
}


/** Transformation types that can be applied to a node  */
enum class TransformType {
    LOOKAT,
    ROTATE,
    TRANSLATE,
    SCALE,
    SKEW,
    MATRIX
}

/** Different types of input data to a vertex or face  */
enum class InputType {
    Invalid,
    Vertex, // special type for per-index data referring to the <vertices> element carrying the per-vertex data.
    Position,
    Normal,
    Texcoord,
    Color,
    Tangent,
    Bitangent
}

/** Supported controller types */
enum class ControllerType {
    Skin,
    Morph
}

/** Supported morph methods */
enum class MorphMethod {
    Normalized,
    Relative
}

/** Contains all data for one of the different transformation types */
class Transform(
        var mID: String = "", ///< SID of the transform step, by which anim channels address their target node
        var mType: TransformType = TransformType.LOOKAT,
        var f: FloatArray = floatArrayOf()) ///< Interpretation of data depends on the type of the transformation

/** A collada camera. */
class Camera(
        // Name of camera
        var mName: String = "",

        // True if it is an orthografic camera
        var mOrtho: Boolean = false,

        //! Horizontal field of view in degrees
        var mHorFov: Float = 10e10f,

        //! Vertical field of view in degrees
        var mVerFov: Float = 10e10f,

        //! Screen aspect
        var mAspect: Float = 10e10f,

        //! Near& far z
        var mZNear: Float = .1f,
        var mZFar: Float = 1_000f)

val ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET = 1e9f

/** A collada light source. */
class Light(
        //! Type of the light source aiLightSourceType + ambient
        var mType: AiLightSourceType = AiLightSourceType.UNDEFINED,

        //! Color of the light
        var mColor: AiColor3D = AiColor3D(1f),

        //! Light attenuation
        var mAttConstant: Float = 1f,
        var mAttLinear: Float = 0f,
        var mAttQuadratic: Float = 0f,

        //! Spot light falloff
        var mFalloffAngle: Float = 180f,
        var mFalloffExponent: Float = 0f,

        // FCOLLADA extension from here, ... related stuff from maja and max extensions
        var mPenumbraAngle: Float = ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET,
        var mOuterAngle: Float = ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET,

        //! Common light intensity
        var mIntensity: Float = 1f)

/** Short vertex index description */
class InputSemanticMapEntry(
        //! Index of set, optional
        var mSet: Int = 0,
        //! Type of referenced vertex input
        var mType: InputType = InputType.Invalid)

/** Table to map from effect to vertex input semantics */
class SemanticMappingTable(
        //! Name of material
        var mMatName: String = "",
        //! List of semantic map commands, grouped by effect semantic name
        var mMap: MutableMap<String, InputSemanticMapEntry> = mutableMapOf())

/** A reference to a mesh inside a node, including materials assigned to the various subgroups.
 * The ID refers to either a mesh or a controller which specifies the mesh  */
class MeshInstance(
        ///< ID of the mesh or controller to be instanced
        var mMeshOrController: String = "",
        ///< Map of materials by the subgroup ID they're applied to
        var mMaterials: MutableMap<String, SemanticMappingTable> = mutableMapOf())

/** A reference to a camera inside a node*/
class CameraInstance(
        ///< ID of the camera
        var mCamera: String = "")

/** A reference to a light inside a node*/
class LightInstance(
        ///< ID of the camera
        var mLight: String = "")

/** A reference to a node inside a node*/
class NodeInstance(
        ///< ID of the node
        var mNode: String = "")

/** A node in a scene hierarchy */
class Node(
        var mName: String = "",
        var mID: String = "",
        var mSID: String = "",
        var mParent: Node? = null,
        var mChildren: ArrayList<Node> = arrayListOf(),

        /** Operations in order to calculate the resulting transformation to parent. */
        var mTransforms: ArrayList<Transform> = arrayListOf(),

        /** Meshes at this node */
        var mMeshes: ArrayList<MeshInstance> = arrayListOf(),

        /** Lights at this node */
        var mLights: ArrayList<LightInstance> = arrayListOf(),

        /** Cameras at this node */
        var mCameras: ArrayList<CameraInstance> = arrayListOf(),

        /** Node instances at this node */
        var mNodeInstances: ArrayList<NodeInstance> = arrayListOf(),

        /** Rootnodes: Name of primary camera, if any */
        var mPrimaryCamera: String = "")

/** Data source array: either floats or strings */
class Data(
        var mIsStringArray: Boolean = false,
        var values: ArrayList<Float> = arrayListOf(),
        var mStrings: ArrayList<String> = arrayListOf())

/** Accessor to a data array */
class Accessor {
    /** in number of objects    */
    var count = 0L
    /** size of an object, in elements (floats or strings, mostly 1)    */
    var size = 0L
    /** in number of values */
    var offset = 0
    /** Stride in number of values  */
    var stride = 0L
    /** names of the data streams in the accessors. Empty string tells to ignore.   */
    var mParams = ArrayList<String>()
    /** Suboffset inside the object for the common 4 elements. For a vector, thats XYZ, for a color RGBA and so on.
     *  For example, SubOffset[0] denotes which of the values inside the object is the vector X component.  */
    var mSubOffset = LongArray(4, { 0 })
    /** URL of the source array */
    var source = ""
    /** Pointer to the source array, if resolved. NULL else */
    var mData: Data? = null
}

/** A single face in a mesh */
typealias Face = ArrayList<Int>

/** An input channel for mesh data, referring to a single accessor */
class InputChannel(
        var mType: InputType = InputType.Invalid, // Type of the data
        var mIndex: Int = 0, // Optional index, if multiple sets of the same data type are given
        var mOffset: Int = 0, // Index offset in the indices array of per-face indices. Don't ask, can't explain that any better.
        var mAccessor: String = "", // ID of the accessor where to read the actual values from.
        var resolved: Accessor? = null) // Pointer to the accessor, if resolved. NULL else

/** Subset of a mesh with a certain material */
class SubMesh(
        var mMaterial: String = "", ///< subgroup identifier
        var mNumFaces: Int = 0) ///< number of faces in this submesh

/** Contains data for a single mesh */
class Mesh(

        var mName: String = "",

        // just to check if there's some sophisticated addressing involved...
        // which we don't support, and therefore should warn about.
        var mVertexID: String = "",

        // Vertex data addressed by vertex indices
        var mPerVertexData: ArrayList<InputChannel> = arrayListOf(),

        // actual mesh data, assembled on encounter of a <p> element. Verbose format, not indexed
        var mPositions: ArrayList<AiVector3D> = arrayListOf(),
        var mNormals: ArrayList<AiVector3D> = arrayListOf(),
        var mTangents: ArrayList<AiVector3D> = arrayListOf(),
        var mBitangents: ArrayList<AiVector3D> = arrayListOf(),
        var mTexCoords: ArrayList<ArrayList<FloatArray>> = arrayListOf(),
        var mColors: Array<ArrayList<AiColor4D>> = arrayOf(),

        var mNumUVComponents: IntArray = IntArray(AI_MAX_NUMBER_OF_TEXTURECOORDS, { 2 }),

        // Faces. Stored are only the number of vertices for each face. 1 == point, 2 == line, 3 == triangle, 4+ == poly
        var mFaceSize: ArrayList<Int> = arrayListOf(),

        // Position indices for all faces in the sequence given in mFaceSize - necessary for bone weight assignment
        var mFacePosIndices: ArrayList<Int> = arrayListOf(),

        // Submeshes in this mesh, each with a given material
        var mSubMeshes: ArrayList<SubMesh> = arrayListOf())

/** Which type of primitives the ReadPrimitives() function is going to read */
enum class PrimitiveType {
    Invalid,
    Lines,
    LineStrip,
    Triangles,
    TriStrips,
    TriFans,
    Polylist,
    Polygon
}

/** A skeleton controller to deform a mesh with the use of joints */
class Controller(
        // controller type
        var mType: ControllerType = ControllerType.Morph,

        // Morphing method if type is Morph
        var mMethod: MorphMethod = MorphMethod.Normalized,

        // the URL of the mesh deformed by the controller.
        var mMeshId: String = "",

        // accessor URL of the joint names
        var mJointNameSource: String = "",

        ///< The bind shape matrix, as array of floats. I'm not sure what this matrix actually describes, but it can't be ignored in all cases
        var mBindShapeMatrix: FloatArray = floatArrayOf(),

        // accessor URL of the joint inverse bind matrices
        var mJointOffsetMatrixSource: String = "",

        // input channel: joint names.
        var mWeightInputJoints: InputChannel = InputChannel(),
        // input channel: joint weights
        var mWeightInputWeights: InputChannel = InputChannel(),

        // Number of weights per vertex.
        var weightCounts: LongArray = longArrayOf(),

        // JointIndex-WeightIndex pairs for all vertices
        var weights: ArrayList<Pair<Long, Long>> = ArrayList(),

        var mMorphTarget: String = "",
        var mMorphWeight: String = "")

/** A collada material. Pretty much the only member is a reference to an effect. */
class Material(
        var mName: String = "",
        var mEffect: String = "")

/** Type of the effect param */
enum class ParamType {
    Sampler,
    Surface
}

/** A param for an effect. Might be of several types, but they all just refer to each other, so I summarize them */
class EffectParam(
        var mType: ParamType = ParamType.Sampler,
        var mReference: String = "") // to which other thing the param is referring to.

/** Shading type supported by the standard effect spec of Collada */
enum class ShadeType {
    Invalid,
    Constant,
    Lambert,
    Phong,
    Blinn
}

/** Represents a texture sampler in collada */
class Sampler(
        /** Name of image reference     */
        var mName: String = "",

        /** Wrap U?     */
        var mWrapU: Boolean = true,

        /** Wrap V?     */
        var mWrapV: Boolean = true,

        /** Mirror U?     */
        var mMirrorU: Boolean = false,

        /** Mirror V?     */
        var mMirrorV: Boolean = false,

        /** Blend mode     */
        var mOp: AiTexture.Op = AiTexture.Op.multiply,

        /** UV transformation         */
        var mTransform: AiUVTransform = AiUVTransform(),

        /** Name of source UV channel         */
        var mUVChannel: String = "",

        /** Resolved UV channel index or UINT_MAX if not known         */
        var mUVId: Int = 0xFFFFFFFF.i,

        // OKINO/MAX3D extensions from here
        // -------------------------------------------------------

        /** Weighting factor                 */
        var mWeighting: Float = 1f,

        /** Mixing factor from OKINO */
        var mMixWithPrevious: Float = 1f)

typealias ParamLibrary = MutableMap<String, EffectParam>

/** A collada effect. Can contain about anything according to the Collada spec, but we limit our version to a reasonable subset. */
class Effect(
        // Shading mode
        var mShadeType: ShadeType = ShadeType.Phong,

        // Colors
        var mEmissive: AiColor4D = AiColor4D(0, 0, 0, 1),
        var mAmbient: AiColor4D = AiColor4D(.1f, .1f, .1f, 1f),
        var mDiffuse: AiColor4D = AiColor4D(.6f, .6f, .6f, 1f),
        var mSpecular: AiColor4D = AiColor4D(.4, .4f, .4f, 1f),
        var mTransparent: AiColor4D = AiColor4D(0, 0, 0, 1),
        var mReflective: AiColor4D = AiColor4D(),

        // TODO merge 'em all
        // Textures
        var mTexEmissive: Sampler = Sampler(),
        var mTexAmbient: Sampler = Sampler(),
        var mTexDiffuse: Sampler = Sampler(),
        var mTexSpecular: Sampler = Sampler(),
        var mTexTransparent: Sampler = Sampler(),
        var mTexBump: Sampler = Sampler(),
        var mTexReflective: Sampler = Sampler(),

        // Scalar factory
        var mShininess: Float = 10f,
        var mRefractIndex: Float = 1f,
        var mReflectivity: Float = 0f,
        var mTransparency: Float = 1f,
        var mHasTransparency: Boolean = false,
        var mRGBTransparency: Boolean = false,
        var mInvertTransparency: Boolean = false,

        // local params referring to each other by their SID
        var mParams: ParamLibrary = mutableMapOf(),

        // MAX3D extensions
        // ---------------------------------------------------------
        // Double-sided?
        var mDoubleSided: Boolean = false,
        var mWireframe: Boolean = false,
        var mFaceted: Boolean = false)

/** An image, meaning texture */
class Image(

        var mFileName: String = "",

        /** If image file name is zero, embedded image data     */
        var mImageData: ByteArray = byteArrayOf(),

        /** If image file name is zero, file format of embedded image data.     */
        var mEmbeddedFormat: String = "")

/** An animation channel. */
class AnimationChannel(

        /** URL of the data to animate. Could be about anything, but we support only the "NodeID/TransformID.SubElement" notation     */
        var mTarget: String = "",

        /** Source URL of the time values. Collada calls them "input". Meh. */
        var mSourceTimes: String = "",
        /** Source URL of the value values. Collada calls them "output". */
        var mSourceValues: String = "",
        /** Source URL of the IN_TANGENT semantic values. */
        var mInTanValues: String = "",
        /** Source URL of the OUT_TANGENT semantic values. */
        var mOutTanValues: String = "",
        /** Source URL of the INTERPOLATION semantic values. */
        var mInterpolationValues: String = "")

/** An animation. Container for 0-x animation channels or 0-x animations */
class Animation(
        /** Anim name */
        var mName: String = "",

        /** the animation channels, if any */
        var mChannels: ArrayList<AnimationChannel> = arrayListOf(),

        /** the sub-animations, if any */
        var mSubAnims: ArrayList<Animation> = arrayListOf()) {


    /** Collect all channels in the animation hierarchy into a single channel list. */
    fun collectChannelsRecursively(channels: ArrayList<AnimationChannel>) {
        channels.addAll(mChannels)
        mSubAnims.forEach { it.collectChannelsRecursively(channels) }
    }

    /** Combine all single-channel animations' channel into the same (parent) animation channel list. */
    fun combineSingleChannelAnimations() = combineSingleChannelAnimationsRecursively(this)

    fun combineSingleChannelAnimationsRecursively(parent: Animation) {
        val iterator = parent.mSubAnims.iterator()
        while (iterator.hasNext()) {
            val anim = iterator.next()
            combineSingleChannelAnimationsRecursively(anim)
            if (anim.mChannels.size == 1) {
                parent.mChannels.add(anim.mChannels[0])
                iterator.remove()
            }
        }
    }
}

/** Description of a collada animation channel which has been determined to affect the current node */
class ChannelEntry() {
    /** the source channel  */
    var mChannel = AnimationChannel()

    var targetId = ""
    /** the ID of the transformation step of the node which is influenced   */
    var mTransformId = ""
    /** Index into the node's transform chain to apply the channel to   */
    var mTransformIndex = 0L
    /** starting index inside the transform data    */
    var mSubElement = 0L

    // resolved data references
    /** Collada accessor to the time values */
    var mTimeAccessor = Accessor()
    /** Source data array for the time values   */
    var timeData = Data()
    /** Collada accessor to the key value values    */
    var mValueAccessor = Accessor()
    /** Source datat array for the key value values */
    var valueData = Data()
}