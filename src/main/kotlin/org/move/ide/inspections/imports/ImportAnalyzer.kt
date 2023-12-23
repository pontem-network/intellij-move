package org.move.ide.inspections.imports

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.inspections.imports.PathStart.Companion.pathStart
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.stdext.chain

private typealias VisitedNameMap = MutableMap<ItemScope, MutableSet<String>>

private fun VisitedNameMap.getOrPut(itemScope: ItemScope): MutableSet<String> =
    this.getOrPut(itemScope) { mutableSetOf() }

private val MvImportsOwner.useSpeckTypes: List<UseSpeckType>
    get() {
        val specks = mutableListOf<UseSpeckType>()
        for (stmt in this.useStmtList) {
            specks.addAll(stmt.useSpeckTypes)
        }
        return specks
    }

private val MvImportsOwner.importOwnerWithSiblings: List<MvImportsOwner>
    get() {
        return when (this) {
            is MvModuleBlock -> {
                // add all module spec blocks
                listOf(this).chain(this.module.allModuleSpecBlocks()).toList()
            }
            is MvModuleSpecBlock -> {
                // add module block
                val moduleBlock = this.moduleSpec.moduleItem?.moduleBlock
                if (moduleBlock != null) {
                    listOf(moduleBlock, this)
                } else {
                    listOf(this)
                }
            }
            else -> listOf(this)
        }
    }

class ImportAnalyzer(val holder: ProblemsHolder): MvVisitor() {
    override fun visitModuleBlock(o: MvModuleBlock) {

        val allSpecksHit = mutableSetOf<UseSpeckType>()
        val moduleBlockWithSiblings = o.importOwnerWithSiblings

        val reachablePaths = moduleBlockWithSiblings.flatMap { it.descendantsOfType<MvPath>() }
        for (path in reachablePaths) {
            for (importOwner in path.ancestorsOfType<MvImportsOwner>()) {
                val start = path.pathStart
                val useSpeckTypes =
                    importOwner.importOwnerWithSiblings.flatMap { it.useSpeckTypes }
                val speckHit =
                    when (start) {
                        is PathStart.Module ->
                            useSpeckTypes
                                .filter { it is UseSpeckType.Module || it is UseSpeckType.SelfModule }
                                // only hit first encountered to remove duplicates
                                .firstOrNull { it.nameOrAlias == start.modName }
                        is PathStart.Item ->
                            useSpeckTypes.filterIsInstance<UseSpeckType.Item>()
                                // only hit first encountered to remove duplicates
                                .firstOrNull { it.nameOrAlias == start.itemName }
                        // PathStart.Address is fq path, and doesn't participate in imports
                        else -> null
                    }
                if (speckHit != null) {
                    allSpecksHit.add(speckHit)
                    break
                }
            }
        }

        // includes self
        val reachableImportOwners =
            o.importOwnerWithSiblings.flatMap { it.descendantsOfTypeOrSelf<MvImportsOwner>() }
        for (importsOwner in reachableImportOwners) {
            for (useStmt in importsOwner.useStmtList) {
                val unusedSpecks = useStmt.useSpeckTypes.toSet() - allSpecksHit
                holder.registerStmtSpeckTypesError(useStmt, unusedSpecks)
            }
        }
    }

    private fun analyzeImportsOwner(useStmtOwner: MvImportsOwner) {
        val pathUsagesInScopes = useStmtOwner.pathUsages

        val visitedItemsInScopes: VisitedNameMap = mutableMapOf()
        val visitedModulesInScopes: VisitedNameMap = mutableMapOf()

        for (useStmt in useStmtOwner.useStmtList) {
            val stmtScope = useStmt.itemScope

            val pathUsage = pathUsagesInScopes.getScopeUsages(stmtScope)

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
        val originalItemName = useItem.identifier.text
        val aliasName = useItem.useAlias?.let { it.name ?: return null }

        if (originalItemName == "Self") {
            // Self reference to module, check against visitedModules
            val moduleName = aliasName ?: useItem.moduleName
            if (moduleName in visitedModules) {
                return useItem
            }
            visitedModules.add(moduleName)

            if (!useItem.isUsed(pathUsage)) {
                return useItem
            }
            return null
        }

        val itemName = aliasName ?: originalItemName
        if (itemName in visitedItems) {
            return useItem
        }
        visitedItems.add(itemName)

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

private fun ProblemsHolder.registerStmtSpeckTypesError(
    useStmt: MvUseStmt,
    speckTypes: Set<UseSpeckType>
) {
    val moduleSpeckTypes = speckTypes.filterIsInstance<UseSpeckType.Module>()
    if (moduleSpeckTypes.isNotEmpty()) {
        this.registerProblem(
            useStmt,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
        return
    }

    val itemSpeckTypes = speckTypes
    if (useStmt.useSpeckTypes.size == itemSpeckTypes.size) {
        // all inner speck types are covered, highlight complete useStmt
        this.registerProblem(
            useStmt,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    } else {
        for (itemUseSpeck in itemSpeckTypes) {
            val useItem = when (itemUseSpeck) {
                is UseSpeckType.SelfModule -> itemUseSpeck.useItem
                is UseSpeckType.Item -> itemUseSpeck.useItem
                else -> continue
            }
            this.registerProblem(
                useItem,
                "Unused use item",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
            )
        }
    }
//    when (useSpeckType) {
//        is UseSpeckType.Module -> {
//            val useStmt = useSpeckType.moduleUseSpeck.parent as MvUseStmt
//        }
//        is UseSpeckType.Item -> {
//            val useItem = useSpeckType.useItem
//            val maybeUseGroup = useItem.useGroup
//            if (maybeUseGroup == null) {
//                val useStmt = useItem.useStmt
//                this.registerProblem(
//                    useStmt,
//                    "Unused use item",
//                    ProblemHighlightType.LIKE_UNUSED_SYMBOL
//                )
//            } else {
//                this.registerProblem(
//                    useItem,
//                    "Unused use item",
//                    ProblemHighlightType.LIKE_UNUSED_SYMBOL
//                )
//            }
//        }
//    }
}

inline fun <reified T: PsiElement> PsiElement.leafsWithParentsOfType(): List<Sequence<T>> {
    return PsiTreeUtil.findChildrenOfType(this, T::class.java)
        .map {
            val ancestors = it.ancestorsOfType<T>()
            ancestors
        }
}

inline fun <reified T: PsiElement> PsiElement.leafsWithParentsOfTypeWithDepth(): List<Pair<Sequence<T>, Int>> {
    return PsiTreeUtil.findChildrenOfType(this, T::class.java)
        .map {
            val ancestors = it.ancestorsOfType<T>()
            Pair(ancestors, ancestors.count())
        }
}

