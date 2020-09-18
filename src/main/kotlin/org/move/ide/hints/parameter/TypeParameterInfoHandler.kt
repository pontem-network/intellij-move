package org.move.ide.hints.parameter

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.utils.AsyncParameterInfoHandler

class TypeParameterInfoHandler : AsyncParameterInfoHandler<MoveTypeArgumentList, TypeParametersDescription>() {
    override fun findTargetElement(file: PsiFile, offset: Int): MoveTypeArgumentList? =
        file.findElementAt(offset)?.ancestorStrict()

    override fun calculateParameterInfo(element: MoveTypeArgumentList): Array<TypeParametersDescription>? {
        val qualifiedPath = element.parent
        val container = qualifiedPath.parent
        if (container is MoveReferenceElement) {
            val referred = container.reference.resolve() ?: return null
            if (referred is MoveTypeParametersOwner) {
                val paramsDescription = getDescription(referred.typeParameters)
                return arrayOf(paramsDescription)
            }
        }
        return emptyArray()
//        if (parent is MoveCallExpr || parent is MoveStructLiteralExpr) {
//
//        }
//        val genericDeclaration = if (parent is RsMethodCall || parent is RsPath) {
//            parent.reference?.resolve() as? RsGenericDeclaration ?: return null
//        } else {
//            return null
//        }
//        val typesWithBounds = genericDeclaration.typeParameters.nullize() ?: return null
//        return listOf(getDescription(typesWithBounds)).toTypedArray()
//        return listOfNotNull(firstLine(typesWithBounds), secondLine(typesWithBounds)).toTypedArray()
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
            ParameterInfoUtils.getCurrentParameterIndex(parameterOwner.node,
                context.offset,
                MoveElementTypes.COMMA)
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

    override fun getParametersForLookup(item: LookupElement?, context: ParameterInfoContext?): Array<Any>? =
        null

    override fun couldShowInLookup() = false
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
private fun getDescription(params: List<MoveTypeParameter>): TypeParametersDescription {
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
    val start = this.take(index).sumBy { it.length + 2 } // plus ", "
    return TextRange(start, start + this[index].length)
}
