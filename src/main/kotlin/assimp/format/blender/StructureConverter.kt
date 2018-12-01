package assimp.format.blender

import assimp.*
import glm_.*
import kotlin.reflect.*

private fun <T>KMutableProperty0<T?>.setIfNull(value: T): T = this() ?: value.also { set(value) }

// HINT those properties are aliases for the properties of Structure.Companion.
// This way it's easier to access them as KMutableProperty0
private var tempInt
	get() = Structure.tempInt
	set(i) {
		Structure.tempInt = i
	}

private var tempAny
	get() = Structure.tempAny
	set(a) {
		Structure.tempAny = a
	}

private var tempElemBase
	get() = Structure.tempElemBase
	set(e) {
		Structure.tempElemBase = e
	}

private var isElem
	get() = Structure.isElem
	set(b) {
		Structure.isElem = b
	}

fun Structure.convertObject(dest: KMutableProperty0<Object?>) {

	val d = dest.setIfNull(Object())

	readField(ErrorPolicy.Fail, d.id, "id")
	readField(ErrorPolicy.Fail, ::tempInt, "type")
	d.type = Object.Type.of(tempInt)
	readFieldArray2(ErrorPolicy.Warn, d.obmat, "obmat")
	readFieldArray2(ErrorPolicy.Warn, d.parentinv, "parentinv")
	d.parSubstr = readFieldString(ErrorPolicy.Warn, "parsubstr")
	readFieldPtr(ErrorPolicy.Warn, d::parent, "*parent", false)
	readFieldPtr(ErrorPolicy.Warn, d::track, "*track", false)
	readFieldPtr(ErrorPolicy.Warn, d::proxy, "*proxy", false)
	readFieldPtr(ErrorPolicy.Warn, d::proxyFrom, "*proxy_from", false)
	readFieldPtr(ErrorPolicy.Warn, d::proxyGroup, "*proxy_group", false)
	readFieldPtr(ErrorPolicy.Warn, d::dupGroup, "*dup_group", false)
	isElem = true
	readFieldPtr(ErrorPolicy.Fail, d::data, "*data", false)
	readField(ErrorPolicy.Igno, d.modifiers, "modifiers")

	db.reader.pos += size.i
}

fun Structure.convertGroup(dest: KMutableProperty0<Group?>) {

	val d = dest.setIfNull(Group())

	readField(ErrorPolicy.Fail, d.id, "id")
	readField(ErrorPolicy.Igno, d::layer, "layer")
	readFieldPtr(ErrorPolicy.Igno, d::gObject, "*gobject", false)

	db.reader.pos += size.i
}

fun Structure.convertMTex(dest: KMutableProperty0<MTex?>) {

	val d = dest.setIfNull(MTex())

	readField(ErrorPolicy.Igno, ::tempInt, "mapto")
	d.mapTo = MTex.MapType.of(tempInt)
	readField(ErrorPolicy.Igno, ::tempInt, "blendtype")
	d.blendType = MTex.BlendType.of(tempInt)
	readFieldPtr(ErrorPolicy.Igno, d::object_, "*object", false)
	readFieldPtr(ErrorPolicy.Igno, d::tex, "*tex", false)
	d.uvName = readFieldString(ErrorPolicy.Igno, "uvname")
	readField(ErrorPolicy.Igno, ::tempInt, "projx")
	d.projX = MTex.Projection.of(tempInt)
	readField(ErrorPolicy.Igno, ::tempInt, "projy")
	d.projY = MTex.Projection.of(tempInt)
	readField(ErrorPolicy.Igno, ::tempInt, "projz")
	d.projZ = MTex.Projection.of(tempInt)
	readField(ErrorPolicy.Igno, d::mapping, "mapping")
	readFieldFloatArray(ErrorPolicy.Igno, d.ofs, "ofs")
	readFieldFloatArray(ErrorPolicy.Igno, d.size, "size")
	readField(ErrorPolicy.Igno, d::rot, "rot")
	readField(ErrorPolicy.Igno, d::texFlag, "texflag")
	readField(ErrorPolicy.Igno, d::colorModel, "colormodel")
	readField(ErrorPolicy.Igno, d::pMapTo, "pmapto")
	readField(ErrorPolicy.Igno, d::pMapToNeg, "pmaptoneg")
	readField(ErrorPolicy.Warn, d::r, "r")
	readField(ErrorPolicy.Warn, d::g, "g")
	readField(ErrorPolicy.Warn, d::b, "b")
	readField(ErrorPolicy.Warn, d::k, "k")
	readField(ErrorPolicy.Igno, d::colSpecFac, "colspecfac")
	readField(ErrorPolicy.Igno, d::mirrFac, "mirrfac")
	readField(ErrorPolicy.Igno, d::alphaFac, "alphafac")
	readField(ErrorPolicy.Igno, d::diffFac, "difffac")
	readField(ErrorPolicy.Igno, d::specFac, "specfac")
	readField(ErrorPolicy.Igno, d::emitFac, "emitfac")
	readField(ErrorPolicy.Igno, d::hardFac, "hardfac")
	readField(ErrorPolicy.Igno, d::norFac, "norfac")

	db.reader.pos += size.i
}

fun Structure.convertTFace(dest: KMutableProperty0<TFace?>) {

	val d = dest.setIfNull(TFace())

	readFieldArray2(ErrorPolicy.Fail, d.uv, "uv")
	readFieldIntArray(ErrorPolicy.Fail, d.col, "col")
	readField(ErrorPolicy.Igno, d::flag, "flag")
	readField(ErrorPolicy.Igno, d::mode, "mode")
	readField(ErrorPolicy.Igno, d::tile, "tile")
	readField(ErrorPolicy.Igno, d::unwrap, "unwrap")

	db.reader.pos += size.i
}

fun Structure.convertSubsurfModifierData(dest: KMutableProperty0<SubsurfModifierData?>) {

	val d = dest.setIfNull(SubsurfModifierData())

	readField(ErrorPolicy.Fail, d.modifier, "modifier")
	readField(ErrorPolicy.Warn, d::subdivType, "subdivType")
	readField(ErrorPolicy.Fail, d::levels, "levels")
	readField(ErrorPolicy.Igno, d::renderLevels, "renderLevels")
	readField(ErrorPolicy.Igno, d::flags, "flags")

	db.reader.pos += size.i
}

fun Structure.convertMFace(dest: KMutableProperty0<MFace?>) {

	val d = dest.setIfNull(MFace())

	readField(ErrorPolicy.Fail, d::v1, "v1")
	readField(ErrorPolicy.Fail, d::v2, "v2")
	readField(ErrorPolicy.Fail, d::v3, "v3")
	readField(ErrorPolicy.Fail, d::v4, "v4")
	readField(ErrorPolicy.Fail, d::matNr, "mat_nr")
	readField(ErrorPolicy.Igno, d::flag, "flag")

	db.reader.pos += size.i
}

fun Structure.convertLamp(dest: KMutableProperty0<Lamp?>) {

	val d = dest.setIfNull(Lamp())

	readField(ErrorPolicy.Fail, d.id, "id")
	readField(ErrorPolicy.Fail, ::tempInt, "type")
	d.type = Lamp.Type.of(tempInt)
	readField(ErrorPolicy.Igno, d::flags, "flags")
	readField(ErrorPolicy.Igno, d::colorModel, "colormodel")
	readField(ErrorPolicy.Igno, d::totex, "totex")
	readField(ErrorPolicy.Warn, d::r, "r")
	readField(ErrorPolicy.Warn, d::g, "g")
	readField(ErrorPolicy.Warn, d::b, "b")
	readField(ErrorPolicy.Warn, d::k, "k")
	readField(ErrorPolicy.Igno, d::energy, "energy")
	readField(ErrorPolicy.Igno, d::dist, "dist")
	readField(ErrorPolicy.Igno, d::spotSize, "spotsize")
	readField(ErrorPolicy.Igno, d::spotBlend, "spotblend")
	readField(ErrorPolicy.Igno, d::att1, "att1")
	readField(ErrorPolicy.Igno, d::att2, "att2")
	readField(ErrorPolicy.Igno, ::tempInt, "falloff_type")
	d.falloffType = Lamp.FalloffType.of(tempInt)
	readField(ErrorPolicy.Igno, d::sunBrightness, "sun_brightness")
	readField(ErrorPolicy.Igno, d::areaSize, "area_size")
	readField(ErrorPolicy.Igno, d::areaSizeY, "area_sizey")
	readField(ErrorPolicy.Igno, d::areaSizeZ, "area_sizez")
	readField(ErrorPolicy.Igno, d::areaShape, "area_shape")

	db.reader.pos += size.i
}

fun Structure.convertMDeformWeight(dest: KMutableProperty0<MDeformWeight?>) {

	val d = dest.setIfNull(MDeformWeight())

	readField(ErrorPolicy.Fail, d::defNr, "def_nr")
	readField(ErrorPolicy.Fail, d::weight, "weight")

	db.reader.pos += size.i
}

fun Structure.convertPackedFile(dest: KMutableProperty0<PackedFile?>) {

	val d = dest.setIfNull(PackedFile())

	readField(ErrorPolicy.Warn, d::size, "size")
	readField(ErrorPolicy.Warn, d::seek, "seek")
	readFieldPtr(ErrorPolicy.Warn, d::data, "*data", false)

	db.reader.pos += size.i
}


fun Structure.convertBase(dest: KMutableProperty0<Base?>) {
	/*  note: as per https://github.com/assimp/assimp/issues/128, reading the Object linked list recursively is
		prone to stack overflow.
		This structure converter is therefore an hand-written exception that does it iteratively.   */

	val initialPos = db.reader.pos

	var todo = dest.setIfNull(Base()) to initialPos
	while (true) {

		val curDest = todo.first
		db.reader.pos = todo.second

		/*  we know that this is a double-linked, circular list which we never traverse backwards,
			so don't bother resolving the back links.             */
		curDest.prev = null

		readFieldPtr(ErrorPolicy.Warn, curDest::obj, "*object", false)

		/*  the return value of ReadFieldPtr indicates whether the object was already cached.
			In this case, we don't need to resolve it again.    */
		if (!readFieldPtr(ErrorPolicy.Warn, curDest::next, "*next", false, true) && curDest.next != null) {
			todo = (curDest.next ?: Base().also { curDest.next = it }) to db.reader.pos
			continue
		}
		break
	}

	db.reader.pos = initialPos + size.i
}

fun Structure.convertMTFace(dest: KMutableProperty0<MTFace?>) {

	val d = dest.setIfNull(MTFace())

	readFieldArray2(ErrorPolicy.Fail, d.uv, "uv")
	readField(ErrorPolicy.Igno, d::flag, "flag")
	readField(ErrorPolicy.Igno, d::tile, "tile")
	readField(ErrorPolicy.Igno, d::unwrap, "unwrap")

	db.reader.pos += size.i
}

fun Structure.convertMaterial(dest: KMutableProperty0<Material?>) {

	val d = dest.setIfNull(Material())

	readField(ErrorPolicy.Fail, d.id, "id")
	readField(ErrorPolicy.Warn, d::r, "r")
	readField(ErrorPolicy.Warn, d::g, "g")
	readField(ErrorPolicy.Warn, d::b, "b")
	readField(ErrorPolicy.Warn, d::specr, "specr")
	readField(ErrorPolicy.Warn, d::specg, "specg")
	readField(ErrorPolicy.Warn, d::specb, "specb")
	readField(ErrorPolicy.Igno, d::har, "har")
	readField(ErrorPolicy.Warn, d::ambr, "ambr")
	readField(ErrorPolicy.Warn, d::ambg, "ambg")
	readField(ErrorPolicy.Warn, d::ambb, "ambb")
	readField(ErrorPolicy.Igno, d::mirr, "mirr")
	readField(ErrorPolicy.Igno, d::mirg, "mirg")
	readField(ErrorPolicy.Igno, d::mirb, "mirb")
	readField(ErrorPolicy.Warn, d::emit, "emit")
	readField(ErrorPolicy.Igno, d::rayMirror, "ray_mirror")
	readField(ErrorPolicy.Warn, d::alpha, "alpha")
	readField(ErrorPolicy.Igno, d::ref, "ref")
	readField(ErrorPolicy.Igno, d::translucency, "translucency")
	readField(ErrorPolicy.Igno, d::mode, "mode")
	readField(ErrorPolicy.Igno, d::roughness, "roughness")
	readField(ErrorPolicy.Igno, d::darkness, "darkness")
	readField(ErrorPolicy.Igno, d::refrac, "refrac")
	readFieldPtr(ErrorPolicy.Igno, d::group, "*group", false)
	readField(ErrorPolicy.Warn, d::diffShader, "diff_shader")
	readField(ErrorPolicy.Warn, d::specShader, "spec_shader")
	readFieldPtr(ErrorPolicy.Igno, d.mTex, "*mtex")

	readField(ErrorPolicy.Igno, d::amb, "amb")
	readField(ErrorPolicy.Igno, d::ang, "ang")
	readField(ErrorPolicy.Igno, d::spectra, "spectra")
	readField(ErrorPolicy.Igno, d::spec, "spec")
	readField(ErrorPolicy.Igno, d::zoffs, "zoffs")
	readField(ErrorPolicy.Igno, d::add, "add")
	readField(ErrorPolicy.Igno, d::fresnelMir, "fresnel_mir")
	readField(ErrorPolicy.Igno, d::fresnelMirI, "fresnel_mir_i")
	readField(ErrorPolicy.Igno, d::fresnelTra, "fresnel_tra")
	readField(ErrorPolicy.Igno, d::fresnelTraI, "fresnel_tra_i")
	readField(ErrorPolicy.Igno, d::filter, "filter")
	readField(ErrorPolicy.Igno, d::txLimit, "tx_limit")
	readField(ErrorPolicy.Igno, d::txFalloff, "tx_falloff")
	readField(ErrorPolicy.Igno, d::glossMir, "gloss_mir")
	readField(ErrorPolicy.Igno, d::glossTra, "gloss_tra")
	readField(ErrorPolicy.Igno, d::adaptThreshMir, "adapt_thresh_mir")
	readField(ErrorPolicy.Igno, d::adaptThreshTra, "adapt_thresh_tra")
	readField(ErrorPolicy.Igno, d::anisoGlossMir, "aniso_gloss_mir")
	readField(ErrorPolicy.Igno, d::distMir, "dist_mir")
	readField(ErrorPolicy.Igno, d::hasize, "hasize")
	readField(ErrorPolicy.Igno, d::flaresize, "flaresize")
	readField(ErrorPolicy.Igno, d::subsize, "subsize")
	readField(ErrorPolicy.Igno, d::flareboost, "flareboost")
	readField(ErrorPolicy.Igno, d::strandSta, "strand_sta")
	readField(ErrorPolicy.Igno, d::strandEnd, "strand_end")
	readField(ErrorPolicy.Igno, d::strandEase, "strand_ease")
	readField(ErrorPolicy.Igno, d::strandSurfnor, "strand_surfnor")
	readField(ErrorPolicy.Igno, d::strandMin, "strand_min")
	readField(ErrorPolicy.Igno, d::strandWidthfade, "strand_widthfade")
	readField(ErrorPolicy.Igno, d::sbias, "sbias")
	readField(ErrorPolicy.Igno, d::lbias, "lbias")
	readField(ErrorPolicy.Igno, d::shadAlpha, "shad_alpha")
	readField(ErrorPolicy.Igno, d::param, "param")
	readField(ErrorPolicy.Igno, d::rms, "rms")
	readField(ErrorPolicy.Igno, d::rampfacCol, "rampfac_col")
	readField(ErrorPolicy.Igno, d::rampfacSpec, "rampfac_spec")
	readField(ErrorPolicy.Igno, d::friction, "friction")
	readField(ErrorPolicy.Igno, d::fh, "fh")
	readField(ErrorPolicy.Igno, d::reflect, "reflect")
	readField(ErrorPolicy.Igno, d::fhdist, "fhdist")
	readField(ErrorPolicy.Igno, d::xyfrict, "xyfrict")
	readField(ErrorPolicy.Igno, d::sssRadius, "sss_radius")
	readField(ErrorPolicy.Igno, d::sssCol, "sss_col")
	readField(ErrorPolicy.Igno, d::sssError, "sss_error")
	readField(ErrorPolicy.Igno, d::sssScale, "sss_scale")
	readField(ErrorPolicy.Igno, d::sssIor, "sss_ior")
	readField(ErrorPolicy.Igno, d::sssColfac, "sss_colfac")
	readField(ErrorPolicy.Igno, d::sssTexfac, "sss_texfac")
	readField(ErrorPolicy.Igno, d::sssFront, "sss_front")
	readField(ErrorPolicy.Igno, d::sssBack, "sss_back")

	readField(ErrorPolicy.Igno, d::material_type, "material_type")
	readField(ErrorPolicy.Igno, d::flag, "flag")
	readField(ErrorPolicy.Igno, d::rayDepth, "ray_depth")
	readField(ErrorPolicy.Igno, d::rayDepthTra, "ray_depth_tra")
	readField(ErrorPolicy.Igno, d::sampGlossMir, "samp_gloss_mir")
	readField(ErrorPolicy.Igno, d::sampGlossTra, "samp_gloss_tra")
	readField(ErrorPolicy.Igno, d::fadetoMir, "fadeto_mir")
	readField(ErrorPolicy.Igno, d::shadeFlag, "shade_flag")
	readField(ErrorPolicy.Igno, d::flarec, "flarec")
	readField(ErrorPolicy.Igno, d::starc, "starc")
	readField(ErrorPolicy.Igno, d::linec, "linec")
	readField(ErrorPolicy.Igno, d::ringc, "ringc")
	readField(ErrorPolicy.Igno, d::prLamp, "pr_lamp")
	readField(ErrorPolicy.Igno, d::prTexture, "pr_texture")
	readField(ErrorPolicy.Igno, d::mlFlag, "ml_flag")
	readField(ErrorPolicy.Igno, d::diffShader, "diff_shader")
	readField(ErrorPolicy.Igno, d::specShader, "spec_shader")
	readField(ErrorPolicy.Igno, d::texco, "texco")
	readField(ErrorPolicy.Igno, d::mapto, "mapto")
	readField(ErrorPolicy.Igno, d::rampShow, "ramp_show")
	readField(ErrorPolicy.Igno, d::pad3, "pad3")
	readField(ErrorPolicy.Igno, d::dynamode, "dynamode")
	readField(ErrorPolicy.Igno, d::pad2, "pad2")
	readField(ErrorPolicy.Igno, d::sssFlag, "sss_flag")
	readField(ErrorPolicy.Igno, d::sssPreset, "sss_preset")
	readField(ErrorPolicy.Igno, d::shadowonlyFlag, "shadowonly_flag")
	readField(ErrorPolicy.Igno, d::index, "index")
	readField(ErrorPolicy.Igno, d::vcolAlpha, "vcol_alpha")
	readField(ErrorPolicy.Igno, d::pad4, "pad4")

	readField(ErrorPolicy.Igno, d::seed1, "seed1")
	readField(ErrorPolicy.Igno, d::seed2, "seed2")

	db.reader.pos += size.i
}

fun Structure.convertMTexPoly(dest: KMutableProperty0<MTexPoly?>) {
	val d = dest.setIfNull(MTexPoly())

	readFieldPtr(ErrorPolicy.Igno, d::tpage, "*tpage", false)
	readField(ErrorPolicy.Igno, d::flag, "flag")
	readField(ErrorPolicy.Igno, d::transp, "transp")
	readField(ErrorPolicy.Igno, d::mode, "mode")
	readField(ErrorPolicy.Igno, d::tile, "tile")
	readField(ErrorPolicy.Igno, d::pad, "pad")

	db.reader.pos += size.i
}

fun Structure.convertMesh(dest: KMutableProperty0<Mesh?>) {

	val d = dest.setIfNull(Mesh())

	readField(ErrorPolicy.Fail, d.id, "id")
	readField(ErrorPolicy.Fail, d::totface, "totface")
	readField(ErrorPolicy.Fail, d::totedge, "totedge")
	readField(ErrorPolicy.Fail, d::totvert, "totvert")
	readField(ErrorPolicy.Igno, d::totloop, "totloop")
	readField(ErrorPolicy.Igno, d::totpoly, "totpoly")
	readField(ErrorPolicy.Igno, d::subdiv, "subdiv")
	readField(ErrorPolicy.Igno, d::subdivr, "subdivr")
	readField(ErrorPolicy.Igno, d::subsurftype, "subsurftype")
	readField(ErrorPolicy.Igno, d::subsurftype, "subsurftype")
	readField(ErrorPolicy.Igno, d::smoothresh, "smoothresh")
	readFieldPtr(ErrorPolicy.Fail, d::mface, "*mface", true)
	readFieldPtr(ErrorPolicy.Igno, d::mtface, "*mtface", true)
	readFieldPtr(ErrorPolicy.Igno, d::tface, "*tface", true)
	readFieldPtr(ErrorPolicy.Fail, d::mvert, "*mvert", true)
	readFieldPtr(ErrorPolicy.Warn, d::medge, "*medge", true)
	readFieldPtr(ErrorPolicy.Igno, d::mloop, "*mloop", true)
	readFieldPtr(ErrorPolicy.Igno, d::mloopuv, "*mloopuv", true)
	readFieldPtr(ErrorPolicy.Igno, d::mloopcol, "*mloopcol", true)
	readFieldPtr(ErrorPolicy.Igno, d::mpoly, "*mpoly", true)
	readFieldPtr(ErrorPolicy.Igno, d::mtpoly, "*mtpoly", true)
	readFieldPtr(ErrorPolicy.Igno, d::dvert, "*dvert", true)
	readFieldPtr(ErrorPolicy.Igno, d::mcol, "*mcol", true)
	readFieldPtrList(ErrorPolicy.Fail, d::mat, "**mat")

	readField(ErrorPolicy.Igno, d.vdata, "vdata")
	readField(ErrorPolicy.Igno, d.edata, "edata")
	readField(ErrorPolicy.Igno, d.fdata, "fdata")
	readField(ErrorPolicy.Igno, d.pdata, "pdata")
	readField(ErrorPolicy.Igno, d.ldata, "ldata")

	db.reader.pos += size.i
}

fun Structure.convertMDeformVert(dest: KMutableProperty0<MDeformVert?>) {

	val d = dest.setIfNull(MDeformVert())

	readFieldPtr(ErrorPolicy.Warn, d::dw, "*dw", true)
	readField(ErrorPolicy.Igno, d::totWeight, "totweight")

	db.reader.pos += size.i
}

fun Structure.convertWorld(dest: KMutableProperty0<World?>) {

	val d = dest.setIfNull(World())

	readField(ErrorPolicy.Fail, d.id, "id")

	db.reader.pos += size.i
}

fun Structure.convertMLoopCol(dest: KMutableProperty0<MLoopCol?>) {

	val d = dest.setIfNull(MLoopCol())

	readField(ErrorPolicy.Igno, d::r, "r")
	readField(ErrorPolicy.Igno, d::g, "g")
	readField(ErrorPolicy.Igno, d::b, "b")
	readField(ErrorPolicy.Igno, d::a, "a")

	db.reader.pos += size.i
}

fun Structure.convertMVert(dest: KMutableProperty0<MVert?>) {
	val d = dest.setIfNull(MVert())

	readFieldFloatArray(ErrorPolicy.Fail, d.co, "co")
	readFieldFloatArray(ErrorPolicy.Fail, d.no, "no")
	readField(ErrorPolicy.Igno, d::flag, "flag")
	//readField(Ep.Warn, d.matNr, "matNr")
	readField(ErrorPolicy.Igno, d::weight, "bweight")

	db.reader.pos += size.i
}

fun Structure.convertMEdge(dest: KMutableProperty0<MEdge?>) {

	val d = dest.setIfNull(MEdge())

	readField(ErrorPolicy.Fail, d::v1, "v1")
	readField(ErrorPolicy.Fail, d::v2, "v2")
	readField(ErrorPolicy.Igno, d::crease, "crease")
	readField(ErrorPolicy.Igno, d::weight, "bweight")
	readField(ErrorPolicy.Igno, d::flag, "flag")

	db.reader.pos += size.i
}

fun Structure.convertMLoopUV(dest: KMutableProperty0<MLoopUV?>) {

	val d = dest.setIfNull(MLoopUV())

	readFieldFloatArray(ErrorPolicy.Igno, d.uv, "uv")
	readField(ErrorPolicy.Igno, d::flag, "flag")

	db.reader.pos += size.i
}

fun Structure.convertGroupObject(dest: KMutableProperty0<GroupObject?>) {

	val d = dest.setIfNull(GroupObject())

	readFieldPtr(ErrorPolicy.Fail, d::prev, "*prev", false)
	readFieldPtr(ErrorPolicy.Fail, d::next, "*next", false)
	readFieldPtr(ErrorPolicy.Igno, d::ob, "*ob", false)

	db.reader.pos += size.i
}


fun Structure.convert(dest: ListBase) {

	isElem = true
	readFieldPtr(ErrorPolicy.Igno, dest::first, "*first", false)
	isElem = true
	readFieldPtr(ErrorPolicy.Igno, dest::last, "*last", false)

	db.reader.pos += size.i
}

fun Structure.convertListBase(dest: KMutableProperty0<ListBase?>){
	val d = dest.setIfNull(ListBase())
	convert(d)
}

fun Structure.convertMLoop(dest: KMutableProperty0<MLoop?>) {
	val d = dest.setIfNull(MLoop())

	readField(ErrorPolicy.Igno, d::v, "v")
	readField(ErrorPolicy.Igno, d::e, "e")

	db.reader.pos += size.i
}

fun Structure.convert(data: ModifierData) {

	readFieldPtr(ErrorPolicy.Warn, data::next, "*next", false)
	readFieldPtr(ErrorPolicy.Warn, data::prev, "*prev", false)
	readField(ErrorPolicy.Igno, data::type, "type")
	readField(ErrorPolicy.Igno, data::mode, "mode")
	data.name = readFieldString(ErrorPolicy.Igno, "name")

	db.reader.pos += size.i
}

fun Structure.convertModifierData(dest: KMutableProperty0<ModifierData?>) {

	val d = dest.setIfNull(ModifierData())
	convert(d)
}

fun Structure.convert(id: Id) {

	id.name = readFieldString(ErrorPolicy.Warn, "name")
	readField(ErrorPolicy.Igno, id::flag, "flag")

	db.reader.pos += size.i
}

fun Structure.convertId(dest: KMutableProperty0<Id?>) {
	val d = dest.setIfNull(Id())
	convert(d)
}

fun Structure.convertMCol(dest: KMutableProperty0<MCol?>) {

	val d = dest.setIfNull(MCol())

	readField(ErrorPolicy.Fail, d::r, "r")
	readField(ErrorPolicy.Fail, d::g, "g")
	readField(ErrorPolicy.Fail, d::b, "b")
	readField(ErrorPolicy.Fail, d::a, "a")

	db.reader.pos += size.i
}

fun Structure.convertMPoly(dest: KMutableProperty0<MPoly?>) {
	val d = dest.setIfNull(MPoly())

	readField(ErrorPolicy.Igno, d::loopStart, "loopstart")
	readField(ErrorPolicy.Igno, d::totLoop, "totloop")
	readField(ErrorPolicy.Igno, d::matNr, "mat_nr")
	readField(ErrorPolicy.Igno, d::flag, "flag")

	db.reader.pos += size.i
}

fun Structure.convert(dest: Scene) {

	readField(ErrorPolicy.Fail, dest.id, "id")
	readFieldPtr(ErrorPolicy.Warn, dest::camera, "*camera", false)
	readFieldPtr(ErrorPolicy.Warn, dest::world, "*world", false)
	readFieldPtr(ErrorPolicy.Warn, dest::basact, "*basact", false)
	readField(ErrorPolicy.Igno, dest.base, "base")

	db.reader.pos += size.i
}

fun Structure.convertScene(dest: KMutableProperty0<Scene?>) {

	val d = dest.setIfNull(Scene())
	convert(d)
}

fun Structure.convertLibrary(dest: KMutableProperty0<Library?>) {

	val d = dest.setIfNull(Library())

	readField(ErrorPolicy.Fail, d.id, "id")
	d.name = readFieldString(ErrorPolicy.Warn, "name")
	d.filename = readFieldString(ErrorPolicy.Fail, "filename")
	readFieldPtr(ErrorPolicy.Warn, d::parent, "*parent", false)

	db.reader.pos += size.i
}

fun Structure.convertTex(dest: KMutableProperty0<Tex?>) {

	val d = dest.setIfNull(Tex())

	readField(ErrorPolicy.Fail, d.id, "id")  // TODO not in the C version, investigate

	readField(ErrorPolicy.Igno, d::imaFlag, "imaflag")
	readField(ErrorPolicy.Fail, ::tempInt, "type")
	d.type = Tex.Type.of(tempInt)
	readFieldPtr(ErrorPolicy.Warn, d::ima, "*ima", false)

	db.reader.pos += size.i
}

fun Structure.convertCamera(dest: KMutableProperty0<Camera?>) {

	val d = dest() ?: Camera().also { dest.set(it) }

	readField(ErrorPolicy.Fail, d.id, "id")
	readField(ErrorPolicy.Warn, ::tempInt, "type")
	d.type = Camera.Type.of(tempInt)
	readField(ErrorPolicy.Warn, d::flag, "flag")
	readField(ErrorPolicy.Warn, d::lens, "lens")
	readField(ErrorPolicy.Warn, d::sensorX, "sensor_x")
	readField(ErrorPolicy.Igno, d::clipSta, "clipsta")
	readField(ErrorPolicy.Igno, d::clipEnd, "clipend")

	db.reader.pos += size.i
}

fun Structure.convertMirrorModifierData(dest: KMutableProperty0<MirrorModifierData?>) {

	val d = dest.setIfNull(MirrorModifierData())

	readField(ErrorPolicy.Fail, d.modifier, "modifier")
	readField(ErrorPolicy.Igno, d::axis, "axis")
	readField(ErrorPolicy.Igno, d::flag, "flag")
	readField(ErrorPolicy.Igno, d::tolerance, "tolerance")
	readFieldPtr(ErrorPolicy.Igno, d::mirrorOb, "*mirror_ob", false)

	db.reader.pos += size.i
}

fun Structure.convertImage(dest: KMutableProperty0<Image?>) {

	val d = dest.setIfNull(Image())

	readField(ErrorPolicy.Fail, d.id, "id")
	d.name = readFieldString(ErrorPolicy.Warn, "name")
	readField(ErrorPolicy.Igno, d::ok, "ok")
	readField(ErrorPolicy.Igno, d::flag, "flag")
	readField(ErrorPolicy.Igno, d::source, "source")
	readField(ErrorPolicy.Igno, d::type, "type")
	readField(ErrorPolicy.Igno, d::pad, "pad")
	readField(ErrorPolicy.Igno, d::pad1, "pad1")
	readField(ErrorPolicy.Igno, d::lastFrame, "lastframe")
	readField(ErrorPolicy.Igno, d::tPageFlag, "tpageflag")
	readField(ErrorPolicy.Igno, d::totBind, "totbind")
	readField(ErrorPolicy.Igno, d::xRep, "xrep")
	readField(ErrorPolicy.Igno, d::yRep, "yrep")
	readField(ErrorPolicy.Igno, d::twsta, "twsta")
	readField(ErrorPolicy.Igno, d::twend, "twend")
	readFieldPtr(ErrorPolicy.Igno, d::packedfile, "*packedfile", false)
	readField(ErrorPolicy.Igno, d::lastUpdate, "lastupdate")
	readField(ErrorPolicy.Igno, d::lastUsed, "lastused")
	readField(ErrorPolicy.Igno, d::animSpeed, "animspeed")
	readField(ErrorPolicy.Igno, d::genX, "gen_x")
	readField(ErrorPolicy.Igno, d::genY, "gen_y")
	readField(ErrorPolicy.Igno, d::genType, "gen_type")

	db.reader.pos += size.i
}

fun Structure.convert(data: CustomData) {

	readFieldIntArray(ErrorPolicy.Warn, data.typemap, "typemap")
	readField(ErrorPolicy.Warn, data::totlayer, "totlayer")
	readField(ErrorPolicy.Warn, data::maxlayer, "maxlayer")
	readField(ErrorPolicy.Warn, data::totsize, "totsize")
	readFieldPtrVector(ErrorPolicy.Warn, data.layers, "*layers")

	db.reader.pos += size.i
}

fun Structure.convertCustomData(dest: KMutableProperty0<CustomData?>){

	val d = dest.setIfNull(CustomData())

	convert(d)
}

fun Structure.convertCustomDataLayer(dest: KMutableProperty0<CustomDataLayer?>) {

	val d = dest.setIfNull(CustomDataLayer())

	readField(ErrorPolicy.Fail, ::tempInt, "type")
	d.type = CustomDataType.of(tempInt)
	readField(ErrorPolicy.Fail, d::offset, "offset")
	readField(ErrorPolicy.Fail, d::flag, "flag")
	readField(ErrorPolicy.Fail, d::active, "active")
	readField(ErrorPolicy.Fail, d::activeRnd, "active_rnd")
	readField(ErrorPolicy.Fail, d::activeClone, "active_clone")
	readField(ErrorPolicy.Fail, d::activeMask, "active_mask")
	readField(ErrorPolicy.Warn, d::uid, "uid")   // HINT this is set to Fail in C-version but this does not exist in 2.5
	d.name = readFieldString(ErrorPolicy.Warn, "name")
	readCustomDataPtr(ErrorPolicy.Fail, d::data, d.type, "*data")

	db.reader.pos += size.i
}