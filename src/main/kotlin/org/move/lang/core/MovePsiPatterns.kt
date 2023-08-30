package org.move.lang.core

import com.intellij.patterns.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext
import org.move.lang.MoveFile
import org.move.lang.MvElementTypes.*
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.hasAncestorOrSelf
import org.move.lang.core.psi.ext.leftLeaves

object MvPsiPatterns {
    private val STMT_BOUNDARIES = TokenSet.create(SEMICOLON, L_BRACE, R_BRACE)

    val whitespace: PsiElementPattern.Capture<PsiElement> = PlatformPatterns.psiElement().whitespace()

    val onStmtBeginning: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().with(OnStmtBeginning())

    fun onStmtBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().with(OnStmtBeginning(*startWords))

    fun toplevel(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MoveFile>()

    fun addressBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvAddressBlock>()

    fun moduleBlock(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvModuleBlock>()

    fun function(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvFunction>()

    fun scriptBlock(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvScriptBlock>()

    fun moduleSpecBlock(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvModuleSpecBlock>()

    fun codeStmt(): PsiElementPattern.Capture<PsiElement> = psiElementInside<MvCodeBlock>()

    fun anySpecStart() = psiElementInside<MvItemSpec>().and(onStmtBeginning("spec"))

    fun itemSpecStmt(): PsiElementPattern.Capture<PsiElement> = psiElementInside<MvSpecCodeBlock>()

    fun itemSpecRef(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvItemSpecRef>()

    fun bindingPat(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvBindingPat>()

    fun namedAddress(): PsiElementPattern.Capture<MvNamedAddress> {
        return psiElement()
    }

    fun typeParameter(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvTypeParameter>()

    fun typeParamBound(): PsiElementPattern.Capture<PsiElement> =
        typeParameter()
            .afterLeafSkipping(
                whitespaceAndErrors(),
                PlatformPatterns.psiElement(COLON),
            )

    fun ability(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvAbility>()

    fun path(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvPath>()

    fun refExpr(): PsiElementPattern.Capture<PsiElement> =
        path()
            .withSuperParent(2, MvRefExpr::class.java)

    fun pathType(): PsiElementPattern.Capture<PsiElement> =
        path()
            .withSuperParent<MvPathType>(2)

    fun schemaLit(): PsiElementPattern.Capture<PsiElement> =
        path()
            .withSuperParent<MvSchemaLit>(2)

    fun pathInsideIncludeStmt(): PsiElementPattern.Capture<PsiElement> =
        path()
            .withAncestor(5, psiElement<MvIncludeStmt>())

    fun nameTypeIdentifier(): PsiElementPattern.Capture<PsiElement> =
        pathType()
            .withCond("FirstChild") { it.prevSibling == null }

    fun specIdentifier(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement()
            .withParent<MvItemSpecRef>()

    private fun whitespaceAndErrors() = PlatformPatterns.psiElement().whitespaceCommentEmptyOrError()

    inline fun <reified I : PsiElement> psiElementWithParent() =
        PlatformPatterns.psiElement().withParent(
            StandardPatterns.or(
                psiElement<I>(),
                psiElement<PsiErrorElement>().withParent(psiElement<I>())
            )
        )

    inline fun <reified I : PsiElement> psiElementAfterSiblingSkipping(
        skip: ElementPattern<*>,
    ) =
        StandardPatterns.or(
            PlatformPatterns.psiElement()
                .afterSiblingSkipping(skip, psiElement<I>()),
            PlatformPatterns.psiElement()
                .withParent(psiElement<PsiErrorElement>().afterSiblingSkipping(skip, psiElement<I>()))
        )

    inline fun <reified I : PsiElement> psiElementInside(): PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().inside(
            StandardPatterns.or(
                psiElement<I>(),
                psiElement<PsiErrorElement>().withParent(psiElement<I>())
            )
        )

    class AfterSibling(val sibling: IElementType, val withPossibleError: Boolean = true) :
        PatternCondition<PsiElement>("afterSiblingKeywords") {
        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            var element = t
            if (withPossibleError) {
                if (element.parent is PsiErrorElement) element = element.parent
            }
            var prevSibling = element.prevSibling
            while (prevSibling != null) {
                if (prevSibling.elementType == sibling) {
                    return true
                }
                prevSibling = prevSibling.prevSibling
            }
            return false
        }
    }

    class AfterAnySibling(val siblings: TokenSet, val withPossibleError: Boolean = true) :
        PatternCondition<PsiElement>("afterSiblingKeywords") {
        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            var element = t
            if (withPossibleError) {
                if (element.parent is PsiErrorElement) element = element.parent
            }
            var prevSibling = element.prevSibling
            while (prevSibling != null) {
                if (prevSibling.elementType in siblings) {
                    return true
                }
                prevSibling = prevSibling.prevSibling
            }
            return false
        }
    }

    private class OnStmtBeginning(
        vararg startWords: String
    ) : PatternCondition<PsiElement>("on statement beginning") {
        val myStartWords = startWords
        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            val prev = t.prevVisibleOrNewLine
            return if (myStartWords.isEmpty()) {
                val onBoundary =
                    prev == null || prev is PsiWhiteSpace || prev.node.elementType in STMT_BOUNDARIES
                onBoundary &&
                        !(t.hasAncestorOrSelf<MvStructPat>() || t.hasAncestorOrSelf<MvStructLitExpr>())
            } else {
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

fun <T : Any, Self : ObjectPattern<T, Self>> ObjectPattern<T, Self>.withCond(
    name: String,
    cond: (T) -> Boolean
): Self =
    with(object : PatternCondition<T>(name) {
        override fun accepts(t: T, context: ProcessingContext?): Boolean = cond(t)
    })
