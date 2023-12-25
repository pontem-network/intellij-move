package org.move.lang.core.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import org.move.lang.core.psi.ext.*

enum class ItemScope {
    MAIN,
    TEST,
    VERIFY;

    companion object {
        fun all(): Set<ItemScope> = setOf(MAIN, TEST, VERIFY)
    }
}

fun MvElement.isVisibleInContext(contextScope: ItemScope): Boolean {
    val itemScope = this.itemScope
    // MAIN scope items are visible from any context
    return itemScope == ItemScope.MAIN || itemScope == contextScope
}

fun MvElement.isVisibleInContext(contextScopes: Set<ItemScope>): Boolean {
//    for (requiredScope in this.itemScopes) {
//        if (!contextScopes.contains(requiredScope)) return false
//    }
    val requiredScopes = this.itemScopes
    return (requiredScopes - contextScopes).isEmpty()
    // MAIN scope items are visible from any context
//    return itemScope == ItemScope.MAIN || itemScope == contextScope
}


private val ITEM_SCOPES_KEY =
    Key.create<CachedValue<Set<ItemScope>>>("org.move.ITEM_SCOPES_KEY")

val MvElement.itemScopes: Set<ItemScope>
    get() {
        // TODO: special case module items to use stub-only in some cases
//        project.cacheManager.cache(this, ITEM_SCOPES_KEY) {
        val scopes = mutableSetOf(ItemScope.MAIN)
        var element: MvElement? = this
        while (element != null) {
            when (element) {
                is MvDocAndAttributeOwner -> {
                    val attributeScopes = element.explicitItemScopes()
                    scopes.addAll(attributeScopes)
//                    val ownerItemScope = element.explicitAttributeItemScope()
//                    if (ownerItemScope != null) {
//                        scopes.add(ownerItemScope)
//                    }
                }
                is MvSpecCodeBlock, is MvItemSpecRef -> scopes.add(ItemScope.VERIFY)
            }
            element = element.parent as? MvElement
        }
        return scopes
//            cacheResult(scopes, listOf(PsiModificationTracker.MODIFICATION_COUNT))
    }

val MvElement.itemScope: ItemScope
    get() {
        return CachedValuesManager.getProjectPsiDependentCache(this) {
            var element = it
            while (element != null) {
                when (element) {
                    is MvDocAndAttributeOwner -> {
                        val ownerItemScope = element.explicitAttributeItemScope()
                        if (ownerItemScope != null) {
                            return@getProjectPsiDependentCache ownerItemScope
                        }
                    }
                    is MvSpecCodeBlock, is MvItemSpecRef -> return@getProjectPsiDependentCache ItemScope.VERIFY
                }
                element = element.parent as? MvElement
            }
            ItemScope.MAIN
        }
    }

private fun MvDocAndAttributeOwner.explicitItemScopes(): Set<ItemScope> {
    val scopes = mutableSetOf<ItemScope>()
    if (this.hasTestOnlyAttr || (this is MvFunction && this.hasTestAttr)) {
        scopes.add(ItemScope.TEST)
    }
    if (this.hasVerifyOnlyAttr) {
        scopes.add(ItemScope.VERIFY)
    }
    return scopes
}

private fun MvDocAndAttributeOwner.explicitAttributeItemScope(): ItemScope? =
    when (this) {
//        is MvSpecInlineFunction -> ItemScope.MAIN
        is MvFunction ->
            when {
                this.hasTestOnlyAttr || this.hasTestAttr -> ItemScope.TEST
                this.hasVerifyOnlyAttr -> ItemScope.VERIFY
                else ->
                    this.module?.explicitAttributeItemScope() ?: ItemScope.MAIN
            }
        is MvStruct ->
            when {
                this.hasTestOnlyAttr -> ItemScope.TEST
                this.hasVerifyOnlyAttr -> ItemScope.VERIFY
                else -> this.module.explicitAttributeItemScope() ?: ItemScope.MAIN
            }
        else ->
            when {
                this.hasTestOnlyAttr -> ItemScope.TEST
                this.hasVerifyOnlyAttr -> ItemScope.VERIFY
                else -> null
            }
    }

