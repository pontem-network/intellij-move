package org.move.cli.runConfigurations.aptos

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.allParamsAsBindings
import org.move.lang.core.psi.ext.functionId
import org.move.lang.core.psi.ext.isEntry
import org.move.lang.core.psi.ext.isView
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.core.psi.parameters
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ty.*

data class FunctionCallParam(val value: String, val type: String) {
    fun cmdText(): String = "$type:$value"

    companion object {
        fun tyTypeName(ty: Ty): String {
            return when (ty) {
                is TyInteger -> ty.kind.name
                is TyAddress -> "address"
                is TyBool -> "bool"
                is TyVector -> "vector"
                else -> "unknown"
            }
        }
    }
}

data class FunctionCall(
    val item: MvFunction,
    val typeParams: MutableMap<String, String?>,
    val valueParams: MutableMap<String, FunctionCallParam?>
) {
    fun itemName(): String? = item.qualName?.editorText()
    fun functionId(moveProject: MoveProject): String? = item.functionId(moveProject)

    fun parametersRequired(): Boolean {
        val fn = item
        return when {
            fn.isView -> fn.typeParameters.isNotEmpty() || fn.parameters.isNotEmpty()
            fn.isEntry -> fn.typeParameters.isNotEmpty() || fn.transactionParameters.isNotEmpty()
            else -> true
        }
    }

    companion object {
        fun template(function: MvFunction): FunctionCall {
            val typeParameterNames = function.typeParameters.mapNotNull { it.name }

            val nullTypeParams = mutableMapOf<String, String?>()
            for (typeParameterName in typeParameterNames) {
                nullTypeParams[typeParameterName] = null
            }

            val parameterBindings = function.allParamsAsBindings.drop(1)
            val parameterNames = parameterBindings.map { it.name }

            val nullParams = mutableMapOf<String, FunctionCallParam?>()
            for (parameterName in parameterNames) {
                nullParams[parameterName] = null
            }
            return FunctionCall(function, nullTypeParams, nullParams)
        }
    }
}
