package org.move.lang.core.psi

import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.ext.*
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
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

class AttributesItemScope(override val owner: MvDocAndAttributeOwner): PsiCachedValueProvider<NamedItemScope?> {
    override fun compute(): CachedValueProvider.Result<NamedItemScope?> {
        val itemScope = owner.attributesItemScopeInner()
        return owner.psiCacheResult(itemScope)
    }
}

val MvDocAndAttributeOwner.itemScopeFromAttributes: NamedItemScope?
    get() {
        return AttributesItemScope(this).getResults()
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

