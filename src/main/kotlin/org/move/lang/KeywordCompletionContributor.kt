package org.move.lang

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.MovePatterns
import org.move.lang.core.psi.MvAddressDefBlock
import org.move.lang.core.psi.MvCodeBlock
import org.move.lang.core.psi.MvModuleDefBlock
import org.move.lang.core.psi.MvScriptDefBlock
import org.move.lang.core.psiElement

class KeywordCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            rootDefinition().and(onStatementBeginning()),
            KeywordCompletionProvider("address", "module", "script")
        )
        extend(
            CompletionType.BASIC,
            addressBlockDefinition().and(onStatementBeginning()),
            KeywordCompletionProvider("module")
        )
        extend(
            CompletionType.BASIC,
            moduleBlockDefinition().and(onStatementBeginning()),
            KeywordCompletionProvider(
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
            scriptBlockDefinition().and(onStatementBeginning()),
            KeywordCompletionProvider("public", "fun", "const", "use")
        )
        extend(
            CompletionType.BASIC,
            baseCodeStatement().and(onStatementBeginning()),
            KeywordCompletionProvider(
                "let",
                "loop",
                "if",
                "while",
                "abort",
                "return",
                "copy",
                "move"
            )
        )
        extend(
            CompletionType.BASIC,
            baseCodeStatement().and(onStatementBeginning("let")),
            KeywordCompletionProvider(
                "mut"
            )
        )
    }

    private fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        psiElement(IDENTIFIER).and(MovePatterns.onStatementBeginning(*startWords))

    private fun rootDefinition(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvFile>()

    private fun addressBlockDefinition(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvAddressDefBlock>()

    private fun moduleBlockDefinition(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvModuleDefBlock>()

    private fun scriptBlockDefinition(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvScriptDefBlock>()

    private fun baseCodeStatement(): PsiElementPattern.Capture<PsiElement> =
        psiElement().inside(psiElement<MvCodeBlock>())

//    private fun newCodeStatement(): PsiElementPattern.Capture<PsiElement> =
//        psiElement().inside(psiElement<MvCodeBlock>())

//    private fun letStatement(): PsiElementPattern.Capture<PsiElement> =
//        baseCodeStatement().and(onStatementBeginning("let"))

    private inline fun <reified I : PsiElement> psiElementWithParent() = psiElement().withParent(
        or(
            psiElement<I>(),
            psiElement<PsiErrorElement>().withParent(psiElement<I>())
        )
    )

//
//    private fun scriptBodyPattern(): PsiElementPattern.Capture<PsiElement> =
//        psiElement()
//            .withSuperParent(2, psiElement<MvScriptDefBlock>())
//
//    private fun functionBodyPattern(): PsiElementPattern.Capture<PsiElement> =
//        psiElement()
//            .withSuperParent(2, psiElement<MvBlock>())
//            .and(psiElement().withSuperParent(3, psiElement<MvFunctionDef>()))

//    private fun statementBeginningPattern(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement(IDENTIFIER)
//            .and(MovePsiPatterns.onStatementBeginning(*startWords))
//
//    private fun functionDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .withParent(psiElement<MvBlock>())
//
//    private fun moduleDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .inside(psiElement<MvModuleDef>())
//            .andNot(functionDeclarationPattern())
//
//    private fun scriptDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .inside(psiElement<MvScriptDef>())
//            .andNot(functionDeclarationPattern())
//
//    private fun addressDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .inside(psiElement<MvAddressDef>())
//            .andNot(moduleDeclarationPattern())
//            .andNot(scriptDeclarationPattern())
//            .andNot(functionDeclarationPattern())
//
//    private fun fileDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .andNot(scriptDeclarationPattern())
//            .andNot(moduleDeclarationPattern())
//            .andNot(addressDeclarationPattern())
//            .andNot(functionDeclarationPattern())


}