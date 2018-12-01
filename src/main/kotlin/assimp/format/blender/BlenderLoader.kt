package assimp.format.blender

import assimp.*
import assimp.format.X.*
import glm_.*
import uno.kotlin.parseInt
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.io.FileOutputStream
import java.lang.IllegalStateException
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.collections.ArrayList
import kotlin.math.*

private val tokens = "BLENDER"

// typedef std::map<uint32_t, const MLoopUV *> TextureUVMapping;
// key is material number, value is the TextureUVMapping for the material
// typedef std::map<uint32_t, TextureUVMapping> MaterialTextureUVMappings;
private typealias TextureUVMap = MutableMap<Int, MLoopUV>
private typealias MaterialTextureUVMap = MutableMap<Int, TextureUVMap>

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

        var buffer = stream.readBytes()

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
                    val buf = ByteArray(1024)
                    var len = gzip.read(buf)
                    while (len != -1) {
                        out.write(buf, 0, len)
                        len = gzip.read(buf)
                    }
                }
            }
            val fc = RandomAccessFile(output, "r").channel
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).order(ByteOrder.nativeOrder())
            // .. and retry
            match = buffer.strncmp(tokens)
            if (!match) throw Exception("Found no BLENDER magic word in decompressed GZIP file")
        }
	    buffer.pos += tokens.length

	    val i64bit = buffer.get().c == '-'      // 32 bit should be '_'
	    val little = buffer.get().c == 'v'      // big endian should be 'V'

        val major = buffer.get().c.parseInt()
        val minor = buffer.get().c.parseInt() * 10 + buffer.get().c.parseInt()
        logger.info("Blender version is $major.$minor (64bit: $i64bit, little endian: $little)")
	    if(ASSIMP.BLENDER_DEBUG) logger.info { "Blender DEBUG ENABLED" }

        val reader = buffer.slice().order(if(little) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

	    val db = FileDatabase(reader, i64bit, little)

        parseBlendFile(db)

        val blendScene = extractScene(db)

	    blendScene.convertBlendFile(db, scene)
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
        if (dna == null) throw Exception("SDNA not found")

        out.entries.sort()
    }

    private fun extractScene(file: FileDatabase): Scene {

        val sceneIndex = file.dna.indices["Scene"] ?: throw Exception("There is no `Scene` structure record")

        val ss = file.dna.structures[sceneIndex]

	    // we need a scene somewhere to start with.
	    val block = file.entries.find {
		    // Fix: using the DNA index is more reliable to locate scenes
		    //if (bl.id == "SC") {
		    it.dnaIndex == sceneIndex
	    } ?: throw Exception("There is not a single `Scene` record to load")

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

	protected fun Scene.convertBlendFile(db: FileDatabase, out: AiScene) {

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
			throw Exception("Expected at least one object with no parent")
		}

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
			out.meshes = ArrayList(conv.meshes.size) { conv.meshes[it] }
			conv.meshes.clear()
		}

		if(conv.lights.size > 0) {
			out.numLights = conv.lights.size
			out.lights = ArrayList(conv.lights.size) { conv.lights[it] }
			conv.lights.clear()
		}

		if(conv.cameras.size > 0) {
			out.numCameras = conv.cameras.size
			out.cameras = ArrayList(conv.cameras.size) { conv.cameras[it] }
			conv.cameras.clear()
		}

		if(conv.materials.size > 0) {
			out.numMaterials = conv.materials.size
			out.materials = ArrayList(conv.materials.size) { conv.materials[it] }
			conv.materials.clear()
		}

		if(conv.textures.isNotEmpty()) {
			out.numTextures = conv.textures.size
			for((name, tex) in conv.textures) {
				tex.achFormatHint
				// TODO convert to gli.Texture out.textures[name] = tex ??
			}
//			out->mTextures = new aiTexture*[out->mNumTextures = static_cast<unsigned int>( conv.textures->size() )];
//			std::copy(conv.textures->begin(),conv.textures->end(),out->mTextures);
//			conv.textures.dismiss();
		}


        // acknowledge that the scene might come out incomplete
  		// by Assimp's definition of `complete`: blender scenes
  		// can consist of thousands of cameras or lights with
  		// not a single mesh between them.
		if(out.numMeshes == 0){
			out.flags = out.flags or AI_SCENE_FLAGS_INCOMPLETE
		}
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
					convertMesh(obj, data as Mesh, conv, conv.meshes)

					if(conv.meshes.size > old) {
						node.meshes = IntArray(conv.meshes.size - old) { it + old }
						node.numMeshes = node.meshes.size
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
				else -> throw Exception("When should be exhaustive")
			}
			Unit // return Unit from let explicitly so that when and the contained if statements don't need to be exhaustive
		}

		for(x in 0 until 4) {
			for(y in 0 until 4) {
				node.transformation[y][x] = obj.obmat[y][x]
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

	private fun checkActualType(dt: ElemBase, check: String) {
		assert(dt.dnaType == check) {
			"Expected object `$dt` to be of type `$check`, but it claims to be a `${dt.dnaType}` instead"
		}
	}

	private fun buildMaterials(conv: ConversionData) {

		conv.materials.reserve(conv.materialsRaw.size)

		buildDefaultMaterial(conv)

		for(mat in conv.materialsRaw) {

			// reset per material global counters
			for(i in 0 until conv.nextTexture.size) { // i < sizeof(conv.next_texture)/sizeof(conv.next_texture[0])
				conv.nextTexture[i] = 0
			}

			val mout = AiMaterial()
			conv.materials.pushBack(mout)
			// For any new material field handled here, the default material above must be updated with an appropriate default value.

			// set material name
			mout.name = mat.id.name.substring(2)

			val color = AiMaterial.Color()
			mout.color = color

			// basic material colors
			val col = AiColor3D(mat.r, mat.g, mat.b)
			if(mat.r > 0 || mat.g > 0 || mat.b > 0) {

				// Usually, zero diffuse color means no diffuse color at all in the equation.
				// So we omit this member to express this intent.
				color.diffuse = col

				if(mat.emit > 0) {
					val emitCol = col * mat.emit
					color.emissive = emitCol
				}
			}

			color.specular = AiColor3D(mat.specr, mat.specg, mat.specb)

			// is hardness/shininess set?
			if(mat.har > 0) {
				mout.shininess = mat.har.f
			}

			color.ambient = AiColor3D(mat.ambr, mat.ambg, mat.ambb)

			// is mirror enabled?
			if(mat.mode and MA_RAYMIRROR > 0){
				mout.reflectivity = mat.rayMirror
			}

			color.reflective = AiColor3D(mat.mirr, mat.mirg, mat.mirb)

			for(i in 0 until mat.mTex.size) {
				if(mat.mTex[i] == null){
					continue
				}

				resolveTexture(mout, mat, mat.mTex[i]!!, conv)
			}
		}
	}

	private fun buildDefaultMaterial(conv: ConversionData) {
		// add a default material if necessary

		var index = -1
		for(mesh in conv.meshes) {
			if(mesh.materialIndex == -1) {

				if(index == -1) {
					// Setup a default material
					val default = Material()
					default.id.name = "MA$AI_DEFAULT_MATERIAL_NAME" // HINT all other materials start with MA and the first 2 letters get removed

					// Note: MSVC11 does not zero-initialize Material here, although it should.
					// Thus all relevant fields should be explicitly initialized. We cannot add
					// a default constructor to Material since the DNA codegen does not support
					// parsing it.
					default.r = 0.6f
					default.g = 0.6f
					default.b = 0.6f

					default.specr = 0.6f
					default.specg = 0.6f
					default.specb = 0.6f

					default.mirr = 0f
					default.mirg = 0f
					default.mirb = 0f

					default.emit = 0f
					default.alpha = 0f
					default.har = 0

					index = conv.materialsRaw.size
					conv.materialsRaw.pushBack(default)
					logger.info("Adding default material")
				}
				mesh.materialIndex = index
			}
		}
	}

	private fun resolveTexture(out: AiMaterial, mat: Material, tex: MTex, conv: ConversionData) {
		TODO("resolveTexture")
	}

	private fun convertMesh(obj: Object, mesh: Mesh, conv: ConversionData, meshList/* temp in C version */: ArrayList<AiMesh>) {

		/*
		// TODO: Resolve various problems with BMesh triangulation before re-enabling.
        //       See issues #400, #373, #318  #315 and #132.
	#if defined(TODO_FIX_BMESH_CONVERSION)
	    BlenderBMeshConverter BMeshConverter( mesh );
	    if ( BMeshConverter.ContainsBMesh( ) )
	    {
	        mesh = BMeshConverter.TriangulateBMesh( );
	    }
	#endif
		 */

		// typedef std::pair<const int,size_t> MyPair;
		if((mesh.totface == 0 && mesh.totloop == 0) || mesh.totvert == 0 ) {
			return
		}

		// extract nullables
		val verts = requireNotNull(mesh.mvert) { "mvert in mesh is null!" }

		fun face(i: Int) = mesh.mface!![i]
		fun loop(i: Int) = mesh.mloop!![i]
		fun poly(i: Int) = mesh.mpoly!![i]

		// some sanity checks
		if(mesh.totface > 0) {
			val faces = requireNotNull(mesh.mface) { "mface in mesh is null!" }
			require(mesh.totface <= faces.size ) { "Number of faces is larger than the corresponding array" }
		}
		require(mesh.totvert <= verts.size ) { "Number of vertices is larger than the corresponding array" }
		if(mesh.totloop > 0) {
			val loops = requireNotNull(mesh.mloop) { "mloop in mesh is null!" }
			require(mesh.totloop <= loops.size) { "Number of loops is larger than the corresponding array" }
		}
		if(mesh.totpoly > 0) {
			val polys = requireNotNull(mesh.mpoly) { "mpoly in mesh is null!" }
			require(mesh.totpoly <= polys.size) { "Number of polygons is larger than the corresponding array" }
		}

		// collect per-submesh numbers
		val perMat = mutableMapOf<Int, Int>()
		val perMatVerts = mutableMapOf<Int, Int>()

		for(i in 0 until mesh.totface) {
			val face = face(i)

			perMat[face.matNr] = perMat.getOrDefault(face.matNr, 0) + 1
			val vertCount = if(face.v4 != 0) 4 else 3

			perMatVerts[face.matNr] = perMatVerts.getOrDefault(face.matNr, 0) + vertCount
		}
		for(i in 0 until mesh.totpoly) {
			val poly = poly(i)

			perMat[poly.matNr] = perMat.getOrDefault(poly.matNr, 0) + 1
			perMatVerts[poly.matNr] = perMatVerts.getOrDefault(poly.matNr, 0) + poly.totLoop
		}

		// ... and allocate the corresponding meshes
		val old = meshList.size
		meshList.ensureCapacity(meshList.size + perMat.size)

		val matNumToMeshIndex = mutableMapOf<Int, Int>()
		fun getMesh(matNr: Int): AiMesh = meshList[matNumToMeshIndex[matNr]!!]

		for((matNr, faceCount) in perMat) {

			matNumToMeshIndex[matNr] = meshList.size

			val out = AiMesh()
			meshList.pushBack(out)

			val vertexCount = perMatVerts[matNr]!!
			out.vertices = MutableList(vertexCount) { AiVector3D() }
			out.normals = MutableList(vertexCount) { AiVector3D() }

			//out->mNumFaces = 0
			//out->mNumVertices = 0
			out.faces = MutableList(faceCount) { mutableListOf<Int>() }

			// all sub-meshes created from this mesh are named equally. this allows
			// curious users to recover the original adjacency.
			out.name = mesh.id.name.substring(2)
			// skip over the name prefix 'ME'

			// resolve the material reference and add this material to the set of
			// output materials. The (temporary) material index is the index
			// of the material entry within the list of resolved materials.
			if (mesh.mat != null) {

				val materials = mesh.mat!!

				if(matNr >= materials.size) {
					throw IndexOutOfBoundsException("Material index is out of range")
				}

				val mat = checkNotNull(materials[matNr]) { "Material with index $matNr does not exist!" }

				val index = conv.materialsRaw.indexOf(mat)
				if (index == -1) {
					out.materialIndex = conv.materialsRaw.size
					conv.materialsRaw.pushBack(mat)
				} else {
					out.materialIndex = index
				}
			} else {
				out.materialIndex = -1 // static_cast<unsigned int>( -1 );
			}

		}

		fun AiMesh.addVertexToFace(f: AiFace, pos: Int) {
			if(pos >= mesh.totvert) {
				throw IndexOutOfBoundsException("Vertex index out of range")
			}
			val v = verts[pos]

			vertices[numVertices] = AiVector3D(v.co)
			normals[numVertices] = AiVector3D(v.no)
			f.pushBack(numVertices)
			numVertices++
		}

		for(i in 0 until mesh.totface) {

			val mf = face(i)

			val out = getMesh(mf.matNr)

			val f = out.faces[out.numFaces] // AiFace == MutableList<AiFace>
			out.numFaces++

			out.addVertexToFace(f, mf.v1)
			out.addVertexToFace(f, mf.v2)
			out.addVertexToFace(f, mf.v3)
			if(mf.v4 > 0) {
				out.addVertexToFace(f, mf.v4)
				out.primitiveTypes = out.primitiveTypes or AiPrimitiveType.POLYGON
			} else {
				out.primitiveTypes = out.primitiveTypes or AiPrimitiveType.TRIANGLE
			}
		}

		for(i in 0 until mesh.totpoly) {
			val mp = poly(i)

			val out = getMesh(mp.matNr)

			val f = out.faces[out.numFaces]
			out.numFaces++


			for(j in 0 until mp.totLoop) {
				val loop = loop(mp.loopStart + j)

				out.addVertexToFace(f, loop.v)
			}
			if(mp.totLoop == 3) {
				out.primitiveTypes = out.primitiveTypes or AiPrimitiveType.TRIANGLE
			} else {
				out.primitiveTypes = out.primitiveTypes or AiPrimitiveType.POLYGON
			}
		}
	    // TODO should we create the TextureUVMapping map in Convert<Material> to prevent redundant processing?

	    // create texture <-> uvname mapping for all materials
	    // key is texture number, value is data *
	    // typedef std::map<uint32_t, const MLoopUV *> TextureUVMapping;
	    // key is material number, value is the TextureUVMapping for the material
	    // typedef std::map<uint32_t, TextureUVMapping> MaterialTextureUVMappings;

		val matTexUvMappings: MaterialTextureUVMap = mutableMapOf()

		if(mesh.mat != null) {
			val mats = mesh.mat!!
			val maxMat = mats.size
			for (m in 0 until maxMat) {
				val mat = checkNotNull(mats[m])

				val texUV: TextureUVMap = mutableMapOf()
				val maxTex = mat.mTex.size
				for (t in 0 until maxTex) {
					val tex = mat.mTex[t]
					if (tex != null && tex.uvName.isNotEmpty()) {
						// get the CustomData layer for given uvname and correct type
						val loop = mesh.ldata.getLayerData<MLoopUV>(CustomDataType.MLoopUV, tex.uvName)
						if (loop != null) {
							texUV[t] = loop
						}
					}
				}
				if (texUV.isNotEmpty()) {
					matTexUvMappings[m] = texUV
				}
			}
		}

		// collect texture coordinates, they're stored in a separate per-face buffer
		if(mesh.mtface != null || mesh.mloopuv != null) {

			if(mesh.totface > 0 && mesh.totface > mesh.mtface!!.size) {
				throw IndexOutOfBoundsException("number of uv faces is larger than the corresponding uv face array (#1)")
			}

			for(itMesh in meshList.subList(old, meshList.size)) {

				assert(itMesh.numVertices > 0 && itMesh.numFaces > 0)

				val itMatTexUvMap = matTexUvMappings[itMesh.materialIndex]
				if(itMatTexUvMap == null) {
					// default behaviour like before
					itMesh.textureCoords.add(MutableList(itMesh.numVertices) { FloatArray(2) })
				} else {
					// create texture coords for every mapped tex
					for (i in 0 until itMatTexUvMap.size) {
						itMesh.textureCoords.add(MutableList(itMesh.numVertices)  { FloatArray(2) })
					}
				}
				itMesh.numFaces = 0
				itMesh.numVertices = 0
			}

			for(meshIndex in 0 until mesh.totface) {

				val mtface = mesh.mtface!![meshIndex]

				val out = getMesh(face(meshIndex).matNr)
				val f = out.faces[out.numFaces]
				out.numFaces++

				for(i in 0 until f.size) {
					val vo = out.textureCoords[0][out.numVertices]
					vo[0] = mtface.uv[i][0] // x
					vo[1] = mtface.uv[i][1] // y
					out.numVertices++
				}
			}

			for(loopIndex in 0 until mesh.totpoly) {
				val poly = poly(loopIndex)
				val out = getMesh(poly.matNr)

				val f = out.faces[out.numFaces]
				out.numFaces++

				val itMatTexUvMap = matTexUvMappings[poly.matNr]
				if(itMatTexUvMap == null) {
					// old behavior
					for(j in 0 until f.size) {
						val vo = out.textureCoords[0][out.numVertices]
						val uv = mesh.mloopuv!![poly.loopStart + j]

						vo[0] = uv.uv[0]
						vo[1] = uv.uv[1]
						out.numVertices++
					}
				} else {
					// create textureCoords for every mapped tex
					for(m in 0 until itMatTexUvMap.size) {
						val tm = itMatTexUvMap[m]!!
						for(j in 0 until f.size) {
							val vo = out.textureCoords[m][out.numVertices]
							val uv = tm.uv
							vo[0] = uv[0]
							vo[1] = uv[1]
							out.numVertices++
						}
					}
				}
			}
		}

		// collect texture coordinates, old-style (marked as deprecated in current blender sources)
		if(mesh.tface != null) {
			val tfaces = mesh.tface!!

			assert(mesh.totface <= tfaces.size) { "Number of faces is larger than the corresponding UV face array (#2)" }

			for(itMesh in meshList) {
				assert(itMesh.numVertices > 0 && itMesh.numFaces > 0)

				itMesh.textureCoords[0] = MutableList(itMesh.numVertices) { FloatArray(2) }

				itMesh.numFaces = 0
				itMesh.numVertices = 0
			}

			for(faceIndex in 0 until mesh.totface) {
				val v = tfaces[faceIndex]

				val out = getMesh(face(faceIndex).matNr)
				val f = out.faces
				for(i in 0 until f.size) {
					val vo = out.textureCoords[0][out.numVertices]
					vo[0] = v.uv[i][0]
					vo[1] = v.uv[i][1]
				}
			}
		}

		if(mesh.mcol != null && mesh.mloopcol != null) {
			val mcol = mesh.mcol!!
			val mloopcol = mesh.mloopcol!!

			if(mesh.totface > mcol.size / 4) {
				throw IllegalStateException("Number of faces is larger than corresponding color face array")
			}
			for(meshIt in meshList) {

				assert(meshIt.numVertices and meshIt.numFaces != 0) // What the hell??? I think this checks that both numVertices and numFaces is not 0

				meshIt.colors[0] = MutableList(meshIt.numVertices) { AiColor4D() }
				meshIt.numVertices = 0
				meshIt.numFaces = 0
			}

			for(faceIndex in 0 until mesh.totface) {

				val out = getMesh(face(faceIndex).matNr)
				val f = out.faces[out.numFaces]
				out.numFaces++

				for(n in 0 until f.size) {
					val col = mcol[(faceIndex shl 2) + n]   // I see the IndexOutOfBoundsException coming already
					val vo = out.colors[0][out.numVertices]
					vo.r = col.r.f
					vo.g = col.g.f
					vo.b = col.b.f
					vo.a = col.a.f
					out.numVertices++
				}
				// for (unsigned int n = f.mNumIndices; n < 4; ++n); I don't even ....
			}

			for (polyIndex in 0 until mesh.totpoly) {
				val v = poly(polyIndex)
				val out = getMesh(v.matNr)
				val f = out.faces[out.numFaces]
				out.numFaces++

				val scaleZeroToOne = 1f/255f
				for(j in 0 until f.size) {
					val col = mloopcol[v.loopStart + j]
					val vo = out.colors[0][out.numVertices]
					vo.r = col.r.f * scaleZeroToOne
					vo.g = col.g.f * scaleZeroToOne
					vo.b = col.b.f * scaleZeroToOne
					vo.a = col.a.f * scaleZeroToOne
				}
			}
		}
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
			else -> {} // TODO missing light types??? do nothing?? really??
		}

		out.colorAmbient = AiColor3D(lamp.r, lamp.b, lamp.g) * lamp.energy
		out.colorSpecular= AiColor3D(lamp.r, lamp.b, lamp.g) * lamp.energy
		out.colorDiffuse = AiColor3D(lamp.r, lamp.b, lamp.g) * lamp.energy

		return out
	}
}

fun error(policy: ErrorPolicy, value: Any?, message: String?): Unit = when(policy) {
    ErrorPolicy.Warn -> logger.warn { "value: $value, $message" }
    ErrorPolicy.Fail -> throw Exception( "value: $value, $message" )
    ErrorPolicy.Igno -> if (ASSIMP.BLENDER_DEBUG) logger.info { "value: $value, $message" } else Unit
}
