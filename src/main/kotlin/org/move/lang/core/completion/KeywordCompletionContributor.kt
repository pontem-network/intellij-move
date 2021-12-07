package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.MvPsiPatterns.acquiresPlacement
import org.move.lang.core.MvPsiPatterns.addressBlock
import org.move.lang.core.MvPsiPatterns.afterSibling
import org.move.lang.core.MvPsiPatterns.codeStatement
import org.move.lang.core.MvPsiPatterns.functionDef
import org.move.lang.core.MvPsiPatterns.moduleBlock
import org.move.lang.core.MvPsiPatterns.nativeFunctionDef
import org.move.lang.core.MvPsiPatterns.scriptBlock
import org.move.lang.core.MvPsiPatterns.toplevel
import org.move.lang.core.psi.MvFunctionVisibilityModifier

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
                "resource",
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
            functionDef().and(afterSibling<MvFunctionVisibilityModifier>()),
            KeywordCompletionProvider("fun")
        )
        extend(
            CompletionType.BASIC,
            nativeFunctionDef().and(afterSibling<MvFunctionVisibilityModifier>()),
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
//        extend(
//            CompletionType.BASIC,
//            structTrait(),
//            KeywordCompletionProvider(
//                *STRUCT_TRAITS,
//                addWhitespaceAfter = false
//            )
//        )
        extend(
            CompletionType.BASIC,
            acquiresPlacement(),
            KeywordCompletionProvider(
                "acquires",
            )
        )
    }

    private fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        psiElement(IDENTIFIER).and(MvPsiPatterns.onStatementBeginning(*startWords))

//    private fun newCodeStatement(): PsiElementPattern.Capture<PsiElement> =
//        psiElement().inside(psiElement<MvCodeBlock>())

//    private fun letStatement(): PsiElementPattern.Capture<PsiElement> =
//        baseCodeStatement().and(onStatementBeginning("let"))

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
//            .and(MvPsiPatterns.onStatementBeginning(*startWords))
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
    companion object {
        private val VISIBILITY_MODIFIERS = arrayOf("public", "public(script)", "public(friend)")
        private val STRUCT_TRAITS = arrayOf("copy", "drop", "store", "key")
    }


}
