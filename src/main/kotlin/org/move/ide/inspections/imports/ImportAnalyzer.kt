package org.move.ide.inspections.imports

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.imports.PathStart.Companion.pathStart
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.stdext.chain

private val MvImportsOwner.useSpecks: List<UseSpeck>
    get() {
        val specks = mutableListOf<UseSpeck>()
        for (stmt in this.useStmtList) {
            specks.addAll(stmt.useSpecks)
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

    override fun visitModuleBlock(o: MvModuleBlock) = analyzeImportsOwner(o)

    override fun visitScriptBlock(o: MvScriptBlock) = analyzeImportsOwner(o)

    override fun visitModuleSpecBlock(o: MvModuleSpecBlock) = analyzeImportsOwner(o)

    fun analyzeImportsOwner(importsOwner: MvImportsOwner) {
        analyzeUseStmtsForScope(importsOwner, ItemScope.TEST)
        analyzeUseStmtsForScope(importsOwner, ItemScope.MAIN)
    }

    private fun analyzeUseStmtsForScope(rootImportOwner: MvImportsOwner, itemScope: ItemScope) {
        val allSpecksHit = mutableSetOf<UseSpeck>()
        val rootImportOwnerWithSiblings = rootImportOwner.importOwnerWithSiblings
        val reachablePaths =
            rootImportOwnerWithSiblings
                .flatMap { it.descendantsOfType<MvPath>() }
                .mapNotNull { path -> path.pathStart?.let { Pair(path, it) } }
                .filter { it.second.usageScope == itemScope }
        for ((path, start) in reachablePaths) {
            for (importOwner in path.ancestorsOfType<MvImportsOwner>()) {
                val useSpecks =
                    importOwner.importOwnerWithSiblings
                        .flatMap { it.useSpecks }
                        .filter { it.scope == itemScope }
                val speckHit =
                    when (start) {
                        is PathStart.Module ->
                            useSpecks
                                .filter { it is UseSpeck.Module || it is UseSpeck.SelfModule }
                                // only hit first encountered to remove duplicates
                                .firstOrNull { it.nameOrAlias == start.modName }
                        is PathStart.Item ->
                            useSpecks.filterIsInstance<UseSpeck.Item>()
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
        val reachableImportOwners = rootImportOwner.descendantsOfTypeOrSelf<MvImportsOwner>()
        for (importsOwner in reachableImportOwners) {
            val scopeUseStmts = importsOwner.useStmtList.filter { it.declScope == itemScope }
            for (useStmt in scopeUseStmts) {
                val unusedSpecks = useStmt.useSpecks.toSet() - allSpecksHit
                holder.registerStmtSpeckError(useStmt, unusedSpecks)
            }
        }
    }
}

private fun ProblemsHolder.registerStmtSpeckError(useStmt: MvUseStmt, specks: Set<UseSpeck>) {
    val moduleSpecks = specks.filterIsInstance<UseSpeck.Module>()
    if (moduleSpecks.isNotEmpty()) {
        this.registerProblem(
            useStmt,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
        return
    }

    val itemSpecks = specks
    if (useStmt.useSpecks.size == itemSpecks.size) {
        // all inner speck types are covered, highlight complete useStmt
        this.registerProblem(
            useStmt,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    } else {
        for (itemUseSpeck in itemSpecks) {
            val useItem = when (itemUseSpeck) {
                is UseSpeck.SelfModule -> itemUseSpeck.useItem
                is UseSpeck.Item -> itemUseSpeck.useItem
                else -> continue
            }
            this.registerProblem(
                useItem,
                "Unused use item",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
            )
        }
    }
}

