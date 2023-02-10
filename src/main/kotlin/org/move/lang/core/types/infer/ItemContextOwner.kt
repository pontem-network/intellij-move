package org.move.lang.core.types.infer

import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.MvElement

interface ItemContextOwner : MvElement

val MvElement.itemContextOwner: ItemContextOwner?
    get() {
        return CachedValuesManager.getProjectPsiDependentCache(this) {
            PsiTreeUtil.findFirstParent(it, false) { p -> p is ItemContextOwner }
                    as? ItemContextOwner
        }
    }
