package org.move.ide.refactoring

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.move.ide.inspections.isUsed
import org.move.ide.intentions.removeCurlyBraces
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvItemsOwner
import org.move.lang.modules

class MvImportOptimizer: ImportOptimizer {
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

    private fun optimizeImports(useStmtOwner: MvItemsOwner) {
        val useStmts = useStmtOwner.useStmtList
        for (useStmt in useStmts) {
            val moduleSpeck = useStmt.moduleUseSpeck
            if (moduleSpeck != null) {
                if (!moduleSpeck.isUsed()) {
                    useStmt.delete()
                    return
                }
            }
            val useSpeck = useStmt.itemUseSpeck
            if (useSpeck != null) {
                val itemGroup = useSpeck.useItemGroup
                if (itemGroup != null) {
                    if (itemGroup.useItemList.size == 1) {
                        if (itemGroup.useItemList.first().isUsed()) {
                            // used 0x1::M::{call};
                            itemGroup.removeCurlyBraces()
                        } else {
                            // unused 0x1::M::{call};
                            useStmt.delete()
                        }
                    } else {
                        val items = itemGroup.useItemList.filter { it.isUsed() }
                        if (items.size == 1) {
                            // 0x1::M::{Used, unused} -> 0x1::M::Used;
                            itemGroup.replace(items.first())
                        }
                    }
                }
                if (!useSpeck.isUsed()) {
                    // single unused import 0x1::M::call;
                    useStmt.delete()
                }
            }
        }
    }
}
