package assimp.format.blender

import kotlin.reflect.*

enum class CustomDataType{

	AutoFromName {
		override val i = -1
	},

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
	MLoopUv,
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
	CustomLoopNormal;

	open val i = ordinal
}

fun <T: ElemBase>readCustomData(out: KMutableProperty0<T?>, cdtype: Int, cnt: Int): Boolean {
	TODO()
}

