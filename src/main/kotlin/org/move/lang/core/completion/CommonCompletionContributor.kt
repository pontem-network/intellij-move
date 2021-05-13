package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.COLON
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.ext.elementType

class CommonCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
        extend(CompletionType.BASIC, NamesCompletionProvider)
        extend(CompletionType.BASIC, TypesCompletionProvider)
        extend(CompletionType.BASIC, ModulesCompletionProvider)
//        extend(CompletionType.BASIC, QualModulesCompletionProvider)
        extend(CompletionType.BASIC, StructFieldsCompletionProvider)
        extend(
            CompletionType.BASIC,
            MovePsiPatterns.structTrait(),
            TraitsCompletionProvider()
        )
    }

    fun extend(type: CompletionType?, provider: MoveCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }

    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean =
        typeChar == ':' && position.elementType == COLON
}
