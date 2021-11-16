package org.move.ide.hints

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.utils.AsyncParameterInfoHandler

class TypeParameterInfoHandler :
    AsyncParameterInfoHandler<MoveTypeArgumentList, TypeParametersDescription>() {
    override fun findTargetElement(file: PsiFile, offset: Int): MoveTypeArgumentList? =
        file.findElementAt(offset)?.ancestorStrict()

    override fun calculateParameterInfo(element: MoveTypeArgumentList): Array<TypeParametersDescription>? {
        val owner =
            (element.parent as? MovePath)
                ?.reference?.resolve() ?: return null
        if (owner !is MoveTypeParametersOwner) return null
        return arrayOf(typeParamsDescription(owner.typeParameters))
    }

    override fun showParameterInfo(element: MoveTypeArgumentList, context: CreateParameterInfoContext) {
        context.highlightedElement = null
        super.showParameterInfo(element, context)
    }

    override fun updateParameterInfo(
        parameterOwner: MoveTypeArgumentList,
        context: UpdateParameterInfoContext,
    ) {
        if (context.parameterOwner != parameterOwner) {
            context.removeHint()
            return
        }
        val curParam =
            ParameterInfoUtils.getCurrentParameterIndex(
                parameterOwner.node,
                context.offset,
                MoveElementTypes.COMMA
            )
        context.setCurrentParameter(curParam)
    }

    override fun updateUI(p: TypeParametersDescription, context: ParameterInfoUIContext) {
        context.setupUIComponentPresentation(
            p.presentText,
            p.getRange(context.currentParameterIndex).startOffset,
            p.getRange(context.currentParameterIndex).endOffset,
            false, // define whole hint line grayed
            false,
            false, // define grayed part of args before highlight
            context.defaultParameterColor
        )
    }
}

/**
 * Stores the text representation and ranges for parameters
 */
class TypeParametersDescription(
    val presentText: String,
    private val ranges: List<TextRange>,
) {
    fun getRange(index: Int): TextRange =
        if (index !in ranges.indices) TextRange.EMPTY_RANGE else ranges[index]
}

/**
 * Calculates the text representation and ranges for parameters
 */
private fun typeParamsDescription(params: List<MoveTypeParameter>): TypeParametersDescription {
    val parts = params.map {
        val name = it.name ?: "_"
        val bound = it.typeParamBound?.text ?: ""
        name + bound
    }
    val presentText = if (parts.isEmpty()) "<no arguments>" else parts.joinToString(", ")
    return TypeParametersDescription(
        presentText,
        parts.indices.map { parts.calculateRange(it) }
    )
}

private fun List<String>.calculateRange(index: Int): TextRange {
    val start = this.take(index).sumOf({ it.length + 2 }) // plus ", "
    return TextRange(start, start + this[index].length)
}
