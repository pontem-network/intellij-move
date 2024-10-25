package org.move.ide.hints.paramInfo

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.move.ide.hints.CallInfo
import org.move.ide.hints.paramInfo.ParameterInfoProvider.ParametersInfo
import org.move.lang.MvElementTypes.VALUE_ARGUMENT_LIST
import org.move.lang.core.psi.MvAssertMacroExpr
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvMethodCall
import org.move.lang.core.psi.MvValueArgumentList
import org.move.lang.core.psi.ext.MvCallable

class FunctionParameterInfoProvider: ParameterInfoProvider {
    override val targetElementType: IElementType get() = VALUE_ARGUMENT_LIST

    class FunctionParametersInfo(val parameters: List<String>): ParametersInfo {
        override val presentText: String
            get() = if (parameters.isEmpty()) "<no arguments>" else parameters.joinToString(", ")

        override fun getRangeInParent(index: Int): TextRange {
            if (index < 0 || index >= parameters.size) return TextRange.EMPTY_RANGE
            val start = parameters.take(index).sumOf { it.length + 2 }
            return TextRange(start, start + parameters[index].length)
        }
    }

    override fun findParameterInfo(listElement: PsiElement): FunctionParametersInfo? {
        val argumentList = listElement as MvValueArgumentList
        val call = argumentList.parent as? MvCallable ?: return null
        val callInfo = when (call) {
            is MvCallExpr -> CallInfo.resolve(call)
            is MvMethodCall -> CallInfo.resolve(call)
            is MvAssertMacroExpr -> CallInfo.resolve(call)
            else -> null
        } ?: return null
        val parameters = buildList {
            // self parameter with non-dot-expr call
            if (callInfo.selfParameter != null && call is MvCallExpr) {
                add(callInfo.selfParameter)
            }
            addAll(callInfo.parameters.map {
                buildString {
                    if (it.name != null) {
                        append("${it.name}: ")
                    }
                    append(it.renderType())
                }
            })
        }
        return FunctionParametersInfo(parameters)
    }
}