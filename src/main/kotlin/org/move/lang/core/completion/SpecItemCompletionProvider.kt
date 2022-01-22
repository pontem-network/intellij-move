package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvNameSpecDef
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.processItemsInScopesBottomUp
import org.move.lang.core.resolve.ref.Namespace

object SpecItemCompletionProvider : MvCompletionProvider() {
    override val elementPattern get() = MvPsiPatterns.itemSpecLabel()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val specItem = parameters.position.parent as? MvNameSpecDef ?: return

        val itemVis = ItemVis(setOf(Namespace.SPEC_ITEM), msl = MslScope.NONE)
        processItemsInScopesBottomUp(specItem, itemVis) {
            if (it.element != null) {
                val lookup = it.element.createLookupElement { ctx, _ ->
                    if (!ctx.alreadyHasSpace) ctx.addSuffix(" ")
                }
                result.addElement(lookup)
            }
            false
        }
    }


}
