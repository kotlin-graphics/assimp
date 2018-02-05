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

class TypedProperty<T>(var value: T) : Property()

/**
 *  Represents a property table as can be found in the newer FBX files (Properties60, Properties70)
 */
class PropertyTable(val element: Element? = null, val templateProps: PropertyTable? = null) {

    val lazyProps = HashMap<String, Element>()
    val props = HashMap<String, Property>()

    init {
        element?.let {
            val scope = it.scope
            for (e in scope.elements) {
                val key = e.key
                for (value in e.value) {
                    if (key != "P") {
                        domWarning("expected only P elements in property table", value)
                        continue
                    }
                    val name = value.peekPropertyName
                    if (name.isEmpty()) {
                        domWarning("could not read property name", value)
                        continue
                    }

                    if (lazyProps.contains(name)) {
                        domWarning("duplicate property name, will hide previous value: $name", value)
                        continue
                    }
                    lazyProps[name] = value
                }
            }
        }
    }

    /** PropertyGet */
    operator fun <T> invoke(name: String, defaultValue: T) = (get(name) as? TypedProperty<T>)?.value ?: defaultValue

    /** PropertyGet */
    operator fun <T> invoke(name: String, useTemplate: Boolean = false): T? {
        var prop = get(name)
        if (null == prop) {
            if (!useTemplate || null == templateProps) return null
            prop = templateProps[name]
            if (null == prop) return null
        }
        // strong typing, no need to be lenient
        return (prop as? TypedProperty<T>)?.value
    }

    operator fun get(name: String): Property? {

        var it = props[name]
        if (it == null) {
            // hasn't been parsed yet?
            val lit = lazyProps[name]
            if (lit != null) {
                it = lit.readTypedProperty()!!  // = assert
                props[name] = it
            }
            if (it == null)
            // check property template
                return templateProps?.get(name)
        }
        return it
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
}