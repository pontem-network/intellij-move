package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvPath
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.mslScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace

object ModulesCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.pathIdent()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val maybePathIdent = parameters.position.parent
        val maybePath = maybePathIdent.parent
        val refElement =
            maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== refElement.referenceNameElement) return
        if (refElement.pathIdent.moduleRef != null) return

        val itemVis = ItemVis(setOf(Namespace.MODULE), emptySet(), refElement.mslScope)
        processItems(refElement, itemVis) {
            val lookup = it.element.createCompletionLookupElement()
            result.addElement(lookup)
            false
        }
    }
}
