package assimp.postProcess

import assimp.AiScene
import glm_.vec3.Vec3

object fastQuadraticMeshSimplification {

    class SymmetricMatrix(val m: DoubleArray) {

        constructor(c: Double = 0.0) : this(DoubleArray(10) { c })

        constructor(m11: Double, m12: Double, m13: Double, m14: Double,
                    m22: Double, m23: Double, m24: Double,
                    m33: Double, m34: Double,
                    m44: Double) :
                this(doubleArrayOf(
                        m11, m12, m13, m14,
                        m22, m23, m24,
                        m33, m34,
                        m44))

        // Make plane
        constructor(a: Double, b: Double, c: Double, d: Double) :
                this(doubleArrayOf(
                        a * a, a * b, a * c, a * d,
                        b * b, b * c, b * d,
                        c * c, c * d,
                        d * d))

        operator fun get(c: Int) = m[c]

        // Determinant
        fun det(a11: Int, a12: Int, a13: Int,
                a21: Int, a22: Int, a23: Int,
                a31: Int, a32: Int, a33: Int) =
                m[a11] * m[a22] * m[a33] + m[a13] * m[a21] * m[a32] + m[a12] * m[a23] * m[a31] -
                        m[a13] * m[a22] * m[a31] - m[a11] * m[a23] * m[a32] - m[a12] * m[a21] * m[a33]

        operator fun plus(n: SymmetricMatrix) = SymmetricMatrix(
                m[0] + n[0], m[1] + n[1], m[2] + n[2], m[3] + n[3],
                m[4] + n[4], m[5] + n[5], m[6] + n[6],
                m[7] + n[7], m[8] + n[8],
                m[9] + n[9])

        operator fun plusAssign(n: SymmetricMatrix) {
            m[0] += n[0]; m[1] += n[1]; m[2] += n[2]; m[3] += n[3]
            m[4] += n[4]; m[5] += n[5]; m[6] += n[6]; m[7] += n[7]
            m[8] += n[8]; m[9] += n[9]
        }
    }

    // Global Variables & Strctures

    class Triangle(
            val v: IntArray, val err: DoubleArray,
            var deleted: Int, var dirty: Int,
            val n: Vec3)

    class Vertex(
            val p: Vec3,
            var tStart: Int,
            var tCount: Int,
            var q: SymmetricMatrix,
            var border: Int)

    class Ref(val tid: Int, val tvertex: Int)

    val triangles = ArrayList<Triangle>()
    val vertices = ArrayList<Vertex>()
    val refs = ArrayList<Ref>()

    /**
     * Main simplification function
     *
     * target_count  : target nr. of triangles
     * agressiveness : sharpness to increase the threshold.
     *                  5..8 are good numbers
     *                  more iterations yield higher quality
     */
    fun simplify(scene: AiScene, targetCount: Int, aggressiveness: Int = 7) {

        println("simplify - start")
        val timeStart = System.currentTimeMillis()

        for (mesh in scene.meshes) {

            triangles.forEach { it.deleted = 0 }

            // main iteration loop

            var deletedTriangles = 0
            val deleted0 = ArrayList<Int>()
            val deleted1 = ArrayList<Int>()
            val triangleCount = triangles.size

            for (iteration in 0..99) {

                // target number of triangles reached ? Then break
                println("iteration $iteration - triangles ${triangleCount - deletedTriangles}")

                if (triangleCount - deletedTriangles <= targetCount) break

                // update mesh once in a while
                if (iteration % 5 == 0)
                    updateMesh(iteration)
            }
        }
    }

    /** compact triangles, compute edge error and build reference list */
    fun updateMesh(iteration: Int)    {

//        if(iteration>0) { // compact triangles
//
//            var dst=0;
//            loopi(0,triangles.size())
//            if(!triangles[i].deleted)
//            {
//                triangles[dst++]=triangles[i];
//            }
//            triangles.resize(dst);
//        }
//        //
//        // Init Quadrics by Plane & Edge Errors
//        //
//        // required at the beginning ( iteration == 0 )
//        // recomputing during the simplification is not required,
//        // but mostly improves the result for closed meshes
//        //
//        if( iteration == 0 )
//        {
//            loopi(0,vertices.size())
//            vertices[i].q=SymetricMatrix(0.0);
//
//            loopi(0,triangles.size())
//            {
//                Triangle &t=triangles[i];
//                vec3f n,p[3];
//                loopj(0,3) p[j]=vertices[t.v[j]].p;
//                n.cross(p[1]-p[0],p[2]-p[0]);
//                n.normalize();
//                t.n=n;
//                loopj(0,3) vertices[t.v[j]].q =
//                        vertices[t.v[j]].q+SymetricMatrix(n.x,n.y,n.z,-n.dot(p[0]));
//            }
//            loopi(0,triangles.size())
//            {
//                // Calc Edge Error
//                Triangle &t=triangles[i];vec3f p;
//                loopj(0,3) t.err[j]=calculate_error(t.v[j],t.v[(j+1)%3],p);
//                t.err[3]=min(t.err[0],min(t.err[1],t.err[2]));
//            }
//        }
//
//        // Init Reference ID list
//        loopi(0,vertices.size())
//        {
//            vertices[i].tstart=0;
//            vertices[i].tcount=0;
//        }
//        loopi(0,triangles.size())
//        {
//            Triangle &t=triangles[i];
//            loopj(0,3) vertices[t.v[j]].tcount++;
//        }
//        int tstart=0;
//        loopi(0,vertices.size())
//        {
//            Vertex &v=vertices[i];
//            v.tstart=tstart;
//            tstart+=v.tcount;
//            v.tcount=0;
//        }
//
//        // Write References
//        refs.resize(triangles.size()*3);
//        loopi(0,triangles.size())
//        {
//            Triangle &t=triangles[i];
//            loopj(0,3)
//            {
//                Vertex &v=vertices[t.v[j]];
//                refs[v.tstart+v.tcount].tid=i;
//                refs[v.tstart+v.tcount].tvertex=j;
//                v.tcount++;
//            }
//        }
//
//        // Identify boundary : vertices[].border=0,1
//        if( iteration == 0 )
//        {
//            std::vector<int> vcount,vids;
//
//            loopi(0,vertices.size())
//            vertices[i].border=0;
//
//            loopi(0,vertices.size())
//            {
//                Vertex &v=vertices[i];
//                vcount.clear();
//                vids.clear();
//                loopj(0,v.tcount)
//                {
//                    int k=refs[v.tstart+j].tid;
//                    Triangle &t=triangles[k];
//                    loopk(0,3)
//                    {
//                        int ofs=0,id=t.v[k];
//                        while(ofs<vcount.size())
//                        {
//                            if(vids[ofs]==id)break;
//                            ofs++;
//                        }
//                        if(ofs==vcount.size())
//                        {
//                            vcount.push_back(1);
//                            vids.push_back(id);
//                        }
//                        else
//                            vcount[ofs]++;
//                    }
//                }
//                loopj(0,vcount.size()) if(vcount[j]==1)
//                vertices[vids[j]].border=1;
//            }
//        }
    }
}