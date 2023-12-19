package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.addSuffix
import org.move.lang.core.completion.alreadyHasSpace
import org.move.lang.core.completion.createLookupElementWithIcon
import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.psi.ext.itemSpec
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.mslSpecifiableItems
import org.move.lang.core.psi.itemScope
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslLetScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility

object SpecItemCompletionProvider: MvCompletionProvider() {
    override val elementPattern get() = MvPsiPatterns.itemSpecRef()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val itemSpecRef = parameters.position.parent as? MvItemSpecRef ?: return
        val module = itemSpecRef.itemSpec.module ?: return
        module.mslSpecifiableItems
            .forEach {
                val lookup = it.createLookupElementWithIcon()
                    .withInsertHandler { ctx, _ ->
                        if (!ctx.alreadyHasSpace) ctx.addSuffix(" ")
                    }
                result.addElement(lookup)
            }
    }


}
