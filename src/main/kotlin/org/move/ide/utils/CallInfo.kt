/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.parameters

val MvFunction.callInfo: CallInfo?
    get() = getProjectPsiDependentCache(this) {
        val name = it.name ?: return@getProjectPsiDependentCache null

        val parameters = it.parameters.map { param ->
            val paramName = param.bindingPat.name
            val paramType = param.typeAnnotation?.type
            if (paramName == null || paramType == null) {
                return@getProjectPsiDependentCache null
            }
            CallInfo.Parameter(paramName, paramType.text)
        }
        CallInfo(name, parameters)
    }

class CallInfo(
    val name: String,
    val parameters: List<Parameter>,
) {
    class Parameter(val name: String, val type: String)

    companion object {
        fun resolve(callExpr: MvCallExpr): CallInfo? {
            val function = callExpr.path.reference?.resolve() as? MvFunction
            return function?.callInfo
        }
    }
}
