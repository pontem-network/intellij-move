package org.move.lang.core.psi.ext

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.move.lang.MoveParserDefinition
import org.move.lang.core.psi.MvAttr
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.MvElement

interface MvDocAndAttributeOwner: MvElement, NavigatablePsiElement {
    val attrList: List<MvAttr>

    fun docComments(): Sequence<PsiElement> {
        return childrenWithLeaves
            // All these outer elements have been edge bound; if we reach something that isn't one
            // of these, we have reached the actual parse children of this item.
            .takeWhile { it is PsiComment || it is PsiWhiteSpace }
            .filter { it is PsiComment && it.tokenType == MoveParserDefinition.EOL_DOC_COMMENT }
    }
}

val MvDocAndAttributeOwner.hasTestOnlyAttr: Boolean
    get() {
        return queryAttributes.isTestOnly
    }

val MvDocAndAttributeOwner.hasVerifyOnlyAttr: Boolean get() = queryAttributes.isVerifyOnly

/**
 * Returns [QueryAttributes] for given PSI element.
 */
val MvDocAndAttributeOwner.queryAttributes: QueryAttributes
    get() {
        return QueryAttributes(this.attrList.asSequence())
    }

/**
 * Allows for easy querying [MvDocAndAttributeOwner] for specific attributes.
 *
 * **Do not instantiate directly**, use [MvDocAndAttributeOwner.queryAttributes] instead.
 */
class QueryAttributes(
    private val attributes: Sequence<MvAttr>
) {
    val isTestOnly: Boolean get() = hasAttrItem("test_only")
    val isVerifyOnly: Boolean get() = hasAttrItem("verify_only")

    fun hasAttrItem(attributeName: String): Boolean = getAttrItem(attributeName) != null
    fun getAttrItem(attributeName: String): MvAttrItem? =
        this.attrItems.find { it.unqualifiedName == attributeName }

    fun getAttrItemsByPath(fqPath: String): Sequence<MvAttrItem> =
        this.attrItems.filter { it.path.text == fqPath }

    val attrItems: Sequence<MvAttrItem> get() = this.attributes.flatMap { it.attrItemList }

    override fun toString(): String =
        "QueryAttributes(${attributes.joinToString { it.text }})"
}
