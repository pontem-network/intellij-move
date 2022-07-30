package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.*
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.MvPsiPatterns.addressBlock
import org.move.lang.core.MvPsiPatterns.codeStmt
import org.move.lang.core.MvPsiPatterns.function
import org.move.lang.core.MvPsiPatterns.itemSpecLabel
import org.move.lang.core.MvPsiPatterns.moduleBlock
import org.move.lang.core.MvPsiPatterns.scriptBlock
import org.move.lang.core.MvPsiPatterns.specStmt
import org.move.lang.core.MvPsiPatterns.toplevel
import org.move.lang.core.MvPsiPatterns.typeParameter
import org.move.lang.core.TYPES
import org.move.lang.core.completion.providers.KeywordCompletionProvider

class KeywordCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            toplevel().and(onStmtBeginning()),
            KeywordCompletionProvider("address", "module", "script")
        )
        extend(
            CompletionType.BASIC,
            addressBlock().and(onStmtBeginning()),
            KeywordCompletionProvider("module")
        )
        extend(
            CompletionType.BASIC,
            scriptBlock().and(onStmtBeginning()),
            KeywordCompletionProvider(
                "public",
                "fun",
                "const",
                "use"
            )
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStmtBeginning()),
            KeywordCompletionProvider(
                *VISIBILITY_MODIFIERS,
                "native",
                "entry",
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
            function().with(MvPsiPatterns.AfterSibling(FUNCTION_VISIBILITY_MODIFIER)),
            KeywordCompletionProvider("fun")
        )
        extend(
            CompletionType.BASIC,
            function().with(MvPsiPatterns.AfterSibling(FUNCTION_VISIBILITY_MODIFIER)),
            KeywordCompletionProvider("entry")
        )
        extend(
            CompletionType.BASIC,
            function().with(MvPsiPatterns.AfterSibling(NATIVE)),
            KeywordCompletionProvider("fun")
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStmtBeginning("native")),
            KeywordCompletionProvider(*VISIBILITY_MODIFIERS, "fun", "struct")
        )
        extend(
            CompletionType.BASIC,
            codeStmt().and(onStmtBeginning()),
            KeywordCompletionProvider(
                "let",
                "loop",
                "if",
                "while",
                "abort",
                "return",
                "continue",
                "break",
                "else"
            )
        )
        extend(
            CompletionType.BASIC,
            specStmt().and(onStmtBeginning()),
            KeywordCompletionProvider(
                "pragma",
                "let",
                "use",
                "include",
                "apply",
                "requires",
                "ensures",
                "invariant",
                "modifies",
                "aborts_if",
                "aborts_with",
                "assume",
                "assert",
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
        extend(
            CompletionType.BASIC,
            typeParameter(),
            KeywordCompletionProvider("phantom")
        )
        extend(
            CompletionType.BASIC,
            itemSpecLabel(),
            KeywordCompletionProvider("module", "fun", "schema")
        )
    }

    private fun onStmtBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        psiElement(IDENTIFIER).and(MvPsiPatterns.onStmtBeginning(*startWords))

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(
            parameters,
            CommonCompletionContributor.withSorter(parameters, result)
        )
    }

    companion object {
        private val VISIBILITY_MODIFIERS = arrayOf("public", "public(script)", "public(friend)")
    }
}
