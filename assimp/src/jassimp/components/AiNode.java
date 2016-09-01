/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jassimp.components;

import jassimp.components.material.AiMaterial;

/**
 A node in the imported hierarchy.
 
 Each node has name, a parent node (except for the root node),
 a transformation relative to its parent and possibly several child nodes.
 Simple file formats don't support hierarchical structures - for these formats
 the imported scene does consist of only a single root node without children.
 *
 * @author gbarbieri
 */
public class AiNode {

    /** The name of the node.
     *
     * The name might be empty (length of zero) but all nodes which
     * need to be referenced by either bones or animations are named.
     * Multiple nodes may have the same name, except for nodes which are referenced
     * by bones (see #aiBone and #aiMesh::mBones). Their names *must* be unique.
     *
     * Cameras and lights reference a specific node by name - if there
     * are multiple nodes with this name, they are assigned to each of them.
     * <br>
     * There are no limitations with regard to the characters contained in
     * the name string as it is usually taken directly from the source file.
     *
     * Implementations should be able to handle tokens such as whitespace, tabs,
     * line feeds, quotation marks, ampersands etc.
     *
     * Sometimes assimp introduces new nodes not present in the source file
     * into the hierarchy (usually out of necessity because sometimes the
     * source hierarchy format is simply not compatible). Their names are
     * surrounded by @verbatim <> @endverbatim e.g.
     *  @verbatim<DummyRootNode> @endverbatim.
     */
    public String mName;
    
    /** Parent node. NULL if this node is the root node. */
    public AiNode mParent;

    /** The number of child nodes of this node. */
    public int mNumChildren;
    
    /** The child nodes of this node. NULL if mNumChildren is 0. */
    public AiNode[] mChildren;
    
    /** The number of meshes of this node. */
    public int mNumMeshes;
    
    /** The meshes of this node. Each entry is an index into the mesh */
    public int[] mMeshes;
    public AiMaterial aiMaterial;
}
