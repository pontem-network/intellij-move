package org.move.ide.hints.parameter

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.ParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.utils.CallInfo
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveCallArguments
import org.move.lang.core.psi.MoveCallExpr
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.startOffset

class FunctionParameterInfoHandler : AsyncParameterInfoHandler<MoveCallArguments, ParametersDescription>() {
    override fun couldShowInLookup(): Boolean = true

    override fun getParametersForLookup(
        item: LookupElement,
        context: ParameterInfoContext?,
    ): Array<out Any>? {
        val elem = item.`object` as? PsiElement ?: return null
        val parent = elem.parent?.parent ?: return null

        if (parent is MoveCallExpr && CallInfo.resolve(parent) != null) {
            return arrayOf(parent)
        } else {
            return emptyArray()
        }
    }

    override fun findTargetElement(file: PsiFile, offset: Int): MoveCallArguments? =
        file.findElementAt(offset)?.ancestorStrict()

    override fun calculateParameterInfo(element: MoveCallArguments): Array<ParametersDescription>? =
        ParametersDescription.findDescription(element)?.let { arrayOf(it) }

    override fun updateParameterInfo(parameterOwner: MoveCallArguments, context: UpdateParameterInfoContext) {
        if (context.parameterOwner != parameterOwner) {
            context.removeHint()
            return
        }
        val currentParameterIndex = if (parameterOwner.startOffset == context.offset) {
            -1
        } else {
            ParameterInfoUtils.getCurrentParameterIndex(parameterOwner.node,
                context.offset,
                MoveElementTypes.COMMA)
        }
        context.setCurrentParameter(currentParameterIndex)
    }

    override fun updateUI(description: ParametersDescription, context: ParameterInfoUIContext) {
        val range = description.getArgumentRange(context.currentParameterIndex)
        context.setupUIComponentPresentation(
            description.presentText,
            range.startOffset,
            range.endOffset,
            !context.isUIComponentEnabled,
            false,
            false,
            context.defaultParameterColor)
    }

}

class ParametersDescription(val parameters: Array<String>) {
    fun getArgumentRange(index: Int): TextRange {
        if (index < 0 || index >= parameters.size) return TextRange.EMPTY_RANGE
        val start = parameters.take(index).sumBy { it.length + 2 }
        return TextRange(start, start + parameters[index].length)
    }

    val presentText = if (parameters.isEmpty()) "<no arguments>" else parameters.joinToString(", ")

    companion object {
        /**
         * Finds declaration of the func/method and creates description of its arguments
         */
        fun findDescription(args: MoveCallArguments): ParametersDescription? {
            val call = args.parent
            val callInfo = (call as? MoveCallExpr)?.let { CallInfo.resolve(it) } ?: return null

            val params = callInfo.parameters.map { "${it.name}: ${it.type}" }
            return ParametersDescription(params.toTypedArray())
        }
    }
}