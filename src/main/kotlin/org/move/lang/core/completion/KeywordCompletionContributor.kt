package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.lang.MvElementTypes
import org.move.lang.MvElementTypes.*
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.MvPsiPatterns.addressBlock
import org.move.lang.core.MvPsiPatterns.codeStatement
import org.move.lang.core.MvPsiPatterns.function
import org.move.lang.core.MvPsiPatterns.moduleBlock
import org.move.lang.core.MvPsiPatterns.scriptBlock
import org.move.lang.core.MvPsiPatterns.toplevel
import org.move.lang.core.TYPES
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionVisibilityModifier
import org.move.lang.core.psi.MvModuleBlock
import org.move.lang.core.psi.MvReturnType
import org.move.lang.core.withParent

class KeywordCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            toplevel().and(onStatementBeginning()),
            KeywordCompletionProvider("address", "module", "script")
        )
        extend(
            CompletionType.BASIC,
            addressBlock().and(onStatementBeginning()),
            KeywordCompletionProvider("module")
        )
        extend(
            CompletionType.BASIC,
            scriptBlock().and(onStatementBeginning()),
            KeywordCompletionProvider(
                "public",
                "fun",
                "const",
                "use"
            )
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStatementBeginning()),
            KeywordCompletionProvider(
                *VISIBILITY_MODIFIERS,
                "native",
                "fun",
                "struct",
                "const",
                "use",
                "spec",
                "friend",
            )
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStatementBeginning("spec")),
            KeywordCompletionProvider("module", "fun", "schema")
        )
        extend(
            CompletionType.BASIC,
            function().with(MvPsiPatterns.AfterSibling(FUNCTION_VISIBILITY_MODIFIER)),
            KeywordCompletionProvider("fun")
        )
        extend(
            CompletionType.BASIC,
            function().with(MvPsiPatterns.AfterSibling(NATIVE)),
            KeywordCompletionProvider("fun")
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStatementBeginning("native")),
            KeywordCompletionProvider(*VISIBILITY_MODIFIERS, "fun", "struct")
        )
        extend(
            CompletionType.BASIC,
            codeStatement().and(onStatementBeginning()),
            KeywordCompletionProvider(
                "let",
                "loop",
                "if",
                "while",
                "abort",
                "return",
            )
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.or(
                psiElement()
                    .with(MvPsiPatterns.AfterSibling(FUNCTION_PARAMETER_LIST)),
                psiElement()
                    .with(MvPsiPatterns.AfterAnySibling(TYPES))
            ),
            KeywordCompletionProvider("acquires")
        )
    }

    private fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        psiElement(IDENTIFIER).and(MvPsiPatterns.onStatementBeginning(*startWords))

    companion object {
        private val VISIBILITY_MODIFIERS = arrayOf("public", "public(script)", "public(friend)")
    }


}
