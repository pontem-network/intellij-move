package org.move.ide.inspections.imports

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.inspections.imports.UseItemType.*
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.stdext.chain

class ImportAnalyzer2(val holder: ProblemsHolder): MvVisitor() {

    override fun visitModule(o: MvModule) = analyzeImportsOwner(o)
//    override fun visitModuleBlock(o: MvModuleBlock) = analyzeImportsOwner(o)
    override fun visitScript(o: MvScript) = analyzeImportsOwner(o)
//    override fun visitScriptBlock(o: MvScriptBlock) = analyzeImportsOwner(o)
    override fun visitModuleSpecBlock(o: MvModuleSpecBlock) = analyzeImportsOwner(o)

    fun analyzeImportsOwner(importsOwner: MvItemsOwner) {
        analyzeUseStmtsForScope(importsOwner, NamedItemScope.TEST)
        analyzeUseStmtsForScope(importsOwner, NamedItemScope.MAIN)
    }

    private fun analyzeUseStmtsForScope(rootItemsOwner: MvItemsOwner, itemScope: NamedItemScope) {
        val allUseItemsHit = mutableSetOf<UseItem>()
        val rootItemOwnerWithSiblings = rootItemsOwner.itemsOwnerWithSiblings

        val paths = rootItemOwnerWithSiblings
            .flatMap { it.descendantsOfType<MvPath>() }
            .filter { it.basePath() == it }
            .filter { it.usageScope == itemScope }
            .filter { !it.hasAncestor<MvUseSpeck>() }

        for (path in paths) {
            val basePathType = path.basePathType()
            for (itemsOwner in path.ancestorsOfType<MvItemsOwner>()) {
                val useItems =
                    itemsOwner.itemsOwnerWithSiblings
                        .flatMap { it.useItems }.filter { it.scope == itemScope }

                val useItemHit =
                    when (basePathType) {
                        is BasePathType.Item -> {
                            useItems.filter { it.type == ITEM }
                                // only hit first encountered to remove duplicates
                                .firstOrNull { it.nameOrAlias == basePathType.itemName }
                        }
                        is BasePathType.Module -> {
                            useItems.filter { it.type == MODULE || it.type == SELF_MODULE }
                                // only hit first encountered to remove duplicates
                                .firstOrNull { it.nameOrAlias == basePathType.moduleName }
                        }
                        // BasePathType.Address is fq path, and doesn't participate in imports
                        else -> null
                    }

                if (useItemHit != null) {
                    allUseItemsHit.add(useItemHit)
                    break
                }
            }
        }

        // includes self
        val reachableItemsOwners = rootItemsOwner.descendantsOfTypeOrSelf<MvItemsOwner>()
        for (itemsOwner in reachableItemsOwners) {
            val scopeUseStmts = itemsOwner.useStmtList.filter { it.declaredItemScope == itemScope }
            for (useStmt in scopeUseStmts) {
                val unusedUseItems = useStmt.useItems.toSet() - allUseItemsHit
                holder.registerStmtSpeckError2(useStmt, unusedUseItems)
            }
        }
    }
}

fun ProblemsHolder.registerStmtSpeckError2(useStmt: MvUseStmt, useItems: Set<UseItem>) {
    val moduleUseItems = useItems.filter { it.type == MODULE }
    if (moduleUseItems.isNotEmpty()) {
        this.registerProblem(
            useStmt,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
        return
    }

    if (useStmt.useItems.size == useItems.size) {
        // all inner speck types are covered, highlight complete useStmt
        this.registerProblem(
            useStmt,
            "Unused use item",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL
        )
    } else {
        for (useItem in useItems) {
            this.registerProblem(
                useItem.useSpeck,
                "Unused use item",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL
            )
        }
    }
}

val MvItemsOwner.itemsOwnerWithSiblings: List<MvItemsOwner>
    get() {
        return when (this) {
            is MvModule -> {
                // add all module spec blocks
                listOf(this).chain(this.allModuleSpecBlocks()).toList()
            }
            is MvModuleSpecBlock -> {
                // add module block
                val moduleItem = this.moduleSpec.moduleItem
                if (moduleItem != null) {
                    listOf(moduleItem, this)
                } else {
                    listOf(this)
                }
            }
            else -> listOf(this)
        }
    }

