package org.move.ide.refactoring

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.inspections.imports.pathUsages
import org.move.ide.inspections.isUseItemUsed
import org.move.ide.intentions.removeCurlyBraces
import org.move.lang.MoveFile
import org.move.lang.MvElementTypes.L_BRACE
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

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
        for (moduleSpec in moveFile.moduleSpecs()) {
            val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
            optimizeImports(moduleSpecBlock)
        }
    }

    private fun optimizeImports(importsOwner: MvImportsOwner) {
        removeUnusedImports(importsOwner)
//        mergeTestOnlyImportsIntoMainImports(importsOwner)
        mergeItemGroups(importsOwner)
//        removeDuplicates(importsOwner)
        sortImports(importsOwner)
    }

    private fun removeUnusedImports(importsOwner: MvImportsOwner) {
        val psiFactory = importsOwner.project.psiFactory
        val pathUsages = importsOwner.pathUsages
        val useStmts = importsOwner.useStmtList
        for (useStmt in useStmts) {
            val moduleSpeck = useStmt.moduleUseSpeck
            if (moduleSpeck != null) {
                if (!moduleSpeck.isUseItemUsed(pathUsages)) {
                    useStmt.deleteWithLeadingWhitespace()
                    continue
                }
            }
            val useSpeck = useStmt.itemUseSpeck
            if (useSpeck != null) {
                var itemGroup = useSpeck.useItemGroup
                if (itemGroup != null) {
                    val usedItems = itemGroup.useItemList.filter { it.isUseItemUsed(pathUsages) }
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
                if (!useSpeck.isUseItemUsed(pathUsages)) {
                    // single unused import 0x1::M::call;
                    useStmt.deleteWithLeadingWhitespace()
                }
            }
        }
    }

    private fun mergeTestOnlyImportsIntoMainImports(useStmtOwner: MvImportsOwner) {
        useStmtOwner.useStmtList
            .flatMap { it.childUseItems }
    }

    private fun mergeItemGroups(useStmtOwner: MvImportsOwner) {
        val psiFactory = useStmtOwner.project.psiFactory
        val leftBrace = useStmtOwner.findFirstChildByType(L_BRACE) ?: return

        val useStmts = useStmtOwner.useStmtList
        useStmts
            .groupBy { Pair(it.fqModuleText, it.isTestOnly) }
            .forEach { (key, stmts) ->
                val (fqModuleText, isTestOnly) = key
                if (fqModuleText == null) return@forEach

                // special case: if single stmt and import like `use 0x1::Main::Self;`, change to `0x1::Main`
                if (stmts.size == 1) {
                    val stmt = stmts.single()
                    val useItem = stmt.childUseItems.singleOrNull()?.takeIf { it.text == "Self" }
                    if (useItem != null) {
                        val newStmt = psiFactory.useStmt(fqModuleText, isTestOnly)
                        stmt.replace(newStmt)
                    }
                    return@forEach
                }

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
    }

    private fun removeDuplicates(importsOwner: MvImportsOwner) {

    }

    private fun sortImports(useStmtOwner: MvImportsOwner) {
        val psiFactory = useStmtOwner.project.psiFactory
        val offset =
            (useStmtOwner.findFirstChildByType(L_BRACE)?.textOffset ?: return) + 1
        val first = useStmtOwner.childrenOfType<MvElement>()
            .firstOrNull { it.textOffset >= offset && it !is MvAttr } ?: return

        val useStmts = useStmtOwner.useStmtList
        val sortedUseGroups = useStmts
            .groupBy { it.useGroupLevel }
            .map { (groupLevel, items) ->
                val sortedItems = items
                    .sortedBy {
                        val address = it.addressRef?.normalizedText
                        val speck = it.useSpeckText
                        when (address) {
                            "aptos_std" ->
                                speck.replaceRange(0 until "aptos_std".length, "1")
                            "aptos_framework" ->
                                speck.replaceRange(0 until "aptos_framework".length, "2")
                            else -> speck
                        }
                    }
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
        useStmts.forEach { it.deleteWithLeadingWhitespace() }
    }
}

private fun MvUseStmt.deleteWithLeadingWhitespace() {
    this.nextSibling?.takeIf { it.isWhitespace() }?.delete()
    this.delete()
}
