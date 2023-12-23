package org.move.lang.core.psi

import org.move.ide.inspections.imports.ItemUsages
import org.move.ide.inspections.imports.ScopePathUsages
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.moduleName

interface MvUseSpeck: MvElement

fun MvUseSpeck.isUsed(pathUsages: ScopePathUsages): Boolean {
    return when (this) {
        is MvModuleUseSpeck -> {
            val useAlias = this.useAlias
            val moduleName =
                (if (useAlias != null) useAlias.name else this.fqModuleRef?.referenceName)
                    ?: return true
            // null if import is never used
            val usageResolvedItems = pathUsages.nameUsages[moduleName] ?: return false
            if (usageResolvedItems.isEmpty()) {
                // import is used but usages are unresolved
                return true
            }
            val speckResolvedItems =
                if (useAlias != null) listOf(useAlias) else this.fqModuleRef?.reference?.multiResolve().orEmpty()
            // any of path usages resolve to the same named item
            speckResolvedItems.any { it in usageResolvedItems }
        }
        is MvItemUseSpeck -> {
            // Use speck with an empty group is always unused
            val itemGroup = this.useItemGroup
            if (itemGroup != null && itemGroup.useItemList.isEmpty()) return false
            val useItem = this.useItem ?: return true
            useItem.isUsed(pathUsages)
        }
        else -> error("unreachable")
    }
}

val MvUseItem.useGroup: MvUseItemGroup? get() = this.ancestorStrict()

fun MvUseItem.isUsed(pathUsage: ScopePathUsages): Boolean {
    val (itemName, itemUsages) = this.itemUsageInfo(pathUsage) ?: return true
    // null if import is never used
    val usageResolvedItems = itemUsages[itemName] ?: return false
    if (usageResolvedItems.isEmpty()) {
        // import is used but usages are unresolved
        return true
    }
    val useAlias = this.useAlias
    val speckResolvedItems =
        if (useAlias != null) listOf(useAlias) else this.reference.multiResolve()
    // any of path usages resolve to the same named item
    return speckResolvedItems.any { it in usageResolvedItems }
}

data class UseItemInfo(val name: String, val itemUsages: ItemUsages)

private fun MvUseItem.itemUsageInfo(pathUsage: ScopePathUsages): UseItemInfo? {
    val refName = this.referenceName
    val useAlias = this.useAlias

    val itemName: String
    val itemUsages: ItemUsages
    when {
        useAlias != null && refName == "Self" -> {
            // use 0x1::module::Self as mymodule;
            itemName = useAlias.name ?: return null
            itemUsages = pathUsage.nameUsages
        }
        useAlias != null -> {
            // use 0x1::module::item as myitem;
            itemName = useAlias.name ?: return null
            itemUsages = pathUsage.all()
        }
        refName == "Self" -> {
            itemName = this.moduleName
            itemUsages = pathUsage.nameUsages
        }
        else -> {
            itemName = this.referenceName
            itemUsages = pathUsage.all()
        }
    }
    return UseItemInfo(itemName, itemUsages)
}
