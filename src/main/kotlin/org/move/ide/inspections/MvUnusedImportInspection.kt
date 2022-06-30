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
    if (!this.resolvable) return true
    val owner = this.ancestorStrict<MvItemsOwner>() ?: return true
    val usageMap = owner.pathUsageMap
    return isUseItemUsed(this, usageMap)
}

fun MvModuleUseSpeck.isUsed(): Boolean {
    if (this.fqModuleRef?.resolvable != true) return true

    val owner = this.parentOfType<MvUseStmt>()?.parentOfType<MvItemsOwner>() ?: return true
    val usageMap = owner.pathUsageMap
    return isModuleUseSpeckUsed(this, usageMap)
}

fun MvItemUseSpeck.isUsed(): Boolean {
    if (this.useItem?.resolvable != true) return true

    val owner = this.parentOfType<MvUseStmt>()?.parentOfType<MvItemsOwner>() ?: return true
    val usageMap = owner.pathUsageMap
    return isItemUseSpeckUsed(this, usageMap)
}

private fun isModuleUseSpeckUsed(moduleUse: MvModuleUseSpeck, usages: PathUsageMap): Boolean {
    val moduleName = moduleUse.fqModuleRef?.referenceName ?: return true
    val items = moduleUse.fqModuleRef?.reference?.multiResolve().orEmpty()
    return items.any { it in usages.pathUsages[moduleName].orEmpty() }
}

private fun isItemUseSpeckUsed(useSpeck: MvItemUseSpeck, usages: PathUsageMap): Boolean {
    // Use speck with an empty group is always unused
    val itemGroup = useSpeck.useItemGroup
    if (itemGroup != null && itemGroup.useItemList.isEmpty()) return false

    val useItem = useSpeck.useItem ?: return true
    return isUseItemUsed(useItem, usages)
}

private fun isUseItemUsed(useItem: MvUseItem, usages: PathUsageMap): Boolean {
    val items = useItem.reference.multiResolve()
    var name = useItem.referenceName
    if (name == "Self") {
        val useStmt = useItem.ancestorStrict<MvUseStmt>()
        name = useStmt?.itemUseSpeck?.fqModuleRef?.referenceName.orEmpty()
    }
    return items.any { it in usages.pathUsages[name].orEmpty() }
}
