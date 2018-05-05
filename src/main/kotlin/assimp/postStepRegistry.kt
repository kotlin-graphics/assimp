/*
---------------------------------------------------------------------------
Open Asset Import Library (assimp)
---------------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team


All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the following
conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
---------------------------------------------------------------------------
*/

package assimp

import assimp.postProcess.*

val postProcessingStepInstanceList: ArrayList<BaseProcess>
    get() = ArrayList<BaseProcess>().apply {

        // ----------------------------------------------------------------------------
        // Add an instance of each post processing step here in the order
        // of sequence it is executed. Steps that are added here are not
        // validated - as RegisterPPStep() does - all dependencies must be given.
        // ----------------------------------------------------------------------------
        if (!ASSIMP.NO.PROCESS.MAKELEFTHANDED)
            add(MakeLeftHandedProcess)

        if (!ASSIMP.NO.PROCESS.FLIPUVS)
            add(FlipUVsProcess)

        if (!ASSIMP.NO.PROCESS.FLIPWINDINGORDER)
            add(FlipWindingOrderProcess)

//        #if (!defined ASSIMP_BUILD_NO_REMOVEVC_PROCESS)
//        out.push_back(new RemoveVCProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_REMOVE_REDUNDANTMATERIALS_PROCESS)
//        out.push_back(new RemoveRedundantMatsProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_EMBEDTEXTURES_PROCESS)
//        out.push_back(new EmbedTexturesProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_FINDINSTANCES_PROCESS)
//        out.push_back(new FindInstancesProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_OPTIMIZEGRAPH_PROCESS)
//        out.push_back(new OptimizeGraphProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_FINDDEGENERATES_PROCESS)
//        out.push_back(new FindDegeneratesProcess ());
//        #endif
//        #ifndef ASSIMP_BUILD_NO_GENUVCOORDS_PROCESS
//            out.push_back(new ComputeUVMappingProcess ());
//        #endif
//        #ifndef ASSIMP_BUILD_NO_TRANSFORMTEXCOORDS_PROCESS
//            out.push_back(new TextureTransformStep ());
//        #endif
        if (!ASSIMP.NO.PROCESS.PRETRANSFORMVERTICES)
            add(PretransformVertices())
        if (!ASSIMP.NO.PROCESS.TRIANGULATE)
            add(TriangulateProcess())

//        #if (!defined ASSIMP_BUILD_NO_SORTBYPTYPE_PROCESS)
//        out.push_back(new SortByPTypeProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_FINDINVALIDDATA_PROCESS)
//        out.push_back(new FindInvalidDataProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_OPTIMIZEMESHES_PROCESS)
//        out.push_back(new OptimizeMeshesProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_FIXINFACINGNORMALS_PROCESS)
//        out.push_back(new FixInfacingNormalsProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_SPLITBYBONECOUNT_PROCESS)
//        out.push_back(new SplitByBoneCountProcess ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_SPLITLARGEMESHES_PROCESS)
//        out.push_back(new SplitLargeMeshesProcess_Triangle ());
//        #endif
//        #if (!defined ASSIMP_BUILD_NO_GENFACENORMALS_PROCESS)
//        out.push_back(new GenFaceNormalsProcess ());
//        #endif
    }
