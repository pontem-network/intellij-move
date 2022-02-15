package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.ext.elementType

class CommonCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
        extend(CompletionType.BASIC, NamesCompletionProvider)
        extend(CompletionType.BASIC, SchemasCompletionProvider)
        extend(CompletionType.BASIC, SpecItemCompletionProvider)
        extend(CompletionType.BASIC, AddressesCompletionProvider)
        extend(CompletionType.BASIC, AddressInModuleDeclCompletionProvider)
        extend(CompletionType.BASIC, TypesCompletionProvider)
        extend(CompletionType.BASIC, ImportsCompletionProvider)
        extend(CompletionType.BASIC, ModulesCompletionProvider)
        extend(CompletionType.BASIC, FQModuleCompletionProvider)
        extend(CompletionType.BASIC, StructFieldsCompletionProvider)
        extend(CompletionType.BASIC, StructPatCompletionProvider)
        extend(
            CompletionType.BASIC,
            MvPsiPatterns.ability(),
            TraitsCompletionProvider
        )
        extend(CompletionType.BASIC, MacrosCompletionProvider)
    }

    fun extend(type: CompletionType?, provider: MvCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val element = context.file.findElementAt(offset) ?: return
        if (element.parent is MvModuleDef) {
            context.dummyIdentifier = "DummyAddress::"
        }
    }

    override fun invokeAutoPopup(position: PsiElement, typeChar: Char): Boolean =
        typeChar == ':' && position.elementType == MvElementTypes.COLON
}
