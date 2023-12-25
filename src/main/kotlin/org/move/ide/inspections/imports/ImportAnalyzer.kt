package org.move.ide.inspections.imports

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.inspections.imports.PathStart.Companion.pathStart
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.stdext.chain

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

    override fun visitModuleBlock(o: MvModuleBlock) = analyzeImportsOwner3(o)

    override fun visitScriptBlock(o: MvScriptBlock) = analyzeImportsOwner3(o)

    override fun visitModuleSpecBlock(o: MvModuleSpecBlock) = analyzeImportsOwner3(o)

    fun analyzeImportsOwner3(importsOwner: MvImportsOwner) {
        analyzeUseStmtsForScope(importsOwner, ItemScope.TEST)
        analyzeUseStmtsForScope(importsOwner, ItemScope.MAIN)
    }

    private fun analyzeUseStmtsForScope(rootImportOwner: MvImportsOwner, itemScope: ItemScope) {
        val allSpecksHit = mutableSetOf<UseSpeckType>()
        val moduleBlockWithSiblings = rootImportOwner.importOwnerWithSiblings
        val reachablePaths =
            moduleBlockWithSiblings.flatMap { it.descendantsOfType<MvPath>() }
                .mapNotNull { path -> path.pathStart?.let { Pair(path, it) } }
                .filter { it.second.usageScope == itemScope }
        for ((path, start) in reachablePaths) {
            for (importOwner in path.ancestorsOfType<MvImportsOwner>()) {
                val useSpeckTypes =
                    importOwner.importOwnerWithSiblings
                        .flatMap { it.useSpeckTypes }
                        .filter { it.scope == itemScope }
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
        val reachableImportOwners = rootImportOwner.descendantsOfTypeOrSelf<MvImportsOwner>()
        for (importsOwner in reachableImportOwners) {
            val scopeUseStmts = importsOwner.useStmtList.filter { it.declScope == itemScope }
            for (useStmt in scopeUseStmts) {
                val unusedSpecks = useStmt.useSpeckTypes.toSet() - allSpecksHit
                holder.registerStmtSpeckTypesError(useStmt, unusedSpecks)
            }
        }
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
}

