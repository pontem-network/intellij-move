package org.move.ide.hints

import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.move.ide.utils.CallInfo
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvCallArguments
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvStructLitFieldsBlock
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.contains
import org.move.lang.core.psi.ext.startOffset
import org.move.utils.AsyncParameterInfoHandler

class FunctionParameterInfoHandler : AsyncParameterInfoHandler<MvCallArguments, ParamsDescription>() {

    override fun findTargetElement(file: PsiFile, offset: Int): MvCallArguments? {
        val element = file.findElementAt(offset) ?: return null
        val callExpr = element.ancestorStrict<MvCallArguments>() ?: return null
        val block = element.ancestorStrict<MvStructLitFieldsBlock>()
        if (block != null && callExpr.contains(block)) return null
        return callExpr
    }

    override fun calculateParameterInfo(element: MvCallArguments): Array<ParamsDescription>? =
        ParamsDescription.findDescription(element)?.let { arrayOf(it) }

    override fun updateParameterInfo(parameterOwner: MvCallArguments, context: UpdateParameterInfoContext) {
        if (context.parameterOwner != parameterOwner) {
            context.removeHint()
            return
        }
        val currentParameterIndex = if (parameterOwner.startOffset == context.offset) {
            -1
        } else {
            ParameterInfoUtils.getCurrentParameterIndex(
                parameterOwner.node,
                context.offset,
                MvElementTypes.COMMA
            )
        }
        context.setCurrentParameter(currentParameterIndex)
    }

    override fun updateUI(description: ParamsDescription, context: ParameterInfoUIContext) {
        val range = description.getArgumentRange(context.currentParameterIndex)
        context.setupUIComponentPresentation(
            description.presentText,
            range.startOffset,
            range.endOffset,
            !context.isUIComponentEnabled,
            false,
            false,
            context.defaultParameterColor
        )
    }

}

class ParamsDescription(val parameters: Array<String>) {
    fun getArgumentRange(index: Int): TextRange {
        if (index < 0 || index >= parameters.size) return TextRange.EMPTY_RANGE
        val start = parameters.take(index).sumOf { it.length + 2 }
        return TextRange(start, start + parameters[index].length)
    }

    val presentText = if (parameters.isEmpty()) "<no arguments>" else parameters.joinToString(", ")

    companion object {
        /**
         * Finds declaration of the func/method and creates description of its arguments
         */
        fun findDescription(args: MvCallArguments): ParamsDescription? {
            val call = args.parent
            val callInfo = (call as? MvCallExpr)?.let { CallInfo.resolve(it) } ?: return null

            val params = callInfo.parameters.map { "${it.name}: ${it.type}" }
            return ParamsDescription(params.toTypedArray())
        }
    }
}
