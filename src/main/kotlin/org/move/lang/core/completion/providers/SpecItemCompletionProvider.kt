package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.completion.addSuffix
import org.move.lang.core.completion.alreadyHasSpace
import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.psi.ext.itemSpec
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.mslSpecifiableItems

object SpecItemCompletionProvider: MvCompletionProvider() {
    override val elementPattern get() = MvPsiPattern.itemSpecRef()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val itemSpecRef = parameters.position.parent as? MvItemSpecRef ?: return
        val module = itemSpecRef.itemSpec.module ?: return
        module.mslSpecifiableItems
            .forEach {
                val name = it.name ?: return@forEach
                val lookup = LookupElementBuilder.createWithSmartPointer(name, it)
                    .withIcon(it.getIcon(0))
                    .withInsertHandler { ctx, _ ->
                        if (!ctx.alreadyHasSpace) ctx.addSuffix(" ")
                    }
                result.addElement(lookup)
            }
    }


}
