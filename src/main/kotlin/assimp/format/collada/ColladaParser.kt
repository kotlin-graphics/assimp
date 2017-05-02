package assimp.format.collada

import assimp.*
import glm.*
import glm.mat4x4.Mat4
import java.io.File
import java.io.FileReader
import java.net.URI
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent
import kotlin.collections.set


/**
 * Created by elect on 23/01/2017.
 */

typealias DataLibrary = MutableMap<String, Data>
typealias AccessorLibrary = MutableMap<String, Accessor>
typealias MeshLibrary = MutableMap<String, Mesh>
typealias NodeLibrary = MutableMap<String, Node>
typealias ImageLibrary = MutableMap<String, Image>
typealias EffectLibrary = MutableMap<String, Effect>
typealias MaterialLibrary = MutableMap<String, Material>
typealias LightLibrary = MutableMap<String, Light>
typealias CameraLibrary = MutableMap<String, Camera>
typealias ControllerLibrary = MutableMap<String, Controller>
typealias AnimationLibrary = MutableMap<String, Animation>
typealias AnimationClipLibrary = ArrayList<Pair<String, ArrayList<String>>>

typealias ChannelMap = MutableMap<String, AnimationChannel>

/** Parser helper class for the Collada loader.
 *
 *  Does all the XML reading and builds internal data structures from it,
 *  but leaves the resolving of all the references to the loader.
 */
class ColladaParser(pFile: URI) {

    /** Filename, for a verbose error message */
    internal var mFileName = pFile

    /** XML reader, member for everyday use */
    internal lateinit var mReader: XMLEventReader
    internal lateinit var event: XMLEvent
    internal lateinit var element: StartElement
    internal lateinit var endElement: EndElement

    /** All data arrays found in the file by ID. Might be referred to by actually
    everyone. Collada, you are a steaming pile of indirection. */
    internal var mDataLibrary: DataLibrary = mutableMapOf()

    /** Same for accessors which define how the data in a data array is accessed. */
    internal var mAccessorLibrary: AccessorLibrary = mutableMapOf()

    /** Mesh library: mesh by ID */
    internal var mMeshLibrary: MeshLibrary = mutableMapOf()

    /** node library: root node of the hierarchy part by ID */
    internal var mNodeLibrary: NodeLibrary = mutableMapOf()

    /** Image library: stores texture properties by ID */
    internal var mImageLibrary: ImageLibrary = mutableMapOf()

    /** Effect library: surface attributes by ID */
    internal var mEffectLibrary: EffectLibrary = mutableMapOf()

    /** Material library: surface material by ID */
    internal var mMaterialLibrary: MaterialLibrary = mutableMapOf()

    /** Light library: surface light by ID */
    internal var mLightLibrary: LightLibrary = mutableMapOf()

    /** Camera library: surface material by ID */
    internal var mCameraLibrary: CameraLibrary = mutableMapOf()

    /** Controller library: joint controllers by ID */
    internal var mControllerLibrary: ControllerLibrary = mutableMapOf()

    /** Animation library: animation references by ID */
    internal var mAnimationLibrary: AnimationLibrary = mutableMapOf()

    /** Animation clip library: clip animation references by ID */
    internal var mAnimationClipLibrary: AnimationClipLibrary = ArrayList()

    /** Pointer to the root node. Don't delete, it just points to one of
    the nodes in the node library. */
    internal var mRootNode: Node? = null

    /** Root animation container */
    internal var mAnims = Animation()

    /** Size unit: how large compared to a meter */
    internal var mUnitSize = 1f

    internal var mUpDirection = UpDirection.Y

    /** Which is the up vector */
    enum class UpDirection { X, Y, Z }

    /** Collada file format version */
    internal var mFormat = FormatVersion._1_5_n // We assume the newest file format by default

    init {

        val file = File(pFile)
        // open the file
        if (!file.exists())
            throw Error("Failed to open file: $pFile")

//        val dbFactory = DocumentBuilderFactory.newInstance()
//        val dBuilder = dbFactory.newDocumentBuilder()
//        val doc = dBuilder.parse(file)
//        doc.getDocumentElement().normalize()
//
//        val traversal = doc as DocumentTraversal
//
//        val walker = traversal.createTreeWalker(
//                doc.getDocumentElement(),
//                NodeFilter.SHOW_ELEMENT, null, true)
//
//        var n = walker.nextNode()
//        while(n!=null) {
//            println(n.nodeName)
//            n = n.nextSibling
//        }

        // generate a XML reader for it
        val factory = XMLInputFactory.newInstance()

        mReader = factory.createXMLEventReader(FileReader(file))

        // start reading
        readContents()
    }

    /** Read bool from text contents of current element */
    internal fun readBoolFromTextContent(): Boolean {
        val cur = getTextContent().trimStart()
        return cur.startsWith("true") || cur[0] == '1'
    }

    /** Read float from text contents of current element    */
    internal fun readFloatFromTextContent() = getTextContent().trimStart().split("\\s+".toRegex())[0].f

    /** Reads the contents of the file  */
    internal fun readContents() {

        while (mReader.read())

//            if(event.isAttribute)   println("attribute")
//            if(event.isCharacters)   println("characters ${event.asCharacters()}")
//            if(event.isEndDocument)   println("endDocument")
//            if(event.isEndElement)   println("endElement: ${event.asEndElement().name.localPart}")
//            if(event.isEntityReference)   println("entityReference")
//            if(event.isNamespace)   println("namespace")
//            if(event.isProcessingInstruction)   println("processingInstruction")
//            if(event.isStartDocument)   println("startDocument")
//            if(event.isStartElement)   println("startElement: ${event.asStartElement().name.localPart}")

        // handle the root element "COLLADA"
            if (event is StartElement)

                if (element.name_ == "COLLADA") {

                    val a = element["version"]

                    element["version"]?.let {

                        // check for 'version' attribute
                        if (it.startsWith("1.5")) {
                            mFormat = FormatVersion._1_5_n
                            println("Collada schema version is 1.5.n")
                        } else if (it.startsWith("1.4")) {
                            mFormat = FormatVersion._1_4_n
                            println("Collada schema version is 1.4.n")
                        } else if (it.startsWith("1.3")) {
                            mFormat = FormatVersion._1_3_n
                            println("Collada schema version is 1.3.n")
                        }
                    }
                    readStructure()

                } else {
                    println("Ignoring global element ${element.name}>.")
                    skipElement()
                }
            else {  // skip everything else silently
            }

        val s = '§'
    }

    /** Reads the structure of the file */
    internal fun readStructure() {

        while (mReader.read()) {

            // beginning of elements
            if (event is StartElement)

                when (element.name_) {

                    "asset" -> readAssetInfo()

                    "library_animations" -> readAnimationLibrary()

                    "library_animation_clips" -> readAnimationClipLibrary()

                    "library_controllers" -> readControllerLibrary()

                    "library_images" -> readImageLibrary()

                    "library_materials" -> readMaterialLibrary()

                    "library_effects" -> readEffectLibrary()

                    "library_geometries" -> readGeometryLibrary()

                    "library_visual_scenes" -> readSceneLibrary()

                    "library_lights" -> readLightLibrary()

                    "library_cameras" -> readCameraLibrary()

                    "library_nodes" -> readSceneNode(null)  /* some hacking to reuse this piece of code */

                    "scene" -> readScene()

                    else -> skipElement()
                }
            else if (event is EndElement)
                break
        }
        postProcessRootAnimations()
    }

    /** Reads asset informations such as coordinate system informations and legal blah  */
    fun readAssetInfo() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement) {

                when (element.name_) {

                    "unit" -> {
                        // read unit data from the element's attributes
                        mUnitSize = (element["meter"] ?: "1").f

                        // consume the trailing stuff
                        if (!isEmptyElement())
                            skipElement()
                    }
                    "up_axis" -> {
                        // read content, strip whitespace, compare
                        mUpDirection = when (getTextContent()) {
                            "X_UP" -> UpDirection.X
                            "Y_UP" -> UpDirection.Y
                            else -> UpDirection.Z
                        }
                        // check element end
                        testClosing("up_axis")
                    }
                    else -> skipElement()
                }

            } else if (event is EndElement) {

                if (endElement.name_ != "asset")
                    throw Exception("Expected end of <asset> element.")

                break
            }
    }

    /** Reads the animation clips   */
    fun readAnimationClipLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "animation_clip") {

                    // optional name given as an attribute
                    val animName = element["name"] ?: element["id"] ?: "animation_${mAnimationClipLibrary.size}"

                    val clip = Pair(animName, ArrayList<String>())

                    while (mReader.read())

                        if (event is StartElement)

                            if (element.name_ == "instance_animation")

                                element["url"]?.let {

                                    val url = it
                                    if (url[0] != '#')
                                        throw Exception("Unknown reference format")

                                    clip.second.add(url.removePrefix("#"))
                                }
                            else
                                skipElement()   // ignore the rest
//
                        else if (event is EndElement) {

                            if (endElement.name_ != "animation_clip")
                                throw Exception("Expected end of <animation_clip> element.")

                            break
                        }

                    if (clip.second.size > 0)
                        mAnimationClipLibrary.add(clip)

                } else
                    skipElement()   // ignore the rest
//
            else if (event is EndElement) {

                if (endElement.name_ != "library_animation_clips")
                    throw Exception("Expected end of <library_animation_clips> element.")

                break
            }
    }

    /** Re-build animations from animation clip library, if present, otherwise combine single-channel animations    */
    internal fun postProcessRootAnimations() {

        if (mAnimationClipLibrary.size > 0) {

            val temp = Animation()

            mAnimationClipLibrary.forEach {

                val clipName = it.first

                val clip = Animation(mName = clipName)

                temp.mSubAnims.add(clip)

                it.second.forEach {
                    mAnimationLibrary[it]?.collectChannelsRecursively(clip.mChannels)
                }
            }

            mAnims = temp

            // Ensure no double deletes.
            temp.mSubAnims.clear()
        } else
            mAnims.combineSingleChannelAnimations()
    }

    /** Reads the animation library */
    fun readAnimationLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "animation")
                // delegate the reading. Depending on the inner elements it will be a container or a anim channel
                    readAnimation(mAnims)
                else
                    skipElement()   // ignore the rest
//
            else if (event is EndElement) {

                if (endElement.name_ != "library_animations")
                    throw Exception("Expected end of <library_animations> element.")

                break
            }
    }

    /** Reads an animation into the given parent structure  */
    fun readAnimation(pParent: Animation?) {

        // an <animation> element may be a container for grouping sub-elements or an animation channel
        // this is the channel collection by ID, in case it has channels
        val channels: ChannelMap = mutableMapOf()
        // this is the anim container in case we're a container
        var anim: Animation? = null

        // optional name given as an attribute
        val animName = element["name"] ?: element["id"] ?: "animation"
        val animID = element["id"]

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                // we have subanimations
                    "animation" -> {

                        // create container from our element
                        if (anim == null) {
                            anim = Animation(mName = animName)
                            pParent!!.mSubAnims.add(anim)
                        }

                        // recurse into the subelement
                        readAnimation(anim)
                    }

                    "source" -> readSource()    // possible animation data - we'll never know. Better store it

                    "sampler" -> {

                        // read the ID to assign the corresponding collada channel afterwards.
                        val id = element["id"]!!
                        channels[id] = AnimationChannel()

                        // have it read into a channel
                        readAnimationSampler(channels[id]!!)
                    }

                    "channel" -> {
                        // the binding element whose whole purpose is to provide the target to animate
                        // Thanks, Collada! A directly posted information would have been too simple, I guess.
                        // Better add another indirection to that! Can't have enough of those.
                        var sourceId = element["source"]!!
                        if (sourceId[0] == '#')
                            sourceId = sourceId.removePrefix("#")
                        channels[sourceId]?.let { cit ->
                            cit.mTarget = element["target"]!!
                        }

                        if (!isEmptyElement())
                            skipElement()
                    }

                    else -> skipElement()   // ignore the rest
                }
            else if (event is EndElement) {

                if (endElement.name_ != "animation")
                    throw Exception("Expected end of <animation> element.")

                break
            }

        // it turned out to have channels - add them
        if (channels.isNotEmpty()) {
            // FIXME: Is this essentially doing the same as "single-anim-node" codepath in ColladaLoader::StoreAnimations? For now, this has been deferred to after
            //        all animations and all clips have been read. Due to handling of <library_animation_clips> this cannot be done here, as the channel owner is
            //        lost, and some exporters make up animations by referring to multiple single-channel animations from an <instance_animation>.

/*        // special filtering for stupid exporters packing each channel into a separate animation
        if( channels.size() == 1)
        {
            pParent->mChannels.push_back( channels.begin()->second);
        } else {
*/
            // else create the animation, if not done yet, and store the channels
            if (anim == null) {

                anim = Animation(mName = animName)
                pParent!!.mSubAnims.add(anim)
            }
            anim.mChannels.addAll(channels.values)

            animID?.let { mAnimationLibrary[animID] = anim!! }
//            }
        }
    }

    /** Reads an animation sampler into the given anim channel  */
    internal fun readAnimationSampler(pChannel: AnimationChannel) {

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "input") {

                    val semantic = element["semantic"]!!
                    var source = element["source"]!!
                    if (source[0] != '#')
                        throw Exception("Unsupported URL format")
                    source = source.removePrefix("#")

                    when (semantic) {
                        "INPUT" -> pChannel.mSourceTimes = source
                        "OUTPUT" -> pChannel.mSourceValues = source
                        "IN_TANGENT" -> pChannel.mInTanValues = source
                        "OUT_TANGENT" -> pChannel.mOutTanValues = source
                        "INTERPOLATION" -> pChannel.mInterpolationValues = source
                    }
                    if (!isEmptyElement())
                        skipElement()

                } else // ignore the rest
                    skipElement()
//
            else if (event is EndElement) {

                if (endElement.name_ != "sampler")
                    throw Exception("Expected end of <sampler> element.")

                break
            }
    }

    /** Reads the skeleton controller library   */
    internal fun readControllerLibrary() {

        if (isEmptyElement())
            return

        while ((mReader.read()))

            if (event is StartElement)

                if (element.name_ == "controller") {

                    // read ID. Ask the spec if it's necessary or optional... you might be surprised.
                    val id = element["id"]!!

                    // create an entry and store it in the library under its ID
                    mControllerLibrary[id] = Controller()

                    // read on from there
                    readController(mControllerLibrary[id]!!)

                } else
                    skipElement()   // ignore the rest
            //
            else if (event is EndElement) {

                if (endElement.name_ != "library_controllers")
                    throw Exception("Expected end of <library_controllers> element.")

                break
            }
    }

    /** Reads a controller into the given mesh structure    */
    internal fun readController(pController: Controller) {

        // initial values
        pController.mType = ControllerType.Skin
        pController.mMethod = MorphMethod.Normalized

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                // two types of controllers: "skin" and "morph". Only the first one is relevant, we skip the other
                    "morph" -> {
                        pController.mType = ControllerType.Morph
                        pController.mMeshId = element["source"]!!.substring(1)
                        element["method"]?.let {
                            if (it == "RELATIVE")
                                pController.mMethod = MorphMethod.Relative
                        }
                    }

                    "skin" ->
                        // read the mesh it refers to. According to the spec this could also be another controller, but I refuse to implement every single idea
                        // they've come up with
                        pController.mMeshId = element["source"]!!.substring(1)

                    "bind_shape_matrix" -> {
                        // content is 16 floats to define a matrix... it seems to be important for some models
                        val content = getTextContent()

                        // read the 16 floats
                        pController.mBindShapeMatrix = content.words.map { it.f }.toFloatArray()

                        testClosing("bind_shape_matrix")
                    }

                    "source" -> readSource()    // data array - we have specialists to handle this

                    "joints" -> readControllerJoints(pController)

                    "vertex_weights" -> readControllerWeights(pController)

                    "targets" -> {

                        while (mReader.read())

                            if (event is StartElement) {

                                if (element.name_ == "input") {

                                    val semantics = element["semantic"]!!
                                    val source = element["source"]!!

                                    if (semantics == "MORPH_TARGET")
                                        pController.mMorphTarget = source.substring(1)
                                    else if (semantics == "MORPH_WEIGHT")
                                        pController.mMorphWeight = source.substring(1)
                                }
                            } else if (event is EndElement)
                                if (endElement.name_ == "targets")
                                    break
                                else
                                    throw Exception("Expected end of <targets> element.")
                    }

                    else -> skipElement()   // ignore the rest
                }
            else if (event is EndElement) {

                if (endElement.name_ == "controller")
                    break
                else if (endElement.name_ != "skin" && endElement.name_ != "morph")
                    throw Exception("Expected end of <controller> element.")
            }
    }

    /** Reads the joint definitions for the given controller    */
    internal fun readControllerJoints(pController: Controller) {

        while (mReader.read())

            if (event is StartElement)
            // Input channels for joint data. Two possible semantics: "JOINT" and "INV_BIND_MATRIX"
                if (element.name_ == "input") {

                    val semantic = element["semantic"]!!
                    var source = element["source"]!!

                    // local URLS always start with a '#'. We don't support global URLs
                    if (source[0] != '#')
                        throw Exception("Unsupported URL format in $source in source attribute of <joints> data <input> element")
                    source = source.removePrefix("#")

                    // parse source URL to corresponding source
                    when (semantic) {
                        "JOINT" -> pController.mJointNameSource = source
                        "INV_BIND_MATRIX" -> pController.mJointOffsetMatrixSource = source
                        else -> throw Exception("Unknown semantic $semantic in <joints> data <input> element")
                    }

                    // skip inner data, if present
                    if (!isEmptyElement())
                        skipElement()

                } else
                    skipElement()    // ignore the rest
            //
            else if (event is EndElement) {

                if (endElement.name_ != "joints")
                    throw Exception("Expected end of <joints> element.")

                break
            }
    }

    /** Reads the joint weights for the given controller    */
    internal fun readControllerWeights(pController: Controller) {

        // read vertex count from attributes and resize the array accordingly
        val vertexCount = element["count"]!!.i
        pController.mWeightCounts = MutableList(vertexCount, { 0 })

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {
                // Input channels for weight data. Two possible semantics: "JOINT" and "WEIGHT"
                    "input" -> if (vertexCount > 0) {

                        val channel = InputChannel()

                        val semantic = element["semantic"]!!
                        val source = element["source"]!!
                        element["offset"]?.let { channel.mOffset = it.i }

                        // local URLS always start with a '#'. We don't support global URLs
                        if (source[0] != '#')
                            throw Exception("Unsupported URL format in $source in source attribute of <vertex_weights> data <input> element")
                        channel.mAccessor = source.removePrefix("#")

                        // parse source URL to corresponding source
                        when (semantic) {
                            "JOINT" -> pController.mWeightInputJoints = channel
                            "WEIGHT" -> pController.mWeightInputWeights = channel
                            else -> throw Exception("Unknown semantic $semantic in <vertex_weights> data <input> element")
                        }

                        // skip inner data, if present
                        if (!isEmptyElement())
                            skipElement()
                    }

                    "vcount" -> if (vertexCount > 0) {

                        // read weight count per vertex
                        val ints = getTextContent().words.map { it.i }
                        var numWeights = 0
                        var i = 0
                        pController.mWeightCounts.replaceAll {

                            if (i == ints.size)
                                throw Exception("Out of data while reading <vcount>")

                            val int = ints[i++]
                            numWeights += int
                            int
                        }

                        testClosing("vcount")

                        // reserve weight count
                        pController.mWeights = MutableList(numWeights, { Pair(0, 0) })
                    }

                    "v" -> if (vertexCount > 0) {

                        // read JointIndex - WeightIndex pairs
                        val ints = getTextContent().words.map { it.i }
                        var i = 0
                        // FIXME crap solution, alternatives?
                        pController.mWeights.replaceAll { weight ->
                            if (i > ints.size - 1)
                                throw Exception("Out of data while reading <vertex_weights>")
                            Pair(i++, i++)
                        }

                        testClosing("v")
                    }

                    else -> skipElement()   // ignore the rest
                }
            //
            else if (event is EndElement) {

                if (endElement.name_ != "vertex_weights")
                    throw Exception("Expected end of <vertex_weights> element.")

                break
            }
    }

    /** Reads the image library contents    */
    internal fun readImageLibrary() {

        if (!isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "image") {
                    // read ID. Another entry which is "optional" by design but obligatory in reality
                    val id = element["id"]!!

                    // create an entry and store it in the library under its ID
                    mImageLibrary[id] = Image()

                    // read on from there
                    readImage(mImageLibrary[id]!!)

                } else
                    skipElement()   // ignore the rest

            else if (event is EndElement) {
                if (endElement.name_ != "library_images")
                    throw Exception("Expected end of <library_images> element.")

                break
            }
    }

    /** Reads an image entry into the given image   */
    internal fun readImage(pImage: Image) {

        while (mReader.read())

            if (event is StartElement)
            // Need to run different code paths here, depending on the Collada XSD version
                if (element.name_ == "image")
                    skipElement()
//
                else if (element.name_ == "init_from") {

                    if (mFormat == FormatVersion._1_4_n) {
                        // FIX: C4D exporter writes empty <init_from/> tags
                        if (!isEmptyElement()) {
                            // element content is filename - hopefully
                            testTextContent()?.let { pImage.mFileName = it }
                            testClosing("init_from")
                        }
                        if (pImage.mFileName.isEmpty())
                            pImage.mFileName = "unknown_texture"

                    } else if (mFormat == FormatVersion._1_5_n) {
                        // make sure we skip over mip and array initializations, which we don't support, but which could confuse the loader if they're not skipped.
                        // but in Kotlin we don't need ^^
//                        element["array_index"]?.i.let {
//                            if (attrib != -1 && mReader->getAttributeValueAsInt(attrib) > 0) {
//                            DefaultLogger::get()->warn("Collada: Ignoring texture array index");
//                            continue;
//                        }
//                        }
//
//                        attrib = TestAttribute("mip_index");
//                        if (attrib != -1 && mReader->getAttributeValueAsInt(attrib) > 0) {
//                            DefaultLogger::get()->warn("Collada: Ignoring MIP map layer");
//                            continue;
//                        }

                        // TODO: correctly jump over cube and volume maps?
                    }
                } else if (mFormat == FormatVersion._1_5_n) {

                    if (element.name_ == "ref") {

                        // element content is filename - hopefully
                        testTextContent()?.let { pImage.mFileName = it }
                        testClosing("ref")

                    } else if (element.name_ == "hex" && pImage.mFileName.isNotEmpty()) {

                        // embedded image. get format
                        val format = element["format"]
                        if (format == null)
                            System.err.println("Collada: Unknown image file format")
                        else pImage.mEmbeddedFormat = format

                        val data = getTextContent()

                        // hexadecimal-encoded binary octets. First of all, find the required buffer size to reserve enough storage.
                        pImage.mImageData = hexStringToByteArray(data)

                        testClosing("hex")
                    }
                } else
                    skipElement()   // ignore the rest

            else if (event is EndElement && endElement.name_ == "image")
                break
    }

    /** Reads the material library  */
    internal fun readMaterialLibrary() {

        if (isEmptyElement())
            return

        val names = mutableMapOf<String, Int>()
        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "material") {

                    // read ID. By now you probably know my opinion about this "specification"
                    val id = element["id"]!!

                    var name = element["name"] ?: ""

                    // create an entry and store it in the library under its ID
                    mMaterialLibrary[id] = Material()

                    if (name.isNotEmpty()) {

                        if (names.contains(name)) {
                            val nextId = names.keys.sorted().indexOf(name) + 1
                            val nextName = names.keys.sorted()[nextId]
                            name += " " + names[nextName]
                        } else
                            names[name] = 0

                        mMaterialLibrary[id]!!.mName = name
                    }

                    readMaterial(mMaterialLibrary[id]!!)

                } else
                    skipElement()   // ignore the rest

            else if (event is EndElement) {
                if (endElement.name_ != "library_materials")
                    throw Exception("Expected end of <library_materials> element.")

                break
            }

    }

    /** Reads the light library */
    internal fun readLightLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read()) {

            if (event is StartElement) {

                if (element.name_ == "light") {

                    // read ID. By now you probably know my opinion about this "specification"
                    val id = element["light"]!!

                    // create an entry and store it in the library under its ID
                    val light = Light()
                    mLightLibrary[id] = Light()
                    readLight(light)

                } else skipElement()    // ignore the rest

            } else if (event is EndElement) {
                if (endElement.name_ != "library_lights")
                    throw Exception("Expected end of <library_lights> element.")

                break
            }
        }
    }

    /** Reads the camera library    */
    internal fun readCameraLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "camera") {

                    // read ID. By now you probably know my opinion about this "specification"
                    val id = element["id"]!!

                    // create an entry and store it in the library under its ID
                    val cam = mCameraLibrary[id]!!
                    element["name"]?.let {
                        cam.mName = it
                    }

                    readCamera(cam)

                } else
                    skipElement()   // ignore the rest

            else if (event is EndElement) {
                if (endElement.name_ != "library_cameras")
                    throw Exception("Expected end of <library_cameras> element.")

                break
            }
    }

    /** Reads a material entry into the given material  */
    internal fun readMaterial(pMaterial: Material) {

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "material" -> skipElement()

                    "instance_effect" -> {
                        // referred effect by URL
                        val url = element["url"]!!
                        if (url[0] != '#')
                            throw Exception("Unknown reference format")

                        pMaterial.mEffect = url.substring(1)

                        skipElement()
                    }
                    else -> skipElement()   // ignore the rest
                }
            else if (event is EndElement) {

                if (endElement.name_ != "material")
                    throw Exception("Expected end of <material> element.")

                break
            }
    }

    /** Reads a light entry into the given light    */
    internal fun readLight(pLight: Light) {

        while (mReader.read())

            if (event is StartElement) {

                when (element.name_) {

                    "light" -> skipElement()

                    "spot" -> pLight.mType = AiLightSourceType.SPOT

                    "ambient" -> pLight.mType = AiLightSourceType.AMBIENT

                    "directional" -> pLight.mType = AiLightSourceType.DIRECTIONAL

                    "point" -> pLight.mType = AiLightSourceType.POINT

                    "color" -> {
                        // text content contains 3 floats
                        val floats = getTextContent().words.map { it.f }

                        pLight.mColor = AiColor3D(floats)

                        testClosing("color")
                    }
                    "constant_attenuation" -> {
                        pLight.mAttConstant = readFloatFromTextContent()
                        testClosing("constant_attenuation")
                    }
                    "linear_attenuation" -> {
                        pLight.mAttLinear = readFloatFromTextContent()
                        testClosing("linear_attenuation")
                    }
                    "quadratic_attenuation" -> {
                        pLight.mAttQuadratic = readFloatFromTextContent()
                        testClosing("quadratic_attenuation")
                    }
                    "falloff_angle" -> {
                        pLight.mFalloffAngle = readFloatFromTextContent()
                        testClosing("falloff_angle")
                    }
                    "falloff_exponent" -> {
                        pLight.mFalloffExponent = readFloatFromTextContent()
                        testClosing("falloff_exponent")
                    }
                // FCOLLADA extensions
                // -------------------------------------------------------
                    "outer_cone" -> {
                        pLight.mOuterAngle = readFloatFromTextContent()
                        testClosing("outer_cone")
                    }
                // ... and this one is even deprecated
                    "penumbra_angle" -> {
                        pLight.mPenumbraAngle = readFloatFromTextContent()
                        testClosing("penumbra_angle")
                    }
                    "intensity" -> {
                        pLight.mIntensity = readFloatFromTextContent()
                        testClosing("intensity")
                    }
                    "falloff" -> {
                        pLight.mOuterAngle = readFloatFromTextContent()
                        testClosing("falloff")
                    }
                    "hotspot_beam" -> {
                        pLight.mFalloffAngle = readFloatFromTextContent()
                        testClosing("hotspot_beam")
                    }
                // OpenCOLLADA extensions
                // -------------------------------------------------------
                    "decay_falloff" -> {
                        pLight.mOuterAngle = readFloatFromTextContent()
                        testClosing("decay_falloff")
                    }
                }
            } else if (event is EndElement)
                if (endElement.name_ == "light")
                    break
    }

    /** Reads a camera entry into the given light   */
    internal fun readCamera(pCamera: Camera) {

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "camera" -> skipElement()

                    "orthographic" -> pCamera.mOrtho = true

                    "xfov", "xmag" -> {
                        pCamera.mHorFov = readFloatFromTextContent()
                        testClosing(if (pCamera.mOrtho) "xmag" else "xfov")
                    }
                    "yfov", "ymag" -> {
                        pCamera.mVerFov = readFloatFromTextContent()
                        testClosing(if (pCamera.mOrtho) "ymag" else "yfov")
                    }
                    "aspect_ratio" -> {
                        pCamera.mAspect = readFloatFromTextContent()
                        testClosing("aspect_ratio")
                    }
                    "znear" -> {
                        pCamera.mZNear = readFloatFromTextContent()
                        testClosing("znear")
                    }
                    "zfar" -> {
                        pCamera.mZFar = readFloatFromTextContent()
                        testClosing("zfar")
                    }
                }
            else if (event is EndElement)
                if (endElement.name_ == "camera")
                    break
    }

    /** Reads the effect library    */
    internal fun readEffectLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "effect") {

                    // read ID. Do I have to repeat my ranting about "optional" attributes?
                    val id = element["id"]!!

                    // create an entry and store it in the library under its ID
                    mEffectLibrary[id] = Effect()
                    // read on from there
                    readEffect(mEffectLibrary[id]!!)

                } else
                    skipElement()   // ignore the rest
//
            else if (event is EndElement) {

                if (endElement.name_ != "library_effects")
                    throw Exception("Expected end of <library_effects> element.")

                break
            }
    }

    /** Reads an effect entry into the given effect */
    internal fun readEffect(pEffect: Effect) {

        // for the moment we don't support any other type of effect.
        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "profile_COMMON")
                    readEffectProfileCommon(pEffect)
                else
                    skipElement()
            else if (event is EndElement) {

                if (endElement.name_ == "effect")
                    throw Exception("Expected end of <effect> element.")

                break
            }
    }

    /** Reads an COMMON effect profile  */
    internal fun readEffectProfileCommon(pEffect: Effect) {

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "newparam" -> {
                        // save ID
                        val sid = element["sid"]!!
                        pEffect.mParams[sid] = EffectParam()
                        readEffectParam(pEffect.mParams[sid]!!)
                    }
                    "technique", "extra" -> {   // just syntactic sugar
                    }

                    "image" -> if (mFormat == FormatVersion._1_4_n) {

                        // read ID. Another entry which is "optional" by design but obligatory in reality
                        val id = element["id"]!!

                        // create an entry and store it in the library under its ID
                        mImageLibrary[id] = Image()

                        // read on from there
                        readImage(mImageLibrary[id]!!)
                    }

                /* Shading modes */

                    "phong" -> pEffect.mShadeType = ShadeType.Phong

                    "constant" -> pEffect.mShadeType = ShadeType.Constant

                    "lambert" -> pEffect.mShadeType = ShadeType.Lambert

                    "blinn" -> pEffect.mShadeType = ShadeType.Blinn

                /* Color + texture properties */

                    "emission" -> readEffectColor(pEffect.mEmissive, pEffect.mTexEmissive)

                    "ambient" -> readEffectColor(pEffect.mAmbient, pEffect.mTexAmbient)

                    "diffuse" -> readEffectColor(pEffect.mDiffuse, pEffect.mTexDiffuse)

                    "specular" -> readEffectColor(pEffect.mSpecular, pEffect.mTexSpecular)

                    "reflective" -> readEffectColor(pEffect.mReflective, pEffect.mTexReflective)

                    "transparent" -> {

                        pEffect.mHasTransparency = true

                        val opaque = element["opaque"] ?: ""

                        if (opaque == "RGB_ZERO" || opaque == "RGB_ONE")
                            pEffect.mRGBTransparency = true

                        // In RGB_ZERO mode, the transparency is interpreted in reverse, go figure...
                        if (opaque == "RGB_ZERO" || opaque == "A_ZERO")
                            pEffect.mInvertTransparency = true

                        readEffectColor(pEffect.mTransparent, pEffect.mTexTransparent)
                    }

                    "shininess" -> pEffect.mShininess = readEffectFloat(pEffect.mShininess)

                    "reflectivity" -> pEffect.mReflectivity = readEffectFloat(pEffect.mReflectivity)

                /* Single scalar properties */

                    "transparency" -> pEffect.mTransparency = readEffectFloat(pEffect.mTransparency)

                    "index_of_refraction" -> pEffect.mRefractIndex = readEffectFloat(pEffect.mRefractIndex)

                // GOOGLEEARTH/OKINO extensions
                // -------------------------------------------------------

                    "double_sided" -> pEffect.mDoubleSided = readBoolFromTextContent()

                // FCOLLADA extensions
                // -------------------------------------------------------

                    "bump" -> readEffectColor(AiColor4D(), pEffect.mTexBump)

                // MAX3D extensions
                // -------------------------------------------------------

                    "wireframe" -> {
                        pEffect.mWireframe = readBoolFromTextContent()
                        testClosing("wireframe")
                    }
                    "faceted" -> {
                        pEffect.mFaceted = readBoolFromTextContent()
                        testClosing("faceted")
                    }
                    else -> skipElement()   // ignore the rest
                }
            else if (event is EndElement && endElement.name_ == "profile_COMMON")
                break
    }

    /** Read texture wrapping + UV transform settings from a profile==Maya chunk    */
    internal fun readSamplerProperties(oSampler: Sampler) {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {
                // MAYA extensions
                // -------------------------------------------------------
                    "wrapU" -> {
                        oSampler.mWrapU = readBoolFromTextContent()
                        testClosing("wrapU")
                    }
                    "wrapV" -> {
                        oSampler.mWrapV = readBoolFromTextContent()
                        testClosing("wrapV")
                    }
                    "mirrorU" -> {
                        oSampler.mMirrorU = readBoolFromTextContent()
                        testClosing("mirrorU")
                    }
                    "mirrorV" -> {
                        oSampler.mMirrorV = readBoolFromTextContent()
                        testClosing("mirrorV")
                    }
                    "repeatU" -> {
                        oSampler.mTransform.mScaling.x = readFloatFromTextContent()
                        testClosing("repeatU")
                    }
                    "repeatV" -> {
                        oSampler.mTransform.mScaling.y = readFloatFromTextContent()
                        testClosing("repeatV")
                    }
                    "offsetU" -> {
                        oSampler.mTransform.mTranslation.x = readFloatFromTextContent()
                        testClosing("offsetU")
                    }
                    "offsetV" -> {
                        oSampler.mTransform.mTranslation.y = readFloatFromTextContent()
                        testClosing("offsetV")
                    }
                    "rotateUV" -> {
                        oSampler.mTransform.mRotation = readFloatFromTextContent()
                        testClosing("rotateUV")
                    }
                    "blend_mode" -> {

                        // http://www.feelingsoftware.com/content/view/55/72/lang,en/
                        // NONE, OVER, IN, OUT, ADD, SUBTRACT, MULTIPLY, DIFFERENCE, LIGHTEN, DARKEN, SATURATE, DESATURATE and ILLUMINATE
                        when (getTextContent().words[0]) {
                            "ADD" -> oSampler.mOp = AiTexture.Op.add
                            "SUBTRACT" -> oSampler.mOp = AiTexture.Op.subtract
                            "MULTIPLY" -> oSampler.mOp = AiTexture.Op.multiply
                            else -> System.out.println("Collada: Unsupported MAYA texture blend mode")
                        }
                        testClosing("blend_mode")
                    }
                // OKINO extensions
                // -------------------------------------------------------
                    "weighting" -> {
                        oSampler.mWeighting = readFloatFromTextContent()
                        testClosing("weighting")
                    }
                    "mix_with_previous_layer" -> {
                        oSampler.mMixWithPrevious = readFloatFromTextContent()
                        testClosing("mix_with_previous_layer")
                    }
                // MAX3D extensions
                // -------------------------------------------------------
                    "amount" -> {
                        oSampler.mWeighting = readFloatFromTextContent()
                        testClosing("amount")
                    }
                }
            else if (event is EndElement && endElement.name_ == "technique")
                break
    }

    /** Reads an effect entry containing a color or a texture defining that color   */
    internal fun readEffectColor(pColor: AiColor4D, pSampler: Sampler) {

        if (isEmptyElement())
            return

        // Save current element name
        val curElem = element.name_

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "color" -> {
                        // text content contains 4 floats
                        val floats = getTextContent().words.map { it.f }

                        pColor put floats

                        testClosing("color")
                    }
                    "texture" -> {
                        // get name of source texture/sampler
                        pSampler.mName = element["texture"]!!

                        // get name of UV source channel. Specification demands it to be there, but some exporters don't write it.
                        // It will be the default UV channel in case it's missing.
                        element["texcoord"]?.let {
                            pSampler.mUVChannel = it
                        }
                        //SkipElement();

                        // as we've read texture, the color needs to be 1,1,1,1
                        pColor put 1f
                    }
                    "technique" -> {

                        val profile = element["profile"]

                        // Some extensions are quite useful ... ReadSamplerProperties processes several extensions in MAYA, OKINO and MAX3D profiles.
                        if (profile == "MAYA" || profile == "MAX3D" || profile == "OKINO")
                        // get more information on this sampler
                            readSamplerProperties(pSampler)
                        else
                            skipElement()
                    }
                    "extra" -> skipElement()    // ignore the rest
                }
            else if (event is EndElement && endElement.name_ == curElem)
                break
    }

    /** Reads an effect entry containing a float    */
    internal fun readEffectFloat(pFloat: Float): Float {

        var result = pFloat

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "float") {
                    // text content contains a single floats
                    result = getTextContent().words[0].f

                    testClosing("float")

                } else
                    skipElement()
//
            else if (event is EndElement)
                break

        return result
    }

    /** Reads an effect parameter specification of any kind */
    internal fun readEffectParam(pParam: EffectParam) {

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "surface" -> {
                        // image ID given inside <init_from> tags
                        testOpening("init_from")
                        val content = getTextContent()
                        pParam.mType = ParamType.Surface
                        pParam.mReference = content
                        testClosing("init_from")

                        // don't care for remaining stuff
                        skipElement("surface")
                    }
                    "sampler2D" ->
                        if (mFormat == FormatVersion._1_4_n || mFormat == FormatVersion._1_3_n) {

                            // surface ID is given inside <source> tags
                            testOpening("source")
                            val content = getTextContent()
                            pParam.mType = ParamType.Sampler
                            pParam.mReference = content
                            testClosing("source")

                            // don't care for remaining stuff
                            skipElement("sampler2D")

                        } else {
                            // surface ID is given inside <instance_image> tags
                            testOpening("instance_image")
                            var url = element["url"]!!
                            if (url[0] != '#')
                                throw Exception("Unsupported URL format in instance_image")
                            url.substring(1)
                            pParam.mType = ParamType.Sampler
                            pParam.mReference = url
                            skipElement("sampler2D")
                        }
                    else -> skipElement()   // ignore unknown element
                }
            else if (event is EndElement)
                break
    }

    /** Reads the geometry library contents */
    internal fun readGeometryLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "geometry") {

                    // read ID. Another entry which is "optional" by design but obligatory in reality
                    val id = element["id"]!!

                    // TODO: (thom) support SIDs
                    // ai_assert( TestAttribute( "sid") == -1);

                    // create a mesh and store it in the library under its ID
                    val mesh = Mesh()
                    mMeshLibrary[id] = mesh

                    // read the mesh name if it exists
                    element["name"]?.let {
                        mesh.mName = it
                    }

                    // read on from there
                    readGeometry(mesh)

                } else skipElement()    // ignore the rest

            else if (event is EndElement) {
                if (endElement.name_ != "library_geometries")
                    throw Exception("Expected end of <library_geometries> element.")

                break
            }
    }

    /** Reads a geometry from the geometry library. */
    internal fun readGeometry(pMesh: Mesh) {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "mesh")
                    readMesh(pMesh) // read on from there
                else
                    skipElement()   // ignore the rest

            else if (event is EndElement) {
                if (endElement.name_ != "geometry")
                    throw Exception("Expected end of <geometry> element.")

                break
            }
    }

    /** Reads a mesh from the geometry library  */
    internal fun readMesh(pMesh: Mesh) {

        if (isEmptyElement())
            return

        w@ while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "source" -> readSource()    // we have professionals dealing with this

                    "vertices" -> readVertexData(pMesh) // read per-vertex mesh data

                    "triangles", "lines", "linestrips", "polygons", "polylist", "trifans", "tristrips" ->
                        readIndexData(pMesh)    // read per-index mesh data and faces setup

                    else -> skipElement()   // ignore the rest
                }
            else if (event is EndElement)

                when (endElement.name_) {

                    "technique_common" -> { // end of another meaningless element - read over it
                    }

                    "mesh" -> break@w

                    else -> throw Exception("Expected end of <mesh> element.")
                }
    }

    /** Reads a source element  */
    internal fun readSource() {

        val sourceID = element["id"]!!

        w@ while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "float_array", "IDREF_array", "Name_array" -> readDataArray()

                    "technique_common" -> { // I don't care for your profiles
                    }

                    "accessor" -> readAccessor(sourceID)

                    else -> skipElement()   // ignore the rest
                }
            else if (event is EndElement)

                when (endElement.name_) {

                    "source" -> break@w   // end of <source> - we're done

                    "technique_common" -> { // end of another meaningless element - read over it
                    }
                // everything else should be punished
                    else -> throw Exception("Expected end of <source> element.")
                }
    }

    /** Reads a data array holding a number of floats, and stores it in the global library  */
    internal fun readDataArray() {

        val elmName = element.name_
        val isStringArray = elmName == "IDREF_array" || elmName == "Name_array"
        val isEmptyElement = isEmptyElement()

        // read attributes
        val id = element["id"]!!
        val count = element["count"]!!.i

        // read values and store inside an array in the data library
        mDataLibrary[id] = Data(mIsStringArray = isStringArray)
        val data = mDataLibrary[id]!!

        // some exporters write empty data arrays, but we need to conserve them anyways because others might reference them
        testTextContent()?.let { content ->

            if (isStringArray) {

                val strings = content.words

                if (strings.size < count)
                    throw Exception("Expected more values while reading IDREF_array contents.")

                data.mStrings.addAll(strings)

            } else {

                val ints = content.words.map { it.f }

                if (ints.size < count)
                    throw Exception("Expected more values while reading float_array contents.")

                data.mValues.addAll(ints)
            }
        }
        // test for closing tag
        if (!isEmptyElement)
            testClosing(elmName)
    }

    /** Reads an accessor and stores it in the global library   */
    internal fun readAccessor(pID: String) {

        // read accessor attributes
        val source = element["source"]!!
        if (source[0] != '#')
            throw Exception("Unknown reference format in url $source in source attribute of <accessor> element.")

        val count = element["count"]!!.i
        val offset = element["offset"]?.i ?: 0
        val stride = element["stride"]?.i ?: 1

        // store in the library under the given ID
        mAccessorLibrary[pID] = Accessor(mCount = count, mOffset = offset, mStride = stride,
                mSource = source.removePrefix("#"), // ignore the leading '#'
                mSize = 0)
        val acc = mAccessorLibrary[pID]!!

        // and read the components
        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "param") {

                    // read data param
                    val name = element["name"] ?: ""
                    if (name.isNotEmpty())
                    // analyse for common type components and store it's sub-offset in the corresponding field
                        when (name) {

                        /* Cartesian coordinates */
                            "X" -> acc.mSubOffset[0] = acc.mParams.size
                            "Y" -> acc.mSubOffset[1] = acc.mParams.size
                            "Z" -> acc.mSubOffset[2] = acc.mParams.size

                        /* RGBA colors */
                            "R" -> acc.mSubOffset[0] = acc.mParams.size
                            "G" -> acc.mSubOffset[1] = acc.mParams.size
                            "B" -> acc.mSubOffset[2] = acc.mParams.size
                            "A" -> acc.mSubOffset[3] = acc.mParams.size

                        /* UVWQ (STPQ) texture coordinates */
                            "S" -> acc.mSubOffset[0] = acc.mParams.size
                            "T" -> acc.mSubOffset[1] = acc.mParams.size
                            "P" -> acc.mSubOffset[2] = acc.mParams.size
                        //  else if( name == "Q") acc.mSubOffset[3] = acc.mParams.size();
                        /* 4D uv coordinates are not supported in Assimp */

                        /* Generic extra data, interpreted as UV data, too*/
                            "U" -> acc.mSubOffset[0] = acc.mParams.size
                            "V" -> acc.mSubOffset[1] = acc.mParams.size
                        //else
                        //  DefaultLogger::get()->warn( format() << "Unknown accessor parameter \"" << name << "\". Ignoring data channel." );
                        }

                    // read data type
                    element["type"]?.let {
                        // for the moment we only distinguish between a 4x4 matrix and anything else.
                        // TODO: (thom) I don't have a spec here at work. Check if there are other multi-value types which should be tested for here.
                        acc.mSize += if (it == "float4x4") 16 else 1
                    }

                    acc.mParams.add(name)

                    // skip remaining stuff of this element, if any
                    skipElement()

                } else
                    throw Exception("Unexpected sub element <${element.name}> in tag <accessor>")
//
            else if (event is EndElement) {

                if (endElement.name_ != "accessor")
                    throw Exception("Expected end of <accessor> element.")
                break
            }
    }

    /** Reads input declarations of per-vertex mesh data into the given mesh    */
    internal fun readVertexData(pMesh: Mesh) {

        // extract the ID of the <vertices> element. Not that we care, but to catch strange referencing schemes we should warn about
        pMesh.mVertexID = element["id"]!!

        // a number of <input> elements
        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "input")
                    readInputChannel(pMesh.mPerVertexData)
                else
                    throw Exception("Unexpected sub element <${element.name}> in tag <vertices>")
//
            else if (event is EndElement) {
                if (endElement.name_ != "vertices")
                    throw Exception("Expected end of <vertices> element.")

                break
            }
    }

    /** Reads input declarations of per-index mesh data into the given mesh */
    internal fun readIndexData(pMesh: Mesh) {

        val vcount = ArrayList<Int>()
        val perIndexData = ArrayList<InputChannel>()

        // read primitive count from the attribute
        val numPrimitives = element["count"]!!.i
        // some mesh types (e.g. tristrips) don't specify primitive count upfront, so we need to sum up the actual number of primitives while we read the <p>-tags
        var actualPrimitives = 0

        // material subgroup
        val subgroup = SubMesh()
        element["material"]?.let {
            subgroup.mMaterial = it
        }

        // distinguish between polys and triangles
        val elementName = element.name_
        val primType = when (elementName) {
            "lines" -> PrimitiveType.Lines
            "linestrips" -> PrimitiveType.LineStrip
            "polygons" -> PrimitiveType.Polygon
            "polylist" -> PrimitiveType.Polylist
            "triangles" -> PrimitiveType.Triangles
            "trifans" -> PrimitiveType.TriFans
            "tristrips" -> PrimitiveType.TriStrips
            else -> PrimitiveType.Invalid
        }

        assert(primType != PrimitiveType.Invalid)

        // also a number of <input> elements, but in addition a <p> primitive collection and probably index counts for all primitives
        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "input" -> readInputChannel(perIndexData)

                    "vcount" -> {

                        if (!isEmptyElement()) {

                            if (numPrimitives > 0) {  // It is possible to define a mesh without any primitives

                                // case <polylist> - specifies the number of indices for each polygon
                                val ints = getTextContent().words.map { it.ui.v }
                                if (numPrimitives > ints.size)
                                    throw Exception("Expected more values while reading <vcount> contents.")
                                vcount.addAll(ints)
                            }
                            testClosing("vcount")
                        }
                    }

                    "p" ->  // now here the actual fun starts - these are the indices to construct the mesh data from
                        if (!isEmptyElement())
                            actualPrimitives += readPrimitives(pMesh, perIndexData, numPrimitives, vcount, primType)

                    "extra" -> skipElement("extra")

                    else -> throw Exception("Unexpected sub element <${element.name}> in tag <$elementName>")
                }
            else if (event is EndElement) {
                if (endElement.name_ != elementName)
                    throw Exception("Expected end of <$elementName> element.")

                break
            }

        //TODO ASSIMP_BUILD_DEBUG?
        if (primType != PrimitiveType.TriFans && primType != PrimitiveType.TriStrips && primType != PrimitiveType.Lines)
        // this is ONLY to workaround a bug in SketchUp 15.3.331 where it writes the wrong 'count' when it writes out the 'lines'.
            assert(actualPrimitives == numPrimitives)

        // only when we're done reading all <p> tags (and thus know the final vertex count) can we commit the submesh
        subgroup.mNumFaces = actualPrimitives
        pMesh.mSubMeshes.add(subgroup)
    }

    /** Reads a single input channel element and stores it in the given array, if valid */
    internal fun readInputChannel(poChannels: ArrayList<InputChannel>) {

        val channel = InputChannel()

        // read semantic
        val semantic = element["semantic"]!!
        channel.mType = getTypeForSemantic(semantic)

        // read source
        val source = element["source"]!!
        if (source[0] != '#')
            throw Exception("Unknown reference format in url \"$source\" in source attribute of <input> element.")
        channel.mAccessor = source.substring(1) // skipping the leading #, hopefully the remaining text is the accessor ID only

        // read index offset, if per-index <input>
        element["offset"]?.let {
            channel.mOffset = it.i
        }

        // read set if texture coordinates
        if (channel.mType == InputType.Texcoord || channel.mType == InputType.Color)
            element["set"]?.let {
                val attrSet = it.i
                if (attrSet < 0)
                    throw Exception("Invalid index \"$attrSet\" in set attribute of <input> element ")

                channel.mIndex = attrSet
            }

        // store, if valid type
        if (channel.mType != InputType.Invalid)
            poChannels.add(channel)

        // skip remaining stuff of this element, if any
        skipElement()
    }

    /** Reads a <p> primitive index list and assembles the mesh data into the given mesh    */
    internal fun readPrimitives(pMesh: Mesh, pPerIndexChannels: ArrayList<InputChannel>, pNumPrimitives: Int, pVCount: ArrayList<Int>, pPrimType: PrimitiveType): Int {

        var numPrimitives = pNumPrimitives

        // determine number of indices coming per vertex find the offset index for all per-vertex channels
        var numOffsets = 1
        var perVertexOffset = 0xffffffff.i // invalid value
        pPerIndexChannels.forEach {
            numOffsets = glm.max(numOffsets, it.mOffset + 1)
            if (it.mType == InputType.Vertex)
                perVertexOffset = it.mOffset
        }

        // determine the expected number of indices
        val expectedPointCount = when (pPrimType) {
            PrimitiveType.Polylist -> pVCount.sum()
            PrimitiveType.Lines -> 2 * numPrimitives
            PrimitiveType.Triangles -> 3 * numPrimitives
            else -> 0   // other primitive types don't state the index count upfront... we need to guess
        }

        // and read all indices into a temporary array
        val indices = ArrayList<Int>()

        if (numPrimitives > 0) // It is possible to not contain any indices
            indices.addAll(getTextContent().words.map { it.i })

        // complain if the index count doesn't fit
        if (expectedPointCount > 0 && indices.size != expectedPointCount * numOffsets)

            if (pPrimType == PrimitiveType.Lines) {
                // HACK: We just fix this number since SketchUp 15.3.331 writes the wrong 'count' for 'lines'
                System.out.println("Expected different index count in <p> element, ${indices.size} instead of ${expectedPointCount * numOffsets}.")
                numPrimitives = (indices.size / numOffsets) / 2
            } else
                throw Exception("Expected different index count in <p> element.")
//
        else if (expectedPointCount == 0 && (indices.size % numOffsets) != 0)
            throw Exception("Expected different index count in <p> element.")

        // find the data for all sources
        pMesh.mPerVertexData.filter { it.mResolved == null }.forEach { input ->

            // find accessor
            input.mResolved = mAccessorLibrary[input.mAccessor]!!
            // resolve accessor's data pointer as well, if necessary
            val acc = input.mResolved!!
            if (acc.mData == null)
                acc.mData = mDataLibrary[acc.mSource]
        }
        // and the same for the per-index channels
        pPerIndexChannels.filter { it.mResolved == null }.filter {
            // ignore vertex pointer, it doesn't refer to an accessor
            if (it.mType == InputType.Vertex) {
                // warn if the vertex channel does not refer to the <vertices> element in the same mesh
                if (it.mAccessor != pMesh.mVertexID)
                    throw Exception("Unsupported vertex referencing scheme.")
                false
            } else
                true
        }.forEach {

            // find accessor
            it.mResolved = mAccessorLibrary[it.mAccessor]
            // resolve accessor's data pointer as well, if necessary
            val acc = it.mResolved!!
            if (acc.mData == null)
                acc.mData = mDataLibrary[acc.mSource]
        }

        // For continued primitives, the given count does not come all in one <p>, but only one primitive per <p>
        if (pPrimType == PrimitiveType.TriFans || pPrimType == PrimitiveType.Polygon)
            numPrimitives = 1
        // For continued primitives, the given count is actually the number of <p>'s inside the parent tag
        if (pPrimType == PrimitiveType.TriStrips) {
            val numberOfVertices = indices.size / numOffsets
            numPrimitives = numberOfVertices - 2
        }

        var polylistStartVertex = 0
        for (currentPrimitive in 0 until numPrimitives) {
            // determine number of points for this primitive
            var numPoints = 0
            when (pPrimType) {

                PrimitiveType.Lines -> {
                    numPoints = 2
                    for (currentVertex in 0 until numPoints)
                        copyVertex(currentVertex, numOffsets, numPoints, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                }
                PrimitiveType.Triangles -> {
                    numPoints = 3
                    for (currentVertex in 0 until numPoints)
                        copyVertex(currentVertex, numOffsets, numPoints, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                }
                PrimitiveType.TriStrips -> {
                    numPoints = 3
                    readPrimTriStrips(numOffsets, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                }
                PrimitiveType.Polylist -> {
                    numPoints = pVCount [currentPrimitive]
                    for (currentVertex in 0 until numPoints)
                        copyVertex(polylistStartVertex + currentVertex, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, 0, indices)
                    polylistStartVertex += numPoints
                }
                PrimitiveType.TriFans, PrimitiveType.Polygon -> {
                    numPoints = indices.size / numOffsets
                    for (currentVertex in 0 until numPoints)
                        copyVertex(currentVertex, numOffsets, numPoints, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                }
                else -> throw Exception("Unsupported primitive type.")  // LineStrip is not supported due to expected index unmangling
            }

            // store the face size to later reconstruct the face from
            pMesh.mFaceSize.add(numPoints)
        }

        // if I ever get my hands on that guy who invented this steaming pile of indirection...
        testClosing("p")
        return numPrimitives
    }

    /** Copies the data for a single primitive into the mesh, based on the InputChannels */
    internal fun copyVertex(currentVertex: Int, numOffsets: Int, numPoints: Int, perVertexOffset: Int, pMesh: Mesh, pPerIndexChannels: ArrayList<InputChannel>,
                            currentPrimitive: Int, indices: ArrayList<Int>) {

        // calculate the base offset of the vertex whose attributes we ant to copy
        val baseOffset = currentPrimitive * numOffsets * numPoints + currentVertex * numOffsets

        // don't overrun the boundaries of the index list
        val maxIndexRequested = baseOffset + numOffsets - 1
        assert(maxIndexRequested < indices.size)

        // extract per-vertex channels using the global per-vertex offset
        pMesh.mPerVertexData.forEach {
            extractDataObjectFromChannel(it, indices[baseOffset + perVertexOffset], pMesh)
        }
        // and extract per-index channels using there specified offset
        pPerIndexChannels.forEach {
            extractDataObjectFromChannel(it, indices[baseOffset + it.mOffset], pMesh)
        }

        // store the vertex-data index for later assignment of bone vertex weights
        pMesh.mFacePosIndices.add(indices[baseOffset + perVertexOffset])
    }

    /** Reads one triangle of a tristrip into the mesh */
    internal fun readPrimTriStrips(numOffsets: Int, perVertexOffset: Int, pMesh: Mesh, pPerIndexChannels: ArrayList<InputChannel>, currentPrimitive: Int,
                                   indices: ArrayList<Int>) =
            if (currentPrimitive % 2 != 0) {
                //odd tristrip triangles need their indices mangled, to preserve winding direction
                copyVertex(1, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                copyVertex(0, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                copyVertex(2, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
            } else {//for non tristrips or even tristrip triangles
                copyVertex(0, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                copyVertex(1, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
                copyVertex(2, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
            }

    /** Extracts a single object from an input channel and stores it in the appropriate mesh data array */
    internal fun extractDataObjectFromChannel(pInput: InputChannel, pLocalIndex: Int, pMesh: Mesh) {

        // ignore vertex referrer - we handle them that separate
        if (pInput.mType == InputType.Vertex)
            return

        val acc = pInput.mResolved!!
        if (pLocalIndex >= acc.mCount)
            throw Exception("Invalid data index ($pLocalIndex/${acc.mCount}) in primitive specification")

        // get a pointer to the start of the data object referred to by the accessor and the local index
        val offset = acc.mOffset + pLocalIndex * acc.mStride

        // assemble according to the accessors component sub-offset list. We don't care, yet, what kind of object exactly we're extracting here
        val obj = FloatArray(4, { acc.mData!!.mValues[offset + acc.mSubOffset[it]] })

        // now we reinterpret it according to the type we're reading here
        when (pInput.mType) {

            InputType.Position -> // ignore all position streams except 0 - there can be only one position
                if (pInput.mIndex == 0)
                    pMesh.mPositions.add(AiVector3D(obj))
                else
                    System.err.println("Collada: just one vertex position stream supported")

            InputType.Normal -> {
                // pad to current vertex count if necessary
                repeat(pMesh.mPositions.size - pMesh.mNormals.size - 1) {
                    pMesh.mNormals.add(AiVector3D(0, 1, 0))
                }

                // ignore all normal streams except 0 - there can be only one normal
                if (pInput.mIndex == 0)
                    pMesh.mNormals.add(AiVector3D(obj))
                else
                    System.err.println("Collada: just one vertex normal stream supported")
            }
            InputType.Tangent -> {
                // pad to current vertex count if necessary
                repeat(pMesh.mPositions.size - pMesh.mTangents.size - 1) {
                    pMesh.mTangents.add(AiVector3D(1, 0, 0))
                }
                // ignore all tangent streams except 0 - there can be only one tangent
                if (pInput.mIndex == 0)
                    pMesh.mTangents.add(AiVector3D(obj))
                else
                    System.err.println("Collada: just one vertex tangent stream supported")
            }
            InputType.Bitangent -> {
                // pad to current vertex count if necessary
                repeat(pMesh.mPositions.size - pMesh.mBitangents.size - 1) {
                    pMesh.mBitangents.add(AiVector3D(0, 0, 1))
                }

                // ignore all bitangent streams except 0 - there can be only one bitangent
                if (pInput.mIndex == 0)
                    pMesh.mBitangents.add(AiVector3D(obj))
                else
                    System.err.println("Collada: just one vertex bitangent stream supported")
            }
            InputType.Texcoord -> {
                // up to 4 texture coord sets are fine, ignore the others
                if (pInput.mIndex < AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                    // pad to current vertex count if necessary
//                    repeat(pMesh.mPositions.size - pMesh.mTexCoords[pInput.mIndex].size - 1) {
//                        pMesh.mTexCoords[pInput.mIndex].add(AiVector3D(0))
//                    }
//
//                    pMesh.mTexCoords[pInput.mIndex].add(AiVector3D(obj))
//                    if (0 != acc.mSubOffset[2] || 0 != acc.mSubOffset[3]) /* hack ... consider cleaner solution */
//                        pMesh.mNumUVComponents[pInput.mIndex] = 3
                } else
                    System.err.println("Collada: too many texture coordinate sets. Skipping.")
            }
            InputType.Color -> {
                // up to 4 color sets are fine, ignore the others
                if (pInput.mIndex < AI_MAX_NUMBER_OF_COLOR_SETS) {
                    // pad to current vertex count if necessary
                    repeat(pMesh.mPositions.size - pMesh.mColors[pInput.mIndex].size - 1) {
                        pMesh.mColors[pInput.mIndex].add(AiColor4D(0, 0, 0, 1))
                    }

                    val result = AiColor4D(0, 0, 0, 1)
                    repeat(pInput.mResolved!!.mSize) {
                        result[it] = obj[pInput.mResolved!!.mSubOffset[it]]
                    }
                    pMesh.mColors[pInput.mIndex].add(result)
                } else
                    System.err.println("Collada: too many vertex color sets. Skipping.")
            }
        // IT_Invalid and IT_Vertex
            else -> throw Error("shouldn't ever get here")
        }
    }

    /** Reads the library of node hierarchies and scene parts   */
    internal fun readSceneLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement) {

                // a visual scene - generate root node under its ID and let ReadNode() do the recursive work
                if (element.name_ == "visual_scene") {

                    // read ID. Is optional according to the spec, but how on earth should a scene_instance refer to it then?
                    val attrID = element["id"]!!

                    // read name if given.
                    val attrName = element["name"] ?: "unnamed"

                    // create a node and store it in the library under its ID
                    val node = Node(mID = attrID, mName = attrName)
                    mNodeLibrary[node.mID] = node

                    readSceneNode(node)

                } else
                    skipElement()   // ignore the rest

            } else if (event is EndElement) {
                if (endElement.name_ == "library_visual_scenes")
                //ThrowException( "Expected end of \"library_visual_scenes\" element.");

                    break
            }
    }

    /** Reads a scene node's contents including children and stores it in the given node    */
    internal fun readSceneNode(pNode: Node?) {
        println("readSceneNode in")
        // quit immediately on <bla/> elements
        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement) {

                if (element.name_ == "node") {

                    val child = Node()
                    element["id"]?.let {
                        child.mID = it
                    }
                    element["sid"]?.let {
                        child.mSID = it
                    }
                    element["name"]?.let {
                        child.mName = it
                    }

                    // TODO: (thom) support SIDs
                    // ai_assert( TestAttribute( "sid") == -1);

                    if (pNode != null) {
                        pNode.mChildren.add(child)
                        child.mParent = pNode
                    } else
                    // no parent node given, probably called from <library_nodes> element.
                    // create new node in node library
                        mNodeLibrary[child.mID] = child

                    // read on recursively from there
                    readSceneNode(child)
                    continue
                }
                // For any further stuff we need a valid node to work on
                else if (pNode == null)
                    continue

                when (element.name_) {

                    "lookat" -> readNodeTransformation(pNode, TransformType.LOOKAT)

                    "matrix" -> readNodeTransformation(pNode, TransformType.MATRIX)

                    "rotate" -> readNodeTransformation(pNode, TransformType.ROTATE)

                    "scale" -> readNodeTransformation(pNode, TransformType.SCALE)

                    "skew" -> readNodeTransformation(pNode, TransformType.SKEW)

                    "translate" -> readNodeTransformation(pNode, TransformType.TRANSLATE)

                    "render" ->
                        if (pNode.mParent == null && pNode.mPrimaryCamera.isEmpty())
                        // ... scene evaluation or, in other Words, postprocessing pipeline, or, again in other main.main.getWords, a turing-complete description how to
                        // render a Collada scene. The only thing that is interesting for us is the primary camera.
                            element["camera_node"]?.let {
                                if (it[0] != '#')
                                    System.err.println("Collada: Unresolved reference format of camera")
                                else
                                    pNode.mPrimaryCamera = it.substring(1)
                            }

                    "instance_node" ->
                        // find the node in the library
                        element["url"]?.let {
                            if (it[0] != '#')
                                System.err.println("Collada: Unresolved reference format of node")
                            else {
                                pNode.mNodeInstances.add(NodeInstance())
                                pNode.mNodeInstances.last().mNode = it.substring(1)
                            }
                        }

                    "instance_geometry", "instance_controller" -> readNodeGeometry(pNode) // Reference to a mesh or controller, with possible material associations

                    "instance_light" -> {
                        val url = element["url"]
                        // Reference to a light, name given in 'url' attribute
                        if (url == null)
                            System.err.println("Collada: Expected url attribute in <instance_light> element")
                        else {
                            if (url[0] != '#')
                                throw Exception("Unknown reference format in <instance_light> element")

                            pNode.mLights.add(LightInstance())
                            pNode.mLights.last().mLight = url.substring(1)
                        }
                        testClosing("instance_light")
                    }
                    "instance_camera" -> {
                        // Reference to a camera, name given in 'url' attribute
                        val url = element["url"]
                        if (url == null)
                            System.err.println("Collada: Expected url attribute in <instance_camera> element")
                        else {
                            val url = element["url"]!!
                            if (url[0] != '#')
                                throw Exception("Unknown reference format in <instance_camera> element")

                            pNode.mCameras.add(CameraInstance())
                            pNode.mCameras.last().mCamera = url.substring(1)
                        }
                        testClosing("instance_camera")
                    }
                    else -> skipElement()  // skip everything else for the moment
                }
            } else if (event is EndElement) {
                println("readSceneNode out")
                break
            }
    }

    /** Reads a node transformation entry of the given type and adds it to the given node's transformation list.    */
    internal fun readNodeTransformation(pNode: Node, pType: TransformType) {

        if (isEmptyElement())
            return

        val tagName = element.name_

        val tf = Transform(mType = pType)

        // read SID
        element["sid"]?.let {
            tf.mID = it
        }

        // how many parameters to read per transformation type
        val sNumParameters = intArrayOf(9, 4, 3, 3, 7, 16)
        val floats = getTextContent().words.map { it.f }

        // read as many parameters and store in the transformation
        tf.f = FloatArray(sNumParameters[pType.ordinal], { floats[it] })

        // place the transformation at the queue of the node
        pNode.mTransforms.add(tf)

        // and consume the closing tag
        testClosing(tagName)
    }

    /** Processes bind_vertex_input and bind elements   */
    internal fun readMaterialVertexInputBinding(tbl: SemanticMappingTable) {

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "bind_vertex_input" -> {

                        val vn = InputSemanticMapEntry()

                        // effect semantic
                        val s = element["semantic"]!!

                        // input semantic
                        vn.mType = getTypeForSemantic(element["input_semantic"]!!)

                        // index of input set
                        element["input_set"]?.let {
                            vn.mSet = it.i
                        }

                        tbl.mMap[s] = vn
                    }

                    "bind" -> System.out.println("Collada: Found unsupported <bind> element")
                }
            else if (event is EndElement)
                if (endElement.name_ == "instance_material")
                    break
    }

    /** Reads a mesh reference in a node and adds it to the node's mesh list    */
    internal fun readNodeGeometry(pNode: Node) {

        // referred mesh is given as an attribute of the <instance_geometry> element
        val url = element["url"]!!
        if (url[0] != '#')
            throw Exception("Unknown reference format")

        val instance = MeshInstance(mMeshOrController = url.substring(1))   // skipping the leading #

        if (!isEmptyElement()) {

            // read material associations. Ignore additional elements in between
            while (mReader.read())

                if (event is StartElement) {

                    if (element.name_ == "instance_material") {

                        // read ID of the geometry subgroup and the target material
                        val group = element["symbol"]!!
                        var urlMat = element["target"]!!
                        val s = SemanticMappingTable()
                        if (urlMat[0] == '#')
                            urlMat = urlMat.substring(1)

                        s.mMatName = urlMat

                        // resolve further material details + THIS UGLY AND NASTY semantic mapping stuff
                        if (!isEmptyElement())
                            readMaterialVertexInputBinding(s);

                        // store the association
                        instance.mMaterials[group] = s;
                    }
                } else if (event is EndElement)
                    if (endElement.name_ == "instance_geometry" || endElement.name_ == "instance_controller")
                        break
        }

        // store it
        pNode.mMeshes.add(instance)
    }

    /** Reads the collada scene */
    internal fun readScene() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "instance_visual_scene") {

                    // should be the first and only occurrence
                    mRootNode?.let { throw Exception("Invalid scene containing multiple root nodes in <instance_visual_scene> element") }

                    // read the url of the scene to instance. Should be of format "#some_name"
                    val url = element["url"]!!
                    if (url[0] != '#')
                        throw Exception("Unknown reference format in <instance_visual_scene> element")

                    // find the referred scene, skip the leading #
                    val sit = mNodeLibrary[url.substring(1)]
                    if (sit == null)
                        throw Exception("Unable to resolve visual_scene reference \"$url\" in <instance_visual_scene> element.")
                    mRootNode = sit
                } else
                    skipElement()
//
            else if (event is EndElement)
                break
    }

    /** Calculates the resulting transformation fromm all the given transform steps */
    internal fun calculateResultTransform(pTransforms: ArrayList<Transform>): Mat4 {

        var res = Mat4()

        pTransforms.forEach { tf ->

            when (tf.mType) {

                TransformType.LOOKAT -> {

                    val pos = AiVector3D(tf.f)
                    val dstPos = AiVector3D(tf.f, 3)
                    val up = AiVector3D(tf.f, 6).normalize()
                    val dir = (dstPos - pos).normalize()
                    val right = (dir cross up).normalize()

                    res *= Mat4(
                            right.x, up.x, -dir.x, pos.x,
                            right.y, up.y, -dir.y, pos.y,
                            right.z, up.z, -dir.z, pos.z,
                            0, 0, 0, 1)
                }
                TransformType.ROTATE -> {

                    val angle = tf.f[3] * glm.PIf / 180f
                    val axis = AiVector3D(tf.f)
                    val rot = glm.rotate(Mat4(), angle, axis)
                    res *= rot
                }
                TransformType.TRANSLATE -> {
                    val trans = glm.translate(Mat4(), AiVector3D(tf.f))
                    res *= trans
                }
                TransformType.SCALE -> {
                    val scale = Mat4(
                            tf.f[0], 0.0f, 0.0f, 0.0f,
                            0.0f, tf.f[1], 0.0f, 0.0f,
                            0.0f, 0.0f, tf.f[2], 0.0f,
                            0.0f, 0.0f, 0.0f, 1.0f)
                    res *= scale
                }
                TransformType.SKEW -> assert(false) // TODO: (thom)

                TransformType.MATRIX -> {
                    val mat = Mat4(tf.f)
                    res *= mat
                }

                else -> assert(false)
            }
        }

        return res;
    }

    /** Determines the input data type for the given semantic string    */
    internal fun getTypeForSemantic(semantic: String) = when (semantic) {

        "" -> {
            System.err.println("Vertex input type is empty.")
            InputType.Invalid
        }

        "POSITION" -> InputType.Position

        "TEXCOORD" -> InputType.Texcoord

        "NORMAL" -> InputType.Normal

        "COLOR" -> InputType.Color

        "VERTEX" -> InputType.Vertex

        "BINORMAL", "TEXBINORMAL" -> InputType.Bitangent

        "TANGENT", "TEXTANGENT" -> InputType.Tangent

        else -> {
            System.err.println("Unknown vertex input type $semantic. Ignoring.")
            InputType.Invalid
        }
    }


    /** Reads the text contents of an element, throws an exception if not given. Skips leading whitespace.  */
    internal fun getTextContent() = testTextContent() ?: throw Exception("Invalid contents in element.")

    /** Reads the text contents of an element, returns NULL if not given. Skips leading whitespace. */
    internal fun testTextContent(): String? {
        // present node should be the beginning of an element
        if (!event.isStartElement || isEmptyElement())
            return null

        // read contents of the element
        if (!mReader.peek().isCharacters)
            return null

        mReader.read()

        // skip leading whitespace
        return event.asCharacters().data.trimStart()
    }

    /** Returns if an element is an empty element, like <foo /> */
    internal fun isEmptyElement(): Boolean {
        if (mReader.peek().isEndElement)
            if (element.name_ == mReader.peek().asEndElement().name_) {
                mReader.nextEvent()
                return true
            }
        return false
    }

    /** Skips all data until the end node of the current element    */
    internal fun skipElement() {

        // nothing to skip if it's an </element>
        if (event.isEndElement)
            return

        // reroute
        skipElement(element.name_)
    }

    /** Skips all data until the end node of the given element  */
    internal fun skipElement(pElement: String) {
        while (mReader.read())
            if (event is EndElement && endElement.name_ == pElement)
                break
    }

    /** Tests for an opening element of the given name, throws an exception if not found    */
    internal fun testOpening(pName: String) {

        // read element start
        if (!mReader.read())
            throw Exception("Unexpected end of file while beginning of <$pName> element.")
        // whitespace in front is ok, just read again if found
        if (event is Characters)
            if (!mReader.read())
                throw Exception("Unexpected end of file while reading beginning of <$pName> element.")

        if (event !is StartElement || element.name_ != pName)
            throw Exception("Expected start of <$pName> element.")
    }

    /** Tests for the closing tag of the given element, throws an exception if not found    */
    internal fun testClosing(pName: String) {
        // check if we're already on the closing tag and return right away
        if (event.isEndElement && endElement.name_ == pName)
            return

        // if not, read some more
        if (!mReader.read())
            throw Exception("Unexpected end of file while reading end of <$pName> element.")
        // whitespace in front is ok, just read again if found
        if (event is Characters)
            if (!mReader.read())
                throw Exception("Unexpected end of file while reading end of <$pName> element.")

        // but this has the be the closing tag, or we're lost
        if (event !is EndElement || endElement.name_ != pName)
            throw Exception("Expected end of <$pName> element.")
    }

    operator fun StartElement.get(string: String) = getAttributeByName(QName(string))?.value

    val StartElement.name_
        get() = name.localPart
    val EndElement.name_
        get() = name.localPart

    fun XMLEventReader.read(): Boolean {
        if (hasNext()) {
            event = nextEvent()
            if (event.isStartElement)
                element = event.asStartElement()
            else if (event.isEndElement)
                endElement = event.asEndElement()
            return true
        }
        return false
    }

    internal fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2, { 0 })
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).b
        }
        return data
    }
}