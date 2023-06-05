package org.move.ide.inspections

import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import org.move.ide.inspections.imports.ScopePathUsages
import org.move.ide.inspections.imports.pathUsages
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

private typealias VisitedNameMap = MutableMap<ItemScope, MutableSet<String>>

private fun VisitedNameMap.getOrPut(itemScope: ItemScope): MutableSet<String> =
    this.getOrPut(itemScope) { mutableSetOf() }

class UnusedImportVisitor(val holder: ProblemsHolder) : MvVisitor() {
    override fun visitImportsOwner(useStmtOwner: MvImportsOwner) = analyzeImportsOwner(useStmtOwner)

    private fun analyzeImportsOwner(useStmtOwner: MvImportsOwner) {
        val pathUsagesInScopes = useStmtOwner.pathUsages
        val visitedItemsInScopes: VisitedNameMap = mutableMapOf()
        val visitedModulesInScopes: VisitedNameMap = mutableMapOf()

        for (useStmt in useStmtOwner.useStmtList) {
            val stmtScope = useStmt.itemScope

            val pathUsage = pathUsagesInScopes.get(stmtScope)
            val visitedModules = visitedModulesInScopes.getOrPut(stmtScope)
            val visitedItems = visitedItemsInScopes.getOrPut(stmtScope)

            val useSpeck = useStmt.useSpeck ?: continue
            when (useSpeck) {
                is MvModuleUseSpeck -> {
                    val moduleName = useSpeck.name ?: continue
                    if (moduleName in visitedModules) {
                        useStmt.highlightUnusedImport()
                        continue
                    }
                    visitedModules.add(moduleName)

                    if (!useSpeck.isUsed(pathUsage)) {
                        useStmt.highlightUnusedImport()
                    }
                    continue
                }
                is MvItemUseSpeck -> {
                    val useGroup = useSpeck.useItemGroup
                    if (useGroup == null) {
                        val useItem = useSpeck.useItem ?: continue
                        analyzeUseItem(useItem, visitedModules, visitedItems, pathUsage)
                            ?.annotationItem
                            ?.highlightUnusedImport()
                    } else {
                        val unusedItems = useGroup.useItemList
                            .mapNotNull {
                                analyzeUseItem(it, visitedModules, visitedItems, pathUsage)
                            }
                        if (unusedItems.size == useGroup.useItemList.size) {
                            useStmt.highlightUnusedImport()
                        } else {
                            unusedItems.forEach { it.annotationItem.highlightUnusedImport() }
                        }
                    }
                }
            }
        }
    }

    // returns MvUseItem if unused, else null
    private fun analyzeUseItem(
        useItem: MvUseItem,
        visitedModules: MutableSet<String>,
        visitedItems: MutableSet<String>,
        pathUsage: ScopePathUsages,
    ): MvUseItem? {
        if (useItem.originalName == "Self") {
            // Self reference to module, check against visitedModules
            val moduleName = useItem.moduleName
            if (moduleName in visitedModules) {
                return useItem
            }
            visitedModules.add(moduleName)

            if (!useItem.isUsed(pathUsage)) {
                return useItem
            }
            return null
        }

        val itemNameOrAlias = useItem.nameOrAlias ?: return useItem
        if (itemNameOrAlias in visitedItems) {
            return useItem
        }
        visitedItems.add(itemNameOrAlias)

        if (!useItem.isUsed(pathUsage)) {
            return useItem
        }
        return null
    }

    private fun MvElement.highlightUnusedImport() {
        holder.registerProblem(
            this,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    }
}

class MvUnusedImportInspection : MvLocalInspectionTool() {

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = UnusedImportVisitor(holder)

    companion object {
        fun isEnabled(project: Project): Boolean {
            val profile = InspectionProjectProfileManager.getInstance(project).currentProfile
            return profile.isToolEnabled(HighlightDisplayKey.find(SHORT_NAME))
        }

        const val SHORT_NAME: String = "MvUnusedImport"
    }
}
