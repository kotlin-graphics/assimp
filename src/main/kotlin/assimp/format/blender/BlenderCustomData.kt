package assimp.format.blender

import java.lang.IllegalArgumentException
import kotlin.reflect.*

enum class CustomDataType {
	// HINT: the name of the enum must be the same as the structure(dna) name

	MVert,
	MSticky,  /* deprecated */
	MDeformVert,
	MEdge,
	MFace,
	MTFace,
	MCol,
	OrigIndex,
	Normal,
	PolyIndex,
	PropFlt,
	PropInt,
	PropStr,
	OrigSpace,  /* for modifier stack face location mapping */
	Orco,
	MTexPoly,
	MLoopUV,
	MLoopCol,
	Tangent,
	MDisps,
	PreviewMCol,  /* for displaying weightpaint colors */
	IdMcol,
	TextureMLoopCol,
	ClothOrco,
	Recast,

	/* BMESH ONLY START */
	MPoly,
	MLoop,
	ShapeKeyIndex,
	ShapeKey,
	BWeight,
	Crease,
	OrigSpaceMloop,
	PreviewMloopCol,
	BmElemPyPtr,
	/* BMESH ONLY END */

	PaintMask,
	GridPaintMask,
	MVertSkin,
	FreestyleEdge,
	FreestyleFace,
	MLoopTangent,
	TessLoopNormal,
	CustomLoopNormal,


	// HINT enums with custom values need to come last otherwise 'ordinal' will not work
	/*
	AutoFromName (""){
		override val i = -1
		override val isValid: Boolean
			get() = false
	},
	*/

	None {
		override val i = -2
		override val isValid: Boolean
			get() = false
	};

	open val i = ordinal

	open val isValid: Boolean get() = true

	companion object {
		const val numTypes = 42 // this does not count AutoFromName of None

		fun of(value: Int) = values().first { it.i == value }
	}
}

/*
/**
*   @brief  pointer to function read memory for n CustomData types
*/
typedef bool        (*PRead)(ElemBase *pOut, const size_t cnt, const FileDatabase &db);
typedef ElemBase *  (*PCreate)(const size_t cnt);
typedef void(*PDestroy)(ElemBase *);
 */
private typealias ReadCustomDataFunction = (s: Structure) -> ElemBase

// supported structures for CustomData
private val customDataTypeDescriptions = mapOf<CustomDataType, ReadCustomDataFunction>(
		CustomDataType.MVert to ::readMVertCustomData,
		CustomDataType.MEdge to ::readMEdgeCustomData,
		CustomDataType.MFace to ::readMFaceCustomData,
		CustomDataType.MTFace to ::readMTFaceCustomData,
		CustomDataType.MTexPoly to ::readMTexPolyCustomData,
		CustomDataType.MLoopUV to ::readMLoopUvCustomData,
		CustomDataType.MLoopCol to ::readMLoopColCustomData,
		CustomDataType.MPoly to ::readMPolyCustomData,
		CustomDataType.MLoop to ::readMLoopCustomData
                                                                                      )

private var tempAny: ElemBase? = null

fun readMVertCustomData(s: Structure): MVert {
	tempAny = MVert()
	@Suppress("UNCHECKED_CAST")
	s.convertMVert(::tempAny as KMutableProperty0<MVert?>)
	return tempAny as MVert
}

fun readMEdgeCustomData(s: Structure): MEdge {
	tempAny = MEdge()
	@Suppress("UNCHECKED_CAST")
	s.convertMEdge(::tempAny as KMutableProperty0<MEdge?>)
	return tempAny as MEdge
}

fun readMFaceCustomData(s: Structure): MFace {
	tempAny = MFace()
	@Suppress("UNCHECKED_CAST")
	s.convertMFace(::tempAny as KMutableProperty0<MFace?>)
	return tempAny as MFace
}

fun readMTFaceCustomData(s: Structure): MTFace {
	tempAny = MTFace()
	@Suppress("UNCHECKED_CAST")
	s.convertMTFace(::tempAny as KMutableProperty0<MTFace?>)
	return tempAny as MTFace
}

fun readMTexPolyCustomData(s: Structure): MTexPoly {
	tempAny = MTexPoly()
	@Suppress("UNCHECKED_CAST")
	s.convertMTexPoly(::tempAny as KMutableProperty0<MTexPoly?>)
	return tempAny as MTexPoly
}

fun readMLoopUvCustomData(s: Structure): MLoopUV {
	tempAny = MLoopUV()
	@Suppress("UNCHECKED_CAST")
	s.convertMLoopUV(::tempAny as KMutableProperty0<MLoopUV?>)
	return tempAny as MLoopUV
}

fun readMLoopColCustomData(s: Structure): MLoopCol {
	tempAny = MLoopCol()
	@Suppress("UNCHECKED_CAST")
	s.convertMLoopCol(::tempAny as KMutableProperty0<MLoopCol?>)
	return tempAny as MLoopCol
}

fun readMPolyCustomData(s: Structure): MPoly {
	tempAny = MPoly()
	@Suppress("UNCHECKED_CAST")
	s.convertMPoly(::tempAny as KMutableProperty0<MPoly?>)
	return tempAny as MPoly
}

fun readMLoopCustomData(s: Structure): MLoop {
	tempAny = MLoop()
	@Suppress("UNCHECKED_CAST")
	s.convertMLoop(::tempAny as KMutableProperty0<MLoop?>)
	return tempAny as MLoop
}

/**
 *   read CustomData's data to ptr to mem
 *   @param out memory ptr to set
 *   @param cdtype  to read
 *   @param cnt cnt of elements to read
 *   @param db to read elements from
 *   @return true when ok
 */
fun <T: ElemBase>readCustomData(out: KMutableProperty0<T?>, cdtype: CustomDataType, cnt: Int, db: FileDatabase): Boolean {

	if(!cdtype.isValid) {
		throw IllegalArgumentException("cdtype is not valid")
	}
	if(cnt == 0) return false

	if(cnt != 1) {
		throw UnsupportedOperationException("Reading lists of customData is not yet supported") // TODO
	}

	val readFunction = customDataTypeDescriptions[cdtype] ?: return false

	val s = db.dna[cdtype.name]

	@Suppress("UNCHECKED_CAST")
	out.set(readFunction(s) as T)

	return true
}

/**
 *   returns CustomDataLayer ptr for given cdtype and name
 *   @param this@getLayer CustomData to search for wanted layer
 *   @param cdtype to search for
 *   @param name to search for
 *   @return CustomDataLayer * or nullptr if not found
 */
fun CustomData.getLayer(cdtype: CustomDataType, name: String): CustomDataLayer?
	= layers.asSequence().filterNotNull().firstOrNull { it.type == cdtype && it.name == name }

/**
 *   returns CustomDataLayer data ptr for given cdtype and name
 *   @param customdata CustomData to search for wanted layer
 *   @param cdtype to search for
 *   @param name to search for
 *   @return * to struct data or nullptr if not found
 */
inline fun <reified T: ElemBase> CustomData.getLayerData(cdtype: CustomDataType, name: String): T? = getLayer(cdtype, name)?.data as T?
