package org.move.lang.core.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import org.move.lang.core.psi.ext.*
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.psiCacheResult

enum class NamedItemScope {
    MAIN,
    TEST,
    VERIFY;

    val isTest get() = this == TEST

    fun shrinkScope(adjustmentScope: NamedItemScope): NamedItemScope {
        if (this == MAIN) {
            return adjustmentScope
        }
        return this
    }
}

private val ATTRIBUTES_ITEM_SCOPE: Key<CachedValue<NamedItemScope?>> = Key.create("ATTRIBUTES_ITEM_SCOPE")

val MvDocAndAttributeOwner.itemScopeFromAttributes: NamedItemScope?
    get() {
        return project.cacheManager.cache(
            this,
            ATTRIBUTES_ITEM_SCOPE
        ) { psiCacheResult(this.attributesItemScopeInner()) }
    }

private fun MvDocAndAttributeOwner.attributesItemScopeInner(): NamedItemScope? =
    when (this) {
        is MvFunction ->
            when {
                this.hasTestOnlyAttr || this.hasTestAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else ->
                    this.module?.itemScopeFromAttributes ?: NamedItemScope.MAIN
            }
        is MvStruct ->
            when {
                this.hasTestOnlyAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else -> this.module.itemScopeFromAttributes ?: NamedItemScope.MAIN
            }
        else ->
            when {
                this.hasTestOnlyAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else -> null
            }
    }

