package assimp.format.assxml

import assimp.AiMaterial
import assimp.AiTexture

fun materialToMap(mat : AiMaterial) : Map<String, Any> {
    var result = HashMap<String, Any>()

    if(mat.name!=null) result.put("?mat.name", mat.name!!)
    if(mat.twoSided!=null) result.put("\$mat.twosided", mat.twoSided!!)
    if(mat.shadingModel!=null) result.put("\$mat.shadingm", mat.shadingModel!!.i)
    if(mat.wireframe!=null) result.put("\$mat.wireframe", mat.wireframe!!)
    if(mat.blendFunc!=null) result.put("\$mat.blend", mat.blendFunc!!.i)
    if(mat.opacity!=null) result.put("\$mat.opacity", mat.opacity!!)
    if(mat.bumpScaling!=null) result.put("\$mat.bumpscaling", mat.bumpScaling!!)
    if(mat.shininess!=null) result.put("\$mat.shininess", mat.shininess!!)
    if(mat.reflectivity!=null) result.put("\$mat.reflectivity", mat.reflectivity!!)
    if(mat.shininessStrength!=null) result.put("\$mat.shinpercent", mat.shininessStrength!!)
    if(mat.refracti!=null) result.put("\$mat.refracti", mat.refracti!!)
    if(mat.color!=null) {
        if (mat.color!!.diffuse != null) result.put("\$clr.diffuse", mat.color!!.diffuse!!)
        if (mat.color!!.ambient != null) result.put("\$clr.ambient", mat.color!!.ambient!!)
        if (mat.color!!.specular != null) result.put("\$clr.specular", mat.color!!.specular!!)
        if (mat.color!!.emissive != null) result.put("\$clr.emissive", mat.color!!.emissive!!)
        if (mat.color!!.transparent != null) result.put("\$clr.transparent", mat.color!!.transparent!!)
        if (mat.color!!.reflective != null) result.put("\$clr.reflective", mat.color!!.reflective!!)
    }
    // if(mat.global!=null) result.put("?bg.global", bg.global!!)

    return result
}

fun textureToMap(tex : AiMaterial.Texture) : Map<String, Any> {
    var result = HashMap<String, Any>()
    if(tex.file!=null) result.put("\$tex.file",tex.file!!)
    if(tex.uvwsrc!=null && tex.uvwsrc!! != 0) result.put("\$tex.uvwsrc",tex.uvwsrc!!)
    if(tex.op!=null) result.put("\$tex.op",tex.op!!.i)
    if(tex.mapping!=null) result.put("\$tex.mapping",tex.mapping!!.i)
    if(tex.blend!=null) result.put("\$tex.blend",tex.blend!!)
    if(tex.mapModeU!=null) result.put("\$tex.mapmodeu",tex.mapModeU!!.i)
    if(tex.mapModeV!=null) result.put("\$tex.mapmodev",tex.mapModeV!!.i)
    if(tex.mapAxis!=null) result.put("\$tex.mapaxis",tex.mapAxis!!)
    if(tex.uvTrafo!=null) result.put("\$tex.uvtrafo",tex.uvTrafo!!)
    if(tex.flags!=null) result.put("\$tex.flags",tex.flags!!)
    return result
}


fun TextureTypeToString(i : AiTexture.Type) : String
{
    when (i)
    {
        AiTexture.Type.none ->
            return "n/a";
        AiTexture.Type.diffuse->
            return "Diffuse";
        AiTexture.Type.specular->
            return "Specular";
        AiTexture.Type.ambient->
            return "Ambient";
        AiTexture.Type.emissive->
            return "Emissive";
        AiTexture.Type.opacity->
            return "Opacity";
        AiTexture.Type.normals->
            return "Normals";
        AiTexture.Type.height->
            return "Height";
        AiTexture.Type.shininess->
            return "Shininess";
        AiTexture.Type.displacement->
            return "Displacement";
        AiTexture.Type.lightmap->
            return "Lightmap";
        AiTexture.Type.reflection->
            return "Reflection";
        AiTexture.Type.unknown->
            return "Unknown";
        else ->
            throw RuntimeException("BUG")
    }

    assert(false);
    return  "BUG";
}


