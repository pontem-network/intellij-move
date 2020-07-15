package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import org.move.lang.MoveElementTypes

class MvKeywordCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            MvKeywordCompletionProvider(
                "let",
                "mut",
                "loop",
                "if",
                "else",
                "while",
                "abort",
                "return",
                "copy",
                "move"
            )
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            MvKeywordCompletionProvider("address", "module", "script")
        )
    }

//    private fun baseBlockPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
}