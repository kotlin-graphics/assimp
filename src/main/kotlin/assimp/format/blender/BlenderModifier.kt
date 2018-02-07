/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2018, assimp team


All rights reserved.

Redistribution and use of this software in source and binary forms,
with or without modification, are permitted provided that the
following conditions are met:

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

----------------------------------------------------------------------
*/

package assimp.format.blender

import assimp.AiNode


// -------------------------------------------------------------------------------------------
/** Dummy base class for all blender modifiers. Modifiers are reused between imports, so
 *  they should be stateless and not try to cache model data. */
// -------------------------------------------------------------------------------------------
//        class BlenderModifier
//        {
//            public:
//            virtual ~BlenderModifier() {
//            // empty
//        }
//
//            public:
//
//            // --------------------
//            /** Check if *this* modifier is active, given a ModifierData& block.*/
//            virtual bool IsActive( const ModifierData& /*modin*/) {
//            return false;
//        }
//
//            // --------------------
//            /** Apply the modifier to a given output node. The original data used
//             *  to construct the node is given as well. Not called unless IsActive()
//             *  was called and gave positive response. */
//            virtual void DoIt(aiNode& /*out*/,
//            ConversionData& /*conv_data*/,
//            const ElemBase& orig_modifier,
//            const Scene& /*in*/,
//            const Object& /*orig_object*/
//            ) {
//            DefaultLogger::get()->warn((Formatter::format("This modifier is not supported, skipping: "),orig_modifier.dna_type));
//            return;
//        }
//        };

class SharedModifierData : ElemBase() {
    var modifier = ModifierData()
}

/** Manage all known modifiers and instance and apply them if necessary */
class BlenderModifierShowcase {

    /** Apply all requested modifiers provided we support them. */
    fun applyModifiers(out: AiNode, convData: ConversionData, in_: Scene , origObject: Object) {

        var cnt = 0L
        var ful = 0L

        /*  NOTE: this cast is potentially unsafe by design, so we need to perform type checks before we're allowed to
            dereference the pointers without risking to crash. We might still be invoking UB btw - we're assuming that
            the ModifierData member of the respective modifier structures is at offset sizeof(vftable) with no padding. */
        var cur = origObject.modifiers?.first as? SharedModifierData
        var begin = true
        while(cur != null) {

            if(!begin) {
                cur =  cur.modifier.next as? SharedModifierData
                ++ful
            }
            begin = false

            assert(cur!!.dnaType.isNotEmpty())

//            val s = convData.db.dna.Get( cur->dna_type )
//            if (!s) {
//                ASSIMP_LOG_WARN_F("BlendModifier: could not resolve DNA name: ",cur->dna_type)
//                continue
//            }
//
//            // this is a common trait of all XXXMirrorData structures in BlenderDNA
//            const Field* f = s->Get("modifier")
//            if (!f || f->offset != 0) {
//            ASSIMP_LOG_WARN("BlendModifier: expected a `modifier` member at offset 0")
//                continue
//            }
//
//            s = conv_data.db.dna.Get( f->type )
//            if (!s || s->name != "ModifierData") {
//            ASSIMP_LOG_WARN("BlendModifier: expected a ModifierData structure as first member")
//                continue
//            }
//
//            // now, we can be sure that we should be fine to dereference *cur* as
//            // ModifierData (with the above note).
//            const ModifierData& dat = cur->modifier
//
//            const fpCreateModifier* curgod = creators
//            std::vector< BlenderModifier* >::iterator curmod = cached_modifiers->begin(), endmod = cached_modifiers->end()
//
//            for (;*curgod;++curgod,++curmod) { // allocate modifiers on the fly
//            if (curmod == endmod) {
//                cached_modifiers->push_back((*curgod)())
//
//                endmod = cached_modifiers->end()
//                curmod = endmod-1
//            }
//
//            BlenderModifier* const modifier = *curmod
//                if(modifier->IsActive(dat)) {
//                modifier->DoIt(out,conv_data,*static_cast<const ElemBase *>(cur),in,orig_object)
//                    cnt++
//
//                    curgod = NULL
//                    break
//                }
//        }
//            if (curgod) {
//                ASSIMP_LOG_WARN_F("Couldn't find a handler for modifier: ",dat.name)
//            }
//        }
//
//        // Even though we managed to resolve some or all of the modifiers on this
//        // object, we still can't say whether our modifier implementations were
//        // able to fully do their job.
//        if (ful) {
//            ASSIMP_LOG_DEBUG_F("BlendModifier: found handlers for ",cnt," of ",ful," modifiers on `",orig_object.id.name,
//                    "`, check log messages above for errors")
        }
    }

//    TempArray< std::vector,BlenderModifier > cached_modifiers
}


// MODIFIERS


// -------------------------------------------------------------------------------------------
/** Mirror modifier. Status: implemented. */
// -------------------------------------------------------------------------------------------
//class BlenderModifier_Mirror : public BlenderModifier
//{
//    public:
//
//    // --------------------
//    virtual bool IsActive(const ModifierData & modin);
//
//    // --------------------
//    virtual void DoIt(aiNode& out,
//    ConversionData& conv_data,
//    const ElemBase & orig_modifier,
//    const Scene & in,
//    const Object & orig_object
//    );
//};
//
//// -------------------------------------------------------------------------------------------
///** Subdivision modifier. Status: dummy. */
//// -------------------------------------------------------------------------------------------
//class BlenderModifier_Subdivision : public BlenderModifier
//{
//    public:
//
//    // --------------------
//    virtual bool IsActive(const ModifierData & modin);
//
//    // --------------------
//    virtual void DoIt(aiNode& out,
//    ConversionData& conv_data,
//    const ElemBase & orig_modifier,
//    const Scene & in,
//    const Object & orig_object
//    );
//};