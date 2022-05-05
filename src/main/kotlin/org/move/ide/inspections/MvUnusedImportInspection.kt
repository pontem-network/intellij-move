package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.parentOfType
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.speck

class MvUnusedImportInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitModuleUseSpeck(o: MvModuleUseSpeck) {
                val useStmt = o.parentOfType<MvUseStmt>() ?: return
                if (!o.isUsed()) {
                    holder.registerProblem(
                        useStmt,
                        "Unused use item",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
                }
            }

            override fun visitUseItem(useItem: MvUseItem) {
                if (!useItem.isUsed()) {
                    val speck = useItem.speck ?: return
                    holder.registerProblem(
                        speck,
                        "Unused use item",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
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
    val name = useItem.referenceName
    val items = useItem.reference.multiResolve()

    return items.any { it in usages.pathUsages[name].orEmpty() }
}
