package org.move.lang.core.psi.ext

import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*

fun MvElement.isInsideSpec(): Boolean {
    return PsiTreeUtil.findFirstParent(this, false) {
        it is MvSpecFunctionDef
                || it is MvUninterpretedSpecFunctionDef
                || it is MvSpecNativeFunctionDef
                || it is MvSpecBlockExpr
                || it is MvSpecDef
    } != null
}

fun MvElement.isInsideAssignmentLeft(): Boolean {
    val parent = PsiTreeUtil.findFirstParent(this, false) {
        it is MvAssignmentExpr || it is MvInitializer
    }
    return parent is MvAssignmentExpr
}
