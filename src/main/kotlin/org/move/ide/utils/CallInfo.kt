/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import org.move.lang.core.psi.*

class CallInfo private constructor(
    val methodName: String,
    val parameters: List<Parameter>,
) {
    class Parameter(val name: String, val type: String)

    companion object {
        fun resolve(callExpr: MoveCallExpr): CallInfo? {
            val resolved = callExpr.reference.resolve() ?: return null

            val name = resolved.name ?: return null
            val parameters = (resolved as? MoveFunctionSignatureOwner)?.parameters.orEmpty().map {
                val paramName = it.name
                val paramType = it.type
                if (paramName == null || paramType == null) {
                    return@resolve null
                }
                Parameter(paramName, paramType.text)
            }
            return CallInfo(name, parameters)
        }
    }
}