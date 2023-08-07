package org.move.lang.core.psi

import com.intellij.psi.util.CachedValuesManager
import org.move.lang.core.psi.ext.*

enum class ItemScope {
    MAIN,
    TEST,
    VERIFY;
}

fun MvElement.isVisibleInScope(contextScope: ItemScope): Boolean {
    val itemScope = this.itemScope
    // MAIN scope items are visible from any context
    return itemScope == ItemScope.MAIN || itemScope == contextScope
}

val MvElement.itemScope: ItemScope
    get() {
        return CachedValuesManager.getProjectPsiDependentCache(this) {
            var element = it
            while (element != null) {
                when {
                    element is MvDocAndAttributeOwner -> {
                        val ownerItemScope = element.attrOwnerItemScope
                        if (ownerItemScope != null) {
                            return@getProjectPsiDependentCache ownerItemScope
                        }
                    }
                    element is MvSpecCodeBlock
                            || element is MvItemSpecRef -> return@getProjectPsiDependentCache ItemScope.VERIFY
                }
                element = element.parent as? MvElement
            }
            ItemScope.MAIN
        }
    }

private val MvDocAndAttributeOwner.attrOwnerItemScope: ItemScope?
    get() =
        when (this) {
            is MvSpecInlineFunction -> ItemScope.MAIN
            is MvFunction ->
                when {
                    this.isTestOnly || this.isTest -> ItemScope.TEST
                    this.isVerifyOnly -> ItemScope.VERIFY
                    else ->
                        this.module?.attrOwnerItemScope ?: ItemScope.MAIN
                }
            is MvStruct ->
                when {
                    this.isTestOnly -> ItemScope.TEST
                    this.isVerifyOnly -> ItemScope.VERIFY
                    else -> this.module.attrOwnerItemScope ?: ItemScope.MAIN
                }
            else ->
                when {
                    this.isTestOnly -> ItemScope.TEST
                    this.isVerifyOnly -> ItemScope.VERIFY
                    else -> null
                }
        }

