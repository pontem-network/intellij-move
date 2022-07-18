package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.addSuffix
import org.move.lang.core.completion.alreadyHasSpace
import org.move.lang.core.completion.createLookupElementWithIcon
import org.move.lang.core.psi.MvItemSpec
import org.move.lang.core.psi.ext.folderScope
import org.move.lang.core.psi.ext.itemScope
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility

object SpecItemCompletionProvider : MvCompletionProvider() {
    override val elementPattern get() = MvPsiPatterns.itemSpecLabel()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val specItem = parameters.position.parent as? MvItemSpec ?: return

        val itemVis = ItemVis(
            namespaces = setOf(Namespace.SPEC_ITEM),
            visibilities = Visibility.none(),
            mslScope = MslScope.NONE,
            itemScope = specItem.itemScope,
            folderScope = specItem.folderScope
        )
        processItems(specItem, itemVis) {
            val lookup = it.element.createLookupElementWithIcon()
                .withInsertHandler { ctx, _ ->
                    if (!ctx.alreadyHasSpace) ctx.addSuffix(" ")
                }
            result.addElement(lookup)
            false
        }
    }


}
