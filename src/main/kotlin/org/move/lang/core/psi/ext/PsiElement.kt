package org.move.lang.core.psi.ext

import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import org.move.lang.core.psi.*

fun <T> T?.wrapWithList(): List<T> = this?.let { listOf(it) }.orEmpty()
fun <T> T?.wrapWithMutableList(): MutableList<T> = this?.let { listOf(it) }.orEmpty().toMutableList()

fun PsiElement.hasChild(tokenType: IElementType): Boolean = childrenByType(tokenType).toList().isNotEmpty()

fun PsiElement.getChild(tokenType: IElementType): PsiElement? = childrenByType(tokenType).firstOrNull()

inline fun <reified T : PsiElement> PsiElement.childOfType(): T? =
    PsiTreeUtil.getChildOfType(this, T::class.java)

inline fun <reified T : PsiElement> PsiElement.childrenOfType(): List<T> =
    PsiTreeUtil.getChildrenOfTypeAsList(this, T::class.java)

inline fun <reified T : PsiElement> PsiElement.stubChildrenOfType(): List<T> {
    return if (this is PsiFileImpl) {
        stub?.childrenStubs?.mapNotNull { it.psi as? T } ?: return childrenOfType()
    } else {
        PsiTreeUtil.getStubChildrenOfTypeAsList(this, T::class.java)
    }
}

val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.parent
    }

fun PsiElement.findFirstParent(strict: Boolean = true, cond: Condition<in PsiElement>) =
    PsiTreeUtil.findFirstParent(this, strict, cond)

inline fun <reified T : PsiElement> PsiElement.ancestorStrict(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, true)

inline fun <reified T : PsiElement> PsiElement.ancestorStrict(stopAt: Class<out PsiElement>): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, true, stopAt)

inline fun <reified T : PsiElement> PsiElement.ancestorOrSelf(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, false)

fun <T : PsiElement> PsiElement.ancestorOfClass(psiClass: Class<T>, strict: Boolean = false): T? =
    PsiTreeUtil.getParentOfType(this, psiClass, strict)

inline fun <reified T : PsiElement> PsiElement.hasAncestor(): Boolean =
    ancestorStrict<T>() != null

inline fun <reified T : PsiElement> PsiElement.hasAncestorOrSelf(): Boolean =
    ancestorOrSelf<T>() != null

inline fun <reified T : PsiElement> PsiElement.ancestorOrSelf(stopAt: Class<out PsiElement>): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, false, stopAt)

inline fun <reified T : PsiElement> PsiElement.stubAncestorStrict(): T? =
    PsiTreeUtil.getStubOrPsiParentOfType(this, T::class.java)

/**
 * Extracts node's element type
 */
val PsiElement.elementType: IElementType
    // XXX: be careful not to switch to AST
//    get() = if (this is RsFile) RsFileStub.Type else PsiUtilCore.getElementType(this)
    get() = PsiUtilCore.getElementType(this)

/**
 * Checks whether this node contains [descendant] one
 */
fun PsiElement.contains(descendant: PsiElement?): Boolean {
    if (descendant == null) return false
    return descendant.ancestors.any { it === this }
}

fun PsiElement.superParent(level: Int): PsiElement? {
    require(level > 0)
    return ancestors.drop(level).firstOrNull()
}

val PsiElement.ancestorPairs: Sequence<Pair<PsiElement, PsiElement>>
    get() {
        val parent = this.parent ?: return emptySequence()
        return generateSequence(Pair(this, parent)) { (_, parent) ->
            val grandPa = parent.parent
            if (parent is PsiFile || grandPa == null) null else parent to grandPa
        }
    }

val PsiElement.leftLeaves: Sequence<PsiElement>
    get() {
        return generateSequence(this, PsiTreeUtil::prevLeaf).drop(1)
    }

val PsiElement.rightSiblings: Sequence<PsiElement>
    get() = generateSequence(this.nextSibling) { it.nextSibling }

val PsiElement.leftSiblings: Sequence<PsiElement>
    get() = generateSequence(this.prevSibling) { it.prevSibling }

val PsiElement.childrenWithLeaves: Sequence<PsiElement>
    get() = generateSequence(this.firstChild) { it.nextSibling }

fun PsiElement.childrenByType(type: IElementType): Sequence<PsiElement> =
    childrenWithLeaves.filter { it.elementType == type }

fun PsiElement.findFirstChildByType(type: IElementType): PsiElement? =
    childrenByType(type).firstOrNull()

fun PsiElement.findLastChildByType(type: IElementType): PsiElement? =
    childrenByType(type).lastOrNull()

fun PsiElement.isChildExists(type: IElementType): Boolean =
    findFirstChildByType(type) != null

/**
 * Extracts node's element type
 */
//val PsiElement.elementType: IElementType
//    get() = PsiUtilCore.getElementType(this)

fun PsiElement.isAncestorOf(child: PsiElement): Boolean =
    child.ancestors.contains(this)

inline fun <reified T : PsiElement> PsiElement.descendantOfTypeStrict(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, true)

val PsiElement.startOffset: Int
    get() = textRange.startOffset

val PsiElement.endOffset: Int
    get() = textRange.endOffset

val PsiElement.endOffsetInParent: Int
    get() = startOffsetInParent + textLength

fun PsiElement.rangeWithPrevSpace(prev: PsiElement?) = when (prev) {
    is PsiWhiteSpace -> textRange.union(prev.textRange)
    else -> textRange
}

val PsiElement.rangeWithPrevSpace: TextRange
    get() = rangeWithPrevSpace(prevSibling)

val PsiElement.rangeWithSurroundingLineBreaks: TextRange
    get() {
        val startOffset = textRange.startOffset
        val endOffset = textRange.endOffset
        val text = containingFile.text
        val newLineBefore =
            text.lastIndexOf('\n', startOffset).takeIf { it >= 0 }?.let { it + 1 } ?: startOffset
        val newLineAfter = text.indexOf('\n', endOffset).takeIf { it >= 0 }?.let { it + 1 } ?: endOffset
        return TextRange(newLineBefore, newLineAfter)
    }

/** Finds first sibling that is neither comment, nor whitespace before given element */
fun PsiElement?.getPrevNonCommentSibling(): PsiElement? =
    PsiTreeUtil.skipWhitespacesAndCommentsBackward(this)

/** Finds first sibling that is neither comment, nor whitespace after given element */
fun PsiElement?.getNextNonCommentSibling(): PsiElement? =
    PsiTreeUtil.skipWhitespacesAndCommentsForward(this)

fun PsiElement.isWhitespace(): Boolean =
    this is PsiWhiteSpace || this is PsiComment

fun PsiElement.isNewline(): Boolean =
    this.isWhitespace() && this.textMatches("\n")

fun PsiElement.isErrorElement(): Boolean =
    this is PsiErrorElement

fun PsiElement.equalsTo(another: PsiElement): Boolean =
    PsiManager.getInstance(this.project).areElementsEquivalent(this, another)

fun PsiElement.isMsl(): Boolean {
    return getProjectPsiDependentCache(this) {
        if (it !is MvElement) return@getProjectPsiDependentCache false
        val specElement = PsiTreeUtil.findFirstParent(it, false) { parent ->
            parent is MvSpecFunction
                    || parent is MvItemSpecBlockExpr
                    || parent is MvSchema
                    || parent is MvItemSpec
                    || parent is MvModuleSpecBlock
        }
        specElement != null
    }
}

fun PsiElement.cameBefore(element: PsiElement) =
    PsiUtilCore.compareElementsByPosition(this, element) <= 0
