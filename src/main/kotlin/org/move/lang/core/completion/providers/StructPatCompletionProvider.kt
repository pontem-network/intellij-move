package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.Completions
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.psi.MvLetStmt
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.getImportableItemsAsEntries
import org.move.lang.core.resolve.ref.TYPES
import org.move.lang.core.resolve.ref.filterByNs
import org.move.lang.core.withParent

object StructPatCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement()
                .withParent<MvPatBinding>()
                .withSuperParent(2, psiElement<MvLetStmt>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val bindingPat = parameters.position.parent as MvPatBinding
        val module = bindingPat.containingModule ?: return
        val completionCtx = MvCompletionContext(bindingPat, bindingPat.isMsl())
        val completions = Completions(completionCtx, result)

        val typeItems = getImportableItemsAsEntries(module).filterByNs(TYPES)
        completions.addEntries(typeItems)
    }
}
