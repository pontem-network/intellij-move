package org.move.lang.core

import com.intellij.patterns.*
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.or
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
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.hasColonColon
import org.move.lang.core.psi.ext.leftLeaves

object MvPsiPattern {
    private val STMT_BOUNDARIES = TokenSet.create(SEMICOLON, L_BRACE, R_BRACE)

//    val whitespace: PsiElementPattern.Capture<PsiElement> = PlatformPatterns.psiElement().whitespace()

    fun identifierStatementBeginningPattern(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        psiElement(IDENTIFIER).and(onStatementBeginning(*startWords))

//    fun quoteIdentifierStatementBeginning(): PsiElementPattern.Capture<PsiElement> =
//        psiElement(QUOTE_IDENTIFIER).and(onStatementBeginning())

//    val onStatementBeginning: PsiElementPattern.Capture<PsiElement> =
//        psiElement().with(OnStatementBeginning())

    fun onStatementBeginning(vararg startWords: String): PsiElementPattern.Capture<PsiElement> =
        psiElement().with(OnStatementBeginning(*startWords))

    fun toplevel(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MoveFile>()

//    fun moduleChildElement(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvModule>()

    fun module(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvModule>()

    fun function(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvFunction>()

    fun script(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvScript>()

    fun moduleSpecBlock(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvModuleSpecBlock>()

    fun codeStatementPattern(): PsiElementPattern.Capture<PsiElement> =
        psiElement()
            .inside(psiElement<MvCodeBlock>())
            .andNot(psiElement().withParent(MvModule::class.java))
            .andNot(psiElement().withSuperParent(3, MvStructLitExpr::class.java))
            // let S { field } = 1
            //  STRUCT_PAT[FIELD_PAT[BINDING[IDENTIFIER]]]
            //  ^ 3         ^ 2           ^ 1       ^ 0
            .andNot(psiElement().withSuperParent(3, MvPatStruct::class.java))

    fun anySpecStart() = psiElementInside<MvItemSpec>().and(onStatementBeginning("spec"))

    fun itemSpecStmt(): PsiElementPattern.Capture<PsiElement> = psiElementInside<MvSpecCodeBlock>()

    fun itemSpecRef(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvItemSpecRef>()

    fun bindingPat(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvPatBinding>()

    fun namedAddress(): PsiElementPattern.Capture<MvNamedAddress> = psiElement<MvNamedAddress>()

    fun typeParameter(): PsiElementPattern.Capture<PsiElement> =
        psiElementWithParent<MvTypeParameter>()

    fun typeParamBound(): PsiElementPattern.Capture<PsiElement> =
        typeParameter()
            .afterLeafSkipping(
                whitespaceAndErrors(),
                psiElement(COLON),
            )

    fun ability(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvAbility>()

    fun path(): PsiElementPattern.Capture<PsiElement> = psiElementWithParent<MvPath>()

    fun pathExpr(): PsiElementPattern.Capture<PsiElement> =
        path()
            .withSuperParent(2, MvPathExpr::class.java)

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
        psiElement()
            .withParent<MvItemSpecRef>()

    private fun whitespaceAndErrors() = psiElement().whitespaceCommentEmptyOrError()

    inline fun <reified I: PsiElement> psiElementWithParent() =
        psiElement()
            .withParent(
                or(psiElement<I>(), psiElement<PsiErrorElement>().withParent(psiElement<I>()))
            )

    inline fun <reified I: PsiElement> psiElementAfterSiblingSkipping(
        skip: ElementPattern<*>,
    ) =
        or(
            psiElement().afterSiblingSkipping(skip, psiElement<I>()),
            psiElement()
                .withParent(psiElement<PsiErrorElement>().afterSiblingSkipping(skip, psiElement<I>()))
        )

    inline fun <reified I: PsiElement> psiElementInside(): PsiElementPattern.Capture<PsiElement> =
        psiElement().inside(
            or(
                psiElement<I>(),
                psiElement<PsiErrorElement>().withParent(psiElement<I>())
            )
        )

    val simplePathPattern: PsiElementPattern.Capture<PsiElement>
        get() {
            val simplePath = psiElement<MvPath>()
                .with(object: PatternCondition<MvPath>("SimplePath") {
                    override fun accepts(path: MvPath, context: ProcessingContext?): Boolean =
                        path.pathAddress == null &&
                                path.path == null &&
//                                path.typeQual == null &&
                                !path.hasColonColon &&
                                path.ancestorStrict<MvUseSpeck>() == null
                })
            return psiElement().withParent(simplePath)
        }

    val inAnyLoop: PsiElementPattern.Capture<PsiElement> =
        psiElement().inside(
            true,
            psiElement<MvCodeBlock>().withParent(
                or(
                    psiElement<MvForExpr>(),
                    psiElement<MvLoopExpr>(),
                    psiElement<MvWhileExpr>()
                )
            ),
            psiElement<MvLambdaExpr>()
        )

    class AfterSibling(val sibling: IElementType, val withPossibleError: Boolean = true):
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

    fun afterAnySibling(siblings: TokenSet, withPossibleError: Boolean = true) =
        AfterAnySibling(siblings, withPossibleError)

    class AfterAnySibling(val siblings: TokenSet, val withPossibleError: Boolean = true):
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

    private class OnStatementBeginning(
        vararg startWords: String
    ): PatternCondition<PsiElement>("on statement beginning") {
        val myStartWords = startWords
        override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
            val prev = t.prevVisibleOrNewLine
            return if (myStartWords.isEmpty()) {
                val onBoundary =
                    prev == null || prev is PsiWhiteSpace || prev.node.elementType in STMT_BOUNDARIES
//                onBoundary &&
//                        !(t.hasAncestorOrSelf<MvStructPat>() || t.hasAncestorOrSelf<MvStructLitExpr>())
                onBoundary
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

inline fun <reified I: PsiElement> psiElement(): PsiElementPattern.Capture<I> {
    return psiElement(I::class.java)
}

inline fun <reified I: PsiElement> PsiElementPattern.Capture<PsiElement>.withParent(): PsiElementPattern.Capture<PsiElement> {
    return this.withSuperParent(1, I::class.java)
}

inline fun <reified I: PsiElement> PsiElementPattern.Capture<PsiElement>.withSuperParent(level: Int): PsiElementPattern.Capture<PsiElement> {
    return this.withSuperParent(level, I::class.java)
}

fun <T: Any, Self: ObjectPattern<T, Self>> ObjectPattern<T, Self>.withCond(
    name: String,
    cond: (T) -> Boolean
): Self =
    with(object: PatternCondition<T>(name) {
        override fun accepts(t: T, context: ProcessingContext?): Boolean = cond(t)
    })
