package org.move.lang.core.psi.ext

import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.MslScope

fun MvElement.isMslEnabled(): Boolean {
    return PsiTreeUtil.findFirstParent(this, false) {
        it is MvSpecFunction
                || it is MvSpecBlockExpr
                || it is MvSpecSchema
                || it is MvSpecDef
    } != null
}

fun MvElement.isInsideAssignmentLeft(): Boolean {
    val parent = PsiTreeUtil.findFirstParent(this, false) {
        it is MvAssignmentExpr || it is MvInitializer
    }
    return parent is MvAssignmentExpr
}
