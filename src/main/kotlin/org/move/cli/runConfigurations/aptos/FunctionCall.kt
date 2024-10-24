package org.move.cli.runConfigurations.aptos

import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.createSmartPointer
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.parametersAsBindings
import org.move.lang.core.psi.ext.*
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
    val item: SmartPsiElementPointer<MvFunction>?,
    val typeParams: MutableMap<String, String?>,
    val valueParams: MutableMap<String, FunctionCallParam?>
) {
    fun itemName(): String? = item?.element?.qualName?.editorText()
    fun functionId(): String? = item?.element?.functionId()

    fun parametersRequired(): Boolean {
        val fn = item?.element ?: return false
        return when {
            fn.isView -> fn.typeParameters.isNotEmpty() || fn.parameters.isNotEmpty()
            fn.isEntry -> fn.typeParameters.isNotEmpty() || fn.transactionParameters.isNotEmpty()
            else -> true
        }
    }

    companion object {
        fun empty(): FunctionCall = FunctionCall(null, mutableMapOf(), mutableMapOf())

        fun template(function: MvFunction): FunctionCall {
            val typeParameterNames = function.typeParameters.mapNotNull { it.name }

            val nullTypeParams = mutableMapOf<String, String?>()
            for (typeParameterName in typeParameterNames) {
                nullTypeParams[typeParameterName] = null
            }

            val parameterBindings = function.parametersAsBindings.drop(1)
            val parameterNames = parameterBindings.map { it.name }

            val nullParams = mutableMapOf<String, FunctionCallParam?>()
            for (parameterName in parameterNames) {
                nullParams[parameterName] = null
            }
            return FunctionCall(function.createSmartPointer(), nullTypeParams, nullParams)
        }
    }
}
