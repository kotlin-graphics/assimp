package java.assimp

/**
 * Created by elect on 13/11/2016.
 */

var NO_VALIDATEDS_PROCESS = false

var ASSIMP_BUILD_NO_X_IMPORTER = false
var ASSIMP_BUILD_NO_OBJ_IMPORTER = false
var ASSIMP_BUILD_NO_AMF_IMPORTER = false
var ASSIMP_BUILD_NO_3DS_IMPORTER = false
var ASSIMP_BUILD_NO_MD3_IMPORTER = false
var ASSIMP_BUILD_NO_MD2_IMPORTER = false
var ASSIMP_BUILD_NO_PLY_IMPORTER = false
var ASSIMP_BUILD_NO_MDL_IMPORTER = false
var ASSIMP_BUILD_NO_ASE_IMPORTER = false
var ASSIMP_BUILD_NO_HMP_IMPORTER = false
var ASSIMP_BUILD_NO_SMD_IMPORTER = false
var ASSIMP_BUILD_NO_MDC_IMPORTER = false
var ASSIMP_BUILD_NO_MD5_IMPORTER = false
var ASSIMP_BUILD_NO_STL_IMPORTER = false
var ASSIMP_BUILD_NO_LWO_IMPORTER = false
var ASSIMP_BUILD_NO_DXF_IMPORTER = false
var ASSIMP_BUILD_NO_NFF_IMPORTER = false
var ASSIMP_BUILD_NO_RAW_IMPORTER = false
var ASSIMP_BUILD_NO_SIB_IMPORTER = false
var ASSIMP_BUILD_NO_OFF_IMPORTER = false
var ASSIMP_BUILD_NO_AC_IMPORTER = false
var ASSIMP_BUILD_NO_BVH_IMPORTER = false
var ASSIMP_BUILD_NO_IRRMESH_IMPORTER = false
var ASSIMP_BUILD_NO_IRR_IMPORTER = false
var ASSIMP_BUILD_NO_Q3D_IMPORTER = false
var ASSIMP_BUILD_NO_B3D_IMPORTER = false
var ASSIMP_BUILD_NO_COLLADA_IMPORTER = false
var ASSIMP_BUILD_NO_TERRAGEN_IMPORTER = false
var ASSIMP_BUILD_NO_CSM_IMPORTER = false
var ASSIMP_BUILD_NO_3D_IMPORTER = false
var ASSIMP_BUILD_NO_LWS_IMPORTER = false
var ASSIMP_BUILD_NO_OGRE_IMPORTER = false
var ASSIMP_BUILD_NO_OPENGEX_IMPORTER = false
var ASSIMP_BUILD_NO_MS3D_IMPORTER = false
var ASSIMP_BUILD_NO_COB_IMPORTER = false
var ASSIMP_BUILD_NO_BLEND_IMPORTER = false
var ASSIMP_BUILD_NO_Q3BSP_IMPORTER = false
var ASSIMP_BUILD_NO_NDO_IMPORTER = false
var ASSIMP_BUILD_NO_IFC_IMPORTER = false
var ASSIMP_BUILD_NO_XGL_IMPORTER = false
var ASSIMP_BUILD_NO_FBX_IMPORTER = false
var ASSIMP_BUILD_NO_ASSBIN_IMPORTER = false
var ASSIMP_BUILD_NO_GLTF_IMPORTER = false
var ASSIMP_BUILD_NO_C4D_IMPORTER = false
var ASSIMP_BUILD_NO_3MF_IMPORTER = false
var ASSIMP_BUILD_NO_X3D_IMPORTER = false


val AI_CONFIG_IMPORT_MD2_KEYFRAME = "IMPORT_MD2_KEYFRAME"

fun getImporterInstanceList(): MutableList<BaseImporter> {
    // ----------------------------------------------------------------------------
    // Add an instance of each worker class here
    // (register_new_importers_here)
    // ----------------------------------------------------------------------------
    val res = mutableListOf<BaseImporter>()
    if (!ASSIMP_BUILD_NO_MD2_IMPORTER) {
//        res.add(Md2Importer())
    }
    if(!ASSIMP_BUILD_NO_STL_IMPORTER) {

    }
    return res
}

fun getPostProcessingStepInstanceList(): List<BaseProcess> = listOf();