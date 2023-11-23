package org.move.lang.core.psi.ext

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.prevLeaf
import com.intellij.util.SmartList
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.impl.MvFileStub
import org.move.lang.toNioPathOrNull
import org.move.openapiext.document
import org.move.openapiext.rootPath
import java.nio.file.Path

fun PsiElement.hasChild(tokenType: IElementType): Boolean = childrenByType(tokenType).toList().isNotEmpty()

fun PsiElement.getChild(tokenType: IElementType): PsiElement? = childrenByType(tokenType).firstOrNull()

inline fun <reified T: PsiElement> PsiElement.childOfType(): T? =
    PsiTreeUtil.getChildOfType(this, T::class.java)

inline fun <reified T: PsiElement> PsiElement.childrenOfType(): List<T> =
    PsiTreeUtil.getChildrenOfTypeAsList(this, T::class.java)

inline fun <reified T: PsiElement> PsiElement.stubChildrenOfType(): List<T> {
    return if (this is PsiFileImpl) {
        stub?.childrenStubs?.mapNotNull { it.psi as? T } ?: return childrenOfType()
    } else {
        PsiTreeUtil.getStubChildrenOfTypeAsList(this, T::class.java)
    }
}


inline fun <reified T: PsiElement> PsiElement.descendantOfTypeStrict(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, /* strict */ true)

inline fun <reified T: PsiElement> PsiElement.descendantOfTypeOrSelf(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, /* strict */ false)

inline fun <reified T: PsiElement> PsiElement.descendantsOfType(): Collection<T> =
    PsiTreeUtil.findChildrenOfType(this, T::class.java)

inline fun <reified T: PsiElement> PsiElement.descendantsOfTypeOrSelf(): Collection<T> =
    PsiTreeUtil.findChildrenOfAnyType(this, false, T::class.java)

inline fun <reified T: PsiElement> PsiElement.descendantOfType(predicate: (T) -> Boolean): T? {
    return descendantsOfType<T>().firstOrNull(predicate)
}

@Suppress("unused")
inline fun <reified T: PsiElement> PsiElement.stubDescendantsOfTypeStrict(): Collection<T> =
    getStubDescendantsOfType(this, true, T::class.java)

inline fun <reified T: PsiElement> PsiElement.stubDescendantsOfTypeOrSelf(): Collection<T> =
    getStubDescendantsOfType(this, false, T::class.java)

inline fun <reified T: PsiElement> PsiElement.stubDescendantOfTypeOrStrict(): T? =
    getStubDescendantOfType(this, true, T::class.java)

@Suppress("unused")
inline fun <reified T: PsiElement> PsiElement.stubDescendantOfTypeOrSelf(): T? =
    getStubDescendantOfType(this, false, T::class.java)

fun <T: PsiElement> getStubDescendantsOfType(
    element: PsiElement?,
    strict: Boolean,
    aClass: Class<T>
): Collection<T> {
    if (element == null) return emptyList()
    val stub = (element as? PsiFileImpl)?.greenStub
        ?: (element as? StubBasedPsiElement<*>)?.greenStub
        ?: return PsiTreeUtil.findChildrenOfAnyType(element, strict, aClass)

    val result = SmartList<T>()

    fun go(childrenStubs: List<StubElement<out PsiElement>>) {
        for (childStub in childrenStubs) {
            val child = childStub.psi
            if (aClass.isInstance(child)) {
                result.add(aClass.cast(child))
            }
            go(childStub.childrenStubs)
        }

    }

    if (strict) {
        go(stub.childrenStubs)
    } else {
        go(listOf(stub))
    }

    return result
}

fun <T: PsiElement> getStubDescendantOfType(
    element: PsiElement?,
    strict: Boolean,
    aClass: Class<T>
): T? {
    if (element == null) return null
    val stub = (element as? PsiFileImpl)?.greenStub
        ?: (element as? StubBasedPsiElement<*>)?.greenStub
        ?: return PsiTreeUtil.findChildOfType(element, aClass, strict)

    fun go(childrenStubs: List<StubElement<out PsiElement>>): T? {
        for (childStub in childrenStubs) {
            val child = childStub.psi
            if (aClass.isInstance(child)) {
                return aClass.cast(child)
            } else {
                go(childStub.childrenStubs)?.let { return it }
            }
        }

        return null
    }

    return if (strict) {
        go(stub.childrenStubs)
    } else {
        go(listOf(stub))
    }
}

val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile)
            null
        else it.parent
    }

inline fun <reified T: PsiElement> PsiElement.ancestorsOfType(): Sequence<T> {
    return this.ancestors.filterIsInstance<T>()
}

fun PsiElement.findFirstParent(strict: Boolean = true, cond: Condition<in PsiElement>) =
    PsiTreeUtil.findFirstParent(this, strict, cond)

inline fun <reified T: PsiElement> PsiElement.ancestorStrict(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, true)

inline fun <reified T: PsiElement> PsiElement.ancestorStrict(stopAt: Class<out PsiElement>): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, true, stopAt)

inline fun <reified T: PsiElement> PsiElement.ancestorOrSelf(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, false)

fun <T: PsiElement> PsiElement.ancestorOfClass(psiClass: Class<T>, strict: Boolean = false): T? =
    PsiTreeUtil.getParentOfType(this, psiClass, strict)

inline fun <reified T: PsiElement> PsiElement.hasAncestor(): Boolean =
    ancestorStrict<T>() != null

inline fun <reified T: PsiElement> PsiElement.hasAncestorOrSelf(): Boolean =
    ancestorOrSelf<T>() != null

inline fun <reified T: PsiElement> PsiElement.ancestorOrSelf(stopAt: Class<out PsiElement>): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, false, stopAt)

inline fun <reified T: PsiElement> PsiElement.stubAncestorStrict(): T? =
    PsiTreeUtil.getStubOrPsiParentOfType(this, T::class.java)

/**
 * Extracts node's element type
 */
val PsiElement.elementType: IElementType
    // XXX: be careful not to switch to AST
    get() = if (this is MoveFile) MvFileStub.Type else PsiUtilCore.getElementType(this)
//    get() = PsiUtilCore.getElementType(this)

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

val PsiElement.startOffset: Int
    get() = textRange.startOffset

val PsiElement.endOffset: Int
    get() = textRange.endOffset

val PsiElement.endOffsetInParent: Int
    get() = startOffsetInParent + textLength

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

/** Finds first sibling that is not whitespace before given element */
fun PsiElement?.getPrevNonWhitespaceSibling(): PsiElement? =
    PsiTreeUtil.skipWhitespacesBackward(this)

/** Finds first sibling that is not whitespace after given element */
fun PsiElement?.getNextNonWhitespaceSibling(): PsiElement? =
    PsiTreeUtil.skipWhitespacesForward(this)

fun PsiElement.isWhitespace(): Boolean =
    this is PsiWhiteSpace || this is PsiComment

fun PsiElement.isNewline(): Boolean =
    this.isWhitespace() && this.textMatches("\n")

fun PsiElement.isErrorElement(): Boolean =
    this is PsiErrorElement

fun PsiElement.equalsTo(another: PsiElement): Boolean =
    PsiManager.getInstance(this.project).areElementsEquivalent(this, another)

fun PsiElement.cameBefore(element: PsiElement) =
    PsiUtilCore.compareElementsByPosition(this, element) <= 0

@Suppress("UNCHECKED_CAST")
inline val <T: StubElement<*>> StubBasedPsiElement<T>.greenStub: T?
    get() = (this as? StubBasedPsiElementBase<T>)?.greenStub

fun <T: PsiElement> T.smartPointer() = SmartPointerManager.createPointer(this)

val PsiElement.stubParent: PsiElement?
    get() {
        if (this is StubBasedPsiElement<*>) {
            val stub = this.greenStub
            if (stub != null) return stub.parentStub?.psi
        }
        return parent
    }

/**
 * Same as [ancestorOrSelf], but with "fake" parent links. See [org.rust.lang.core.macros.RsExpandedElement].
 */
inline fun <reified T: PsiElement> PsiElement.contextOrSelf(): T? =
    PsiTreeUtil.getContextOfType(this, T::class.java, /* strict */ false)

fun PsiElement.textRangeInAncestor(ancestorElement: PsiElement): TextRange {
    if (!ancestorElement.isAncestorOf(this)) return this.textRange
    val startOffset = this.startOffset - ancestorElement.startOffset
    return TextRange.from(startOffset, this.textLength)
}

fun PsiElement.rangeWithPrevSpace(prev: PsiElement?): TextRange =
    when (prev) {
        is PsiWhiteSpace -> textRange.union(prev.textRange)
        else -> textRange
    }

val PsiElement.rangeWithPrevSpace: TextRange
    get() = rangeWithPrevSpace(prevLeaf())

private fun PsiElement.getLineCount(): Int {
    val doc = containingFile?.document
    if (doc != null) {
        val spaceRange = textRange ?: TextRange.EMPTY_RANGE

        if (spaceRange.endOffset <= doc.textLength) {
            val startLine = doc.getLineNumber(spaceRange.startOffset)
            val endLine = doc.getLineNumber(spaceRange.endOffset)

            return endLine - startLine
        }
    }

    return (text ?: "").count { it == '\n' } + 1
}

fun PsiWhiteSpace.isMultiLine(): Boolean = getLineCount() > 1

fun PsiElement.locationPath(tryRelative: Boolean): Path? {
    val containingFilePath = this.containingFile.toNioPathOrNull() ?: return null
    if (tryRelative) {
        val rootPath = this.project.rootPath
        if (rootPath != null) {
            return rootPath.relativize(containingFilePath)
        }
    }
    return containingFilePath
}

fun PsiElement.locationString(tryRelative: Boolean): String? =
    locationPath(tryRelative)?.toString()
