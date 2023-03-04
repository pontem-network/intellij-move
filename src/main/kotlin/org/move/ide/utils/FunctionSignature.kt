/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.name
import org.move.utils.cache
import org.move.utils.cacheManager

private val SIGNATURE_KEY: Key<CachedValue<FunctionSignature?>> = Key.create("SIGNATURE_KEY")

val MvFunction.signature: FunctionSignature?
    get() = project.cacheManager.cache(this, SIGNATURE_KEY) {
        val typeParameters = this.typeParameters
            .map { typeParam ->
                val paramName = typeParam.name ?: return@cache null
                FunctionSignature.TypeParameter(paramName)
            }
        val parameters = this.parameters
            .map { param ->
                val paramType = param.type ?: return@cache null
                FunctionSignature.Parameter(param.name, paramType.text)
            }
        val signature = FunctionSignature(typeParameters, parameters)
        CachedValueProvider.Result.create(
            signature,
            project.moveStructureModificationTracker
        )
    }

data class FunctionSignature(
    val typeParameters: List<TypeParameter>,
    val parameters: List<Parameter>,
) {
    data class TypeParameter(val name: String)

    data class Parameter(val name: String, val type: String)

    companion object {
        fun resolve(callExpr: MvCallExpr): FunctionSignature? {
            val function = callExpr.path.reference?.resolve() as? MvFunction
            return function?.signature
        }
    }
}
