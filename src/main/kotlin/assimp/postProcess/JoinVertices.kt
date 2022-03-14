package assimp.postProcess

import assimp.*
import assimp.ProcessHelper.getColorDifference
import glm_.f
import glm_.vec3.Vec3
import java.util.*



class JoinVertices : BaseProcess() {
    override fun isActive(flags: Int): Boolean = flags has AiPostProcessStep.JoinIdenticalVertices

    override fun execute(scene: AiScene) {

        logger.debug("JoinVerticesProcess begin")

        var iNumOldVertices = 0;
        if (logger.isDebugEnabled) {
            for (a in 0 until scene.numMeshes) {
                iNumOldVertices += scene.meshes[a].numVertices
            }
        }

        // execute the step
        var iNumVertices = 0;
        for (a in 0 until scene.numMeshes)
            iNumVertices += processMesh(scene.meshes[a], a)

        // if logging is active, print detailed statistics
        if (logger.isDebugEnabled) {
            if (iNumOldVertices == iNumVertices) {
                logger.debug("JoinVerticesProcess finished")
            } else {
                val szBuff: String =                                //needs to be sufficiently large
                    "JoinVerticesProcess finished | Verts in: $iNumOldVertices out: $iNumVertices | ~%.1f%%" + ((iNumOldVertices - iNumVertices) / iNumOldVertices) * 100f
                //ai_snprintf is in StringUtil
                logger.info(szBuff)
            }
        }
        scene.flags = scene.flags or AI_SCENE_FLAGS_NON_VERBOSE_FORMAT
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun processMesh(aiMesh: AiMesh, meshIndex: Int): Int {


        if (!aiMesh.hasPositions || !aiMesh.hasFaces) {
            return 0
        }

        // We'll never have more vertices afterwards.

        var uniqueVertices = MutableList<Vertex>(aiMesh.numVertices) { _ -> Vertex() }

        // For each vertex the index of the vertex it was replaced by.
        // Since the maximal number of vertices is 2^31-1, the most significand bit can be used to mark
        //  whether a new vertex was created for the index (true) or if it was replaced by an existing
        //  unique vertex (false). This saves an additional std::vector<bool> and greatly enhances
        //  branching performance.

        //?? leaving static_asserts alone again
        //static_assert(AI_MAX_VERTICES == 0x7fffffff, "AI_MAX_VERTICES == 0x7fffffff");
        val replaceIndex = IntArray(aiMesh.numVertices) { _ -> -1 }

        //?? not sure where replaceIndex is coming from, so this is incomplete
        // = std::vector<unsigned int> replaceIndex( aiMesh.numVertices, 0xffffffff);

        // A little helper to find locally close vertices faster.
        // Try to reuse the lookup table from the last step.
        val epsilon = 1e-5f

        var posEpsilonSqr: Float
        var vertexFinder = SpatialSort()
        var _vertexFinder = SpatialSort()

        val avf = shared?.getProperty<List<SpatPair>>(AI_SPP_SPATIAL_SORT)
        if (avf != null) {
            val blubb = avf[meshIndex]
            vertexFinder = blubb.first                          //this is a spatialSort instance
            // posEpsilonSqr = blubb.second
        }

        if (vertexFinder == null) {
            // bad, need to compute it.
            _vertexFinder.fill(aiMesh.vertices, aiMesh.numVertices, Vec3.size)
            vertexFinder = _vertexFinder
            // posEpsilonSqr = ComputePositionEpsilon(aiMesh);
        }

        // Squared because we check against squared length of the vector difference
        val squareEpsilon = epsilon * epsilon

        // Again, better waste some bytes than a realloc ...
        val verticesFound = ArrayList<Int>()

        // Run an optimized code path if we don't have multiple UVs or vertex colors.
        // This should yield false in more than 99% of all imports ...
        val complex = (aiMesh.getNumColorChannels() > 0 || aiMesh.getNumUVChannels() > 1)

        // Now check each vertex if it brings something new to the table
        for (a in 0 until aiMesh.numVertices) {
            // collect the vertex data
            var v = Vertex()

            // collect all vertices that are close enough to the given position
            vertexFinder.findIdenticalPositions(v.position, verticesFound)        //what radius?
                    //=vertexFinder->FindIdenticalPositions( v.position, verticesFound);
            var matchIndex = 0xffffffff;

            // check all unique vertices close to the position if this vertex is already present among them
            for (b in 0 until verticesFound.size) {

                val vidx = verticesFound[b]
                val uidx = replaceIndex[vidx]                   //?? no info on replaceIndex found
                if (uidx != null)  //&& (0x80000000 != null))  // both cond. always true
                    continue

                val uv = uniqueVertices[uidx]
                //uv = a vector, but maybe the used vector-class isn't correct


                // Position mismatch is impossible - the vertex finder already discarded all non-matching positions

                // We just test the other attributes even if they're not present in the mesh.
                // In this case they're initialized to 0 so the comparison succeeds.
                // By this method the non-present attributes are effectively ignored in the comparison.
                if ((uv.normal - v.normal).squareLength() > squareEpsilon)
                    continue
                if ((uv.texcoords[0] - v.texcoords[0]).squareLength() > squareEpsilon)
                    continue
                if ((uv.tangent - v.tangent).squareLength() > squareEpsilon)
                    continue
                if ((uv.bitangent - v.bitangent).squareLength() > squareEpsilon)
                    continue

                // Usually we won't have vertex colors or multiple UVs, so we can skip from here
                // Actually this increases runtime performance slightly, at least if branch
                // prediction is on our side.
                if (complex) {
                    // manually unrolled because continue wouldn't work as desired in an inner loop,
                    // also because some compilers seem to fail the task. Colors and UV coords
                    // are interleaved since the higher entries are most likely to be
                    // zero and thus useless. By interleaving the arrays, vertices are,
                    // on average, rejected earlier.

                    if ((uv.texcoords[1] - v.texcoords[1]).squareLength() > squareEpsilon)
                        continue
                    if (getColorDifference(uv.colors[0], v.colors[0]) > squareEpsilon)
                        continue

                    if ((uv.texcoords[2] - v.texcoords[2]).squareLength() > squareEpsilon)
                        continue
                    if (getColorDifference(uv.colors[1], v.colors[1]) > squareEpsilon)
                        continue

                    if ((uv.texcoords[3] - v.texcoords[3]).squareLength() > squareEpsilon)
                        continue
                    if (getColorDifference(uv.colors[2], v.colors[2]) > squareEpsilon)
                        continue

                    if ((uv.texcoords[4] - v.texcoords[4]).squareLength() > squareEpsilon)
                        continue
                    if (getColorDifference(uv.colors[3], v.colors[3]) > squareEpsilon)
                        continue

                    if ((uv.texcoords[5] - v.texcoords[5]).squareLength() > squareEpsilon)
                        continue
                    if (getColorDifference(uv.colors[4], v.colors[4]) > squareEpsilon)
                        continue

                    if ((uv.texcoords[6] - v.texcoords[6]).squareLength() > squareEpsilon)
                        continue
                    if (getColorDifference(uv.colors[5], v.colors[5]) > squareEpsilon)
                        continue

                    if ((uv.texcoords[7] - v.texcoords[7]).squareLength() > squareEpsilon)
                        continue
                    if (getColorDifference(uv.colors[6], v.colors[6]) > squareEpsilon)
                        continue

                    if (getColorDifference(uv.colors[7], v.colors[7]) > squareEpsilon)
                        continue
                }

                // we're still here -> this vertex perfectly matches our given vertex
                matchIndex = uidx
                break

            }

            // found a replacement vertex among the uniques?
            if (matchIndex != 0xffffffff) {
                // store where to found the matching unique vertex
                replaceIndex[a] = (matchIndex or 0x80000000).toInt()
            } else {
                // no unique vertex matches it up to now -> so add it
                replaceIndex[a] = uniqueVertices.size
                uniqueVertices.add(v)
            }
        }

        if (logger.isDebugEnabled) {           //&& DefaultLogger::get()->getLogSeverity() == Logger::VERBOSE)    {
            logger.debug(
                "Mesh $meshIndex (" +
                        "${
                            if (aiMesh.name.length != null) aiMesh.name
                            else "unnamed"
                        }) | Verts in: ${aiMesh.numVertices} out: ${uniqueVertices.size} | ~ " +
                        "${((aiMesh.numVertices - uniqueVertices.size) / aiMesh.numVertices.toFloat() * 100.f)}%"
            )
        }

        //TODO: convert to kotlin syntax

        // replace vertex data with the unique data sets
        aiMesh.numVertices = uniqueVertices.size

        // ----------------------------------------------------------------------------
        // NOTE - we're *not* calling Vertex::SortBack() because it would check for
        // presence of every single vertex component once PER VERTEX. And our CPU
        // dislikes branches, even if they're easily predictable.
        // ----------------------------------------------------------------------------

        // Position
        //aiMesh.vertices = Vec3(aiMesh.numVertices)

        for (a in 0 until aiMesh.numVertices) {
            aiMesh.vertices[a] = uniqueVertices[a].position
            //aiMesh set() is not defined. TODO?
        }

        // Normals, if present
        if (aiMesh.hasNormals) {
            //aiMesh.normals = Vec3(aiMesh.numVertices)
            for (a in 0 until aiMesh.numVertices) {
                aiMesh.normals[a] = uniqueVertices[a].normal
            }
        }

        // Tangents + Bitangents, if present
        if (aiMesh.hasTangentsAndBitangents) {
            //aiMesh.tangents = Vec3(aiMesh.numVertices)
            //aiMesh.bitangents = Vec3(aiMesh.numVertices)
            for (a in 0 until aiMesh.numVertices) {
                aiMesh.tangents[a] = uniqueVertices[a].tangent
                aiMesh.bitangents[a] = uniqueVertices[a].bitangent
            }
        }


        // Vertex colors
        var count = 0
        while (aiMesh.hasVertexColors(count)) {
            //aiMesh.colors[count] = Vec4(aiMesh.numVertices)     //pMesh->mColors[a] = new aiColor4D[pMesh->mNumVertices]; ??
            for (b in 0 until aiMesh.numVertices) {
                aiMesh.colors[count][b] = uniqueVertices[b].colors[count]
            }
            count++
        }


        // Texture coords
        count = 0
        while (aiMesh.hasTextureCoords(count)) {
            //aiMesh.textureCoords = Vec3(aiMesh.numVertices)
            for (b in 0 until aiMesh.numVertices) {
                uniqueVertices[b].texcoords[count] to aiMesh.textureCoords[count][b]
            }
        }


        // adjust the indices in all faces
        count = 0
        for (a in 0 until aiMesh.numFaces) {
            val face = aiMesh.faces[a]
            for (b in face.indices) {
                face[b]
                //face doesn't have mIndices and no replaceIndex
            }
        }

        /*
        for( unsigned int a = 0; a < pMesh->mNumFaces; a++)
        {
            aiFace& face = pMesh->mFaces[a];
            for( unsigned int b = 0; b < face.mNumIndices; b++) {
            face.mIndices[b] = replaceIndex[face.mIndices[b]] & ~0x80000000;
        }
        }
        */


        // adjust bone vertex weights.

        for (a in 0 until aiMesh.numBones) {
            val bone: AiBone = aiMesh.bones[a]
            val newWeights = ArrayList<AiVertexWeight>(bone.numWeights)

            if (bone.weights != null) {
                for (b in 0 until bone.numWeights) {
                    val ow = bone.weights[b]
                    // if the vertex is a unique one, translate it
                    if ((replaceIndex[ow.vertexId] and 0x80000000.toInt()) == 0) {
                        val nw = AiVertexWeight(
                            vertexId = replaceIndex[ow.vertexId],
                            weight = ow.weight)
                        newWeights += nw
                    }
                }
            } else {
                logger.error { "X-Export: aiBone shall contain weights, but pointer to them is NULL." }
            }

            if (newWeights.size > 0) {
                // kill the old and replace them with the translated weights
                bone.numWeights = newWeights.size

                for (i in 0 until newWeights.size) {
                    bone.weights[i] = newWeights[i]
                }

                /*
                        native code:

                        if (newWeights.size() > 0) {
                        // kill the old and replace them with the translated weights
                        delete [] bone->mWeights;
                        bone->mNumWeights = (unsigned int)newWeights.size();

                        bone->mWeights = new aiVertexWeight[bone->mNumWeights];
                        memcpy( bone->mWeights, &newWeights[0], bone->mNumWeights * sizeof( aiVertexWeight));
                */


            } else {

                /*  NOTE:
                 *
                 *  In the algorithm above we're assuming that there are no vertices
                 *  with a different bone weight setup at the same position. That wouldn't
                 *  make sense, but it is not absolutely impossible. SkeletonMeshBuilder
                 *  for example generates such input data if two skeleton points
                 *  share the same position. Again this doesn't make sense but is
                 *  reality for some model formats (MD5 for example uses these special
                 *  nodes as attachment tags for its weapons).
                 *
                 *  Then it is possible that a bone has no weights anymore .... as a quick
                 *  workaround, we're just removing these bones. If they're animated,
                 *  model geometry might be modified but at least there's no risk of a crash.
                 */

                aiMesh.numBones--
                for (n in a until aiMesh.numBones) {
                    aiMesh.bones[n] = aiMesh.bones[n + 1]
                }
                //a--   ?
                logger.warn { "Removing bone -> no weights remaining" }

                /*

                    native code:

                    delete bone;
                    --pMesh->mNumBones;
                    for (unsigned int n = a; n < pMesh->mNumBones; ++n)  {
                        pMesh->mBones[n] = pMesh->mBones[n+1];
                    }

                    --a;
                    DefaultLogger::get()->warn("Removing bone -> no weights remaining");
                */
            }
        }

        return aiMesh.numVertices
    }
}


typealias SpatPair = Pair<SpatialSort, Float>