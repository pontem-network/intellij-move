package org.move.ide.refactoring

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.move.ide.inspections.isUsed
import org.move.ide.intentions.removeCurlyBraces
import org.move.lang.MoveFile
import org.move.lang.MvElementTypes.L_BRACE
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.modules

class ImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is MoveFile

    override fun processFile(file: PsiFile) = Runnable {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file)
        if (document != null) {
            documentManager.commitDocument(document)
        }
        val moveFile = file as MoveFile
        for (module in moveFile.modules()) {
            val block = module.moduleBlock ?: continue
            optimizeImports(block)
        }
        for (scriptBlock in moveFile.scriptBlocks()) {
            optimizeImports(scriptBlock)
        }
    }

    private fun optimizeImports(itemsOwner: MvItemsOwner) {
        removeUnusedImports(itemsOwner)
        mergeImportsIntoGroups(itemsOwner)
        sortImports(itemsOwner)
    }

    private fun removeUnusedImports(useStmtOwner: MvItemsOwner) {
        fun MvUseStmt.deleteWithLeadingWhitespace() {
            if (this.nextSibling.isWhitespace()) this.nextSibling.delete()
            this.delete()
        }

        val psiFactory = useStmtOwner.project.psiFactory
        val useStmts = useStmtOwner.useStmtList
        for (useStmt in useStmts) {
            val moduleSpeck = useStmt.moduleUseSpeck
            if (moduleSpeck != null) {
                if (!moduleSpeck.isUsed()) {
                    useStmt.deleteWithLeadingWhitespace()
                    continue
                }
            }
            val useSpeck = useStmt.itemUseSpeck
            if (useSpeck != null) {
                var itemGroup = useSpeck.useItemGroup
                if (itemGroup != null) {
                    val usedItems = itemGroup.useItemList.filter { it.isUsed() }
                    if (usedItems.isEmpty()) {
                        useStmt.deleteWithLeadingWhitespace()
                    } else {
                        val newItemGroup = psiFactory.useItemGroup(usedItems.map { it.text })
                        itemGroup = itemGroup.replace(newItemGroup) as MvUseItemGroup
                    }
                    if (itemGroup.useItemList.size == 1) {
                        itemGroup.removeCurlyBraces()
                    }
                }
                if (!useSpeck.isUsed()) {
                    // single unused import 0x1::M::call;
                    useStmt.deleteWithLeadingWhitespace()
                }
            }
        }
    }

    private fun mergeImportsIntoGroups(useStmtOwner: MvItemsOwner) {
        val psiFactory = useStmtOwner.project.psiFactory
        val leftBrace = useStmtOwner.findFirstChildByType(L_BRACE) ?: return

        val useStmts = useStmtOwner.useStmtList
        useStmts
            .groupBy { Pair(it.fqModuleText, it.isTestOnly) }
            .forEach { (pair, stmts) ->
                val (fqModuleText, isTestOnly) = pair
                if (stmts.size > 1) {
                    val useItemNames = mutableListOf<String>()
                    if (stmts.any { it.moduleUseSpeck != null }) {
                        useItemNames.add("Self")
                    }
                    useItemNames.addAll(stmts.flatMap { it.childUseItems }.map { it.text })
                    val newStmt =
                        psiFactory.useStmt(
                            "$fqModuleText::{${useItemNames.joinToString(", ")}}",
                            isTestOnly
                        )
                    useStmtOwner.addAfter(newStmt, leftBrace)
                    stmts.forEach { it.delete() }
                }
//                }
            }
    }

    private fun sortImports(useStmtOwner: MvItemsOwner) {
        val psiFactory = useStmtOwner.project.psiFactory
        val offset =
            (useStmtOwner.findFirstChildByType(L_BRACE)?.textOffset ?: return) + 1
        val first = useStmtOwner.childrenOfType<MvElement>()
            .firstOrNull { it.textOffset >= offset && it !is MvAttr } ?: return

        val useStmts = useStmtOwner.useStmtList
        val sortedUseGroups = useStmts
            .groupBy { it.addressRef?.useGroupLevel ?: -1 }
            .map { (groupLevel, items) ->
                val sortedItems = items
                    .sortedBy { "${it.isTestOnly}::${it.useSpeckText}" }
                    .mapNotNull { it.copy() as? MvUseStmt }
                groupLevel to sortedItems
            }
            .sortedBy { it.first }
        for ((_, sortedUseStmts) in sortedUseGroups) {
            var lastAddedUseItem: PsiElement? = null
            for (useStmt in sortedUseStmts) {
                lastAddedUseItem = useStmtOwner.addBefore(useStmt, first)
                useStmtOwner.addAfter(psiFactory.createNewline(), lastAddedUseItem)
            }
            useStmtOwner.addAfter(psiFactory.createNewline(), lastAddedUseItem)
        }
        useStmts.forEach {
            (it.nextSibling as? PsiWhiteSpace)?.delete()
            it.delete()
        }
    }
}
