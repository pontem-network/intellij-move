package org.move.lang.core.psi.ext

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.PsiCommentImpl
import org.move.lang.MoveParserDefinition
import org.move.lang.core.MOVE_COMMENTS
import org.move.lang.core.psi.MoveElement

interface MoveDocAndAttributeOwner: MoveElement, NavigatablePsiElement {
    @JvmDefault
    fun docComments(): Sequence<PsiElement> {
        return childrenWithLeaves
            // All these outer elements have been edge bound; if we reach something that isn't one
            // of these, we have reached the actual parse children of this item.
            .takeWhile { it is PsiComment || it is PsiWhiteSpace }
            .filter { it is PsiComment && it.tokenType == MoveParserDefinition.EOL_DOC_COMMENT }
    }
}
