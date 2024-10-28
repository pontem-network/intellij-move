package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.containers.addAllIfNotNull
import org.move.cli.settings.moveSettings
import org.move.lang.MvElementTypes.FUNCTION_PARAMETER_LIST
import org.move.lang.MvElementTypes.LABEL_DECL
import org.move.lang.core.FUNCTION_MODIFIERS
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.MvPsiPattern.afterAnySibling
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
import org.move.lang.core.completion.providers.FunctionModifierCompletionProvider
import org.move.lang.core.completion.providers.KeywordCompletionProvider
import org.move.lang.core.tokenSetOf

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
            KeywordCompletionProvider { project ->
                buildList {
                    addAllIfNotNull(
                        *VISIBILITY_MODIFIERS,
                        *CONTEXT_FUNCTION_MODIFIERS,
                        "native",
                        "fun",
                        "struct",
                        "const",
                        "use",
                        "spec",
                        "friend",
                    )
                    if (project.moveSettings.enableMove2) {
                        add("enum")
                    }
                }
            }
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
            function().with(afterAnySibling(FUNCTION_MODIFIERS)),
            FunctionModifierCompletionProvider()
        )
        extend(
            CompletionType.BASIC,
            function().with(afterAnySibling(FUNCTION_MODIFIERS)),
            KeywordCompletionProvider("fun")
        )
//        extend(
//            CompletionType.BASIC,
//            function().with(MvPsiPattern.AfterSibling(NATIVE)),
//            FunctionModifierCompletionProvider()
//        )
//        extend(
//            CompletionType.BASIC,
//            module().and(identifierStatementBeginningPattern("native")),
//            KeywordCompletionProvider(*VISIBILITY_MODIFIERS, "fun", "entry")
//        )
        extend(
            CompletionType.BASIC,
            codeStatementPattern().and(identifierStatementBeginningPattern()),
            KeywordCompletionProvider { project ->
                buildList {
                    addAllIfNotNull(
                        "let",
                        "loop",
                        "while",
                        "if",
                        "else",
                        "abort",
                        "return",
                        "for",
                    )
                    if (project.moveSettings.enableMove2) {
                        add("match")
                    }
                }
            }
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
            KeywordCompletionProvider("acquires")
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
        extend(
            CompletionType.BASIC,
            MvPsiPattern.inAnyLoop,
//                .and(
//
//                    codeStatementPattern().and(identifierStatementBeginningPattern())
//                ),
            KeywordCompletionProvider("break", "continue")
        )
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(
            parameters,
            CommonCompletionContributor.withSorter(parameters, result)
        )
    }
}

private val VISIBILITY_MODIFIERS = arrayOf(
    "public",
//    "public(script)",
    "public(friend)",
    "public(package)",
    "friend",
    "package",
)

private val CONTEXT_FUNCTION_MODIFIERS = arrayOf("entry", "inline")

