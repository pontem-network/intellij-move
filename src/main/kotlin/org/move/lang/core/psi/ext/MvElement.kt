package org.move.lang.core.psi.ext

import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.MslScope

// TODO: cache
fun MvElement.isMsl(): Boolean {
    return PsiTreeUtil.findFirstParent(this, false) {
        it is MvSpecFunction
                || it is MvSpecBlockExpr
                || it is MvSchema
                || it is MvSpecDef
    } != null
}

fun MvElement.isInsideAssignmentLeft(): Boolean {
    val parent = PsiTreeUtil.findFirstParent(this, false) {
        it is MvAssignmentExpr || it is MvInitializer
    }
    return parent is MvAssignmentExpr
}
