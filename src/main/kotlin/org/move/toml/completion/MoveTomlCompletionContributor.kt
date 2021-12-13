package org.move.toml.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import org.move.toml.MoveTomlPsiPatterns.inKey

class MoveTomlCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, inKey, MoveTomlKeysCompletionProvider())
    }
}
