package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.createCachedResult
import org.move.utils.recursionGuard

interface ItemContextOwner : MvElement

val MvElement.itemContextOwner: ItemContextOwner?
    get() {
        return PsiTreeUtil.getParentOfType(this, ItemContextOwner::class.java, false)
    }

fun MvElement.itemContext(msl: Boolean): ItemContext =
    itemContextOwner?.itemContext(msl) ?: project.itemContext(msl)

private val ITEM_CONTEXT_KEY_MSL: Key<CachedValue<ItemContext>> = Key.create("ITEM_CONTEXT_KEY_MSL")
private val ITEM_CONTEXT_KEY_NON_MSL: Key<CachedValue<ItemContext>> = Key.create("ITEM_CONTEXT_KEY_NON_MSL")
private val ITEM_CONTEXT_GUARD: Key<CachedValue<ItemContext>> = Key.create("ITEM_CONTEXT_GUARD")

fun ItemContextOwner.itemContext(msl: Boolean): ItemContext {
    val itemContext = if (msl) {
        this.project.cacheManager.cache(this, ITEM_CONTEXT_KEY_MSL) {
            val itemContext = recursionGuard(ITEM_CONTEXT_GUARD, { getItemContext(this, true) })
                ?: error("nested itemContext inference for ${(this as? MvModule)?.fqName}")
            createCachedResult(itemContext)
        }
    } else {
        this.project.cacheManager.cache(this, ITEM_CONTEXT_KEY_NON_MSL) {
            val itemContext = recursionGuard(ITEM_CONTEXT_GUARD, { getItemContext(this, false) })
                ?: error("nested itemContext inference for ${(this as? MvModule)?.fqName}")
            createCachedResult(itemContext)
        }
    }
    return itemContext
}
