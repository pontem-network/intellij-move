package org.move.lang.core.psi

import org.move.lang.core.psi.ext.*

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

fun MvDocAndAttributeOwner.itemScopeFromAttributes(): NamedItemScope? =
    when (this) {
        is MvFunction ->
            when {
                this.hasTestOnlyAttr || this.hasTestAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else ->
                    this.module?.itemScopeFromAttributes() ?: NamedItemScope.MAIN
            }
        is MvStruct ->
            when {
                this.hasTestOnlyAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else -> this.module.itemScopeFromAttributes() ?: NamedItemScope.MAIN
            }
        else ->
            when {
                this.hasTestOnlyAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else -> null
            }
    }

