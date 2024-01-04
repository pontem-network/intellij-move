package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.createCompletionLookupElement
import org.move.lang.core.psi.*
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.LetStmtScope
import org.move.lang.core.resolve.processModuleItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.withParent

object StructPatCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement()
                .withParent<MvBindingPat>()
                .withSuperParent(2, psiElement<MvLetStmt>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val bindingPat = parameters.position.parent as MvBindingPat
        val module = bindingPat.containingModule ?: return

        val namespaces = setOf(Namespace.TYPE)
        val itemVis = ItemVis(
            letStmtScope = LetStmtScope.NONE,
            itemScopes = bindingPat.itemScopes,
        )
        processModuleItems(module, namespaces, setOf(Visibility.Internal), itemVis) {
            val lookup = it.element.createCompletionLookupElement()
            result.addElement(lookup)
            false

        }
    }


}
