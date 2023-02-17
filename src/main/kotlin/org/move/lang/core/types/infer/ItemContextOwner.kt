package org.move.lang.core.types.infer

import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.MvElement

interface ItemContextOwner : MvElement

val MvElement.itemContextOwner: ItemContextOwner?
    get() {
        return PsiTreeUtil.getParentOfType(this, ItemContextOwner::class.java, false)
    }

fun MvElement.itemContext(msl: Boolean): ItemContext =
    itemContextOwner?.itemContext(msl) ?: project.itemContext(msl)
