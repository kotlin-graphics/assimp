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

package assimp.format

import assimp.AiMatrix4x4

/** ###########################################################################
 *  LIBRARY SETTINGS
 *  General, global settings
 *  ########################################################################### */
object AiConfig {

    /** @brief Enables time measurements.
     *
     *  If enabled, measures the time needed for each part of the loading process (i.e. IO time, importing,
     *  postprocessing, ..) and dumps these timings to the DefaultLogger. See the @link perf Performance
     *  Page@endlink for more information on this topic.
     *
     * Property type: bool. Default value: false.
     */
    var GLOB_MEASURE_TIME = false

    /** ###########################################################################
     *  POST PROCESSING SETTINGS
     *  Various stuff to fine-tune the behavior of a specific post processing step.
     *  ########################################################################### */
    object pp {

        /** @brief Maximum bone count per mesh for the SplitbyBoneCount step.
         *
         * Meshes are split until the maximum number of bones is reached. The default
         * value is AI_SBBC_DEFAULT_MAX_BONES, which may be altered at
         * compile-time.
         * Property data type: integer.
         */
        var SBBC_MAX_BONES = 60

        /** @brief  Specifies the maximum angle that may be between two vertex tangents that their tangents and
         *  bi-tangents are smoothed.
         *
         *  This applies to the CalcTangentSpace-Step. The angle is specified in degrees. The maximum value is 175.
         *  Property type: float. Default value: 45 degrees
         */
        var CT_MAX_SMOOTHING_ANGLE = 45

        /** @brief Source UV channel for tangent space computation.
         *
         *  The specified channel must exist or an error will be raised.
         *  Property type: integer. Default value: 0
         */
        var CT_TEXTURE_CHANNEL_INDEX = 0

        /** @brief  Specifies the maximum angle that may be between two face normals at the same vertex position
         *  that their are smoothed together.
         *
         *  Sometimes referred to as 'crease angle'.
         *  This applies to the GenSmoothNormals-Step. The angle is specified in degrees, so 180 is PI.
         *  The default value is 175 degrees (all vertex normals are smoothed). The maximum value is 175, too.
         *  Property type: float.
         *  Warning: setting this option may cause a severe loss of performance. The performance is unaffected if
         *  the AI_CONFIG_FAVOUR_SPEED flag is set but the output quality may be reduced.
         */
        var GSN_MAX_SMOOTHING_ANGLE = 175f

        /** @brief Configures the #aiProcess_RemoveRedundantMaterials step to keep materials matching a name in
         *  a given list.
         *
         *  This is a list of 1 to n strings, ' ' serves as delimiter character.
         *  Identifiers containing whitespaces must be enclosed in *single* quotation marks. For example:<tt>
         * "keep-me and_me_to anotherMaterialToBeKept \'name with whitespace\'"</tt>.
         *  If a material matches on of these names, it will not be modified or removed by the postprocessing step
         *  nor will other materials be replaced by a reference to it. <br>
         *  This option might be useful if you are using some magic material names to pass additional semantics through
         *  the content pipeline. This ensures they won't be optimized away, but a general optimization is still
         *  performed for materials not contained in the list.
         *  Property type: String. Default value: n/a
         *  @note Linefeeds, tabs or carriage returns are treated as whitespace.
         *  Material names are case sensitive.
         */
        var RRM_EXCLUDE_LIST = ""

        /** @brief Configures the AiProcess_PreTransformVertices step to keep the scene hierarchy. Meshes are moved
         *  to worldspace, but no optimization is performed (read: meshes with equal materials are not joined.
         *  The total number of meshes won't change).
         *
         *  This option could be of use for you if the scene hierarchy contains important additional information which
         *  you intend to parse.
         *  For rendering, you can still render all meshes in the scene without any transformations.
         * Property type: bool. Default value: false.
         */
        var PTV_KEEP_HIERARCHY = false

        /** @brief Configures the AiProcess_PreTransformVertices step to normalize all vertex components into
         *  the [-1, 1] range. That is, a bounding box for the whole scene is computed, the maximum component is taken
         *  and all meshes are scaled appropriately (uniformly of course!).
         *  This might be useful if you don't know the spatial dimension of the input data  */
        var PTV_NORMALIZE = false

        /** @brief Configures the #aiProcess_PreTransformVertices step to use a users defined matrix as the scene root
         *  node transformation before transforming vertices.
         *  Property type: bool. Default value: false.
         */
        val PTV_ADD_ROOT_TRANSFORMATION = false

        /** @brief Configures the AiProcess_PreTransformVertices step to use a users defined matrix as the scene root
         *  node transformation before transforming vertices. This property correspond to the 'a1' component
         *  of the transformation matrix.
         *  Property type: aiMatrix4x4.
         */
        val PTV_ROOT_TRANSFORMATION = AiMatrix4x4()

        /** @brief Configures the AiProcess_FindDegenerates step to remove degenerated primitives from
         *  the import - immediately.
         *
         *  The default behaviour converts degenerated triangles to lines and degenerated lines to points.
         *  See the documentation to the AiProcess_FindDegenerates step for a detailed example of the various ways
         *  to get rid of these lines and points if you don't want them.
         *  Property type: bool. Default value: false.
         */
        var FD_REMOVE = false

        /** @brief Configures the #aiProcess_OptimizeGraph step to preserve nodes matching a name in a given list.
         *
         *  This is a list of 1 to n strings, ' ' serves as delimiter character.
         *  Identifiers containing whitespaces must be enclosed in *single* quotation marks. For example:<tt>
         * "keep-me and_me_to anotherNodeToBeKept \'name with whitespace\'"</tt>.
         *  If a node matches on of these names, it will not be modified or removed by the postprocessing step.<br>
         *  This option might be useful if you are using some magic node names to pass additional semantics through
         *  the content pipeline. This ensures they won't be optimized away, but a general optimization is still
         *  performed for nodes not contained in the list.
         *  Property type: String. Default value: n/a
         *  @note Linefeeds, tabs or carriage returns are treated as whitespace. Node names are case sensitive.
         */
        var OG_EXCLUDE_LIST = ""

        /** @brief  Set the maximum number of triangles in a mesh.
         *
         *  This is used by the "SplitLargeMeshes" PostProcess-Step to determine whether a mesh must be split or not.
         *  @note The default value is 1_000_000. Property type: integer.
         */
        var SLM_TRIANGLE_LIMIT = 1_000_000

        /** @brief  Set the maximum number of vertices in a mesh.
         *
         *  This is used by the "SplitLargeMeshes" PostProcess-Step to determine whether a mesh must be split or not.
         *  @note The default value is 1_000_000. Property type: integer.
         */
        var SLM_VERTEX_LIMIT = 1_000_000

        /** @brief Set the maximum number of bones affecting a single vertex
         *
         *  This is used by the #aiProcess_LimitBoneWeights PostProcess-Step.
         *  @note The default value is 0x4
         * Property type: integer.*/
        var LBW_MAX_WEIGHTS = 0x4

        /** @brief Lower the deboning threshold in order to remove more bones.
         *
         *  This is used by the #aiProcess_Debone PostProcess-Step.
         *  @note The default value is 1f
         *  Property type: float.*/
        var DB_THRESHOLD = 1f

        /** @brief Require all bones qualify for deboning before removing any
         *
         *  This is used by the #aiProcess_Debone PostProcess-Step.
         *  @note The default value is 0
         *  Property type: bool.*/
        var DB_ALL_OR_NONE = false

        /** @brief Set the size of the post-transform vertex cache to optimize the vertices for.
         *  This configures the AiProcess_ImproveCacheLocality step.
         *
         *  The size is given in vertices. Of course you can't know how the vertex format will exactly look like after
         *  the import returns, but you can still guess what your meshes will probably have.
         *  @note The default value is 12. That results in slight performance improvements for most nVidia/AMD cards
         *  since 2002.
         *  Property type: integer.
         */
        var ICL_PTCACHE_SIZE = 12

        /** @brief Enumerates components of the aiScene and aiMesh data structures that can be excluded from the import
         *  using the AiProcess_RemoveComponent step.
         *
         *  See the documentation to AiProcess_RemoveComponent for more details.
         */
        enum class AiComponent(val i: Int) {
            /** Normal vectors */
            NORMALS(0x2),
            /** Tangents and bitangents go always together ... */
            TANGENTS_AND_BITANGENTS(0x4),
            /** ALL color sets
             * Use aiComponent_COLORn(N) to specify the N'th set */
            COLORS(0x8),
            /** ALL texture UV sets
             * aiComponent_TEXCOORDn(N) to specify the N'th set  */
            TEXCOORDS(0x10),
            /** Removes all bone weights from all meshes.
             *  The scenegraph nodes corresponding to the bones are NOT removed.
             *  use the AiProcess_OptimizeGraph step to do this */
            BONEWEIGHTS(0x20),
            /** Removes all node animations (aiScene::mAnimations).
             *  The corresponding scenegraph nodes are NOT removed.
             *  use the AiProcess_OptimizeGraph step to do this */
            ANIMATIONS(0x40),
            /** Removes all embedded textures (aiScene::mTextures) */
            TEXTURES(0x80),
            /** Removes all light sources (aiScene::mLights).
             *  The corresponding scenegraph nodes are NOT removed.
             *  use the #aiProcess_OptimizeGraph step to do this */
            LIGHTS(0x100),
            /** Removes all cameras (aiScene::mCameras).
             *  The corresponding scenegraph nodes are NOT removed.
             *  use the #aiProcess_OptimizeGraph step to do this */
            CAMERAS(0x200),
            /** Removes all meshes (aiScene::mMeshes). */
            MESHES(0x400),
            /** Removes all materials. One default material will be generated, so aiScene::mNumMaterials will be 1. */
            MATERIALS(0x800);

            companion object {
                /** Remove a specific color channel 'n' */
                fun COLORSn(n: Int) = 1 shl (n + 20)

                /** Remove a specific UV channel 'n'    */
                fun TEXCOORDSn(n: Int) = 1 shl (n + 25)
            }
        }

        /** @brief Input parameter to the #aiProcess_RemoveComponent step:
         *  Specifies the parts of the data structure to be removed.
         *
         *  See the documentation to this step for further details. The property is expected to be an integer,
         *  a bitwise combination of the AiComponent flags defined above in this header. The default value is 0.
         *  Important: if no valid mesh is remaining after the step has been executed (e.g you thought it was funny
         *  to specify ALL of the flags defined above) the import FAILS. Mainly because there is no data to work
         *  on anymore ...
         */
        var RVC_FLAGS = 0

        /** @brief Input parameter to the #aiProcess_SortByPType step:
         *  Specifies which primitive types are removed by the step.
         *
         *  This is a bitwise combination of the aiPrimitiveType flags.
         *  Specifying all of them is illegal, of course. A typical use would be to exclude all line and point meshes
         *  from the import. This is an integer property, its default value is 0.
         */
        var SBP_REMOVE = 0

        /** @brief Input parameter to the AiProcess_FindInvalidData step:
         *  Specifies the floating-point accuracy for animation values. The step checks for animation tracks where all
         *  frame values are absolutely equal and removes them. This tweakable controls the epsilon for floating-point
         *  comparisons - two keys are considered equal if the invariant abs(n0 - n1) > epsilon holds true for
         *  all vector respectively quaternion components. The default value is 0.f - comparisons are exact then.
         */
        var FID_ANIM_ACCURACY = 0f

        object UvTrafo {
            /** TransformUVCoords evaluates UV scalings */
            val SCALING = 0x1
            /** TransformUVCoords evaluates UV rotations */
            val ROTATION = 0x2
            /** TransformUVCoords evaluates UV translation */
            val TRANSLATION = 0x4
            /** Everything baked together -> default value */
            val ALL = SCALING or ROTATION or TRANSLATION
        }

        /** @brief Input parameter to the AiProcess_TransformUVCoords step:
         *  Specifies which UV transformations are evaluated.
         *
         *  This is a bitwise combination of the AI_UVTR property, of course).
         *  By default all transformations are enabled (AI_UVTRAFO.ALL).
         */
        var TUV_EVALUATE = UvTrafo.ALL
    }

    /** @brief A hint to assimp to favour speed against import quality.
     *
     *  Enabling this option may result in faster loading, but it needn't.
     *  It represents just a hint to loaders and post-processing steps to use faster code paths, if possible.
     *  This property is expected to be an integer, != 0 stands for true. The default value is 0.
     */
    var FAVOUR_SPEED = false

    /** ###########################################################################
     *  IMPORTER SETTINGS
     *  Various stuff to fine-tune the behaviour of specific importer plugins.
     *  ########################################################################### */
    object Import {

        /** @brief Global setting to disable generation of skeleton dummy meshes
         *
         *  Skeleton dummy meshes are generated as a visualization aid in cases which the input data contains no geometry,
         *  but only animation data.
         *  Property data type: bool. Default value: false
         */
        var NO_SKELETON_MESHES = false

        /** @brief Sets the colormap (= palette) to be used to decode embedded textures in MDL (Quake or 3DGS) files.
         *
         *  This must be a valid path to a file. The file is 768 (256*3) bytes large and contains RGB triplets
         *  for each of the 256 palette entries.
         *  The default value is colormap.lmp. If the file is not found, a default palette (from Quake 1) is used.
         *  Property type: string.
         */
        var MDL_COLORMAP = "colormap.lmp"

        object Fbx {

            object Read {
                /** @brief Set whether the fbx importer will merge all geometry layers present in the source file or
                 *  take only the first.
                 *
                 * The default value is true (1). Property type: bool
                 */
                var ALL_GEOMETRY_LAYERS = true

                /** @brief Set whether the fbx importer will read all materials present in the source file or take only
                 *  the referenced materials.
                 *
                 *  This is void unless IMPORT.FBX_READ_MATERIALS = true
                 *
                 *  The default value is false (0). Property type: bool
                 */
                var ALL_MATERIALS = false

                /** @brief Set whether the fbx importer will read materials.
                 *
                 * The default value is true (1). Property type: bool
                 */
                var MATERIALS = true

                /** @brief Set whether the fbx importer will read embedded textures.
                 *
                 *  The default value is true (1). Property type: bool
                 */
                var TEXTURES = true

                /** @brief Set whether the fbx importer will read cameras.
                 *
                 *  The default value is true (1). Property type: bool
                 */
                var CAMERAS = true

                /** @brief Set whether the fbx importer will read light sources.
                 *
                 *  The default value is true (1). Property type: bool
                 */
                var LIGHTS = true

                /** @brief Set whether the fbx importer will read animations.
                 *
                 *  The default value is true (1). Property type: bool
                 */
                var ANIMATIONS = true
            }

            /** @brief Set whether the fbx importer will act in strict mode in which only FBX 2013 is supported and
             *  any other sub formats are rejected. FBX 2013 is the primary target for the importer, so this format
             *  is best supported and well-tested.
             *
             *  The default value is false (0). Property type: bool
             */
            var STRICT_MODE = false

            /** @brief Set whether the fbx importer will preserve pivot points for transformations (as extra nodes).
             *  If set to false, pivots and offsets will be evaluated whenever possible.
             *
             *  The default value is true (1). Property type: bool
             */
            var PRESERVE_PIVOTS = true

            /** @brief Specifies whether the importer will drop empty animation curves or animation curves which match
             *  the bind pose transformation over their entire defined range.
             *
             *  The default value is true (1). Property type: bool
             */
            var OPTIMIZE_EMPTY_ANIMATION_CURVES = true

            /** @brief Set whether the fbx importer will search for embedded loaded textures, where no embedded texture
             *  data is provided.
             *
             *  The default value is false (0). Property type: bool
             */
            var SEARCH_EMBEDDED_TEXTURES = false
        }

        /** @brief  Set the vertex animation keyframe to be imported
         *
         *  ASSIMP does not support vertex keyframes (only bone animation is supported).
         *  The library reads only one frame of models with vertex animations.
         *  By default this is the first frame.
         *  \note The default value is 0. This option applies to all importers.
         *  However, it is also possible to override the global setting for a specific loader. You can use the
         *  AI_CONFIG_IMPORT_*_KEYFRAME options (where * is a placeholder for the file format for which you
         *  want to override the global setting).
         *  Property type: integer.
         */
        var GLOBAL_KEYFRAME = 0

        var MD3_KEYFRAME = -1
        var MD2_KEYFRAME = -1
        var MDL_KEYFRAME = -1
        var MDC_KEYFRAME = -1
        var SMD_KEYFRAME = -1
        var UNREAL_KEYFRAME = -1

        /** @brief  Configures the AC loader to collect all surfaces which have the "Backface cull" flag set
         *  in separate meshes.
         *
         *  Property type: bool. Default value: true.
         */
        var AC_SEPARATE_BFCULL = true

        /** @brief  Configures whether the AC loader evaluates subdivision surfaces ( indicated by the presence
         *  of the 'subdiv' attribute in the file). By default, Assimp performs the subdivision using the standard
         *  Catmull-Clark algorithm
         *
         * Property type: bool. Default value: true.
         */
        var AC_EVAL_SUBDIVISION = true

        /** @brief  Configures the UNREAL 3D loader to separate faces with different surface flags
         *  (e.g. two-sided vs. single-sided).
         *
         * Property type: bool. Default value: true.
         */
        var UNREAL_HANDLE_FLAGS = true

        /** @brief Configures the terragen import plugin to compute uv's for terrains, if not given.
         *  Furthermore a default texture is assigned.
         *
         *  UV coordinates for terrains are so simple to compute that you'll usually want to compute them on your own,
         *  if you need them. This option is intended for model viewers which want to offer an easy way to apply
         *  textures to terrains.
         *  Property type: bool. Default value: false.
         */
        var TER_MAKE_UVS = false

        /** @brief  Configures the ASE loader to always reconstruct normal vectors basing on the smoothing groups loaded
         *  from the file.
         *
         *  Some ASE files have carry invalid normals, other don't.
         *  Property type: bool. Default value: true.
         */
        var ASE_RECONSTRUCT_NORMALS = true

        /** @brief  Configures the M3D loader to detect and process multi-part Quake player models.
         *
         *  These models usually consist of 3 files, lower.md3, upper.md3 and head.md3. If this property is set to true,
         *  Assimp will try to load and combine all three files if one of them is loaded.
         *  Property type: bool. Default value: true.
         */
        var MD3_HANDLE_MULTIPART = true

        /** @brief  Tells the MD3 loader which skin files to load.
         *
         *  When loading MD3 files, Assimp checks whether a file [md3_file_name]_[skin_name].skin is existing.
         *  These files are used by Quake III to be able to assign different skins (e.g. red and blue team) to models.
         *  'default', 'red', 'blue' are typical skin names.
         *  Property type: String. Default value: "default".
         */
        var MD3_SKIN_NAME = "default"

        /** @brief  Specify the Quake 3 shader file to be used for a particular MD3 file. This can also be a search path.
         *
         *  By default Assimp's behaviour is as follows:
         *  If a MD3 file <tt>any_path/models/any_q3_subdir/model_name/file_name.md3</tt> is loaded, the library tries
         *  to locate the corresponding shader file in <tt>any_path/scripts/model_name.shader</tt>. This property
         *  overrides this behaviour. It can either specify a full path to the shader to be loaded or alternatively
         *  the path (relative or absolute) to the directory where the shaders for all MD3s to be loaded reside.
         *  Assimp attempts to open <tt>IMPORT_MD3_SHADER_SRC/model_name.shader</tt> first,
         *  <tt>IMPORT_MD3_SHADER_SRC/file_name.shader</tt> is the fallback file. Note that IMPORT_MD3_SHADER_SRC should
         *  have a terminal (back)slash.
         *  Property type: String. Default value: n/a.
         */
        var MD3_SHADER_SRC = ""

        /** @brief  Configures the LWO loader to load just one layer from the model.
         *
         *  LWO files consist of layers and in some cases it could be useful to load only one of them. This property can
         *  be either a string - which specifies the name of the layer - or an integer - the index of the layer. If the
         *  property is not set the whole LWO model is loaded. Loading fails if the requested layer is not available.
         *  The layer index is zero-based and the layer name may not be empty.<br>
         *  Property type: Integer. Default value: all layers are loaded.
         */
//        var LWO_ONE_LAYER_ONLY: Nothing = TODO()    // double valence

        /** @brief  Configures the MD5 loader to not load the MD5ANIM file for a MD5MESH file automatically.
         *
         *  The default strategy is to look for a file with the same name but the MD5ANIM extension
         *  in the same directory. If it is found, it is loaded and combined with the MD5MESH file.
         *  This configuration option can be used to disable this behaviour.
         *
         *  Property type: bool. Default value: false.
         */
        var MD5_NO_ANIM_AUTOLOAD = false

        /** @brief Defines the begin of the time range for which the LWS loader evaluates animations and computes
         *  AiNodeAnim's.
         *
         *  Assimp provides full conversion of LightWave's envelope system, including pre and post conditions.
         *  The loader computes linearly subsampled animation chanels with the frame rate given in the LWS file.
         *  This property defines the start time. Note: animation channels are only generated if a node has at least
         *  one envelope with more tan one key assigned. This property. is given in frames, '0' is the first frame.
         *  By default, if this property is not set, the importer takes the animation start from the input LWS
         *  file ('FirstFrame' line)<br>
         *  Property type: Integer. Default value: taken from file.
         *
         *  @see AI_CONFIG_IMPORT_LWS_ANIM_END - end of the imported time range
         */
        var LWS_ANIM_START = 0
        var LWS_ANIM_END = 0

        /** @brief Defines the output frame rate of the IRR loader.
         *
         *  IRR animations are difficult to convert for Assimp and there will always be a loss of quality.
         *  This setting defines how many keys per second are returned by the converter.<br>
         *  Property type: integer. Default value: 100
         */
        var IRR_ANIM_FPS = 100

        /** @brief Ogre Importer will try to find referenced materials from this file.
         *
         *  Ogre meshes reference with material names, this does not tell Assimp the file where it is located in.
         *  Assimp will try to find the source file in the following order: <material-name>.material,
         *  <mesh-filename-base>.material and lastly the material name defined by this config property. <br>
         *  Property type: String. Default value: Scene.material.
         */
        var OGRE_MATERIAL_FILE = "Scene.material"

        /** @brief Ogre Importer detect the texture usage from its filename.
         *
         *  Ogre material texture units do not define texture type, the textures usage depends on the used shader or
         *  Ogre's fixed pipeline. If this config property is true Assimp will try to detect the type from the textures
         *  filename postfix: _n, _nrm, _nrml, _normal, _normals and _normalmap for normal map, _s, _spec, _specular
         *  and _specularmap for specular map, _l, _light, _lightmap, _occ and _occlusion for light map, _disp and
         *  _displacement for displacement map.
         *  The matching is case insensitive. Post fix is taken between the last underscore and the last period.
         *  Default behavior is to detect type from lower cased texture unit name by matching against: normalmap,
         *  specularmap, lightmap and displacementmap.
         *  For both cases if no match is found aiTextureType_DIFFUSE is used. <br>
         *  Property type: Bool. Default value: false.
         */
        var OGRE_TEXTURETYPE_FROM_FILENAME = false

        /** @brief Specifies whether the IFC loader skips over IfcSpace elements.
         *
         *  IfcSpace elements (and their geometric representations) are used to represent, well, free space in a
         *  building storey.<br>
         *  Property type: Bool. Default value: true.
         */
        var IFC_SKIP_SPACE_REPRESENTATIONS = true

        /** @brief Specifies whether the IFC loader will use its own, custom triangulation algorithm to triangulate
         *  wall and floor meshes.
         *
         *  If this property is set to false, walls will be either triangulated by AiProcess_Triangulate or
         *  will be passed through as huge polygons with faked holes (i.e. holes that are connected with
         *  the outer boundary using a dummy edge). It is highly recommended to set this property to true
         *  if you want triangulated data because AiProcess_Triangulate is known to have problems with the kind
         *  of polygons that the IFC loader spits out for complicated meshes.
         *  Property type: Bool. Default value: true.
         */
        var IFC_CUSTOM_TRIANGULATION = true

        /** @brief  Set the tessellation conic angle for IFC smoothing curves.
         *
         *  This is used by the IFC importer to determine the tessellation parameter for smoothing curves.
         *  @note The default value is 10f and the accepted values are in range [5f, 120f].
         *  Property type: Float.
         */
        var IFC_SMOOTHING_ANGLE = 10f

        /** @brief  Set the tessellation for IFC cylindrical shapes.
         *
         *  This is used by the IFC importer to determine the tessellation parameter for cylindrical shapes,
         *  i.e. the number of segments used to aproximate a circle.
         *  @note The default value is 32 and the accepted values are in range [3, 180].
         *  Property type: Integer.
         */
        var IFC_CYLINDRICAL_TESSELLATION = 32

        /** @brief Specifies whether the Collada loader will ignore the provided up direction.
         *
         *  If this property is set to true, the up direction provided in the file header will be ignored and the file
         *  will be loaded as is.
         *  Property type: Bool. Default value: false.
         */
        var COLLADA_IGNORE_UP_DIRECTION = false
    }

    /** @brief Specifies whether the Android JNI asset extraction is supported.
     *
     *  Turn on this option if you want to manage assets in native Android application without having to keep
     *  the internal directory and asset manager pointer.
     */
    var ANDROID_JNI_ASSIMP_MANAGER_SUPPORT = false

    /** @brief Specifies the xfile use double for real values of float
     *
     *  Property type: Bool. Default value: false.
     */
    var EXPORT_XFILE_64BIT = false
}