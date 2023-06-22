package org.move.ide.hints

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.utils.AsyncParameterInfoHandlerBase

class TypeParameterInfoHandler :
    AsyncParameterInfoHandlerBase<MvTypeArgumentList, TypeParamsDescription>() {

    override fun findTargetElement(file: PsiFile, offset: Int): MvTypeArgumentList? =
        file.findElementAt(offset)?.ancestorStrict()

    override fun calculateParameterInfo(element: MvTypeArgumentList): Array<TypeParamsDescription>? {
        val parentPath = element.parent as? MvPath ?: return null
        val owner = parentPath.reference?.resolveWithAliases() ?: return null
        // if zero type parameters
        if (owner !is MvTypeParametersOwner) return null

        return arrayOf(typeParamsDescription(owner.typeParameters))
    }

    override fun showParameterInfo(element: MvTypeArgumentList, context: CreateParameterInfoContext) {
        context.highlightedElement = null
        super.showParameterInfo(element, context)
    }

    override fun updateParameterInfo(
        parameterOwner: MvTypeArgumentList,
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
                MvElementTypes.COMMA
            )
        context.setCurrentParameter(curParam)
    }

    override fun updateUI(p: TypeParamsDescription, context: ParameterInfoUIContext) {
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
class TypeParamsDescription(
    val presentText: String,
    private val ranges: List<TextRange>,
) {
    fun getRange(index: Int): TextRange =
        if (index !in ranges.indices) TextRange.EMPTY_RANGE else ranges[index]
}

/**
 * Calculates the text representation and ranges for parameters
 */
private fun typeParamsDescription(params: List<MvTypeParameter>): TypeParamsDescription {
    val parts = params.map {
        val name = it.name ?: "_"
        val bound = it.typeParamBound?.text ?: ""
        name + bound
    }
    val presentText = if (parts.isEmpty()) "<no arguments>" else parts.joinToString(", ")
    return TypeParamsDescription(
        presentText,
        parts.indices.map { parts.calculateRange(it) }
    )
}

private fun List<String>.calculateRange(index: Int): TextRange {
    val start = this.take(index).sumOf({ it.length + 2 }) // plus ", "
    return TextRange(start, start + this[index].length)
}
