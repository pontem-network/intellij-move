package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class CommonCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, NamesCompletionProvider)
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
    }

    fun extend(type: CompletionType?, provider: MoveCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }
}