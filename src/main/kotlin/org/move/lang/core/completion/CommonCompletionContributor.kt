package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class CommonCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PrimitiveTypesCompletionProvider)
        extend(CompletionType.BASIC, BuiltInsCompletionProvider)
        extend(CompletionType.BASIC, NamesCompletionProvider)
        extend(CompletionType.BASIC, TypesCompletionProvider)
    }

    fun extend(type: CompletionType?, provider: MoveCompletionProvider) {
        extend(type, provider.elementPattern, provider)
    }
}