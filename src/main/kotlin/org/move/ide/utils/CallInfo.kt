/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.parameters

val MvFunction.callInfo: CallInfo?
    get() = CachedValuesManager.getCachedValue(this) {
        val name = this.name ?: return@getCachedValue null

        val parameters = this.parameters.map {
            val paramName = it.bindingPat.name
            val paramType = it.typeAnnotation?.type
            if (paramName == null || paramType == null) {
                return@getCachedValue null
            }
            CallInfo.Parameter(paramName, paramType.text)
        }
        CachedValueProvider.Result(CallInfo(name, parameters), PsiModificationTracker.MODIFICATION_COUNT)
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
