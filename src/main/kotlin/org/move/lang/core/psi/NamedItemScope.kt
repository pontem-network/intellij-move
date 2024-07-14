package org.move.lang.core.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.MvReferenceElement

enum class NamedItemScope {
    MAIN,
    TEST,
    VERIFY;

    val isTest get() = this == TEST

    companion object {
        fun all(): Set<NamedItemScope> = setOf(MAIN, TEST, VERIFY)
    }
}

//fun MvElement.isVisibleInContext(contextScope: NamedItemScope): Boolean {
//    val itemScope = this.itemScope
//    // MAIN scope items are visible from any context
//    return itemScope == NamedItemScope.MAIN || itemScope == contextScope
//}

fun MvNamedElement.isVisibleInContext(refItemScopes: Set<NamedItemScope>): Boolean {
//    for (requiredScope in this.itemScopes) {
//        if (!contextScopes.contains(requiredScope)) return false
//    }
    val requiredScopes = this.namedItemScopes
    return (requiredScopes - refItemScopes).isEmpty()
    // MAIN scope items are visible from any context
//    return itemScope == ItemScope.MAIN || itemScope == contextScope
}


private val ITEM_SCOPES_KEY =
    Key.create<CachedValue<Set<NamedItemScope>>>("org.move.ITEM_SCOPES_KEY")

val MvReferenceElement.refItemScopes: Set<NamedItemScope>
    get() {
        return (this as MvElement).itemScopes
    }

val MvNamedElement.namedItemScopes: Set<NamedItemScope>
    get() {
        return (this as MvElement).itemScopes
    }

val MvElement.itemScopes: Set<NamedItemScope>
    get() {
        // TODO: special case module items to use stub-only in some cases
//        project.cacheManager.cache(this, ITEM_SCOPES_KEY) {
        val scopes = mutableSetOf(NamedItemScope.MAIN)
        var element: MvElement? = this
        if (element is MvStruct || element is MvFunction) {
            val attributeScopes = (element as MvDocAndAttributeOwner).explicitItemScopes()
            return attributeScopes
        }
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
                is MvSpecCodeBlock, is MvItemSpecRef -> scopes.add(NamedItemScope.VERIFY)
            }
            element = element.parent as? MvElement
        }
        return scopes
//            cacheResult(scopes, listOf(PsiModificationTracker.MODIFICATION_COUNT))
    }

val MvElement.itemScope: NamedItemScope
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
                    is MvSpecCodeBlock, is MvItemSpecRef -> return@getProjectPsiDependentCache NamedItemScope.VERIFY
                }
                element = element.parent as? MvElement
            }
            NamedItemScope.MAIN
        }
    }

private fun MvDocAndAttributeOwner.explicitItemScopes(): Set<NamedItemScope> {
    val scopes = mutableSetOf<NamedItemScope>()
    when {
        this.hasTestOnlyAttr -> scopes.add(NamedItemScope.TEST)
        this.hasVerifyOnlyAttr -> scopes.add(NamedItemScope.VERIFY)
        this is MvStruct -> {
            scopes.addAll(this.module.explicitItemScopes())
        }
        this is MvFunction -> {
            if (this.hasTestAttr) {
                scopes.add(NamedItemScope.TEST)
            }
            scopes.addAll(this.module?.explicitItemScopes().orEmpty())
        }
    }
//    if (this.hasTestOnlyAttr || (this is MvFunction && this.hasTestAttr)) {
//        scopes.add(NamedItemScope.TEST)
//    }
//    if (this.hasVerifyOnlyAttr) {
//        scopes.add(NamedItemScope.VERIFY)
//    }
    return scopes
}

private fun MvDocAndAttributeOwner.explicitAttributeItemScope(): NamedItemScope? =
    when (this) {
//        is MvSpecInlineFunction -> ItemScope.MAIN
        is MvFunction ->
            when {
                this.hasTestOnlyAttr || this.hasTestAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else ->
                    this.module?.explicitAttributeItemScope() ?: NamedItemScope.MAIN
            }
        is MvStruct ->
            when {
                this.hasTestOnlyAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else -> this.module.explicitAttributeItemScope() ?: NamedItemScope.MAIN
            }
        else ->
            when {
                this.hasTestOnlyAttr -> NamedItemScope.TEST
                this.hasVerifyOnlyAttr -> NamedItemScope.VERIFY
                else -> null
            }
    }

