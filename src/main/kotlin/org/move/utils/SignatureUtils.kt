package org.move.utils

import org.move.ide.presentation.text
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.ext.name
import org.move.lang.core.psi.parameters
import org.move.lang.core.types.ty.TyCallable
import org.move.lang.core.types.ty.TyReference

fun TyCallable.parametersSignatureText(): String {
    val genericKind = this.genericKind() ?: return ""
    val item = genericKind.item as MvFunctionLike
    val params = item.parameters.zip(this.paramTypes)
        .associate { (param, paramTy) -> Pair(param.name, paramTy) }
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

fun TyCallable.returnTypeSignatureText(): String {
    val retType = this.returnType.text(false)
    val retTypeSuffix = if (retType == "" || retType == "()") "" else ": $retType"
    return retTypeSuffix
}

fun TyCallable.returnTypeLookupText(): String {
    val retType = this.returnType.text(false)
    val retTypeSuffix = if (retType == "" || retType == "()") "" else retType
    return retTypeSuffix
}

fun TyCallable.signatureText(): String = this.parametersSignatureText() + this.returnTypeSignatureText()
