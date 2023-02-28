/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.parameters
import org.move.lang.core.psi.typeParameters
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.createCachedResult

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
                val paramName = param.bindingPat.name
                val paramType = param.typeAnnotation?.type
                if (paramName == null || paramType == null) {
                    return@cache null
                }
                FunctionSignature.Parameter(paramName, paramType.text)
            }
        val signature = FunctionSignature(typeParameters, parameters)
        this.createCachedResult(signature)
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
