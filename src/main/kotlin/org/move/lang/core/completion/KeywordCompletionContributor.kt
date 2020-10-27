package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.MoveElementTypes.STRUCT
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.MovePsiPatterns.acquiresPlacement
import org.move.lang.core.MovePsiPatterns.addressBlock
import org.move.lang.core.MovePsiPatterns.codeStatement
import org.move.lang.core.MovePsiPatterns.moduleBlock
import org.move.lang.core.MovePsiPatterns.scriptBlock
import org.move.lang.core.MovePsiPatterns.toplevel
import org.move.lang.core.MovePsiPatterns.typeParamBound
import org.move.lang.core.MovePsiPatterns.whitespace

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
                "public",
                "native",
                "fun",
                "resource",
                "struct",
                "const",
                "use",
                "spec",
            )
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStatementBeginning("spec")),
            KeywordCompletionProvider("fun", "struct", "module", "schema")
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStatementBeginning("resource")),
            KeywordCompletionProvider("struct")
        )
//        extend(
//            CompletionType.BASIC,
//            moduleBlock().and(
//                psiElement().beforeLeafSkipping(whitespace(), psiElement(STRUCT))),
//            KeywordCompletionProvider("struct")
//        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStatementBeginning("public")),
            KeywordCompletionProvider("fun")
        )
        extend(
            CompletionType.BASIC,
            moduleBlock().and(onStatementBeginning("native")),
            KeywordCompletionProvider("public", "fun")
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
            typeParamBound(),
            KeywordCompletionProvider(
                "copyable",
                "resource",
            )
        )
        extend(
            CompletionType.BASIC,
            acquiresPlacement(),
            KeywordCompletionProvider(
                "acquires",
            )
        )
    }

    private fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        psiElement(IDENTIFIER).and(MovePsiPatterns.onStatementBeginning(*startWords))

//    private fun newCodeStatement(): PsiElementPattern.Capture<PsiElement> =
//        psiElement().inside(psiElement<MoveCodeBlock>())

//    private fun letStatement(): PsiElementPattern.Capture<PsiElement> =
//        baseCodeStatement().and(onStatementBeginning("let"))

//
//    private fun scriptBodyPattern(): PsiElementPattern.Capture<PsiElement> =
//        psiElement()
//            .withSuperParent(2, psiElement<MoveScriptDefBlock>())
//
//    private fun functionBodyPattern(): PsiElementPattern.Capture<PsiElement> =
//        psiElement()
//            .withSuperParent(2, psiElement<MoveBlock>())
//            .and(psiElement().withSuperParent(3, psiElement<MoveFunctionDef>()))

//    private fun statementBeginningPattern(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement(IDENTIFIER)
//            .and(MovePsiPatterns.onStatementBeginning(*startWords))
//
//    private fun functionDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .withParent(psiElement<MoveBlock>())
//
//    private fun moduleDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .inside(psiElement<MoveModuleDef>())
//            .andNot(functionDeclarationPattern())
//
//    private fun scriptDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .inside(psiElement<MoveScriptDef>())
//            .andNot(functionDeclarationPattern())
//
//    private fun addressDeclarationPattern(): PsiElementPattern.Capture<PsiElement> =
//        PlatformPatterns.psiElement()
//            .inside(psiElement<MoveAddressDef>())
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