package org.move.lang.core.completion

import org.move.ide.presentation.text
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.name
import org.move.lang.core.psi.parameters
import org.move.lang.core.types.infer.TypeFoldable
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.functionTy

data class FuncSignature(
    private val params: Map<String, Ty>,
    private val retType: Ty,
): TypeFoldable<FuncSignature> {

    override fun innerFoldWith(folder: TypeFolder): FuncSignature {
        return FuncSignature(
            params = params.mapValues { (_, it) -> folder.fold(it) },
            retType = folder.fold(retType)
        )
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        params.values.any { visitor(it) } || visitor(retType)

    fun paramsText(): String {
        return params.entries
            .withIndex()
            .joinToString(", ", prefix = "(", postfix = ")") { (i, value) ->
                val (paramName, paramTy) = value
                if (i == 0 && paramName == "self") {
                    when (paramTy) {
                        is TyReference -> "&${if (paramTy.isMut) "mut " else ""}self"
                        else -> "self"
                    }
                } else {
                    "$paramName: ${paramTy.text(false)}"
                }
            }
    }

    fun retTypeText(): String = retType.text(false)

    fun retTypeSuffix(): String {
        return if (retType is TyUnit) "" else ": ${retTypeText()}"
    }

    companion object {
        fun fromFunction(function: MvFunction, msl: Boolean): FuncSignature {
            val functionType = function.functionTy(msl)
            val parameters = function.parameters
                .zip(functionType.paramTypes)
                .associate { (param, paramTy) -> Pair(param.name, paramTy) }
            val returnType = functionType.returnType
            return FuncSignature(parameters, returnType)
        }
    }
}