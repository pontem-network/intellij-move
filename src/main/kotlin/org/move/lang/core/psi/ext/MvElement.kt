package org.move.lang.core.psi.ext

import com.intellij.openapi.util.Key
import com.intellij.psi.util.*
import org.move.lang.core.psi.*

private val IS_MSL_KEY: Key<CachedValue<Boolean>> = Key.create("IS_MSL_KEY")

fun MvElement.isMsl(): Boolean {
    return CachedValuesManager.getCachedValue(this, IS_MSL_KEY) {
        val specElement = PsiTreeUtil.findFirstParent(this, false) {
            it is MvSpecFunction
                    || it is MvSpecBlockExpr
                    || it is MvSchema
                    || it is MvSpecDef
        }
        CachedValueProvider.Result(specElement != null, PsiModificationTracker.MODIFICATION_COUNT)
    }
}

fun MvElement.isInsideAssignmentLeft(): Boolean {
    val parent = PsiTreeUtil.findFirstParent(this, false) {
        it is MvAssignmentExpr || it is MvInitializer
    }
    return parent is MvAssignmentExpr
}
