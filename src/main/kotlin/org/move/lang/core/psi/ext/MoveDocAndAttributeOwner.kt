package org.move.lang.core.psi.ext

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.MoveParserDefinition
import org.move.lang.core.psi.MvAttr
import org.move.lang.core.psi.MvElement

interface MvDocAndAttributeOwner : MvElement, NavigatablePsiElement {
    val attrList: List<MvAttr>

    fun docComments(): Sequence<PsiElement> {
        return childrenWithLeaves
            // All these outer elements have been edge bound; if we reach something that isn't one
            // of these, we have reached the actual parse children of this item.
            .takeWhile { it is PsiComment || it is PsiWhiteSpace }
            .filter { it is PsiComment && it.tokenType == MoveParserDefinition.EOL_DOC_COMMENT }
    }
}

fun MvDocAndAttributeOwner.findSingleItemAttr(name: String): MvAttr? =
    this.attrList.find {
        it.attrItemList.size == 1
                && it.attrItemList.first().identifier.textMatches(name)
    }

val MvDocAndAttributeOwner.isTestOnly: Boolean
    get() = getProjectPsiDependentCache(this) {
        it.findSingleItemAttr("test_only") != null
    }
