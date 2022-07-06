package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.parentOfType
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.speck

class MvUnusedImportInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        fun registerUnused(targetElement: PsiElement) {
            holder.registerProblem(
                targetElement,
                "Unused use item",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
            )
        }
        return object : MvVisitor() {
            override fun visitModuleBlock(o: MvModuleBlock) {
                val visitedItems = mutableSetOf<String>()
                val visitedModules = mutableSetOf<String>()
                for (useStmt in o.useStmtList) {
                    val moduleUseSpeck = useStmt.moduleUseSpeck
                    if (moduleUseSpeck != null) {
                        val moduleName = moduleUseSpeck.name ?: continue
                        if (moduleName in visitedModules) {
                            registerUnused(useStmt)
                            continue
                        }
                        visitedModules.add(moduleName)

                        if (!moduleUseSpeck.isUsed()) {
                            registerUnused(useStmt)
                        }
                        continue
                    }

                    // process items. If Self import, add to the visitedModules
                    val itemUseSpeck = useStmt.itemUseSpeck
                    if (itemUseSpeck != null) {
                        for (useItem in itemUseSpeck.descendantsOfType<MvUseItem>()) {
                            val speck = useItem.speck ?: continue

                            val itemName = useItem.name ?: continue
                            if (itemName in visitedItems) {
                                registerUnused(speck)
                                continue
                            }
                            visitedItems.add(itemName)

                            if (!useItem.isUsed()) {
                                registerUnused(speck)
                            }
                        }
                        continue
                    }
                }
            }
        }
    }
}

fun MvUseItem.isUsed(): Boolean {
    val owner = this.ancestorStrict<MvItemsOwner>() ?: return true
    val usageMap = owner.pathUsages
    return isUseItemUsed(this, usageMap)
}

fun MvModuleUseSpeck.isUsed(): Boolean {
    val owner = this.parentOfType<MvItemsOwner>() ?: return true
    val usageMap = owner.pathUsages
    return isModuleUseSpeckUsed(this, usageMap)
}

fun MvItemUseSpeck.isUsed(): Boolean {
    val owner = this.parentOfType<MvItemsOwner>() ?: return true
    val usageMap = owner.pathUsages
    return isItemUseSpeckUsed(this, usageMap)
}

private fun isModuleUseSpeckUsed(moduleUse: MvModuleUseSpeck, pathUsages: PathUsages): Boolean {
    val moduleName = moduleUse.fqModuleRef?.referenceName ?: return true
    val usageResolvedItems = pathUsages.map[moduleName]
    @Suppress("FoldInitializerAndIfToElvis")
    if (usageResolvedItems == null) {
        // import is never used
        return false
    }
    if (usageResolvedItems.isEmpty()) {
        // import is used but usages are unresolved
        return true
    }
    val speckResolvedItems = moduleUse.fqModuleRef?.reference?.multiResolve().orEmpty()
    // any of path usages resolve to the same named item
    return speckResolvedItems.any { it in usageResolvedItems }
}

private fun isItemUseSpeckUsed(useSpeck: MvItemUseSpeck, usages: PathUsages): Boolean {
    // Use speck with an empty group is always unused
    val itemGroup = useSpeck.useItemGroup
    if (itemGroup != null && itemGroup.useItemList.isEmpty()) return false

    val useItem = useSpeck.useItem ?: return true
    return isUseItemUsed(useItem, usages)
}

private fun isUseItemUsed(useItem: MvUseItem, pathUsages: PathUsages): Boolean {
    var itemName = useItem.referenceName
    if (itemName == "Self") {
        val useStmt = useItem.ancestorStrict<MvUseStmt>()
        itemName = useStmt?.itemUseSpeck?.fqModuleRef?.referenceName.orEmpty()
    }
    val usageResolvedItems = pathUsages.map[itemName]
    @Suppress("FoldInitializerAndIfToElvis")
    if (usageResolvedItems == null) {
        // import is never used
        return false
    }
    if (usageResolvedItems.isEmpty()) {
        // import is used but usages are unresolved
        return true
    }
    val speckResolvedItems = useItem.reference.multiResolve()
    // any of path usages resolve to the same named item
    return speckResolvedItems.any { it in usageResolvedItems }
}
