package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.move.ide.inspections.imports.ItemUsages
import org.move.ide.inspections.imports.PathUsages
import org.move.ide.inspections.imports.pathUsages
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.moduleName
import org.move.lang.core.psi.ext.speck

class MvUnusedImportInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitImportsOwner(o: MvImportsOwner) {
                val visitedItems = mutableSetOf<String>()
                val visitedModules = mutableSetOf<String>()
                for (useStmt in o.useStmtList) {
                    val moduleUseSpeck = useStmt.moduleUseSpeck
                    if (moduleUseSpeck != null) {
                        val moduleName = moduleUseSpeck.name ?: continue
                        if (moduleName in visitedModules) {
                            holder.registerUnused(useStmt)
                            continue
                        }
                        visitedModules.add(moduleName)

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
                                // Self reference to module, check against visitedModules
                                val moduleName = useItem.moduleName
                                if (moduleName in visitedModules) {
                                    holder.registerUnused(targetItem)
                                    continue
                                }
                                visitedModules.add(moduleName)

                                if (!useItem.isImportedItemUsed()) {
                                    holder.registerUnused(targetItem)
                                }
                                continue
                            }

                            if (itemName in visitedItems) {
                                holder.registerUnused(targetItem)
                                continue
                            }
                            visitedItems.add(itemName)

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
    val owner = this.ancestorStrict<MvImportsOwner>() ?: return true
    val pathUsages = owner.pathUsages
    return when (this) {
        is MvModuleUseSpeck -> {
            val moduleName = this.fqModuleRef?.referenceName ?: return true
            // null if import is never used
            val usageResolvedItems = pathUsages.nameUsages[moduleName] ?: return false
            if (usageResolvedItems.isEmpty()) {
                // import is used but usages are unresolved
                return true
            }
            val speckResolvedItems = this.fqModuleRef?.reference?.multiResolve().orEmpty()
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

private fun isUseItemUsed(useItem: MvUseItem, pathUsages: PathUsages): Boolean {
    var itemUsages = pathUsages.all()
    var itemName = useItem.referenceName
    if (itemName == "Self") {
        itemName = useItem.moduleName
        itemUsages = pathUsages.nameUsages
    }
    // null if import is never used
    val usageResolvedItems = itemUsages[itemName] ?: return false
    if (usageResolvedItems.isEmpty()) {
        // import is used but usages are unresolved
        return true
    }
    val speckResolvedItems = useItem.reference.multiResolve()
    // any of path usages resolve to the same named item
    return speckResolvedItems.any { it in usageResolvedItems }
}
