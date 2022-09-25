package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.move.ide.inspections.imports.ItemUsages
import org.move.ide.inspections.imports.ScopePathUsages
import org.move.ide.inspections.imports.pathUsages
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.itemScope
import org.move.lang.core.psi.ext.moduleName
import org.move.lang.core.psi.ext.speck
import org.move.lang.core.resolve.ItemScope

class MvUnusedImportInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitImportsOwner(o: MvImportsOwner) {
                val visitedItems = mutableMapOf<ItemScope, MutableSet<String>>()
                val visitedModules = mutableMapOf<ItemScope, MutableSet<String>>()
                for (useStmt in o.useStmtList) {
                    val moduleUseSpeck = useStmt.moduleUseSpeck
                    if (moduleUseSpeck != null) {
                        val itemScope = moduleUseSpeck.itemScope
                        val scopedVisitedModules = visitedModules.getOrPut(itemScope) { mutableSetOf() }

                        val moduleName = moduleUseSpeck.name ?: continue
                        if (moduleName in scopedVisitedModules) {
                            holder.registerUnused(useStmt)
                            continue
                        }
                        scopedVisitedModules.add(moduleName)

                        if (!moduleUseSpeck.isImportedItemUsed()) {
                            holder.registerUnused(useStmt)
                        }
                        continue
                    }

                    // process items. If Self import, add to the visitedModules
                    val itemUseSpeck = useStmt.itemUseSpeck
                    if (itemUseSpeck != null) {
                        val useItems = itemUseSpeck.descendantsOfType<MvUseItem>().toList()
                        if (useItems.isEmpty()) {
                            // empty item group
                            holder.registerUnused(useStmt)
                            continue
                        }
                        for (useItem in useItems) {
                            val targetItem = useItem.speck ?: continue

                            // use .text as .name is overloaded
                            val itemName = useItem.text ?: continue
                            if (itemName == "Self") {
                                val itemScope = useItem.itemScope
                                val scopedVisitedModules = visitedModules.getOrPut(itemScope) { mutableSetOf() }

                                // Self reference to module, check against visitedModules
                                val moduleName = useItem.moduleName
                                if (moduleName in scopedVisitedModules) {
                                    holder.registerUnused(targetItem)
                                    continue
                                }
                                scopedVisitedModules.add(moduleName)

                                if (!useItem.isImportedItemUsed()) {
                                    holder.registerUnused(targetItem)
                                }
                                continue
                            }

                            val itemScope = useItem.itemScope
                            val scopedVisitedItems = visitedItems.getOrPut(itemScope) { mutableSetOf() }

                            if (itemName in scopedVisitedItems) {
                                holder.registerUnused(targetItem)
                                continue
                            }
                            scopedVisitedItems.add(itemName)

                            if (!useItem.isImportedItemUsed()) {
                                holder.registerUnused(targetItem)
                            }
                        }
                        continue
                    }
                }
            }
        }

    private fun ProblemsHolder.registerUnused(targetElement: PsiElement) {
        this.registerProblem(
            targetElement,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    }

}

fun MvElement.isImportedItemUsed(): Boolean {
    val parentImportsOwner = this.ancestorStrict<MvImportsOwner>() ?: return true
    val itemScope = this.itemScope
    val pathUsages = parentImportsOwner.pathUsages.get(itemScope)

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
            isUseItemUsed(useItem, pathUsages)
        }
        is MvUseItem -> isUseItemUsed(this, pathUsages)
        else -> error("unimplemented")
    }
}

private fun isUseItemUsed(useItem: MvUseItem, pathUsages: ScopePathUsages): Boolean {
    val refName = useItem.referenceName
    val useAlias = useItem.useAlias

    val itemName: String
    val itemUsages: ItemUsages
    when {
        useAlias != null && refName == "Self" -> {
            // use 0x1::module::Self as mymodule;
            itemName = useAlias.name ?: return true
            itemUsages = pathUsages.nameUsages
        }
        useAlias != null -> {
            // use 0x1::module::item as myitem;
            itemName = useAlias.name ?: return true
            itemUsages = pathUsages.all()
        }
        refName == "Self" -> {
            itemName = useItem.moduleName
            itemUsages = pathUsages.nameUsages
        }
        else -> {
            itemName = useItem.referenceName
            itemUsages = pathUsages.all()
        }
    }
    // null if import is never used
    val usageResolvedItems = itemUsages[itemName] ?: return false
    if (usageResolvedItems.isEmpty()) {
        // import is used but usages are unresolved
        return true
    }
    val speckResolvedItems =
        if (useAlias != null) listOf(useAlias) else useItem.reference.multiResolve()
    // any of path usages resolve to the same named item
    return speckResolvedItems.any { it in usageResolvedItems }
}
