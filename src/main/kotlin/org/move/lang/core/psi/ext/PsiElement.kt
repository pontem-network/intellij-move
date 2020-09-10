package org.move.lang.core.psi.ext

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore

val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.parent
    }

inline fun <reified T : PsiElement> PsiElement.ancestorStrict(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, true)

inline fun <reified T : PsiElement> PsiElement.ancestorStrict(stopAt: Class<out PsiElement>): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, true, stopAt)

inline fun <reified T : PsiElement> PsiElement.ancestorOrSelf(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, false)

inline fun <reified T : PsiElement> PsiElement.ancestorOrSelf(stopAt: Class<out PsiElement>): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, false, stopAt)

inline fun <reified T : PsiElement> PsiElement.stubAncestorStrict(): T? =
    PsiTreeUtil.getStubOrPsiParentOfType(this, T::class.java)

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
val PsiElement.elementType: IElementType
    get() = PsiUtilCore.getElementType(this)

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