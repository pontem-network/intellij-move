package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns

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
                "move",
                "break",
                "continue",
                "as"
            )
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            MvKeywordCompletionProvider("address", "module", "script")
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            MvKeywordCompletionProvider("public", "native", "fun", "resource", "struct", "const")
        )
    }

//    private fun baseBlockPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
}