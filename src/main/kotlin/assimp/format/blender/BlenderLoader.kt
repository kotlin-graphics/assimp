package assimp.format.blender

import assimp.*
import assimp.format.X.*
import glm_.*
import uno.kotlin.parseInt
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.io.FileOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.*

private lateinit var buffer: ByteBuffer

private val tokens = "BLENDER"

class BlenderImporter : BaseImporter() {    // TODO should this be open? The C++ version has protected methods
	// TODO check member visibility

	private val modifierCache = BlenderModifierShowcase()

    /** Returns whether the class can handle the format of the given file.  */
    override fun canRead(file: String, ioSystem: IOSystem, checkSig: Boolean): Boolean {

        val extension = getExtension(file)
        if (extension == "blend") return true
        else if (extension.isEmpty() || checkSig) {
            // TODO ("check is blend file")
            // note: this won't catch compressed files
//            return SearchFileHeaderForToken(pIOHandler,pFile, TokensForSearch,1);
        }
        return false
    }

    override val info
        get() = AiImporterDesc(
                name = "Blender 3D Importer \nhttp://www.blender3d.org",
                comments = "No animation support yet",
                flags = AiImporterFlags.SupportBinaryFlavour.i,
                minMajor = 0,
                minMinor = 0,
                maxMajor = 2,
                maxMinor = 50,
                fileExtensions = listOf("blend"))

    override fun internReadFile(file: String, ioSystem: IOSystem, scene: AiScene) {

        val stream = ioSystem.open(file)

        buffer = stream.readBytes()

        var match = buffer.strncmp(tokens)
        if (!match) {
            // Check for presence of the gzip header. If yes, assume it is a
            // compressed blend file and try uncompressing it, else fail. This is to
            // avoid uncompressing random files which our loader might end up with.

            val output = File("temp")   // TODO use a temp outputStream instead of writing to disc, maybe?
	        // we could use ByteArrayInputStream / ByteArrayOutputStream
	        // the question is what this would do to memory requirements for big files
	        // we would basically keep up to 3 copies of the file in memory (buffer, output, input)
            output.deleteOnExit()

            GZIPInputStream(stream.read()).use { gzip ->

                FileOutputStream(output).use { out ->
                    val buffer = ByteArray(1024)
                    var len = gzip.read(buffer)
                    while (len != -1) {
                        out.write(buffer, 0, len)
                        len = gzip.read(buffer)
                    }
                }
            }
            val fc = RandomAccessFile(output, "r").channel
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).order(ByteOrder.nativeOrder())
            // .. and retry
            match = buffer.strncmp(tokens)
            if (!match) throw Error("Found no BLENDER magic word in decompressed GZIP file")
        }
	    buffer.pos += tokens.length

        val db = FileDatabase().apply {
            i64bit = buffer.get().c == '-'      // 32 bit should be '_'
            little = buffer.get().c == 'v'      // big endian should be 'V'
        }
        val major = buffer.get().c.parseInt()
        val minor = buffer.get().c.parseInt() * 10 + buffer.get().c.parseInt()
        logger.info("Blender version is $major.$minor (64bit: ${db.i64bit}, little endian: ${db.little})")
	    if(ASSIMP.BLENDER_DEBUG) logger.info { "Blender DEBUG ENABLED" }

        db.reader = buffer.slice().order(if(db.little) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

        parseBlendFile(db)

        val blendScene = extractScene(db)

	    blendScene.convertBlendFile(db)
    }

    private fun parseBlendFile(out: FileDatabase) {

        val dnaReader = DnaParser(out)
        var dna: DNA? = null

        out.entries.ensureCapacity(128)
        // even small BLEND files tend to consist of many file blocks
        val parser = SectionParser(out.reader, out.i64bit)

        // first parse the file in search for the DNA and insert all other sections into the database
        while (true) {
            parser.next()
            val head = parser.current.copy()

            if (head.id == "ENDB")
                break // only valid end of the file
            else if (head.id == "DNA1") {
                dnaReader.parse()
                dna = dnaReader.dna
                continue
            }

            out.entries += head
        }
        if (dna == null) throw Error("SDNA not found")

        out.entries.sort()
    }

    private fun extractScene(file: FileDatabase): Scene {

        val sceneIndex = file.dna.indices["Scene"]?.toInt() ?: throw Error("There is no `Scene` structure record")

        val ss = file.dna.structures[sceneIndex]

	    // we need a scene somewhere to start with.
	    val block = file.entries.find {
		    // Fix: using the DNA index is more reliable to locate scenes
		    //if (bl.id == "SC") {
		    it.dnaIndex == sceneIndex
	    } ?: throw Error("There is not a single `Scene` record to load")

	    file.reader.pos = block.start

	    val out = Scene()
        ss.convert(out)

	    if(!ASSIMP.BLENDER_NO_STATS) {
		    val stats = file.stats
		    logger.info {
			    "(Stats) Fields read: ${stats.fieldsRead}, " +
			    "pointers resolved: ${stats.pointersResolved}, " +
			    "cache hits: ${stats.cacheHits}, " +
			    "cached objects: ${stats.cachedObjects}"
		    }
	    }

	    return out
    }

	protected fun Scene.convertBlendFile(db: FileDatabase): AiScene {

		val conv = ConversionData(db)

		// FIXME it must be possible to take the hierarchy directly from
		// the file. This is terrible. Here, we're first looking for
		// all objects which don't have parent objects at all -
		val noParents = LinkedList<Object>()
		(base.first as? Base)?.forEach{

			it.obj?.let { obj ->
				if (obj.parent == null) {
					noParents.pushBack(obj)
				} else {
					conv.objects.add(obj)
				}
			}
		}
		basact?.forEach {
			it.obj?.let { obj ->
				if(obj.parent != null){
					conv.objects.add(obj)
				}
			}
		}

		if(noParents.isEmpty()){
			throw Error("Expected at least one object with no parent")
		}

		val out = AiScene()
		out.rootNode = AiNode("<BlenderRoot>")
		val root = out.rootNode

		root.numChildren = noParents.size
		root.children = MutableList(root.numChildren) {
			val node: AiNode = convertNode(noParents[it], conv)
			node.parent = root
			node
		}

		buildMaterials(conv)

		if(conv.meshes.size > 0) {
			out.numMeshes = conv.meshes.size
			out.meshes = MutableList(conv.meshes.size) {
				conv.meshes[it]
			}
			conv.meshes.clear()
		}

		if(conv.lights.size > 0) {
			out.numLights = conv.lights.size
			out.lights = MutableList(conv.lights.size) {
				conv.lights[it]
			}
			conv.lights.clear()
		}

		if(conv.cameras.size > 0) {
			out.numCameras = conv.cameras.size
			out.cameras = MutableList(conv.cameras.size) {
				conv.cameras[it]
			}
			conv.cameras.clear()
		}

		if(conv.materials.size > 0) {
			out.numMaterials = conv.materials.size
			out.materials = MutableList(conv.materials.size) {
				conv.materials[it]
			}
			conv.materials.clear()
		}

		if(conv.textures.size > 0) {
			out.numTextures = conv.textures.size
			for((name, tex) in conv.textures) {
				// TODO convert to gli.Texture out.textures[name] = tex ??
			}
		}

//		if (conv.textures->size()) {
//			out->mTextures = new aiTexture*[out->mNumTextures = static_cast<unsigned int>( conv.textures->size() )];
//			std::copy(conv.textures->begin(),conv.textures->end(),out->mTextures);
//			conv.textures.dismiss();
//		}
//
//		// acknowledge that the scene might come out incomplete
//		// by Assimp's definition of `complete`: blender scenes
//		// can consist of thousands of cameras or lights with
//		// not a single mesh between them.
//		if (!out->mNumMeshes) {
//			out->mFlags |= AI_SCENE_FLAGS_INCOMPLETE;
//		}

		return out
	}

	private fun Scene.convertNode(obj: Object, conv: ConversionData, parentTransform: AiMatrix4x4 = AiMatrix4x4()): AiNode {

		fun notSupportedObjectType(obj: Object, type: String) {
			logger.warn { "Object `${obj.id.name}` - type is unsupported: `$type`, skipping" }
		}

		val children = LinkedList<Object>()
		for(it in conv.objects) {
			if(it.parent == obj) {
				children.pushBack(it)
				conv.objects.remove(it)
			}
		}

		val node = AiNode(obj.id.name.substring(2)) // skip over the name prefix 'OB'

		obj.data?.let { data ->
			when(obj.type) {

				Object.Type.EMPTY   -> {} // do nothing
				Object.Type.MESH    -> {
					val old = conv.meshes.size

					checkActualType(data, "Mesh")
					// convertMesh(obj, data as Mesh, conv, conv.meshes)    TODO

					if(conv.meshes.size > old) {
						node.meshes = IntArray(conv.meshes.size - old) { it + old }
					}
				}
				Object.Type.LAMP    -> {
					checkActualType(data, "Lamp")
					val light = convertLight(obj, data as Lamp)
					conv.lights.pushBack(light)
				}
				Object.Type.CAMERA  -> {
					checkActualType(data, "Camera")
					val camera = convertCamera(obj, data as Camera)
					conv.cameras.pushBack(camera)
				}
				Object.Type.CURVE   -> notSupportedObjectType(obj, "Curve")
				Object.Type.SURF    -> notSupportedObjectType(obj, "Surf")
				Object.Type.FONT    -> notSupportedObjectType(obj, "Font")
				Object.Type.MBALL   -> notSupportedObjectType(obj, "Mball")
				Object.Type.WAVE    -> notSupportedObjectType(obj, "Wave")
				Object.Type.LATTICE -> notSupportedObjectType(obj, "Lattice")
				else -> throw Error("When should be exhaustive")
			}
			Unit // return Unit from let explicitly so that when and the contained if statements don't need to be exhaustive
		}

		for(x in 0 until 4) {
			for(y in 0 until 4) {
				node.transformation[y][x] = obj.obmat[x][y]     // TODO do I need to change anything here
				// C++ Assimp uses row-based and kotlin assimp is column-based matrices.
				// https://github.com/kotlin-graphics/assimp/wiki/Instructions-for-porting-code-&-Differences-between-the-C---and-Kotlin-version#matrices
			}
		}

		val m = parentTransform.inverse()
		node.transformation = m*node.transformation

		if(children.size > 0) {
			node.numChildren = children.size
			node.children = MutableList(node.numChildren) {
				convertNode(children[it], conv, node.transformation * parentTransform)
						.apply { parent = node }
			}
		}

		// apply modifiers
		modifierCache.applyModifiers(node, conv, this, obj)

		return node
	}

	private fun checkActualType(dt: ElemBase, check: String): Unit {
		assert(dt.dnaType == check) {
			"Expected object `$dt` to be of type `$check`, but it claims to be a `${dt.dnaType}` instead"
		}
	}

	private fun buildMaterials(conv: ConversionData) {
		TODO("buildMaterials")
	}

	private fun convertCamera(obj: Object, cam: Camera): AiCamera {

		val out = AiCamera()
		out.name = obj.id.name.substring(2)

		out.position = AiVector3D(0f)
		out.up = AiVector3D(0f, 1f,0f)
		out.lookAt = AiVector3D(0f, 0f, -1f)

		if(cam.sensorX > 0f && cam.lens > 0f) {
			out.horizontalFOV = 2f * atan2(cam.sensorX, 2f * cam.lens)
		}

		out.clipPlaneNear = cam.clipSta
		out.clipPlaneFar = cam.clipEnd

		return out
	}

	private fun convertLight(obj: Object, lamp: Lamp): AiLight {

		val out = AiLight()
		out.name = obj.id.name.substring(2)

		when(lamp.type) {
			Lamp.Type.Local -> {
				out.type = AiLightSourceType.POINT
			}
			Lamp.Type.Sun   -> {
				out.type = AiLightSourceType.DIRECTIONAL

				// blender orients directional lights as facing toward -z
				out.direction = AiVector3D(0f, 0f, -1f)
				out.up = AiVector3D(0f, 1f, 0f)
			}
			Lamp.Type.Area  -> {
				out.type = AiLightSourceType.AREA

				if(lamp.areaShape == 0.s){
					out.size = AiVector2D(lamp.areaSize, lamp.areaSize)
				} else {
					out.size = AiVector2D(lamp.areaSize, lamp.areaSizeY)
				}

				// blender orients directional lights as facing toward -z
				out.direction = AiVector3D(0f, 0f, -1f)
				out.up = AiVector3D(0f, 1f, 0f)
			}
			else -> {} // TODO missing light types??? do nothing?? realy??
		}

		out.colorAmbient = AiColor3D(lamp.r, lamp.b, lamp.g) * lamp.energy
		out.colorSpecular= AiColor3D(lamp.r, lamp.b, lamp.g) * lamp.energy
		out.colorDiffuse = AiColor3D(lamp.r, lamp.b, lamp.g) * lamp.energy

		return out
	}
}

fun error(policy: ErrorPolicy, value: Any?, message: String?): Unit = when(policy) {
    ErrorPolicy.Warn -> logger.warn { "value: $value, $message" }
    ErrorPolicy.Fail -> throw Error( "value: $value, $message" )
    ErrorPolicy.Igno -> if (ASSIMP.BLENDER_DEBUG) logger.info { "value: $value, $message" } else Unit
}
