package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.jetbrains.rd.util.remove
import org.move.cli.settings.moveSettings
import org.move.lang.MvElementTypes.*
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.MvPsiPattern.anySpecStart
import org.move.lang.core.MvPsiPattern.codeStatementPattern
import org.move.lang.core.MvPsiPattern.function
import org.move.lang.core.MvPsiPattern.identifierStatementBeginningPattern
import org.move.lang.core.MvPsiPattern.itemSpecStmt
import org.move.lang.core.MvPsiPattern.module
import org.move.lang.core.MvPsiPattern.moduleSpecBlock
import org.move.lang.core.MvPsiPattern.onStatementBeginning
import org.move.lang.core.MvPsiPattern.script
import org.move.lang.core.MvPsiPattern.toplevel
import org.move.lang.core.MvPsiPattern.typeParameter
import org.move.lang.core.TYPES
import org.move.lang.core.completion.providers.KeywordCompletionProvider

class KeywordCompletionContributor: CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            toplevel().and(identifierStatementBeginningPattern()),
            KeywordCompletionProvider("address", "module", "script", "spec")
        )
        extend(
            CompletionType.BASIC,
            script().and(identifierStatementBeginningPattern()),
            KeywordCompletionProvider(
                "public",
                "fun",
                "const",
                "use"
            )
        )
        extend(
            CompletionType.BASIC,
            module().and(identifierStatementBeginningPattern()),
            KeywordCompletionProvider(
                *(VIS_MODIFIERS.remove("public(script)")),
                *FUNCTION_MODIFIERS,
                "native",
                "fun",
                "struct",
                "const",
                "use",
                "spec",
                "friend",
                "enum",
            )
        )
        extend(
            CompletionType.BASIC,
            moduleSpecBlock().and(identifierStatementBeginningPattern()),
            KeywordCompletionProvider(
                "use",
                "spec",
            )
        )
        extend(
            CompletionType.BASIC,
            function().with(MvPsiPattern.AfterSibling(VISIBILITY_MODIFIER)),
            KeywordCompletionProvider("fun", *FUNCTION_MODIFIERS)
        )
        extend(
            CompletionType.BASIC,
            function().with(MvPsiPattern.AfterSibling(NATIVE)),
            KeywordCompletionProvider("fun")
        )
        extend(
            CompletionType.BASIC,
            module().and(identifierStatementBeginningPattern("native")),
            KeywordCompletionProvider(*VIS_MODIFIERS, "fun", "entry")
        )
        extend(
            CompletionType.BASIC,
            codeStatementPattern().and(identifierStatementBeginningPattern()),
            KeywordCompletionProvider(
                "let",
                "loop",
                "while",
                "continue",
                "break",
                "if",
                "else",
                "abort",
                "return",
                "for",
                "match",
            )
        )
        extend(
            CompletionType.BASIC,
            itemSpecStmt().and(identifierStatementBeginningPattern()),
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
                    .with(MvPsiPattern.AfterSibling(FUNCTION_PARAMETER_LIST)),
                psiElement()
                    .with(MvPsiPattern.AfterAnySibling(TYPES))
            ),
            KeywordCompletionProvider {
                buildList {
                    add("acquires")
                    if (it.moveSettings.enableResourceAccessControl) {
                        addAll(listOf("reads", "writes", "pure"))
                    }
                }
            }
        )
        extend(
            CompletionType.BASIC,
            typeParameter(),
            KeywordCompletionProvider("phantom")
        )
        extend(
            CompletionType.BASIC,
            anySpecStart(),
            KeywordCompletionProvider("module", "fun", "schema")
        )
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(
            parameters,
            CommonCompletionContributor.withSorter(parameters, result)
        )
    }
}

private val VIS_MODIFIERS = arrayOf(
    "public",
    "public(script)",
    "public(friend)",
    "public(package)"
)

private val FUNCTION_MODIFIERS = arrayOf("entry", "inline")

