package org.move.lang.core.psi.ext

import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.move.lang.MvElementTypes.SPEC_BLOCK
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvSpecDef
import org.move.lang.core.resolve.walkUpThroughScopes

fun MvElement.isDescendantOf(elementType: IElementType): Boolean {
    var descendant = false
    walkUpThroughScopes(this, { it is PsiFile }) { _, scope ->
        if (scope.elementType == elementType) descendant = true
        false
    }
    return descendant
}

//fun MvElement.isDescendantOf(elementTypes: TokenSet): Boolean {
//    var descendant = false
//    walkUpThroughScopes(this, { it is PsiFile }) { _, scope ->
//        if (scope.elementType in elementTypes) descendant = true;
//        false
//    }
//    return descendant
//}

fun MvElement.isInsideSpecBlock(): Boolean = isDescendantOf(SPEC_BLOCK)

fun MvElement.isSpecElement(): Boolean = this is MvSpecDef
