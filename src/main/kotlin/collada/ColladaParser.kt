package collada

import com.sun.org.apache.xpath.internal.SourceTree
import f
import i
import org.w3c.dom.Document
import org.w3c.dom.Element
import ui
import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import AiVector3D
import AiColor4D
import AI_MAX_NUMBER_OF_TEXTURECOORDS
import AI_MAX_NUMBER_OF_COLOR_SETS
import com.sun.xml.internal.stream.events.StartElementEvent
import elementChildren
import get
import java.io.FileReader
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent


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
    private var mFileName = pFile

    /** XML reader, member for everyday use */
    private lateinit var mReader: XMLEventReader
    private lateinit var event: XMLEvent
    private lateinit var element: StartElement
    private lateinit var endElement: EndElement

    /** All data arrays found in the file by ID. Might be referred to by actually
    everyone. Collada, you are a steaming pile of indirection. */
    private var mDataLibrary: DataLibrary = mutableMapOf()

    /** Same for accessors which define how the data in a data array is accessed. */
    private var mAccessorLibrary: AccessorLibrary = mutableMapOf()

    /** Mesh library: mesh by ID */
    private var mMeshLibrary: MeshLibrary = mutableMapOf()

    /** node library: root node of the hierarchy part by ID */
    private var mNodeLibrary: NodeLibrary = mutableMapOf()

    /** Image library: stores texture properties by ID */
    private var mImageLibrary: ImageLibrary = mutableMapOf()

    /** Effect library: surface attributes by ID */
    private var mEffectLibrary: EffectLibrary = mutableMapOf()

    /** Material library: surface material by ID */
    private var mMaterialLibrary: MaterialLibrary = mutableMapOf()

    /** Light library: surface light by ID */
    private var mLightLibrary: LightLibrary = mutableMapOf()

    /** Camera library: surface material by ID */
    private var mCameraLibrary: CameraLibrary = mutableMapOf()

    /** Controller library: joint controllers by ID */
    private var mControllerLibrary: ControllerLibrary = mutableMapOf()

    /** Animation library: animation references by ID */
    private var mAnimationLibrary: AnimationLibrary = mutableMapOf()

    /** Animation clip library: clip animation references by ID */
    private var mAnimationClipLibrary: AnimationClipLibrary = ArrayList()

    /** Pointer to the root node. Don't delete, it just points to one of
    the nodes in the node library. */
    private var mRootNode: Node? = null

    /** Root animation container */
    private var mAnims: Animation? = null

    /** Size unit: how large compared to a meter */
    private var mUnitSize = 1f

    private var mUpDirection = UpDirection.Y

    /** Which is the up vector */
    enum class UpDirection { X, Y, Z }

    /** Collada file format version */
    private var mFormat = FormatVersion._1_5_n // We assume the newest file format by default

    init {

        val file = File(pFile)
        // open the file
        if (!file.exists())
            throw Error("Failed to open file: $pFile")

        // generate a XML reader for it
        val factory = XMLInputFactory.newInstance()

        mReader = factory.createXMLEventReader(FileReader(file))

        // start reading
        readContents()
    }

    /** Reads the contents of the file  */
    private fun readContents() {

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

                if (isElement("COLLADA")) {

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
        // skip everything else silently
    }

    /** Reads the structure of the file */
    private fun readStructure() {

        while (mReader.read()) {

            // beginning of elements
            if (event is StartElement)

                when (element.name_) {
                    "asset" -> readAssetInfo()
                    "library_animations" -> readAnimationLibrary()
                    "library_animation_clips" -> readAnimationClipLibrary()
                    "library_controllers" -> readControllerLibrary()
                    "library_images" -> readImageLibrary()
//                    "library_geometries" -> readGeometryLibrary(it)

                }
            println()
        }
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

    /** Reads the animation library */
    fun readAnimationLibrary() {

        if (isEmptyElement())
            return

        while (mReader.read())

            if (element is StartElement)

                if (isElement("animation"))
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

                if (endElement.name_ == "animation")
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
    private fun readAnimationSampler(pChannel: AnimationChannel) {

        while (mReader.read())

            if (event is StartElement)

                if (element.name_ == "input") {

                    val semantic = element["semantic"]!!
                    var source = element["source"]!!
                    if (source[0] != '#')
                        throw Exception("Unsupported URL format")
                    source = source.removePrefix("#")

                    if (semantic == "INPUT")
                        pChannel.mSourceTimes = source
                    else if (semantic == "OUTPUT")
                        pChannel.mSourceValues = source

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
    private fun readControllerLibrary() {

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

                if (endElement.name_ == "library_controllers")
                    throw Exception("Expected end of <library_controllers> element.")

                break
            }
    }

    /** Reads a controller into the given mesh structure    */
    private fun readController(pController: Controller) {

        while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                // two types of controllers: "skin" and "morph". Only the first one is relevant, we skip the other
                    "morph" -> skipElement()    // should skip everything inside, so there's no danger of catching elements in between

                    "skin" ->
                        // read the mesh it refers to. According to the spec this could also be another controller, but I refuse to implement every single idea
                        // they've come up with
                        pController.mMeshId = element["source"]!!.substring(1)

                    "bind_shape_matrix" -> {
                        // content is 16 floats to define a matrix... it seems to be important for some models
                        val content = getTextContent()

                        // read the 16 floats
                        pController.mBindShapeMatrix = content.split("\\s+".toRegex()).map { it.f }.toFloatArray()

                        testClosing("bind_shape_matrix")
                    }

                    "source" -> readSource()    // data array - we have specialists to handle this

                    "joints" -> readControllerJoints(pController)

                    "vertex_weights" -> readControllerWeights(pController)

                    else -> skipElement()   // ignore the rest
                }
            else if (event is EndElement) {

                if (endElement.name_ == "controller")
                    break
                else if (endElement.name_ == "skin")
                    throw Exception("Expected end of <controller> element.")
            }
    }

    /** Reads the joint definitions for the given controller    */
    private fun readControllerJoints(pController: Controller) {

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
    private fun readControllerWeights(pController: Controller) {

        // read vertex count from attributes and resize the array accordingly
        val vertexCount = element["count"]!!.i
        pController.mWeightCounts = MutableList(vertexCount, { 0 })

        while (mReader.read())

            if (event is StartElement)

            // Input channels for weight data. Two possible semantics: "JOINT" and "WEIGHT"
                if (element.name_ == "input" && vertexCount > 0) {

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

                } else if (element.name_ == "vcount" && vertexCount > 0) {

                    // read weight count per vertex
                    val ints = getTextContent().split("\\s+".toRegex()).map { it.i }
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

                } else if (element.name_ == "v" && vertexCount > 0) {

                    // read JointIndex - WeightIndex pairs
                    val ints = getTextContent().split("\\s+".toRegex()).map { it.i }
                    var i = 0
                    // FIXME crap solution, alternatives?
                    pController.mWeights.replaceAll { weight ->
                        if (i > ints.size - 1)
                            throw Exception("Out of data while reading <vertex_weights>")
                        Pair(i++, i++)
                    }

                    testClosing("v")

                } else
                    skipElement()   // ignore the rest
            //
            else if (event is EndElement) {

                if (endElement.name_ == "vertex_weights")
                    throw Exception("Expected end of <vertex_weights> element.")

                break
            }
    }

    /** Reads the image library contents    */
    private fun readImageLibrary(element: Element) = element.elementChildren().forEach {

        when(it.nodeName) { qui

        // Need to run different code paths here, depending on the Collada XSD version
            "image" -> {            }
//            "init_from" ->             {
//                if (mFormat == FormatVersion._1_4_n)                {
//                    // FIX: C4D exporter writes empty <init_from/> tags
//                    if (it.textContent.isNotEmpty()) {
//                        // element content is filename - hopefully
//                        val sz = it.textContent
//                        if (sz)pImage.mFileName = sz;
//                    TestClosing( "init_from");
//                }
//                    if (!pImage.mFileName.length()) {
//                        pImage.mFileName = "unknown_texture";
//                    }
//                }
//                else if (mFormat == FV_1_5_n)
//                {
//                    // make sure we skip over mip and array initializations, which
//                    // we don't support, but which could confuse the loader if
//                    // they're not skipped.
//                    int attrib = TestAttribute("array_index");
//                    if (attrib != -1 && mReader->getAttributeValueAsInt(attrib) > 0) {
//                    DefaultLogger::get()->warn("Collada: Ignoring texture array index");
//                    continue;
//                }
//
//                    attrib = TestAttribute("mip_index");
//                    if (attrib != -1 && mReader->getAttributeValueAsInt(attrib) > 0) {
//                    DefaultLogger::get()->warn("Collada: Ignoring MIP map layer");
//                    continue;
//                }
//
//                    // TODO: correctly jump over cube and volume maps?
//                }
//            }
//            else if (mFormat == FV_1_5_n)
//            {
//                if( IsElement( "ref"))
//                {
//                    // element content is filename - hopefully
//                    const char* sz = TestTextContent();
//                    if (sz)pImage.mFileName = sz;
//                    TestClosing( "ref");
//                }
//                else if( IsElement( "hex") && !pImage.mFileName.length())
//                {
//                    // embedded image. get format
//                    const int attrib = TestAttribute("format");
//                    if (-1 == attrib)
//                        DefaultLogger::get()->warn("Collada: Unknown image file format");
//                    else pImage.mEmbeddedFormat = mReader->getAttributeValue(attrib);
//
//                    const char* data = GetTextContent();
//
//                    // hexadecimal-encoded binary octets. First of all, find the
//                    // required buffer size to reserve enough storage.
//                    const char* cur = data;
//                    while (!IsSpaceOrNewLine(*cur)) cur++;
//
//                    const unsigned int size = (unsigned int)(cur-data) * 2;
//                    pImage.mImageData.resize(size);
//                    for (unsigned int i = 0; i < size;++i)
//                    pImage.mImageData[i] = HexOctetToDecimal(data+(i<<1));
//
//                    TestClosing( "hex");
//                }
//            }
        }
    }

    /** Reads the geometry library contents */
    private fun readGeometryLibrary(element: Element) = element.elementChildren().filter { it.nodeName == "geometry" }.forEach {

        // read ID. Another entry which is "optional" by design but obligatory in reality
        val id = it.getAttribute("id")

        // TODO: (thom) support SIDs
        // ai_assert( TestAttribute( "sid") == -1)

        // create a mesh and store it in the library under its ID
        val mesh = Mesh()
        mMeshLibrary[id] = mesh

        // read the mesh name if it exists
        mesh.mName = element.getAttribute("name")

        // read on from there
        readGeometry(it, mesh)
    }

    /** Reads a geometry from the geometry library. */
    private fun readGeometry(element: Element, pMesh: Mesh) = element.elementChildren().filter { it.nodeName == "mesh" }.forEach {
        // read on from there
        readMesh(it, pMesh)
    }

    /** Reads a mesh from the geometry library  */
    private fun readMesh(element: Element, pMesh: Mesh) = element.elementChildren().forEach {

        when (it.nodeName) {
        // we have professionals dealing with this
            "source" -> readSource(it)
        // read per-vertex mesh data
//            "vertices" -> readVertexData(it, pMesh)
        // read per-index mesh data and faces setup
//            "triangles", "lines", "linestrips", "polygons", "polylist", "trifans", "tristrips" -> readIndexData(it, pMesh)
            else -> {// ignore the rest
            }
        }
    }

    /** Reads a source element  */
    private fun readSource() {

        val sourceID = element["id"]!!

        w@ while (mReader.read())

            if (event is StartElement)

                when (element.name_) {

                    "float_array", "IDREF_array", "Name_array" -> readDataArray()

                    "technique_common" -> return    // I don't care for your profiles

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
    private fun readDataArray() {

        val elmName = element.name_
        val isStringArray = elmName == "IDREF_array" || elmName == "Name_array"

        // read attributes
        val id = element["id"]!!
        val count = element["count"]!!.i
        val content = testTextContent()

        // read values and store inside an array in the data library
        mDataLibrary[id] = Data(mIsStringArray = isStringArray)
        val data = mDataLibrary[id]!!

        // some exporters write empty data arrays, but we need to conserve them anyways because others might reference them
        if (content != null)

            if (isStringArray) {

                val strings = content.split("\\s+".toRegex())

                if (strings.size < count)
                    throw Exception("Expected more values while reading IDREF_array contents.")

                data.mStrings.addAll(strings)
            } else {

                val ints = content.split("\\s+".toRegex()).map { it.f }

                if (ints.size < count)
                    throw Exception("Expected more values while reading float_array contents.")

                data.mValues.addAll(ints)
            }

        // test for closing tag
        if (!isEmptyElement())
            testClosing(elmName)
    }

    /** Reads an accessor and stores it in the global library   */
    private fun readAccessor(pID: String) {

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
//
//    /** Reads input declarations of per-vertex mesh data into the given mesh    */
//    private fun readVertexData(element: Element, pMesh: Mesh) {
//
//        // extract the ID of the <vertices> element. Not that we care, but to catch strange referencing schemes we should warn about
//        pMesh.mVertexID = element.getAttribute("id")
//
//        // a number of <input> elements
//        element.elementChildren().forEach {
//
//            if (it.nodeName == "input")
//                readInputChannel(it, pMesh.mPerVertexData)
//            else
//                System.err.println("Unexpected sub element <${element.nodeName}> in tag <vertices>")
//        }
//    }
//
//    /** Reads a single input channel element and stores it in the given array, if valid */
//    private fun readInputChannel(element: Element, poChannels: ArrayList<InputChannel>) {
//
//        val channel = InputChannel()
//
//        // read semantic
//        val semantic = element.getAttribute("semantic")
//        channel.mType = getTypeForSemantic(semantic)
//
//        // read source
//        val source = element.getAttribute("source")
//        if (source[0] != '#')
//            System.err.println("Unknown reference format in url $source in source attribute of <input> element.")
//        channel.mAccessor = source.removePrefix("#")
//
//        // read index offset, if per-index <input>
//        if (element.hasAttribute("offset"))
//            channel.mOffset = element["offset"]!!.i
//
//        // read set if texture coordinates
//        if (channel.mType == InputType.Texcoord || channel.mType == InputType.Color) {
//            val attrSet = element.getAttribute("set")
//            if (attrSet.isNotEmpty()) {
//                if (attrSet.i < 0)
//                    System.err.println("Invalid index $attrSet in set attribute of <input> element")
//
//                channel.mIndex = attrSet.i
//            }
//        }
//
//        // store, if valid type
//        if (channel.mType != InputType.Invalid)
//            poChannels.add(channel)
//    }
//
//    /** Reads input declarations of per-index mesh data into the given mesh */
//    private fun readIndexData(element: Element, pMesh: Mesh) {
//
//        val vcount = ArrayList<Int>()
//        val perIndexData = ArrayList<InputChannel>()
//
//        // read primitive count from the attribute
//        val numPrimitives = element.getAttribute("count").i
//        // some mesh types (e.g. tristrips) don't specify primitive count upfront, so we need to sum up the actual number of primitives while we read the <p>-tags
//        var actualPrimitives = 0
//
//        // material subgroup
//        val subgroup = SubMesh()
//        if (element.hasAttribute("material"))
//            subgroup.mMaterial = element["material"]!!
//
//        // distinguish between polys and triangles
//        val elementName = element.nodeName
//        val primType = when (elementName) {
//            "lines" -> PrimitiveType.Lines
//            "linestrips" -> PrimitiveType.LineStrip
//            "polygons" -> PrimitiveType.Polygon
//            "polylist" -> PrimitiveType.Polylist
//            "triangles" -> PrimitiveType.Triangles
//            "trifans" -> PrimitiveType.TriFans
//            "tristrips" -> PrimitiveType.TriStrips
//            else -> PrimitiveType.Invalid
//        }
//
//        assert(primType != PrimitiveType.Invalid)
//
//        // also a number of <input> elements, but in addition a <p> primitive collection and probably index counts for all primitives
//        element.elementChildren().forEach {
//
//            when (it.nodeName) {
//
//                "input" -> readInputChannel(it, perIndexData)
//
//                "vcount" -> if (numPrimitives != 0) // It is possible to define a mesh without any primitives
//                    it.textContent.split("\\s+".toRegex()).mapTo(vcount, { it.i })  // case <polylist> - specifies the number of indices for each polygon
//
//                "p" -> actualPrimitives += readPrimitives(it, pMesh, perIndexData, numPrimitives, vcount, primType)
//
//                "extra" -> {
//                }
//
//                else -> throw Exception("Unexpected sub element <${it.nodeName}> in tag <$elementName>")
//            }
//        }
//    }
//
//    /** Reads a <p> primitive index list and assembles the mesh data into the given mesh    */
//    private fun readPrimitives(element: Element, pMesh: Mesh, pPerIndexChannels: ArrayList<InputChannel>, pNumPrimitives: Int, pVCount: ArrayList<Int>,
//                               pPrimType: PrimitiveType): Int {
//
//        // determine number of indices coming per vertex find the offset index for all per-vertex channels
//        var numOffsets = 1
//        var perVertexOffset = 0xffffffff.i
//        pPerIndexChannels.forEach {
//            numOffsets = glm.max(numOffsets, it.mOffset + 1)
//            if (it.mType == InputType.Vertex)
//                perVertexOffset = it.mOffset
//        }
//
//        // determine the expected number of indices
//        val expectedPointCount = when (pPrimType) {
//
//            PrimitiveType.Polylist -> pVCount.sum()
//
//            PrimitiveType.Lines -> 2 * pNumPrimitives
//
//            PrimitiveType.Triangles -> 3 * pNumPrimitives
//
//            else -> 0 // other primitive types don't state the index count upfront... we need to guess
//        }
//
//        // and read all indices into a temporary array
//        val indices = ArrayList<Int>()
//        if (pNumPrimitives > 0)   // It is possible to not contain any indices
//        // read a value. Hack: (thom) Some exporters put negative indices sometimes. We just try to carry on anyways.
//            element.textContent.split("\\s+".toRegex()).mapTo(indices, { it.ui.v })
//
//        var numPrimitives = pNumPrimitives
//
//        // complain if the index count doesn't fit
//        if (expectedPointCount > 0 && indices.size != expectedPointCount * numOffsets)
//            if (pPrimType == PrimitiveType.Lines) {
//                // HACK: We just fix this number since SketchUp 15.3.331 writes the wrong 'count' for 'lines'
//                System.err.println("Expected different index count in <p> element, ${indices.size} instead of ${expectedPointCount * numOffsets}.")
//                numPrimitives = (indices.size / numOffsets) / 2
//            } else throw Exception("Expected different index count in <p> element.")
//        else if (expectedPointCount == 0 && (indices.size % numOffsets) != 0)
//            throw Exception("Expected different index count in <p> element.")
//
//        // find the data for all sources
//        pMesh.mPerVertexData.forEach { input ->
//            if (input.mResolved != null)
//                return@forEach
//
//            // find accessor
//            input.mResolved = mAccessorLibrary[input.mAccessor]
//            // resolve accessor's data pointer as well, if necessary
//            val acc = input.mResolved!!
//            if (acc.mData == null)
//                acc.mData = mDataLibrary[acc.mSource]
//        }
//        // and the same for the per-index channels
//        pPerIndexChannels.forEach { input ->
//            if (input.mResolved != null)
//                return@forEach
//
//            // ignore vertex pointer, it doesn't refer to an accessor
//            if (input.mType == InputType.Vertex) {
//                // warn if the vertex channel does not refer to the <vertices> element in the same mesh
//                if (input.mAccessor != pMesh.mVertexID)
//                    throw Exception("Unsupported vertex referencing scheme.")
//                return@forEach
//            }
//
//            // find accessor
//            input.mResolved = mAccessorLibrary[input.mAccessor]
//            // resolve accessor's data pointer as well, if necessary
//            val acc = input.mResolved!!
//            if (acc.mData == null)
//                acc.mData = mDataLibrary[acc.mSource]
//        }
//
//        // For continued primitives, the given count does not come all in one <p>, but only one primitive per <p>
//        if (pPrimType == PrimitiveType.TriFans || pPrimType == PrimitiveType.Polygon)
//            numPrimitives = 1
//        // For continued primitives, the given count is actually the number of <p>'s inside the parent tag
//        if (pPrimType == PrimitiveType.TriStrips) {
//            val numberOfVertices = indices.size / numOffsets
//            numPrimitives = numberOfVertices - 2
//        }
//
//        var polylistStartVertex = 0
//        for (currentPrimitive in 0 until numPrimitives) {
//            // determine number of points for this primitive
//            var numPoints = 0
//            when (pPrimType) {
//                PrimitiveType.Lines -> {
//                    numPoints = 2
//                    for (currentVertex in 0 until numPoints)
//                        copyVertex(currentVertex, numOffsets, numPoints, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                }
//                PrimitiveType.Triangles -> {
//                    numPoints = 3
//                    for (currentVertex in 0 until numPoints)
//                        copyVertex(currentVertex, numOffsets, numPoints, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                }
//                PrimitiveType.TriStrips -> {
//                    numPoints = 3
//                    readPrimTriStrips(numOffsets, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                }
//                PrimitiveType.Polylist -> {
//                    numPoints = pVCount [currentPrimitive]
//                    for (currentVertex in 0 until numPoints)
//                        copyVertex(polylistStartVertex + currentVertex, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, 0, indices)
//                    polylistStartVertex += numPoints
//                }
//                PrimitiveType.TriFans, PrimitiveType.Polygon -> {
//                    numPoints = indices.size / numOffsets
//                    for (currentVertex in 0 until numPoints)
//                        copyVertex(currentVertex, numOffsets, numPoints, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                }
//            // LineStrip is not supported due to expected index unmangling
//                else -> throw Exception("Unsupported primitive type.")
//            }
//            // store the face size to later reconstruct the face from
//            pMesh.mFaceSize.add(numPoints)
//        }
//        // if I ever get my hands on that guy who invented this steaming pile of indirection...
//        return numPrimitives
//    }
//
//    /** Copies the data for a single primitive into the mesh, based on the InputChannels */
//    private fun copyVertex(currentVertex: Int, numOffsets: Int, numPoints: Int, perVertexOffset: Int, pMesh: Mesh, pPerIndexChannels: ArrayList<InputChannel>,
//                           currentPrimitive: Int, indices: ArrayList<Int>) {
//
//        // calculate the base offset of the vertex whose attributes we ant to copy
//        val baseOffset = currentPrimitive * numOffsets * numPoints + currentVertex * numOffsets
//
//        // don't overrun the boundaries of the index list
//        val maxIndexRequested = baseOffset + numOffsets - 1
//        assert(maxIndexRequested < indices.size)
//
//        // extract per-vertex channels using the global per-vertex offset
//        pMesh.mPerVertexData.forEach {
//            extractDataObjectFromChannel(it, indices[baseOffset + perVertexOffset], pMesh)
//        }
//
//        // and extract per-index channels using there specified offset
//        pPerIndexChannels.forEach {
//            extractDataObjectFromChannel(it, indices[baseOffset + it.mOffset], pMesh)
//        }
//
//        // store the vertex-data index for later assignment of bone vertex weights
//        pMesh.mFacePosIndices.add(indices[baseOffset + perVertexOffset])
//    }
//
//    /** Reads one triangle of a tristrip into the mesh */
//    private fun readPrimTriStrips(numOffsets: Int, perVertexOffset: Int, pMesh: Mesh, pPerIndexChannels: ArrayList<InputChannel>, currentPrimitive: Int,
//                                  indices: ArrayList<Int>) =
//            if (currentPrimitive % 2 != 0) {
//                //odd tristrip triangles need their indices mangled, to preserve winding direction
//                copyVertex(1, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                copyVertex(0, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                copyVertex(2, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//            } else {//for non tristrips or even tristrip triangles
//                copyVertex(0, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                copyVertex(1, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//                copyVertex(2, numOffsets, 1, perVertexOffset, pMesh, pPerIndexChannels, currentPrimitive, indices)
//            }
//
//
//    /** Extracts a single object from an input channel and stores it in the appropriate mesh data array */
//    private fun extractDataObjectFromChannel(pInput: InputChannel, pLocalIndex: Int, pMesh: Mesh) {
//
//        // ignore vertex referrer - we handle them that separate
//        if (pInput.mType == InputType.Vertex)
//            return
//
//        val acc = pInput.mResolved!!
//        if (pLocalIndex >= acc.mCount)
//            throw Exception("Invalid data index ($pLocalIndex/${acc.mCount}) in primitive specification")
//
//        // get a pointer to the start of the data object referred to by the accessor and the local index
//        val offsetDataObject = acc.mOffset + pLocalIndex * acc.mStride
//
//        // assemble according to the accessors component sub-offset list. We don't care, yet,
//        // what kind of object exactly we're extracting here
//        val obj = FloatArray(4, { 0f })
//        for (c in 0 until 4)
//            obj[c] = acc.mData!!.mValues[offsetDataObject + acc.mSubOffset[c]]
//
//        // now we reinterpret it according to the type we're reading here
//        when (pInput.mType) {
//            InputType.Position -> { // ignore all position streams except 0 - there can be only one position
//                if (pInput.mIndex == 0)
//                    pMesh.mPositions.add(AiVector3D(obj[0], obj[1], obj[2]))
//                else
//                    System.err.println("Collada: just one vertex position stream supported")
//            }
//            InputType.Normal -> {
//                // pad to current vertex count if necessary
//                if (pMesh.mNormals.size < pMesh.mPositions.size - 1)
//                    pMesh.mNormals.addAll(Array(pMesh.mPositions.size - pMesh.mNormals.size - 1, { AiVector3D(0, 1, 0) }))
//
//                // ignore all normal streams except 0 - there can be only one normal
//                if (pInput.mIndex == 0)
//                    pMesh.mNormals.add(AiVector3D(obj[0], obj[1], obj[2]))
//                else
//                    System.err.println("Collada: just one vertex normal stream supported")
//            }
//            InputType.Tangent -> {
//                // pad to current vertex count if necessary
//                if (pMesh.mTangents.size < pMesh.mPositions.size - 1)
//                    pMesh.mTangents.addAll(Array(pMesh.mPositions.size - pMesh.mTangents.size - 1, { AiVector3D(1, 0, 0) }))
//
//                // ignore all tangent streams except 0 - there can be only one tangent
//                if (pInput.mIndex == 0)
//                    pMesh.mTangents.add(AiVector3D(obj[0], obj[1], obj[2]))
//                else
//                    System.err.println("Collada: just one vertex tangent stream supported")
//            }
//            InputType.Bitangent -> {
//                // pad to current vertex count if necessary
//                if (pMesh.mBitangents.size < pMesh.mPositions.size - 1)
//                    pMesh.mBitangents.addAll(Array(pMesh.mPositions.size - pMesh.mBitangents.size - 1, { AiVector3D(0, 0, 1) }))
//
//                // ignore all bitangent streams except 0 - there can be only one bitangent
//                if (pInput.mIndex == 0)
//                    pMesh.mBitangents.add(AiVector3D(obj[0], obj[1], obj[2]))
//                else
//                    System.err.println("Collada: just one vertex bitangent stream supported")
//            }
//            InputType.Texcoord -> {
//                // up to 4 texture coord sets are fine, ignore the others
//                if (pInput.mIndex < AI_MAX_NUMBER_OF_TEXTURECOORDS) {
//                    // pad to current vertex count if necessary
//                    if (pMesh.mTexCoords[pInput.mIndex].size < pMesh.mPositions.size - 1)
//                        pMesh.mTexCoords[pInput.mIndex].addAll(Array(pMesh.mPositions.size - pMesh.mTexCoords[pInput.mIndex].size - 1, { AiVector3D(0, 0, 0) }))
//
//                    pMesh.mTexCoords[pInput.mIndex].add(AiVector3D(obj[0], obj[1], obj[2]))
//                    if (0 != acc.mSubOffset[2] || 0 != acc.mSubOffset[3]) /* hack ... consider cleaner solution */
//                        pMesh.mNumUVComponents[pInput.mIndex] = 3
//                } else
//                    System.err.println("Collada: too many texture coordinate sets. Skipping.")
//            }
//            InputType.Color -> {
//                // up to 4 color sets are fine, ignore the others
//                if (pInput.mIndex < AI_MAX_NUMBER_OF_COLOR_SETS) {
//                    // pad to current vertex count if necessary
//                    if (pMesh.mColors[pInput.mIndex].size < pMesh.mPositions.size - 1)
//                        pMesh.mColors[pInput.mIndex].addAll(Array(pMesh.mPositions.size - pMesh.mColors[pInput.mIndex].size - 1, { AiColor4D(0, 0, 0, 1) }))
//
//                    val result = AiColor4D(0, 0, 0, 1)
//                    for (i in 0 until pInput.mResolved!!.mSize)
//                        result[i] = obj[pInput.mResolved!!.mSubOffset[i]]
//
//                    pMesh.mColors[pInput.mIndex].add(result)
//                } else
//                    System.err.println("Collada: too many vertex color sets. Skipping.")
//            }
//        // IT_Invalid and IT_Vertex
//            else -> assert(false)
//        }
//    }
//
//    /** Determines the input data type for the given semantic string    */
//    private fun getTypeForSemantic(semantic: String) = when (semantic) {
//
//        "" -> {
//            System.err.println("Vertex input type is empty.")
//            InputType.Invalid
//        }
//
//        "position" -> InputType.Position
//
//        "texcoord" -> InputType.Texcoord
//
//        "normal" -> InputType.Normal
//
//        "color" -> InputType.Color
//
//        "vertex" -> InputType.Vertex
//
//        "binormal", "texbinormal" -> InputType.Bitangent
//
//        "tangent", "textangent" -> InputType.Tangent
//
//        else -> {
//            System.err.println("Unknown vertex input type $semantic. Ignoring.")
//            InputType.Invalid
//        }
//    }

    /** Tests for the closing tag of the given element, throws an exception if not found    */
    private fun testClosing(pName: String) {
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
        if (!event.isEndElement)
            throw Exception("Expected end of <$pName> element.")
    }

    /** Reads the text contents of an element, throws an exception if not given. Skips leading whitespace.  */
    private fun getTextContent() = testTextContent() ?: throw Exception("Invalid contents in element.")

    /** Reads the text contents of an element, returns NULL if not given. Skips leading whitespace. */
    private fun testTextContent(): String? {
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
    private fun isEmptyElement() = mReader.peek().isEndElement

    /** Skips all data until the end node of the current element    */
    private fun skipElement() {

        // nothing to skip if it's an </element>
        if (event.isEndElement)
            return

        // reroute
        skipElement(element.name)
    }

    /** Skips all data until the end node of the given element  */
    private fun skipElement(pElement: QName) {

        // copy the current node's name because it'a pointer to the reader's internal buffer, which is going to change with the upcoming parsing
        val name = pElement.localPart

        while (mReader.read())
            if (event.isEndElement && event.asEndElement().name_ == name)
                break
    }

    operator fun StartElement.get(string: String) = getAttributeByName(QName(string))?.value

    val StartElement.name_
        get() = name.localPart
    val EndElement.name_
        get() = name.localPart

    // TODO remove?
    /** Check for element match */
    fun isElement(pName: String): Boolean {
        assert(event.isStartElement)
        element = event.asStartElement()
        return element.name_ == pName
    }

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
}