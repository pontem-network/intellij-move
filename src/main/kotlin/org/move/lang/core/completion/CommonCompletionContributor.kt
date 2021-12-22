package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.COLON
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.ext.elementType

class CommonCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
        extend(CompletionType.BASIC, AddressesCompletionProvider)
        extend(CompletionType.BASIC, NamesCompletionProvider)
        extend(CompletionType.BASIC, TypesCompletionProvider)
        extend(CompletionType.BASIC, ImportsCompletionProvider)
        extend(CompletionType.BASIC, ModulesCompletionProvider)
        extend(CompletionType.BASIC, QualModulesCompletionProvider)
        extend(CompletionType.BASIC, StructFieldsCompletionProvider)
        extend(
            CompletionType.BASIC,
            MvPsiPatterns.ability(),
            TraitsCompletionProvider()
        )
    }

    fun extend(type: CompletionType?, provider: MvCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }

    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean =
        typeChar == ':' && position.elementType == COLON
}
