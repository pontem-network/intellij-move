package org.move.ide.inspections.imports

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.PsiModificationTracker
import org.move.lang.core.psi.*
import org.move.lang.core.psi.NamedItemScope.*
import org.move.lang.core.psi.ext.*
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.cacheResult

// classifies foo of `foo::bar::baz`
sealed class BasePathType {
    data object Address: BasePathType()
    data class Module(val moduleName: String): BasePathType()
    data class Item(val itemName: String): BasePathType()
}

fun MvPath.basePathType(): BasePathType? {
    val rootPath = this.rootPath()
    val qualifier = rootPath.qualifier
    // foo
    if (qualifier == null) {
        return rootPath.referenceName?.let { BasePathType.Item(it) }
    }

    // 0x1::m
    if (qualifier.pathAddress != null) return BasePathType.Address

    val qualifierBase = qualifier.qualifier
    // aptos_framework::m::foo
    if (qualifierBase != null) {
        return BasePathType.Address
    }

    // todo: `aptos_framework::m`,
    //  first resolve aptos_framework into the NamedAddress, then return the BasePathType.Address

    // m::foo
    return qualifier.referenceName?.let { BasePathType.Module(it) }
}

private val USAGE_SCOPE_KEY: Key<CachedValue<NamedItemScope>> = Key.create("USAGE_SCOPE_KEY")

class UsageScopeProvider(val scopeElement: MvElement): CachedValueProvider<NamedItemScope> {
    override fun compute(): Result<NamedItemScope> {
        var scope = MAIN
        for (ancestor in scopeElement.ancestorsOfType<MvDocAndAttributeOwner>()) {
            // msl items
            if (scopeElement is MvSpecCodeBlock || scopeElement is MvItemSpecRef) {
                scope = VERIFY
                break
            }
            // explicit attrs, #[test], #[test_only], #[verify_only]
            val attributeScope = ancestor.itemScopeFromAttributes()
            if (attributeScope != null) {
                scope = attributeScope
                break
            }
        }
        return scopeElement.cacheResult(scope, listOf(PsiModificationTracker.MODIFICATION_COUNT))
    }
}

val MvElement.usageScope: NamedItemScope
    get() {
        return project.cacheManager
            .cache(this, USAGE_SCOPE_KEY, UsageScopeProvider(this))
    }
