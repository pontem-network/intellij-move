package org.move.ide.refactoring

import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.move.ide.inspections.isUsed
import org.move.ide.intentions.removeCurlyBraces
import org.move.lang.MvFile
import org.move.lang.core.psi.MvUseStmtOwner
import org.move.lang.core.psi.psiFactory
import org.move.lang.modules

class MvImportOptimizer: ImportOptimizer {
    override fun supports(file: PsiFile) = file is MvFile

    override fun processFile(file: PsiFile) = Runnable {
        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file)
        if (document != null) {
            documentManager.commitDocument(document)
        }
        val mvFile = file as MvFile
        for (module in mvFile.modules()) {
            val block = module.moduleBlock ?: continue
            optimizeImports(block)
        }
        for (scriptBlock in mvFile.scriptBlocks()) {
            optimizeImports(scriptBlock)
        }
    }

    fun optimizeImports(useStmtOwner: MvUseStmtOwner) {
        val useStmts = useStmtOwner.useStmtList
        for (useStmt in useStmts) {
            val moduleUse = useStmt.moduleUse
            if (moduleUse != null) {
                if (!moduleUse.isUsed()) {
                    useStmt.delete()
                    return
                }
            }
            val useItem = useStmt.moduleItemUse
            if (useItem != null) {
                val multiUseItem = useStmt.moduleItemUse?.multiItemUse
                if (multiUseItem != null) {
                    if (multiUseItem.itemUseList.size == 1) {
                        if (multiUseItem.itemUseList.first().isUsed()) {
                            // used 0x1::M::{call};
                            multiUseItem.removeCurlyBraces()
                        } else {
                            // unused 0x1::M::{call};
                            useStmt.delete()
                        }
                    } else {
                        val items = multiUseItem.itemUseList.filter { it.isUsed() }
                        if (items.size == 1) {
                            // 0x1::M::{Used, unused} -> 0x1::M::Used;
                            multiUseItem.replace(items.first())
                        }
                    }
                }
                if (!useItem.isUsed()) {
                    // single unused import 0x1::M::call;
                    useStmt.delete()
                }
            }
        }
    }
}
