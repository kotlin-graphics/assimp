package assimp.postProcess

import assimp.*
import assimp.AiPostProcessStep as Pps
import assimp.AiShadingMode as Sm

class OptimizeMeshes : BaseProcess(){

    private val NotSet = 0xffffffff
    private val DeadBeef = 0xdeadbeef
    private lateinit var mScene : AiScene
    private var pts = false
    private var maxVerts = NotSet
    private var maxFaces = NotSet
    var meshes = ArrayList<MeshInfo>()
    var output = ArrayList<AiMesh>()
    private var merge_list = ArrayList<AiMesh>()

    override fun isActive(flags: Int) : Boolean{
        if(flags has Pps.OptimizeMeshes){
            pts = flags has Pps.SortByPType
            maxVerts = when(flags has Pps.SplitLargeMeshes){
                true -> DeadBeef
                false -> maxVerts
            }
            return true
        }
        return false
    }

    override fun execute(scene: AiScene){
        val oldNum = scene.numMeshes
        if (oldNum <= 1) {
            logger.debug("Skipping OptimizeMeshesProcess")
            return
        }
        logger.debug("OptimizeMeshesProcess begin")

        mScene = scene

        findInstancedMeshes(mScene.rootNode)
        if( maxVerts == DeadBeef) /* undo the magic hack */
            maxVerts = NotSet

        var n : Long = 0
        for (i in 0 until mScene.numMeshes) {
            meshes[i].vertex_format = ProcessHelper.getMeshVFormatUnique(mScene.meshes[i])

            if (meshes[i].instance_cnt > 1 && meshes[i].output_id == NotSet) {
                meshes[i].output_id = n++
                output.add(mScene.meshes[i])
            }
        }

        processNode(mScene.rootNode)
        if (output.size == 0) {
            throw RuntimeException("OptimizeMeshes: No meshes remaining; there's definitely something wrong")
        }

        meshes = ArrayList()
        assert(output.size <= oldNum)

        mScene.numMeshes = output.size
        System.arraycopy(output,0, mScene.meshes,0, output.size)

        if (output.size != oldNum) {
            logger.info("OptimizeMeshesProcess finished. Input meshes: %d, Output meshes: %d",oldNum, mScene.numMeshes)
        } else {
            logger.debug( "OptimizeMeshesProcess finished" )
        }
    }

    override fun setupProperties(imp: Importer){
        if(maxVerts == DeadBeef){
            TODO()
            //maxFaces = imp.getPropertyInteger(AI_CONFIG_PP_SLM_TRIANGLE_LIMIT,AI_SLM_DEFAULT_MAX_TRIANGLES);
            //maxVerts = imp.getPropertyInteger(AI_CONFIG_PP_SLM_VERTEX_LIMIT,AI_SLM_DEFAULT_MAX_VERTICES);
        }
    }

    fun findInstancedMeshes (pNode: AiNode)
    {
        for(i in 0 until pNode.numMeshes) {
            ++meshes[ pNode.meshes[ i ] ].instance_cnt
        }
        for(i in 0 until pNode.numChildren) {
            findInstancedMeshes(pNode.children[i])
        }
    }

    fun processNode(pNode: AiNode)
    {
        for (i in 0 until pNode.numMeshes) {
            var im = pNode.meshes[i]

            if (meshes[im].instance_cnt > 1) {
                im = meshes[im].output_id.toInt()
            }
            else  {
                merge_list = ArrayList()
                var verts = 0
                var faces = 0

                // Find meshes to merge with us
                var a = 0
                while(a < pNode.numMeshes){
                    val am = pNode.meshes[a]
                    if (meshes[am].instance_cnt == 1 && canJoin(im, am, verts, faces)) {

                        merge_list.add(mScene.meshes[am])
                        verts += mScene.meshes[am].numVertices
                        faces += mScene.meshes[am].numFaces

                        pNode.meshes[a] = pNode.meshes[pNode.numMeshes - 1]
                        --pNode.numMeshes
                        --a
                    }
                    ++a
                }

                // and merge all meshes which we found, replace the old ones
                if (merge_list.size != 0 ){
                    merge_list.add(mScene.meshes[im])

                    val out = ArrayList<AiMesh>()
                    SceneCombiner.mergeMeshes(out, 0, merge_list, 0, merge_list.size)
                    for(oMesh in out){
                        output.add(oMesh)
                    }
                } else {
                    output.add(mScene.meshes[im])
                }
                im = output.size - 1
            }
        }


        for(i in 0 until pNode.numChildren) {
            processNode(pNode.children[i])
        }
    }

    fun canJoin(a : Int, b : Int, verts: Int, faces: Int ) : Boolean
    {
        if (meshes[a].vertex_format != meshes[b].vertex_format)
            return false

        val ma = mScene.meshes[a]
        val mb = mScene.meshes[b]

        if ((NotSet != maxVerts && verts+mb.numVertices > maxVerts) || (NotSet != maxFaces && faces+mb.numFaces    > maxFaces)) {
            return false
        }

        // Never merge unskinned meshes with skinned meshes
        if (ma.materialIndex != mb.materialIndex || ma.hasBones != mb.hasBones)
            return false

        // Never merge meshes with different kinds of primitives if SortByPType did already
        // do its work. We would destroy everything again ...
        if (pts && ma.primitiveTypes != mb.primitiveTypes)
            return false

        // If both meshes are skinned, check whether we have many bones defined in both meshes.
        // If yes, we can join them.
        if (ma.hasBones) {
            // TODO
            return false
        }

        return true
    }



    class MeshInfo{
        //! Number of times this mesh is referenced
        var instance_cnt = 0
        //! Vertex format id
        var vertex_format = 0
        //! Output ID
        var output_id = 0xffffffff
    }



}