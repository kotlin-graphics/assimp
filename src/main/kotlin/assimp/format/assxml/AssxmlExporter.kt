package assimp.format.assxml

import assimp.*
import java.text.SimpleDateFormat
import java.util.*


class AssxmlExporter {

    companion object {

        fun ioprintf(io: StringBuilder, str: String) {
            io.append(str)
        }

        val LOCALE = Locale.US

        fun ConvertName(out_: StringBuilder, in_: String) {

            loop@ for (i in 0 until in_.length) {
                when (in_[i]) {
                    '<' -> {
                        out_.append("&lt;");continue@loop
                    }
                    '>' -> {
                        out_.append("&gt;");continue@loop
                    }
                    '&' -> {
                        out_.append("&amp;");continue@loop
                    }
                    '\"' -> {
                        out_.append("&quot;");continue@loop
                    }
                    '\'' -> {
                        out_.append("&apos;");continue@loop
                    }
                    else -> out_.append(in_[i])
                }
            }
        }

        fun WriteNode(node : AiNode, io : StringBuilder, depth : Int) {
            var prefix = Array<Char>(depth, {'\t'}).joinToString("")

            var m = node.transformation

            var name = StringBuilder()
            ConvertName(name,node.name)
            ioprintf(io,"%s<Node name=\"%s\"> \n%s\t<Matrix4> \n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t\t%10.6f %10.6f %10.6f %10.6f\n%s\t</Matrix4> \n".format(
                            prefix,name.toString(),prefix,
                            prefix,m.a0,m.b0,m.c0,m.d0,
                            prefix,m.a1,m.b1,m.c1,m.d1,
                            prefix,m.a2,m.b2,m.c2,m.d2,
                            prefix,m.a3,m.b3,m.c3,m.d3,prefix))

            if (node.numMeshes!=0) {
                ioprintf(io, "%s\t<MeshRefs num=\"%d\">\n%s\t".format(
                        prefix,node.numMeshes,prefix))

                for (i in 0 until node.numMeshes) {
                    ioprintf(io,"%d ".format(node.meshes[i]))
                }
                ioprintf(io,"\n%s\t</MeshRefs>\n".format(prefix))
            }

            if (node.numChildren != 0) {
                ioprintf(io,"%s\t<NodeList num=\"%d\">\n".format(
                        prefix,node.numChildren))

                for (i in 0 until  node.numChildren) {
                    WriteNode(node.children[i],io,depth+2)
                }
                ioprintf(io,"%s\t</NodeList>\n".format(prefix))
            }
            ioprintf(io,"%s</Node>\n".format(prefix))
        }

        fun encodeXML(data : String) : String {
            var buffer = StringBuilder(data.length)
            loop@ for(pos in 0 until data.length) {
                when(data[pos]) {
                    '&'->  {buffer.append("&amp;");              continue@loop;}
                    '\"'-> {buffer.append("&quot;");             continue@loop;}
                    '\''-> {buffer.append("&apos;");             continue@loop;}
                    '<'->  {buffer.append("&lt;");                   continue@loop;}
                    '>'->  {buffer.append("&gt;");                   continue@loop;}
                    else->   {buffer.append(data[pos]);    continue@loop;}
                }
            }
            return buffer.toString()
        }

        fun WriteDump(scene : AiScene, io : StringBuilder, shortened : Boolean) {
            var tt=Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            var p = SimpleDateFormat("HH:mm:ss").format(tt.time)
            assert(p!=null)

            // write header
            var header=(
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<ASSIMP format_id=\"1\">\n\n<!-- XML Model dump produced by assimp dump\n  Library version: %d.%d.%d\n  %s\n-. \n\n<Scene flags=\"%d\" postprocessing=\"%d\">\n"
                    )

            var majorVersion=( versionMajor )
            var minorVersion=( versionMinor )
            var rev=( versionRevision )
            var curtime = (p)
            ioprintf( io, header.format(majorVersion, minorVersion, rev, curtime, scene.flags, 0 ))

            // write the node graph
            WriteNode(scene.rootNode, io, 0)

            //#if 0
            // write cameras
            for (i in 0 until scene.numCameras) {
                var cam  = scene.cameras[i];var name = StringBuilder()
                ConvertName(name,cam.name)

                // camera header
                ioprintf(io,"\t<Camera parent=\"%s\">\n\t\t<Vector3 name=\"up\"        > %0 8f %0 8f %0 8f </Vector3>\n\t\t<Vector3 name=\"lookat\"    > %0 8f %0 8f %0 8f </Vector3>\n\t\t<Vector3 name=\"pos\"       > %0 8f %0 8f %0 8f </Vector3>\n\t\t<Float   name=\"fov\"       > %f </Float>\n\t\t<Float   name=\"aspect\"    > %f </Float>\n\t\t<Float   name=\"near_clip\" > %f </Float>\n\t\t<Float   name=\"far_clip\"  > %f </Float>\n\t</Camera>\n".format(
                                name,
                                cam.up.x,cam.up.y,cam.up.z,
                                cam.lookAt.x,cam.lookAt.y,cam.lookAt.z,
                                cam.position.x,cam.position.y,cam.position.z,
                                cam.horizontalFOV,cam.aspect,cam.clipPlaneNear,cam.clipPlaneFar,i))
            }

            // write lights
            for (i in 0 until scene.numLights) {
                var l  = scene.lights[i]; var name = StringBuilder()
                ConvertName(name,l.name)

                // light header
                ioprintf(io,"\t<Light parent=\"%s\"> type=\"%s\"\n\t\t<Vector3 name=\"diffuse\"   > %0 8f %0 8f %0 8f </Vector3>\n\t\t<Vector3 name=\"specular\"  > %0 8f %0 8f %0 8f </Vector3>\n\t\t<Vector3 name=\"ambient\"   > %0 8f %0 8f %0 8f </Vector3>\n".format(
                                name,
                                ( if (l.type == AiLightSourceType.DIRECTIONAL) "directional" else {
                                    (if (l.type == AiLightSourceType.POINT) "point" else "spot" )}),
                                l.colorDiffuse.r, l.colorDiffuse.g, l.colorDiffuse.b,
                                l.colorSpecular.r,l.colorSpecular.g,l.colorSpecular.b,
                                l.colorAmbient.r, l.colorAmbient.g, l.colorAmbient.b))

                if (l.type != AiLightSourceType.DIRECTIONAL) {
                    ioprintf(io,
                            "\t\t<Vector3 name=\"pos\"       > %0 8f %0 8f %0 8f </Vector3>\n\t\t<Float   name=\"atten_cst\" > %f </Float>\n\t\t<Float   name=\"atten_lin\" > %f </Float>\n\t\t<Float   name=\"atten_sqr\" > %f </Float>\n".format(
                                            l.position.x,l.position.y,l.position.z,
                                            l.attenuationConstant,l.attenuationLinear,l.attenuationQuadratic))
                }

                if (l.type != AiLightSourceType.POINT) {
                    ioprintf(io,
                            "\t\t<Vector3 name=\"lookat\"    > %0 8f %0 8f %0 8f </Vector3>\n".format(
                                    l.direction.x,l.direction.y,l.direction.z))
                }

                if (l.type == AiLightSourceType.SPOT) {
                    ioprintf(io,
                            "\t\t<Float   name=\"cone_out\" > %f </Float>\n\t\t<Float   name=\"cone_inn\" > %f </Float>\n".format(
                                            l.angleOuterCone,l.angleInnerCone))
                }
                ioprintf(io,"\t</Light>\n")
            }
            //#endif


            // write textures
            if (scene.numTextures!=0) {
                TODO()
//                ioprintf(io,"<TextureList num=\"%d\">\n".format(scene.numTextures));
//                for (i in 0 until scene.numTextures) {
//                var tex  = scene.textures.values.toTypedArray()[i];
//                var compressed = (tex.maxLevel .height == 0);
//
//                // mesh header
//                ioprintf(io,"\t<Texture width=\"%d\" height=\"%d\" compressed=\"%s\"> \n",
//                        (compressed ? -1 : tex.width),(compressed ? -1 : tex.height),
//                (compressed ? "true" : "false"));
//
//                if (compressed) {
//                    ioprintf(io,"\t\t<Data length=\"%d\"> \n",tex.width);
//
//                    if (!shortened) {
//                        for (n in 0 until tex.width) {
//                            ioprintf(io,"\t\t\t%2x",reinterpret_cast<uint8_t*>(tex.pcData)[n]);
//                            if (n && !(n % 50)) {
//                                ioprintf(io,"\n");
//                            }
//                        }
//                    }
//                }
//                else if (!shortened){
//                    ioprintf(io,"\t\t<Data length=\"%d\"> \n",tex.width*tex.height*4);
//
//                    // const unsigned int width = (unsigned int)std::log10((double)std::max(tex.height,tex.width))+1;
//                    for (y in 0 until tex.height) {
//                        for (x in 0 until tex.width) {
//                        aiTexel* tx = tex.pcData + y*tex.width+x;
//                        unsigned int r = tx.r,g=tx.g,b=tx.b,a=tx.a;
//                        ioprintf(io,"\t\t\t%2x %2x %2x %2x",r,g,b,a);
//
//                        // group by four for readability
//                        if ( 0 == ( x + y*tex.width ) % 4 ) {
//                        ioprintf( io, "\n" );
//                    }
//                    }
//                    }
//                }
//                ioprintf(io,"\t\t</Data>\n\t</Texture>\n");
//            }
//                ioprintf(io,"</TextureList>\n");
            }

            // write materials
            if (scene.numMaterials!=0) {
                ioprintf(io,"<MaterialList num=\"%d\">\n".format(scene.numMaterials))
                for (i in 0 until scene.numMaterials) {
                    var mat = scene.materials[i]

                    var matproperties = materialToMap(mat)

                    ioprintf(io,"\t<Material>\n")
                    ioprintf(io,"\t\t<MatPropertyList  num=\"%d\">\n".format(matproperties.size + if(mat.textures.size>0) textureToMap(mat.textures[0]).size else 0))
                    for (prop in matproperties) {

//                        var prop = matproperties.asSequence()[n];
                        var sz = ""
                        if (prop.value is Float || prop.value is AiColor3D ) {
                            sz = "float"
                        }
                        else if (prop.value is Int) {
                            sz = "integer"
                        }
                        else if (prop.value is String) {
                            sz = "string"
                        }
                        else if (prop.value is ByteArray || prop.value is AiUVTransform) {
                            sz = "binary_buffer"
                        }

                        ioprintf(io,"\t\t\t<MatProperty key=\"%s\" \n\t\t\ttype=\"%s\" tex_usage=\"%s\" tex_index=\"%d\"".format(
                                prop.key, sz,
                                TextureTypeToString(AiTexture.Type.none),i))

                        if (prop.value is Float) {
                            ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                    1))

                            for (p in 0 until 1) {
                                ioprintf(io,"%f ".format(prop.value))
                            }
                        }
                        else if(prop.value is AiColor3D) {
                            ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                    3))
                            val c = prop.value as AiColor3D
                            for (p in 0 until 3) {
                                ioprintf(io,"%f ".format(c.x, c.y, c.z))
                            }
                        }
                        else if (prop.value is Int) {
                            ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                    1))

                            for (p in 0 until 1) {
                                ioprintf(io,"%d ".format(p))
                            }
                        }
                        else if (prop.value is ByteArray) {
                            val v = prop.value as ByteArray
                            ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                    v.size));

                            for (p in 0 until v.size) {
                                ioprintf(io,"%2x ".format(v[p]));
                                if (p!=0 && 0 == p%30) {
                                    ioprintf(io,"\n\t\t\t\t");
                                }
                            }
                        } else if (prop.value is AiUVTransform) {
                            val v = prop.value as AiUVTransform
                            ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                    5));

                            for (p in 0 until 5) {
                                ioprintf(io,"%2x ".format(v.translation.x, v.translation.y, v.scaling.x, v.scaling.y, v.rotation));
                                if (p!=0 && 0 == p%30) {
                                    ioprintf(io,"\n\t\t\t\t");
                                }
                            }
                        }
                        else if (prop.value is String) {
                            ioprintf(io,">\n\t\t\t\t\"%s\"".format(encodeXML(prop.value as String) /* skip length */))
                        }
                        ioprintf(io,"\n\t\t\t</MatProperty>\n")
                    }
                    if(mat.textures.size>1) TODO()
                    for(j in 0 until mat.textures.size) {
                        var tex = mat.textures[j]
                        var texproperties = textureToMap(tex)
                        for (prop in texproperties) {

//                        var prop = matproperties.asSequence()[n];
                            var sz = ""
                            if (prop.value is Float || prop.value is AiColor3D ) {
                                sz = "float"
                            }
                            else if (prop.value is Int) {
                                sz = "integer"
                            }
                            else if (prop.value is String) {
                                sz = "string"
                            }
                            else if (prop.value is ByteArray || prop.value is AiUVTransform) {
                                sz = "binary_buffer"
                            }

                            ioprintf(io,"\t\t\t<MatProperty key=\"%s\" \n\t\t\ttype=\"%s\" tex_usage=\"%s\" tex_index=\"%d\"".format(
                                    prop.key, sz,
                                    TextureTypeToString(AiTexture.Type.none),i))

                            if (prop.value is Float) {
                                ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                        1))

                                for (p in 0 until 1) {
                                    ioprintf(io,"%f ".format(prop.value))
                                }
                            }
                            else if(prop.value is AiColor3D) {
                                ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                        3))
                                val c = prop.value as AiColor3D
                                for (p in 0 until 3) {
                                    ioprintf(io,"%f ".format(c.x, c.y, c.z))
                                }
                            }
                            else if (prop.value is Int) {
                                ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                        1))

                                for (p in 0 until 1) {
                                    ioprintf(io,"%d ".format(p))
                                }
                            }
                            else if (prop.value is ByteArray) {
                                val v = prop.value as ByteArray
                                ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                        v.size));

                                for (p in 0 until v.size) {
                                    ioprintf(io,"%2x ".format(v[p]));
                                    if (p!=0 && 0 == p%30) {
                                        ioprintf(io,"\n\t\t\t\t");
                                    }
                                }
                            } else if (prop.value is AiUVTransform) {
                                val v = prop.value as AiUVTransform
                                ioprintf(io," size=\"%d\">\n\t\t\t\t".format(
                                        5));

                                for (p in 0 until 5) {
                                    ioprintf(io,"%2x ".format(v.translation.x, v.translation.y, v.scaling.x, v.scaling.y, v.rotation));
                                    if (p!=0 && 0 == p%30) {
                                        ioprintf(io,"\n\t\t\t\t");
                                    }
                                }
                            }
                            else if (prop.value is String) {
                                ioprintf(io,">\n\t\t\t\t\"%s\"".format(encodeXML(prop.value as String) /* skip length */))
                            }
                            ioprintf(io,"\n\t\t\t</MatProperty>\n")
                        }
                    }

                    ioprintf(io,"\t\t</MatPropertyList>\n")
                    ioprintf(io,"\t</Material>\n")
                }
                ioprintf(io,"</MaterialList>\n")
            }

            // write animations
            if (scene.numAnimations!=0) {
                ioprintf(io,"<AnimationList num=\"%d\">\n".format(scene.numAnimations))
                for (i in 0 until scene.numAnimations) {
                    var anim = scene.animations[i]
                    var name = StringBuilder()
                    // anim header
                    ConvertName(name,anim.name)
                    ioprintf(io,"\t<Animation name=\"%s\" duration=\"%e\" tick_cnt=\"%e\">\n".format(
                            name, anim.duration, anim.ticksPerSecond))

                    // write bone animation channels
                    if (anim.numChannels!=0) {
                        ioprintf(io,"\t\t<NodeAnilist num=\"%d\">\n".format(anim.numChannels))
                        for (n in 0 until anim.numChannels) {
                            var nd = anim.channels[n]
                            if(nd==null) continue
                            name = StringBuilder()
                            // node anim header
                            ConvertName(name,nd.nodeName)
                            ioprintf(io,"\t\t\t<NodeAnim node=\"%s\">\n".format(name))

                            if (!shortened) {
                                // write position keys
                                if (nd.numPositionKeys!=0) {
                                    ioprintf(io,"\t\t\t\t<PositionKeyList num=\"%d\">\n".format(nd.numPositionKeys))
                                    for (a in 0 until nd.numPositionKeys) {
                                        var vc = nd.positionKeys[a]
                                        ioprintf(io,"\t\t\t\t\t<PositionKey time=\"%e\">\n\t\t\t\t\t\t%0 8f %0 8f %0 8f\n\t\t\t\t\t</PositionKey>\n".format(
                                        vc.time,vc.value.x,vc.value.y,vc.value.z))
                                    }
                                    ioprintf(io,"\t\t\t\t</PositionKeyList>\n")
                                }

                                // write scaling keys
                                if (nd.numScalingKeys!=0) {
                                    ioprintf(io,"\t\t\t\t<ScalingKeyList num=\"%d\">\n".format(nd.numScalingKeys))
                                    for (a in 0 until nd.numScalingKeys) {
                                        var vc = nd.scalingKeys[a]
                                        ioprintf(io,"\t\t\t\t\t<ScalingKey time=\"%e\">\n\t\t\t\t\t\t%0 8f %0 8f %0 8f\n\t\t\t\t\t</ScalingKey>\n".format(
                                        vc.time,vc.value.x,vc.value.y,vc.value.z))
                                    }
                                    ioprintf(io,"\t\t\t\t</ScalingKeyList>\n")
                                }

                                // write rotation keys
                                if (nd.numRotationKeys!=0) {
                                    ioprintf(io,"\t\t\t\t<RotationKeyList num=\"%d\">\n".format(nd.numRotationKeys))
                                    for (a in 0 until nd.numRotationKeys) {
                                        var vc = nd.rotationKeys[a]
                                        ioprintf(io,"\t\t\t\t\t<RotationKey time=\"%e\">\n\t\t\t\t\t\t%0 8f %0 8f %0 8f %0 8f\n\t\t\t\t\t</RotationKey>\n".format(
                                        vc.time,vc.value.x,vc.value.y,vc.value.z,vc.value.w))
                                    }
                                    ioprintf(io,"\t\t\t\t</RotationKeyList>\n")
                                }
                            }
                            ioprintf(io,"\t\t\t</NodeAnim>\n")
                        }
                        ioprintf(io,"\t\t</NodeAnimList>\n")
                    }
                    ioprintf(io,"\t</Animation>\n")
                }
                ioprintf(io,"</AnimationList>\n")
            }

            // write meshes
            if (scene.numMeshes!=0) {
                ioprintf(io,"<MeshList num=\"%d\">\n".format(scene.numMeshes))
                for (i in 0 until scene.numMeshes) {
                    var mesh = scene.meshes[i]
                    // const unsigned int width = (unsigned int)std::log10((double)mesh.numVertices)+1;

                    // mesh header
                    ioprintf(io,"\t<Mesh types=\"%s %s %s %s\" material_index=\"%d\">\n".format(
                            (if (mesh.primitiveTypes.and(AiPrimitiveType.POINT.i) != 0) "points" else ""),
                    (if(mesh.primitiveTypes.and(AiPrimitiveType.LINE.i) != 0) "lines" else ""),
                    (if(mesh.primitiveTypes.and(AiPrimitiveType.TRIANGLE.i) != 0) "triangles" else ""),
                    (if(mesh.primitiveTypes.and(AiPrimitiveType.POLYGON.i) != 0) "polygons" else ""),
                    mesh.materialIndex))

                    // bones
                    if (mesh.numBones!=0) {
                        ioprintf(io,"\t\t<BoneList num=\"%d\">\n".format(mesh.numBones))

                        for (n in 0 until mesh.numBones) {
                            var bone = mesh.bones[n]
                            var name = StringBuilder()
                            ConvertName(name,bone.name)
                            // bone header
                            ioprintf(io,"\t\t\t<Bone name=\"%s\">\n\t\t\t\t<Matrix4> \n\t\t\t\t\t%0 6f %0 6f %0 6f %0 6f\n\t\t\t\t\t%0 6f %0 6f %0 6f %0 6f\n\t\t\t\t\t%0 6f %0 6f %0 6f %0 6f\n\t\t\t\t\t%0 6f %0 6f %0 6f %0 6f\n\t\t\t\t</Matrix4> \n".format(
                            name.toString(),
                            bone.offsetMatrix.a0,bone.offsetMatrix.b0,bone.offsetMatrix.c0,bone.offsetMatrix.d0,
                            bone.offsetMatrix.a1,bone.offsetMatrix.b1,bone.offsetMatrix.c1,bone.offsetMatrix.d1,
                            bone.offsetMatrix.a2,bone.offsetMatrix.b2,bone.offsetMatrix.c2,bone.offsetMatrix.d2,
                            bone.offsetMatrix.a3,bone.offsetMatrix.b3,bone.offsetMatrix.c3,bone.offsetMatrix.d3))

                            if (!shortened && bone.numWeights!=0) {
                                ioprintf(io,"\t\t\t\t<WeightList num=\"%d\">\n".format(bone.numWeights))

                                // bone weights
                                for (a in 0 until bone.numWeights) {
                                    var wght = bone.weights[a]

                                    ioprintf(io,"\t\t\t\t\t<Weight index=\"%d\">\n\t\t\t\t\t\t%f\n\t\t\t\t\t</Weight>\n".format(
                                            wght.vertexId,wght.weight))
                                }
                                ioprintf(io,"\t\t\t\t</WeightList>\n")
                            }
                            ioprintf(io,"\t\t\t</Bone>\n")
                        }
                        ioprintf(io,"\t\t</BoneList>\n")
                    }

                    // faces
                    if (!shortened && mesh.numFaces!=0) {
                        ioprintf(io,"\t\t<FaceList num=\"%d\">\n".format(mesh.numFaces))
                        for (n in 0 until mesh.numFaces) {
                            var f = mesh.faces[n]
                            ioprintf(io,"\t\t\t<Face num=\"%d\">\n\t\t\t\t".format(f.size))

                            for (j in 0 until f.size) //f.numIndices=3
                                ioprintf(io,"%d ".format(f[j]))

                            ioprintf(io,"\n\t\t\t</Face>\n")
                        }
                        ioprintf(io,"\t\t</FaceList>\n")
                    }

                    // vertex positions
                    if (mesh.hasPositions) {
                        ioprintf(io,"\t\t<Positions num=\"%d\" set=\"0\" num_components=\"3\"> \n".format(mesh.numVertices))
                        if (!shortened) {
                            for (n in 0 until mesh.numVertices) {
                                ioprintf(io,"\t\t%0 8f %0 8f %0 8f\n".format(
                                        mesh.vertices[n].x,
                                        mesh.vertices[n].y,
                                        mesh.vertices[n].z))
                            }
                        }
                        ioprintf(io,"\t\t</Positions>\n")
                    }

                    // vertex normals
                    if (mesh.hasNormals) {
                        ioprintf(io,"\t\t<Normals num=\"%d\" set=\"0\" num_components=\"3\"> \n".format(mesh.numVertices))
                        if (!shortened) {
                            for (n in 0 until mesh.numVertices) {
                                ioprintf(io,"\t\t%0 8f %0 8f %0 8f\n".format(
                                        mesh.normals[n].x,
                                        mesh.normals[n].y,
                                        mesh.normals[n].z))
                            }
                        }
                        else {
                        }
                        ioprintf(io,"\t\t</Normals>\n")
                    }

                    // vertex tangents and bitangents
                    if (mesh.hasTangentsAndBitangents) {
                        ioprintf(io,"\t\t<Tangents num=\"%d\" set=\"0\" num_components=\"3\"> \n".format(mesh.numVertices))
                        if (!shortened) {
                            for (n in 0 until mesh.numVertices) {
                                ioprintf(io,"\t\t%0 8f %0 8f %0 8f\n".format(
                                        mesh.tangents[n].x,
                                        mesh.tangents[n].y,
                                        mesh.tangents[n].z))
                            }
                        }
                        ioprintf(io,"\t\t</Tangents>\n")

                        ioprintf(io,"\t\t<Bitangents num=\"%d\" set=\"0\" num_components=\"3\"> \n".format(mesh.numVertices))
                        if (!shortened) {
                            for (n in 0 until mesh.numVertices) {
                                ioprintf(io,"\t\t%0 8f %0 8f %0 8f\n".format(
                                        mesh.bitangents[n].x,
                                        mesh.bitangents[n].y,
                                        mesh.bitangents[n].z))
                            }
                        }
                        ioprintf(io,"\t\t</Bitangents>\n")
                    }

                    // texture coordinates
                    for (a in 0 until AI_MAX_NUMBER_OF_TEXTURECOORDS) {
                        if (a >= mesh.textureCoords.size || mesh.textureCoords[a].isEmpty())
                            break

                        ioprintf(io,"\t\t<TextureCoords num=\"%d\" set=\"%d\" num_components=\"%d\"> \n".format(mesh.numVertices,
                                a,mesh.textureCoords[a][0].size))

                        if (!shortened) {
                            if (mesh.textureCoords[a][0].size == 3) {
                                for (n in 0 until mesh.numVertices) {
                                    ioprintf(io,"\t\t%0 8f %0 8f %0 8f\n".format(
                                            mesh.textureCoords[a][n][0],
                                            mesh.textureCoords[a][n][1],
                                            mesh.textureCoords[a][n][2]))
                                }
                            }
                            else {
                                for (n in 0 until mesh.numVertices) {
                                    ioprintf(io,"\t\t%0 8f %0 8f\n".format(
                                            mesh.textureCoords[a][n][0],
                                            mesh.textureCoords[a][n][1]))
                                }
                            }
                        }
                        ioprintf(io,"\t\t</TextureCoords>\n")
                    }

                    // vertex colors
                    for (a in 0 until AI_MAX_NUMBER_OF_COLOR_SETS) {
                        if (a >= mesh.colors.size || mesh.colors[a].isEmpty())
                            break
                        ioprintf(io,"\t\t<Colors num=\"%d\" set=\"%d\" num_components=\"4\"> \n".format(mesh.numVertices,a))
                        if (!shortened) {
                            for (n in 0 until mesh.numVertices) {
                                ioprintf(io,"\t\t%0 8f %0 8f %0 8f %0 8f\n".format(
                                        mesh.colors[a][n].r,
                                        mesh.colors[a][n].g,
                                        mesh.colors[a][n].b,
                                        mesh.colors[a][n].a))
                            }
                        }
                        ioprintf(io,"\t\t</Colors>\n")
                    }
                    ioprintf(io,"\t</Mesh>\n")
                }
                ioprintf(io,"</MeshList>\n")
            }
            ioprintf(io,"</Scene>\n</ASSIMP>")
        }
    }

    fun ExportSceneAssxml(out : StringBuilder, pScene : AiScene /*, const ExportProperties* pProperties*/)
    {
        var loc = Locale.getDefault()
        Locale.setDefault(Locale.US)
        var shortened = false;
        WriteDump( pScene, out, shortened );
        Locale.setDefault(loc)
    }

}