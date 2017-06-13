package assimp.format.obj

import assimp.AI_MAX_NUMBER_OF_TEXTURECOORDS
import assimp.AiColor3D
import assimp.AiPrimitiveType
import assimp.AiVector3D
import glm_.mat4x4.Mat4

/**
 * Created by elect on 21/11/2016.
 */

// ------------------------------------------------------------------------------------------------
//! \struct Face
//! \brief  Data structure for a simple obj-face, describes discredit,l.ation and materials
// ------------------------------------------------------------------------------------------------
data class Face(

        //! Primitive Type
        var m_PrimitiveType: AiPrimitiveType = AiPrimitiveType.POLYGON,
        //! Vertex indices
        var m_vertices: MutableList<Int> = mutableListOf(),
        //! Normal indices
        var m_normals: MutableList<Int> = mutableListOf(),
        //! Texture coordinates indices
        var m_texturCoords: MutableList<Int> = mutableListOf(),
        //! Pointer to assigned material
        var m_pMaterial: Material? = null
)

// ------------------------------------------------------------------------------------------------
//! \struct Object
//! \brief  Stores all objects of an obj-file object definition
// ------------------------------------------------------------------------------------------------
class Object(
        //! Object name
        var m_strObjName: String = "",
        //! Transformation matrix, stored in OpenGL format
        var m_Transformation: Mat4 = Mat4(),
        //! All sub-objects referenced by this object
        var m_SubObjects: List<Object> = listOf(),
        /// Assigned meshes
        var m_Meshes: MutableList<Int> = mutableListOf()
) {
    enum class Type {    Obj, Group }
}


// ------------------------------------------------------------------------------------------------
//! \struct Material
//! \brief  Data structure to store all material specific data
// ------------------------------------------------------------------------------------------------
data class Material(

        //! Name of material description
        var materialName: String,

        //! Textures
        var textures: ArrayList<Texture> = ArrayList(),

        //! Ambient color
        var ambient: AiColor3D = AiColor3D(),
        //! Diffuse color
        var diffuse: AiColor3D = AiColor3D(0.6),
        //! Specular color
        var specular: AiColor3D = AiColor3D(),
        //! Emissive color
        var emissive: AiColor3D = AiColor3D(),
        //! Alpha value
        var alpha: Float = 1.0f,
        //! Shineness factor
        var shineness: Float = 0.0f,
        //! Illumination model
        var illumination_model: Int = 1,
        //! Index of refraction
        var ior: Float = 1.0f,
        //! Transparency color
        var transparent: AiColor3D = AiColor3D(1f)
) {
    class Texture(
            val name: String,
            val type: Type,
            val clamp: Boolean = false
    ) {
        enum class Type {
            diffuse,
            specular,
            ambient,
            emissive,
            bump,
            normal,
            reflectionSphere,
            reflectionCubeTop,
            reflectionCubeBottom,
            reflectionCubeFront,
            reflectionCubeBack,
            reflectionCubeLeft,
            reflectionCubeRight,
            specularity,
            opacity,
            disp
        }
    }
}

// ------------------------------------------------------------------------------------------------
//! \struct Mesh
//! \brief  Data structure to store a mesh
// ------------------------------------------------------------------------------------------------
data class Mesh(

        /// The name for the mesh
        var m_name: String,
        /// Array with pointer to all stored faces
        var m_Faces: MutableList<Face> = mutableListOf(),
        /// Assigned material
        var m_pMaterial: Material? = null,
        /// Number of stored indices.
        var m_uiNumIndices: Int = 0,
        /// Number of UV
        var m_uiUVCoordinates: IntArray = IntArray(AI_MAX_NUMBER_OF_TEXTURECOORDS, { 0 }),
        /// Material index.
        var m_uiMaterialIndex: Int = NoMaterial,
        // True, if normals are stored.
        var m_hasNormals: Boolean = false,
        /// True, if vertex colors are stored.
        var m_hasVertexColors: Boolean = true
) {
    companion object {
        const val NoMaterial = 0xffffffff.toInt()
    }
}

// ------------------------------------------------------------------------------------------------
//! \struct Model
//! \brief  Data structure to store all obj-specific model datas
// ------------------------------------------------------------------------------------------------
data class Model(

        //! Model name
        var m_ModelName: String = "",
        //! List ob assigned objects
        var m_Objects: MutableList<Object> = mutableListOf(),
        //! Pointer to current object
        var m_pCurrent: Object? = null,
        //! Pointer to current material
        var m_pCurrentMaterial: Material? = null,
        //! Pointer to default material
        var m_pDefaultMaterial: Material? = null,
        //! Vector with all generated materials
        var m_MaterialLib: MutableList<String> = mutableListOf(),
        //! Vector with all generated vertices
        var m_Vertices: MutableList<AiVector3D> = mutableListOf(),
        //! vector with all generated normals
        var m_Normals: MutableList<AiVector3D> = mutableListOf(),
        //! vector with all vertex colors
        var m_VertexColors: MutableList<AiVector3D> = mutableListOf(),
        //! Group map
        var m_Groups: MutableMap<String, MutableList<Int>> = mutableMapOf(),
        //! Group to face id assignment
        var m_pGroupFaceIDs: MutableList<Int> = mutableListOf(),
        //! Active group
        var m_strActiveGroup: String = "",
        //! Vector with generated texture coordinates
        var m_TextureCoord: MutableList<MutableList<Float>> = mutableListOf(),
        //! Current mesh instance
        var m_pCurrentMesh: Mesh? = null,
        //! Vector with stored meshes
        var m_Meshes: MutableList<Mesh> = mutableListOf(),
        //! Material map
        var m_MaterialMap: MutableMap<String, Material> = mutableMapOf()
)