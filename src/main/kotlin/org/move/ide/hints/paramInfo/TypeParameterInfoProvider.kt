package org.move.ide.hints.paramInfo

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.move.ide.hints.paramInfo.ParameterInfoProvider.ParametersInfo
import org.move.lang.MvElementTypes.TYPE_ARGUMENT_LIST
import org.move.lang.core.psi.*

class TypeParameterInfoProvider: ParameterInfoProvider {
    override val targetElementType: IElementType get() = TYPE_ARGUMENT_LIST

    override fun findParameterInfo(listElement: PsiElement): ParametersInfo? {
        val parentPath = listElement.parent as? MvPath ?: return null
        val owner = parentPath.reference?.resolveFollowingAliases() ?: return null
        // if zero type parameters
        if (owner !is MvGenericDeclaration) return null
        val typeParameters =
            owner.typeParameters.map { (it.name ?: "_") + (it.typeParamBound?.text ?: "") }
        val textRanges = typeParameters.indices.map { typeParameters.calculateRange(it) }
        return TypeParametersInfo(typeParameters, textRanges)

    }

    class TypeParametersInfo(
        val typeParameters: List<String>,
        val ranges: List<TextRange>
    ): ParametersInfo {
        override val presentText: String
            get() = if (typeParameters.isEmpty()) "<no arguments>" else typeParameters.joinToString(", ")

        override fun getRangeInParent(index: Int): TextRange {
            return if (index !in ranges.indices) TextRange.EMPTY_RANGE else ranges[index]
        }
    }
}

private fun List<String>.calculateRange(index: Int): TextRange {
    val start = this.take(index).sumOf { it.length + 2 } // plus ", "
    return TextRange(start, start + this[index].length)
}

