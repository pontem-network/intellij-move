package org.move.ide.inspections.imports

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValueProvider.Result
import org.move.lang.core.psi.*
import org.move.lang.core.psi.NamedItemScope.MAIN
import org.move.lang.core.psi.NamedItemScope.VERIFY
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.ancestorsOfType
import org.move.lang.core.psi.ext.qualifier
import org.move.lang.core.psi.ext.rootPath
import org.move.utils.psiCacheResult

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
    // endless_framework::m::foo
    if (qualifierBase != null) {
        return BasePathType.Address
    }

    // todo: `endless_framework::m`,
    //  first resolve endless_framework into the NamedAddress, then return the BasePathType.Address

    // m::foo
    return qualifier.referenceName?.let { BasePathType.Module(it) }
}

val MvElement.usageScope: NamedItemScope
    get() {
        for (ancestor in this.ancestorsOfType<MvDocAndAttributeOwner>()) {
            // msl items
            if (this is MvSpecCodeBlock || this is MvItemSpecRef) {
                return VERIFY
            }
            // explicit attrs, #[test], #[test_only], #[verify_only]
            val attributeScope = ancestor.itemScopeFromAttributes
            if (attributeScope != null) {
                return attributeScope
            }
        }
        return MAIN
    }
