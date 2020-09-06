package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class MoveCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, CommonCompletionProvider.elementPattern, CommonCompletionProvider)
    }
}