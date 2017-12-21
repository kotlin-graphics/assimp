/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2017, assimp team

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

package assimp.format.fbx

/** @file  FBXProperties.h
 *  @brief FBX dynamic properties
 */

/** Represents a dynamic property. Type info added by deriving classes,
 *  see #TypedProperty.
 *  Example:
 *  @verbatim
 *  P: "ShininessExponent", "double", "Number", "",0.5
 *  @endvebatim
 */
open class Property : Any()

class TypedProperty<T>(var value: T) : Property()

/**
 *  Represents a property table as can be found in the newer FBX files (Properties60, Properties70)
 */
class PropertyTable(var element: Element? = null, var templateProps: PropertyTable? = null) {

    val lazyProps = HashMap<String, Element>()
    val props = HashMap<String, Any?>()

    init {
        if (element != null && templateProps != null) {
            val scope = element!!.scope
            for (v in scope.elements.flatMap { e -> List(e.value.size, { Pair(e.key, e.value[it]) }) }) {
                if (v.first != "P") {
                    domWarning("expected only P elements in property table", v.second)
                    continue
                }
                val name = v.second.peekPropertyName
                if (name.isEmpty()) {
                    domWarning("could not read property name", v.second)
                    continue
                }

                if (lazyProps.contains(name)) {
                    domWarning("duplicate property name, will hide previous value: $name", v.second)
                    continue
                }
                lazyProps[name] = v.second
            }
        }
    }

    operator fun <T> get(name: String): T? {
        var p = props[name]
        if (p == null) {
            // hasn't been parsed yet?
            lazyProps[name]?.let {
                props[name] = it.readTypedProperty()
                p = props[name]!!
            }
            if (p == null) // check property template
                return templateProps?.get(name)
        }
        return p as T?
    }

//    DirectPropertyMap GetUnparsedProperties() const
}


// ------------------------------------------------------------------------------------------------
//template <typename T>
//inline
//T PropertyGet(const PropertyTable& in , const std::string& name, const T& defaultValue) {
//    const Property * const prop = in.Get(name)
//    if (nullptr == prop) {
//        return defaultValue
//    }
//
//    // strong typing, no need to be lenient
//    const TypedProperty < T > * const tprop = prop->As< TypedProperty<T> >()
//    if (nullptr == tprop) {
//        return defaultValue
//    }
//
//    return tprop->Value()
//}
//
//// ------------------------------------------------------------------------------------------------
//template <typename T>
//inline
//T PropertyGet(const PropertyTable& in , const std::string& name, bool& result) {
//    const Property * const prop = in.Get(name)
//    if (nullptr == prop) {
//        result = false
//        return T()
//    }
//
//    // strong typing, no need to be lenient
//    const TypedProperty < T > * const tprop = prop->As< TypedProperty<T> >()
//    if (nullptr == tprop) {
//        result = false
//        return T()
//    }
//
//    result = true
//    return tprop->Value()
//}