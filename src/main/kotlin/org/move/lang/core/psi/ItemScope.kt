package org.move.lang.core.psi

import com.intellij.psi.util.CachedValuesManager
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.isTestOnly
import org.move.lang.core.psi.ext.module

enum class ItemScope {
    MAIN, TEST;
}

fun MvElement.isVisibleInScope(expectedItemScope: ItemScope): Boolean {
    return expectedItemScope == ItemScope.TEST
            || this.itemScope == ItemScope.MAIN
}

val MvModule.itemScope: ItemScope
    get() = if (this.isTestOnly) ItemScope.TEST else ItemScope.MAIN

val MvElement.itemScope: ItemScope
    get() {
        return CachedValuesManager.getProjectPsiDependentCache(this) {
            var element = it
            while (element != null) {
                when {
                    element is MvModule -> {
                        return@getProjectPsiDependentCache element.itemScope
                    }
                    element is MvFunction -> {
                        if (element.isTest) return@getProjectPsiDependentCache ItemScope.TEST
                        if (element.isTestOnly) return@getProjectPsiDependentCache ItemScope.TEST
                        val module = element.module
                        if (module != null) {
                            return@getProjectPsiDependentCache module.itemScope
                        }
                    }
                    element is MvStruct -> {
                        if (element.isTestOnly) return@getProjectPsiDependentCache ItemScope.TEST
                        return@getProjectPsiDependentCache element.module.itemScope
                    }
                    element is MvDocAndAttributeOwner && element.isTestOnly -> {
                        return@getProjectPsiDependentCache ItemScope.TEST
                    }
                }
                element = element.parent as? MvElement
            }
            ItemScope.MAIN
        }
    }
