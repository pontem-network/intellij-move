/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.abilityBounds
import org.move.lang.core.psi.ext.ability
import org.move.lang.core.psi.ext.isPhantom
import org.move.lang.core.psi.parameters
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ty.Ability
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.createCachedResult

private val SIGNATURE_KEY: Key<CachedValue<FunctionSignature?>> = Key.create("SIGNATURE_KEY")

val MvFunction.signature: FunctionSignature?
    get() = project.cacheManager.cache(this, SIGNATURE_KEY) {
        val name = this.name ?: return@cache null
        val typeParameters = this.typeParameters
            .map { typeParam ->
                val paramName = typeParam.name ?: return@cache null
                val bounds = typeParam.abilityBounds.mapNotNull { it.ability }.toSet()
                val isPhantom = typeParam.isPhantom
                FunctionSignature.TypeParameter(paramName, bounds, isPhantom)
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
        val signature = FunctionSignature(name, typeParameters, parameters)
        this.createCachedResult(signature)
    }

data class FunctionSignature(
    val name: String,
    val typeParameters: List<TypeParameter>,
    val parameters: List<Parameter>,
) {
    class TypeParameter(val name: String, val bounds: Set<Ability>, val isPhantom: Boolean)
    class Parameter(val name: String, val type: String)

    companion object {
        fun resolve(callExpr: MvCallExpr): FunctionSignature? {
            val function = callExpr.path.reference?.resolve() as? MvFunction
            return function?.signature
        }
    }
}
