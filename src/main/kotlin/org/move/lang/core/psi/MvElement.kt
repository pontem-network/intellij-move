package org.move.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.move.lang.MoveFile
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.*

interface MvElement : PsiElement

abstract class MvElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                              MvElement

val MvElement.containingMoveFile: MoveFile? get() = this.containingFile as? MoveFile

val MvElement.containingScript: MvScript? get() = ancestorStrict()

val MvElement.containingFunction: MvFunction? get() = ancestorStrict()

val MvElement.containingFunctionLike: MvFunctionLike? get() = ancestorStrict()

val MvElement.namespaceModule: MvModule?
    get() {
        val parent = this.findFirstParent(false) { it is MvModule || it is MvModuleSpec }
        return when (parent) {
            is MvModule -> parent
            is MvModuleSpec -> parent.moduleItem
            else -> null
        }
    }


val MvElement.containingModule: MvModule? get() = ancestorStrict()

val MvElement.containingModuleSpec: MvModuleSpec? get() = ancestorStrict()

val MvElement.containingItemsOwner get() = ancestorOrSelf<MvItemsOwner>()

//val MvElement.containingModuleOrScript: MvElement?
//    get() {
//        return this.findFirstParent(false) { it is MvScript || it is MvModule }
//                as? MvElement
//    }

/**
 * Delete the element along with a neighbour comma.
 * If a comma follows the element, it will be deleted.
 * Else if a comma precedes the element, it will be deleted.
 *
 * It is useful to remove elements that are parts of comma separated lists (parameters, arguments, use specks, ...).
 */
fun MvElement.deleteWithSurroundingComma() {
    val followingComma = getNextNonCommentSibling()
    if (followingComma?.elementType == MvElementTypes.COMMA) {
        followingComma?.delete()
    } else {
        val precedingComma = getPrevNonCommentSibling()
        if (precedingComma?.elementType == MvElementTypes.COMMA) {
            precedingComma?.delete()
        }
    }
    delete()
}

/**
 * Delete the element along with all surrounding whitespace and a single surrounding comma.
 * See [deleteWithSurroundingComma].
 */
fun MvElement.deleteWithSurroundingCommaAndWhitespace() {
    val toDelete =
        rightSiblings.takeWhile { it.isWhitespaceOrComment } +
                leftSiblings.takeWhile { it.isWhitespaceOrComment }
    toDelete.forEach {
        it.delete()
    }
    deleteWithSurroundingComma()
}

private val PsiElement.isWhitespaceOrComment
    get(): Boolean = this is PsiWhiteSpace || this is PsiComment

/**
 * It is possible to match value with constant-like element, e.g.
 *      ```
 *      enum Kind { A }
 *      use Kind::A;
 *      match kind { A => ... } // `A` is a constant-like element, not a pat binding
 *      ```
 *
 * But there is no way to distinguish a pat binding from a constant-like element on syntax level,
 * so we resolve an item `A` first, and then use [isConstantLike] to check whether the element is constant-like or not.
 *
 * Constant-like element can be: real constant, static variable, and enum variant without fields.
 */
val MvElement.isConstantLike: Boolean
    get() = this is MvConst || (this is MvFieldsOwner && isFieldless)

val MvElement.isFieldlessFieldsOwner get() = this is MvFieldsOwner && isFieldless
