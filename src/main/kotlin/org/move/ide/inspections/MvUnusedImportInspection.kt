package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.parentOfType
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict

class MvUnusedImportInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return object : MvVisitor() {
            override fun visitModuleUse(o: MvModuleUse) {
                val useStmt = o.parentOfType<MvUseStmt>() ?: return
                if (!o.isUsed()) {
                    holder.registerProblem(
                        useStmt,
                        "Unused use item",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
                }
            }

            override fun visitModuleItemUse(o: MvModuleItemUse) {
                val useStmt = o.parentOfType<MvUseStmt>() ?: return
                if (!o.isUsed()) {
                    holder.registerProblem(
                        useStmt,
                        "Unused use item",
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
                }
            }

            override fun visitMultiItemUse(o: MvMultiItemUse) {
                // will be handled in another method
                if (o.parentOfType<MvModuleItemUse>()?.isUsed() != true) return
                for (itemUse in o.itemUseList) {
                    if (!itemUse.isUsed()) {
                        holder.registerProblem(
                            itemUse,
                            "Unused use item",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL
                        )
                    }
                }
            }
        }
    }
}

fun MvItemUse.isUsed(): Boolean {
    if (!this.resolvable) return true
    val owner = this.ancestorStrict<MvUseStmtOwner>() ?: return true
    val usageMap = owner.pathUsageMap
    return isItemUseUsed(this, usageMap)
}

fun MvModuleUse.isUsed(): Boolean {
    if (this.fqModuleRef?.resolvable != true) return true

    val owner = this.parentOfType<MvUseStmt>()?.parentOfType<MvUseStmtOwner>() ?: return true
    val usageMap = owner.pathUsageMap
    return isModuleUseItemUsed(this, usageMap)
}

fun MvModuleItemUse.isUsed(): Boolean {
    if (this.itemUse?.resolvable != true) return true

    val owner = this.parentOfType<MvUseStmt>()?.parentOfType<MvUseStmtOwner>() ?: return true
    val usageMap = owner.pathUsageMap
    return isUseItemUsed(this, usageMap)
}

private fun isModuleUseItemUsed(moduleUse: MvModuleUse, usages: PathUsageMap): Boolean {
    val moduleName = moduleUse.fqModuleRef?.referenceName ?: return true
    val items = moduleUse.fqModuleRef?.reference?.multiResolve().orEmpty()
    return items.any { it in usages.pathUsages[moduleName].orEmpty() }
}

private fun isUseItemUsed(useSpeck: MvModuleItemUse, usages: PathUsageMap): Boolean {
    // Use speck with an empty group is always unused
    val multiUse = useSpeck.multiItemUse
    if (multiUse != null && multiUse.itemUseList.isEmpty()) return false

    val itemUse = useSpeck.itemUse ?: return true
    return isItemUseUsed(itemUse, usages)
}

private fun isItemUseUsed(itemUse: MvItemUse, usages: PathUsageMap): Boolean {
    val name = itemUse.referenceName
    val items = itemUse.reference.multiResolve()

    return items.any { it in usages.pathUsages[name].orEmpty() }
}
