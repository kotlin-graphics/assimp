package assimp

import assimp.format.X.XFileImporter
import assimp.format.assbin.AssbinLoader
import assimp.format.blender.BlenderImporter
import assimp.format.collada.ColladaLoader
import assimp.format.fbx.FbxImporter
import assimp.format.md2.Md2Importer
import assimp.format.md3.Md3Importer
import assimp.format.md5.Md5Importer
import assimp.format.obj.ObjFileImporter
import assimp.format.ply.PlyLoader
import assimp.format.stl.StlImporter

/**
 * Created by elect on 13/11/2016.
 */

val importerInstanceList: ArrayList<BaseImporter>
    get() = ArrayList<BaseImporter>().apply {
        with(ASSIMP.NO) {
            // ----------------------------------------------------------------------------
            // Add an instance of each worker class here
            // (register_new_importers_here)
            // ---------------------------------------------------------------------------- {
            //if (!assimp.ASSIMP_BUILD_NO_MD2_IMPORTER) add(Md2Importer())
            if (!ASSBIN_IMPORTER)
                add(AssbinLoader())

            if (!COLLADA_IMPORTER)
                add(ColladaLoader())

            if (!FBX_IMPORTER)
                add(FbxImporter())

            if (!MD2_IMPORTER)
                add(Md2Importer())

            if (!MD3_IMPORTER)
                add(Md3Importer())

            if (!MD5_IMPORTER)
                add(Md5Importer())

            if (!OBJ_IMPORTER)
                add(ObjFileImporter())

            if (!PLY_IMPORTER)
                add(PlyLoader())

            if (!STL_IMPORTER)
                add(StlImporter())

            if (!X_IMPORTER)
                add(XFileImporter())

            if (!BLEND_IMPORTER)
                add(BlenderImporter())
        }
    }