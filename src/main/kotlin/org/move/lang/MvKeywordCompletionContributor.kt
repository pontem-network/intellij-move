package org.move.lang

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.psi.MvAddressDef
import org.move.lang.core.psi.MvFunctionDef
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvScriptDef
import org.move.lang.core.psiElement

class MvKeywordCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(
            CompletionType.BASIC,
            functionDeclarationPattern(),
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
            fileDeclarationPattern(),
            MvKeywordCompletionProvider("address", "module", "script")
        )
        extend(
            CompletionType.BASIC,
            moduleDeclarationPattern(),
            MvKeywordCompletionProvider(
                "public",
                "native",
                "fun",
                "resource",
                "struct",
                "const",
                "use"
            )
        )
        extend(
            CompletionType.BASIC,
            scriptDeclarationPattern(),
            MvKeywordCompletionProvider("public", "fun", "const", "use")
        )
    }

    private fun statementBeginningPattern(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(IDENTIFIER)
            .and(MvPsiPattern.onStatementBeginning(*startWords))

    private fun functionDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .inside(psiElement<MvFunctionDef>())

    private fun moduleDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .inside(psiElement<MvModuleDef>())
            .andNot(functionDeclarationPattern())

    private fun scriptDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .inside(psiElement<MvScriptDef>())
            .andNot(functionDeclarationPattern())

    private fun addressDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .inside(psiElement<MvAddressDef>())
            .andNot(moduleDeclarationPattern())
            .andNot(scriptDeclarationPattern())
            .andNot(functionDeclarationPattern())

    private fun fileDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .andNot(scriptDeclarationPattern())
            .andNot(moduleDeclarationPattern())
            .andNot(addressDeclarationPattern())
            .andNot(functionDeclarationPattern())


}