package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedAddress
import org.move.lang.core.psiElement
import org.move.lang.core.withParent
import org.move.lang.moveProject

object AddressInModuleDeclCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns
            .psiElement()
            .withParent<MvNamedAddress>()
            .withSuperParent(3, psiElement<MvModule>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val moveProject = element.moveProject ?: return
        val addresses = moveProject.addressValues()
        for ((name, value) in addresses.entries.sortedBy { it.key }) {
            val lookup = LookupElementBuilder
                .create(name)
                .withTypeText(value.value)
                .withInsertHandler { ctx, _ ->
                    val document = ctx.document
                    if (!ctx.alreadyHasColonColon) {
                        document.insertString(ctx.selectionEndOffset, "::")
                    }
                    EditorModificationUtil.moveCaretRelatively(ctx.editor, 2)
                }
            result.addElement(lookup)
        }
    }
}

object AddressesCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns
            .psiElement().withParent<MvNamedAddress>()
            .andNot(
                PlatformPatterns.psiElement()
                    .withSuperParent(3, psiElement<MvModule>())
            )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val moveProject = element.moveProject ?: return
        val addresses = moveProject.addressValues()
        for ((name, addressVal) in addresses.entries.sortedBy { it.key }) {
            val lookup = addressVal.createCompletionLookupElement(name)
            result.addElement(lookup)
        }
    }
}
