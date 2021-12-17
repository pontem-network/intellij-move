/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.parameters

class CallInfo(
    val name: String,
    val parameters: List<Parameter>,
    val returnType: String?,
) {
    class Parameter(val name: String, val type: String)

    companion object {
        fun resolve(callExpr: MvCallExpr): CallInfo? {
            val function = callExpr.path.reference?.resolve() as? MvFunction ?: return null
            val name = function.name ?: return null

            val parameters = function.parameters.map {
                val paramName = it.bindingPat.name
                val paramType = it.typeAnnotation?.type
                if (paramName == null || paramType == null) {
                    return@resolve null
                }
                Parameter(paramName, paramType.text)
            }
            val returnType = function.returnType?.type?.text

            return CallInfo(name, parameters, returnType)
        }
    }
}
