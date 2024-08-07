package org.move.ide.refactoring

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.ImportOptimizer
import com.intellij.psi.*
import org.move.ide.inspections.MvUnusedImportInspection
import org.move.ide.inspections.imports.ImportAnalyzer2
import org.move.ide.utils.imports.COMPARATOR_FOR_ITEMS_IN_USE_GROUP
import org.move.ide.utils.imports.UseStmtWrapper
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.stdext.withNext

class MvImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is MoveFile

    override fun processFile(file: PsiFile) = Runnable {
        if (!MvUnusedImportInspection.isEnabled(file.project)) return@Runnable

        val documentManager = PsiDocumentManager.getInstance(file.project)
        val document = documentManager.getDocument(file)
        if (document != null) {
            documentManager.commitDocument(document)
        }

        val holder = ProblemsHolder(InspectionManager.getInstance(file.project), file, false)
        val importVisitor = ImportAnalyzer2(holder)
        object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is MvItemsOwner) {
                    importVisitor.analyzeImportsOwner(element)
                } else {
                    super.visitElement(element)
                }
            }
        }.visitFile(file)

        val useElements = holder.results.map { it.psiElement }
        for (useElement in useElements) {
            when (useElement) {
                is MvUseStmt -> {
                    (useElement.nextSibling as? PsiWhiteSpace)?.delete()
                    useElement.delete()
                }
                is MvUseSpeck -> {
                    // remove whitespace following comma, if first position in a group
                    val useGroup = useElement.parent as? MvUseGroup
                    if (useGroup != null
                        && useGroup.useSpeckList.firstOrNull() == useElement
                    ) {
                        val followingComma = useElement.getNextNonCommentSibling()
                        followingComma?.rightSiblings
                            ?.takeWhile { it is PsiWhiteSpace }
                            ?.forEach { it.delete() }
                    }
                    useElement.deleteWithSurroundingCommaAndWhitespace()
                }
            }
        }

        val psiFactory = file.project.psiFactory

        val useStmtsWithOwner = file.descendantsOfType<MvUseStmt>().groupBy { it.parent }
        for ((stmtOwner, useStmts) in useStmtsWithOwner.entries) {
            useStmts.forEach { useStmt ->
                useStmt.useSpeck?.let {
                    removeCurlyBracesIfPossible(it, psiFactory)
                    it.useGroup?.sortUseSpecks()
                }
            }
            if (stmtOwner is MvModule) {
                reorderUseStmtsIntoGroups(stmtOwner)
            }
        }
    }

    /** Returns true if successfully removed, e.g. `use aaa::{bbb};` -> `use aaa::bbb;` */
    private fun removeCurlyBracesIfPossible(rootUseSpeck: MvUseSpeck, psiFactory: MvPsiFactory) {
        val itemUseSpeck = rootUseSpeck.useGroup?.asTrivial ?: return

        val newUseSpeck = psiFactory.useSpeck("0x1::dummy::call")
        val newUseSpeckPath = newUseSpeck.path
        newUseSpeckPath.path?.replace(rootUseSpeck.path)
        itemUseSpeck.path.identifier?.let { newUseSpeckPath.identifier?.replace(it) }

        val useAlias = itemUseSpeck.useAlias
        if (useAlias != null) {
            newUseSpeck.add(useAlias)
        }

        rootUseSpeck.replace(newUseSpeck)
    }

    private fun reorderUseStmtsIntoGroups(itemsOwner: MvItemsOwner) {
        val useStmts = itemsOwner.useStmtList
        val firstItem = itemsOwner.firstItem ?: return
        val psiFactory = itemsOwner.project.psiFactory
        val sortedUses = useStmts
            .asSequence()
            .map { UseStmtWrapper(it) }
            .sorted()
        for ((useWrapper, nextUseWrapper) in sortedUses.withNext()) {
            val addedUseItem = itemsOwner.addBefore(useWrapper.useStmt, firstItem)
            itemsOwner.addAfter(psiFactory.createNewline(), addedUseItem)
            val addNewLine =
                useWrapper.packageGroupLevel != nextUseWrapper?.packageGroupLevel
            if (addNewLine) {
                itemsOwner.addAfter(psiFactory.createNewline(), addedUseItem)
            }
        }
        useStmts.forEach {
            (it.nextSibling as? PsiWhiteSpace)?.delete()
            it.delete()
        }
    }

    private fun MvUseGroup.sortUseSpecks() {
        val sortedList = useSpeckList
            .sortedWith(COMPARATOR_FOR_ITEMS_IN_USE_GROUP)
            .map { it.copy() }
        useSpeckList.zip(sortedList).forEach { it.first.replace(it.second) }
    }
}
