package org.move.lang.core

import com.intellij.patterns.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext
import org.move.lang.MoveElementTypes.*
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.leftLeaves

object MovePsiPatterns {
    private val STATEMENT_BOUNDARIES = TokenSet.create(SEMICOLON, L_BRACE, R_BRACE)

    val whitespace: PsiElementPattern.Capture<PsiElement> = PlatformPatterns.psiElement().whitespace()

    val onStatementBeginning: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().with(OnStatementBeginning())

    fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().with(OnStatementBeginning(*startWords))

    fun toplevel(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveFile>()

    fun addressBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveAddressBlock>()

    fun moduleBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveModuleBlock>()

    fun functionDef(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveFunctionDef>()

    fun nativeFunctionDef(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveNativeFunctionDef>()

    fun scriptBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveScriptBlock>()

    fun codeStatement(): PsiElementPattern.Capture<PsiElement> =
        psiElementInside<MoveCodeBlock>()

    fun namedAddress(): PsiElementPattern.Capture<MoveNamedAddress> {
        return psiElement()
    }

    inline fun <reified I : PsiElement> afterSibling(): ElementPattern<PsiElement> {
        val afterSibling = PlatformPatterns
            .psiElement()
            .afterSiblingSkipping(whitespace(), psiElement<I>())
        val afterSiblingWithError = PlatformPatterns
            .psiElement()
            .withParent(psiElement<PsiErrorElement>().afterSiblingSkipping(whitespace(), psiElement<I>()))
        return PlatformPatterns.or(afterSibling, afterSiblingWithError)
//        val errorIdentifier = PlatformPatterns
//            .psiElement()
//            .withParent(PsiErrorElement::class.java)
//        return errorIdentifier.afterSiblingSkipping(whitespace(), psiElement<I>())
//        val identifier = PlatformPatterns.psiElement()
//        val identifierOrErrorIdentifier = StandardPatterns.or(identifier, errorIdentifier)

//        StandardPatterns.or(
//            psiElement<I>(),
//            psiElement<PsiErrorElement>().withParent(psiElement<I>())
//        )
//        PlatformPatterns.psiElement().withParent(
//            StandardPatterns.or(
//                psiElement<I>(),
//                psiElement<PsiErrorElement>().withParent(psiElement<I>())
//            )
//        )
    }

    fun acquiresPlacement(): ElementPattern<PsiElement> {
//        return psiElementWithParent<MoveFunctionSignature>()
//            .and(
        return PlatformPatterns.or(
            psiElementWithParent<MoveFunctionSignature>()
                .and(afterSibling<MoveFunctionParameterList>()),
            psiElementWithParent<MoveReturnType>()
        )
//        return psiElementWithParent<MoveFunctionSignature>()
//            .and(
//                PlatformPatterns.or(
//                    afterSibling<MoveFunctionParameterList>(),
//                    afterSibling<MoveReturnType>()
//                ))
//               return PlatformPatterns.psiElement().beforeLeafSkipping(
//                    whitespace(), PlatformPatterns.psiElement(MoveCodeBlock::class.java))
//            )
//        return PlatformPatterns
//            .psiElement()
//            .withParent(MoveFunctionSignature::class.java)
//            .and(psiElementAfterSiblingSkipping<MoveFunctionParameterList>(whitespace()))
//        return psiElement<PsiElement>()
//            .and(psiElementAfterSiblingSkipping<MoveFunctionParameterList>(whitespace()))
//        return PlatformPatterns
//            .and(
//                psiElementAfterSiblingSkipping<MoveFunctionParameterList>(whitespace())
//            )
//        return psiElementWithParent<MoveFunctionSignature>()
//            .and(
//                psiElementAfterSiblingSkipping<MoveFunctionParameterList>(
//                    PlatformPatterns.or(whitespace(), psiElement<MoveReturnType>())
//                )
//            )

    }

    fun typeParamBound(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveTypeParameter>()
            .afterLeafSkipping(
                whitespace(),
                PlatformPatterns.psiElement(COLON),
            )

    fun ability(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveAbility>()

    fun pathIdent(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .withParent<MovePathIdent>()
//            .withSuperParent<MovePath>(2)

    fun path(): PsiElementPattern.Capture<PsiElement> =
        pathIdent()
            .withSuperParent<MovePath>(2)

    fun pathType(): PsiElementPattern.Capture<PsiElement> =
        pathIdent()
            .withSuperParent<MovePathType>(3)

    fun nameTypeIdentifier(): PsiElementPattern.Capture<PsiElement> =
        pathType()
            .withCond("FirstChild") { it.prevSibling == null }

    fun specIdentifier(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent<MoveSpecDef>(2)

    fun whitespace() = PlatformPatterns.psiElement().whitespaceCommentEmptyOrError()

    inline fun <reified I : PsiElement> psiElementWithParent() =
        PlatformPatterns.psiElement().withParent(
            StandardPatterns.or(
                psiElement<I>(),
                psiElement<PsiErrorElement>().withParent(psiElement<I>())
            )
        )

    private inline fun <reified I : PsiElement> psiElementAfterSiblingSkipping(
        skip: ElementPattern<*>,
    ) =
        StandardPatterns.or(
            PlatformPatterns.psiElement()
                .afterSiblingSkipping(skip, psiElement<I>()),
            PlatformPatterns.psiElement()
                .withParent(psiElement<PsiErrorElement>().afterSiblingSkipping(skip, psiElement<I>()))
        )

//    private inline fun <reified I : PsiElement> beforeLeaf() =
//        StandardPatterns.or(
//            PlatformPatterns.psiElement().be
//            PlatformPatterns.psiElement()
//                .withParent(psiElement<PsiErrorElement>().afterSiblingSkipping(skip, psiElement<I>()))
//        )

    inline fun <reified I : PsiElement> psiElementInside() =
        PlatformPatterns.psiElement().inside(
            StandardPatterns.or(
                psiElement<I>(),
                psiElement<PsiErrorElement>().withParent(psiElement<I>())
            )
        )

    private class OnStatementBeginning(vararg startWords: String) :
        PatternCondition<PsiElement>("on statement beginning") {
        val myStartWords = startWords
        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            val prev = t.prevVisibleOrNewLine
            return if (myStartWords.isEmpty())
                prev == null || prev is PsiWhiteSpace || prev.node.elementType in STATEMENT_BOUNDARIES
            else {
                prev != null && prev.node.text in myStartWords
            }
        }
    }
}

private val PsiElement.prevVisibleOrNewLine: PsiElement?
    get() {
        return leftLeaves
            .filterNot { it is PsiComment || it is PsiErrorElement }
            .filter { it !is PsiWhiteSpace || it.textContains('\n') }
            .firstOrNull()

    }

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
    return PlatformPatterns.psiElement(I::class.java)
}

inline fun <reified I : PsiElement> PsiElementPattern.Capture<PsiElement>.withParent(): PsiElementPattern.Capture<PsiElement> {
    return this.withSuperParent(1, I::class.java)
}

inline fun <reified I : PsiElement> PsiElementPattern.Capture<PsiElement>.withSuperParent(level: Int): PsiElementPattern.Capture<PsiElement> {
    return this.withSuperParent(level, I::class.java)
}

fun <T, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.withCond(name: String, cond: (T) -> Boolean): Self =
    with(object : PatternCondition<T>(name) {
        override fun accepts(t: T, context: ProcessingContext?): Boolean = cond(t)
    })

fun <T, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.withCondContext(name: String, cond: (T, ProcessingContext?) -> Boolean): Self =
    with(object : PatternCondition<T>(name) {
        override fun accepts(t: T, context: ProcessingContext?): Boolean = cond(t, context)
    })
