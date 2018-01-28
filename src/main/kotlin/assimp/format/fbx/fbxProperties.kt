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

import kotlin.reflect.KMutableProperty0

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

class TypedProperty<T : Any>(var value: T) : Property()

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

    fun getUnparsedProperties(): MutableMap<String, Property> {

        val result = mutableMapOf<String, Property>()

        // Loop through all the lazy properties (which is all the properties)
        for (entry in lazyProps) {

            // Skip parsed properties
            if (props.contains(entry.key)) continue

            // Read the entry's value.
            val prop = entry.value.readTypedProperty() ?: continue  // Element could not be read. Skip it.

            // Add to result
            result[entry.key] = prop
        }
        return result
    }

    fun <T> get(name: String, result: KMutableProperty0<Boolean>): T? {
        val prop = get<T>(name)
        if (null == prop) {
            result.set(false)
            return null
        }

        // strong typing, no need to be lenient
//        val tprop = prop->As< TypedProperty<T> >()
//        if (nullptr == tprop) {
//            result = false
//            return T()
//        }
        result.set(true)
        return prop
    }

    fun <T> get(name: String, defaultValue: T) = get<T>(name) ?: defaultValue
}