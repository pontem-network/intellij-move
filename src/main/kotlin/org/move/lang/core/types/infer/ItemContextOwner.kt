package org.move.lang.core.types.infer

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.MvCodeFragment
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModificationTrackerOwner
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.contextOrSelf
import org.move.lang.core.psi.moveStructureModificationTracker
import org.move.utils.cache
import org.move.utils.cacheManager
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

fun <T> MvElement.createCachedResult(value: T): CachedValueProvider.Result<T> {
    val structureModificationTracker = project.moveStructureModificationTracker
    return when {
        // The case of injected language. Injected PSI don't have its own event system, so can only
        // handle evens from outer PSI. For example, Rust language is injected to Kotlin's string
        // literal. If a user change the literal, we can only be notified that the literal is changed.
        // So we have to invalidate the cached value on any PSI change
        containingFile.virtualFile is VirtualFileWindow -> {
            CachedValueProvider.Result.create(
                value,
                com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
            )
        }

        // Invalidate cached value of code fragment on any PSI change
        this is MvCodeFragment -> CachedValueProvider.Result.create(
            value,
            com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
        )

        // CachedValueProvider.Result can accept a ModificationTracker as a dependency, so the
        // cached value will be invalidated if the modification counter is incremented.
        else -> {
            val modificationTracker = contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
            CachedValueProvider.Result.create(
                value,
                listOfNotNull(structureModificationTracker, modificationTracker)
            )
        }
    }
}
