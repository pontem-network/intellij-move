/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.utils

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.Ability
import org.move.utils.cache
import org.move.utils.cacheManager

data class FunctionSignature(
    val typeParameters: List<TypeParameter>,
    val parameters: List<Parameter>,
    val returnType: String?
) {
    data class TypeParameter(val name: String, val bounds: List<Ability>) {
        override fun hashCode(): Int {
            return 31 * name.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is TypeParameter && name == other.name
        }

        override fun toString(): String {
            val bounds = if (bounds.isNotEmpty()) bounds.joinToString(" + ", prefix = ": ") else ""
            return "$name$bounds"
        }
    }

    data class Parameter(val name: String, val type: String) {
        override fun toString(): String {
            return "$name: $type"
        }
    }

    companion object {
        fun resolve(callable: MvCallable): FunctionSignature? =
            when (callable) {
                is MvCallExpr -> {
                    val function = callable.path.reference?.resolveWithAliases() as? MvFunction
                    function?.getSignature()
                }
                is MvMethodCall -> {
                    val function = callable.reference.resolveWithAliases() as? MvFunction
                    function?.getSignature()
                }
                else -> null
            }

        fun fromFunction(function: MvFunction): FunctionSignature? {
            val typeParameters = function.typeParameters
                .map { typeParam ->
                    val paramName = typeParam.name ?: return null
                    val bounds = typeParam.abilityBounds.mapNotNull { it.ability }
                    TypeParameter(paramName, bounds)
                }
            val parameters = function.parameters
                .map { param ->
                    val paramType = param.type ?: return null
                    Parameter(param.name, paramType.text)
                }
            val returnType = function.returnType?.type?.text
            val signature = FunctionSignature(typeParameters, parameters, returnType)
            return signature
        }

        fun fromItemSpecSignature(specSignature: MvItemSpecSignature): FunctionSignature {
            val paramList = specSignature.itemSpecFunctionParameterList

            val specParameters = paramList.itemSpecFunctionParameterList
            val signatureParams = specParameters.map { specParam ->
                val paramName = specParam.referenceName
                val paramType = specParam.typeAnnotation?.type?.text ?: ""
                Parameter(paramName, paramType)
            }
            val specTypeParameters =
                specSignature.itemSpecTypeParameterList?.itemSpecTypeParameterList.orEmpty()
            val signatureTypeParams = specTypeParameters
                .map { specTypeParam ->
                    TypeParameter(specTypeParam.referenceName, specTypeParam.bounds.mapNotNull { it.ability })
                }
            val returnType = specSignature.returnType?.type?.text
            return FunctionSignature(signatureTypeParams, signatureParams, returnType)
        }
    }
}

private val FUNCTION_SIGNATURE_KEY: Key<CachedValue<FunctionSignature?>> = Key.create("SIGNATURE_KEY")

fun MvFunction.getSignature(): FunctionSignature? =
    project.cacheManager.cache(this, FUNCTION_SIGNATURE_KEY) {
        val signature = FunctionSignature.fromFunction(this)
        CachedValueProvider.Result.create(
            signature,
            project.moveStructureModificationTracker
        )
    }

val MvItemSpecSignature.functionSignature: FunctionSignature
    get() = FunctionSignature.fromItemSpecSignature(this)
