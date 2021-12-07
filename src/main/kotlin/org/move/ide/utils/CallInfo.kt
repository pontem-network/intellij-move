/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunctionSignatureOwner
import org.move.lang.core.psi.parameters

class CallInfo(
    val name: String,
    val parameters: List<Parameter>,
    val returnType: String?,
) {
    class Parameter(val name: String, val type: String)

    companion object {
        fun resolve(callExpr: MvCallExpr): CallInfo? {
            val resolved = callExpr.path.reference?.resolve() ?: return null
            val name = resolved.name ?: return null

            val signature = resolved as? MvFunctionSignatureOwner ?: return null
            val parameters = signature.parameters.map {
                val paramName = it.bindingPat.name
                val paramType = it.typeAnnotation?.type
                if (paramName == null || paramType == null) {
                    return@resolve null
                }
                Parameter(paramName, paramType.text)
            }
            val returnType = signature.returnType?.type?.text

            return CallInfo(name, parameters, returnType)
        }
    }
}
