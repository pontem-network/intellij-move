package org.move.lang.core.psi

import com.intellij.psi.util.CachedValuesManager
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.ancestors
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.isTestOnly

enum class ItemScope {
    MAIN, TEST;
}

fun MvElement.isVisibleInScope(expectedItemScope: ItemScope): Boolean {
    return expectedItemScope == ItemScope.TEST
            || this.itemScope == ItemScope.MAIN
}

val MvElement.itemScope: ItemScope
    get() {
        return CachedValuesManager.getProjectPsiDependentCache(this) {
            for (ancestor in (sequenceOf(it) + it.ancestors)) {
                when {
                    ancestor is MvFunction && ancestor.isTest ->
                        return@getProjectPsiDependentCache ItemScope.TEST
                    ancestor is MvDocAndAttributeOwner && ancestor.isTestOnly ->
                        return@getProjectPsiDependentCache ItemScope.TEST
                }
            }
            ItemScope.MAIN
        }
    }
