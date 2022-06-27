package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace

object SpecItemCompletionProvider : MvCompletionProvider() {
    override val elementPattern get() = MvPsiPatterns.itemSpecLabel()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val specItem = parameters.position.parent as? MvItemSpec ?: return

        val itemVis = ItemVis.default().replace(setOf(Namespace.SPEC_ITEM), msl = MslScope.NONE)
        processItems(specItem, itemVis) {
            val lookup = it.element.createLookupElement()
                .withInsertHandler { ctx, _ ->
                    if (!ctx.alreadyHasSpace) ctx.addSuffix(" ")
                }
            result.addElement(lookup)
            false
        }
    }


}
