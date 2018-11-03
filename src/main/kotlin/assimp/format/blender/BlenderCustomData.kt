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
private typealias ReadCustomDataFunction = (s: Structure, cnt: Int) -> List<ElemBase>

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

private val tempAny: ElemBase? = null

fun readMVertCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MVert>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMVert(::tempAny as KMutableProperty0<MVert?>)
		tempAny!! as MVert
	}
}

fun readMEdgeCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MEdge>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMEdge(::tempAny as KMutableProperty0<MEdge?>)
		tempAny!! as MEdge
	}
}

fun readMFaceCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MFace>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMFace(::tempAny as KMutableProperty0<MFace?>)
		tempAny!! as MFace
	}
}

fun readMTFaceCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MTFace>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMTFace(::tempAny as KMutableProperty0<MTFace?>)
		tempAny!! as MTFace
	}
}

fun readMTexPolyCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MTexPoly>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMTexPoly(::tempAny as KMutableProperty0<MTexPoly?>)
		tempAny!! as MTexPoly
	}
}

fun readMLoopUvCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MLoopUV>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMLoopUV(::tempAny as KMutableProperty0<MLoopUV?>)
		tempAny!! as MLoopUV
	}
}

fun readMLoopColCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MLoopCol>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMLoopCol(::tempAny as KMutableProperty0<MLoopCol?>)
		tempAny!! as MLoopCol
	}
}

fun readMPolyCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MPoly>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMPoly(::tempAny as KMutableProperty0<MPoly?>)
		tempAny!! as MPoly
	}
}

fun readMLoopCustomData(s: Structure, cnt: Int): List<ElemBase> {
	return List<MLoop>(cnt) {
		@Suppress("UNCHECKED_CAST")
		s.convertMLoop(::tempAny as KMutableProperty0<MLoop?> )
		tempAny!! as MLoop
	}
}



/**
 *   read CustomData's data to ptr to mem
 *   @param out memory ptr to set
 *   @param cdtype  to read
 *   @param cnt cnt of elements to read
 *   @param db to read elements from
 *   @return true when ok
 */
fun <T: ElemBase>readCustomData(out: KMutableProperty0<MutableList<T?>>, cdtype: CustomDataType, cnt: Int, db: FileDatabase): Boolean {

	if(!cdtype.isValid) {
		throw IllegalArgumentException("cdtype is not valid")
	}
	if(cnt == 0) return false

	val readFunction = customDataTypeDescriptions[cdtype] ?: return false

	val s = db.dna[cdtype.name]

	val outList = readFunction(s, cnt).toMutableList()
	@Suppress("UNCHECKED_CAST")
	out.set(outList as MutableList<T?>)

	return true
}

/*
	/**
    *   @brief  returns CustomDataLayer ptr for given cdtype and name
    *   @param[in]  customdata CustomData to search for wanted layer
    *   @param[in]  cdtype to search for
    *   @param[in]  name to search for
    *   @return CustomDataLayer * or nullptr if not found
    */
    std::shared_ptr<CustomDataLayer> getCustomDataLayer(const CustomData &customdata, CustomDataType cdtype, const std::string &name);

    /**
    *   @brief  returns CustomDataLayer data ptr for given cdtype and name
    *   @param[in]  customdata CustomData to search for wanted layer
    *   @param[in]  cdtype to search for
    *   @param[in]  name to search for
    *   @return * to struct data or nullptr if not found
    */
    const ElemBase * getCustomDataLayerData(const CustomData &customdata, CustomDataType cdtype, const std::string &name);
 */