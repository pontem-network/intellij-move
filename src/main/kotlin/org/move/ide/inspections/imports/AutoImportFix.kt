package org.move.ide.inspections.imports

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.lang.MvFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.childrenOfType
import org.move.lang.core.psi.ext.hasAncestor
import org.move.lang.core.resolve.MvReferenceElement
import org.move.lang.moveProject
import org.move.openapiext.checkWriteAccessAllowed
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.runWriteCommandAction

class AutoImportFix(element: PsiElement) : LocalQuickFixOnPsiElement(element), HighPriorityAction {
    private var isConsumed: Boolean = false

    override fun getFamilyName() = NAME
    override fun getText() = familyName

    public override fun isAvailable(): Boolean = super.isAvailable() && !isConsumed

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        invoke(project)
    }

    fun invoke(project: Project) {
        val element = startElement as? MvReferenceElement ?: return
        if (element.reference == null) return
        if (element.hasAncestor<MvUseStmt>()) return

        // TODO: no auto-import if name in scope, but cannot be resolved

        val name = element.referenceName ?: return
        val moveProject = element.moveProject ?: return
        val searchScope = moveProject.searchScope()
        val files = MvNamedElementIndex
            .findFilesByElementName(project, name, searchScope)
            .toMutableList()
        if (isUnitTestMode) {
            // always add current file in tests
            val currentFile = element.containingFile as? MvFile ?: return
            files.add(0, currentFile)
        }
        val candidates = files
            .flatMap { it.qualifiedItems() }
            .filter { it.name == name }
            .mapNotNull { el -> el.usePath?.let { ImportCandidate(el, it) } }
        if (candidates.isEmpty()) return

        if (candidates.size == 1) {
            project.runWriteCommandAction {
                candidates.first().import(element)
            }
        } else {
            DataManager.getInstance().dataContextFromFocusAsync.onSuccess {
                chooseItemAndImport(project, it, candidates, element)
            }
        }
    }

    private fun chooseItemAndImport(
        project: Project,
        dataContext: DataContext,
        items: List<ImportCandidate>,
        context: MvElement
    ) {
        showItemsToImportChooser(project, dataContext, items) { selectedValue ->
            project.runWriteCommandAction {
                selectedValue.import(context)
            }
        }
    }

    companion object {
        const val NAME = "Import"
    }
}

data class ImportCandidate(val element: MvQualifiedNamedElement, val usePath: String)

private fun ImportCandidate.import(context: MvElement) {
    checkWriteAccessAllowed()
    val psiFactory = element.project.psiFactory
    val insertionScope = context.containingModule?.moduleBlock
        ?: context.containingScript?.scriptBlock
        ?: return
    insertionScope.insertUseItem(psiFactory, usePath)
}

private fun MvItemsOwner.insertUseItem(psiFactory: MvPsiFactory, usePath: String) {
    val useStmt = psiFactory.useStmt(usePath)
    val anchor = childrenOfType<MvUseStmt>().lastElement
    if (anchor != null) {
        addAfter(useStmt, anchor)
    } else {
        val firstItem = this.items().first()
        addBefore(useStmt, firstItem)
        addAfter(psiFactory.createNewline(), firstItem)
    }
}

private val <T : MvElement> List<T>.lastElement: T? get() = maxByOrNull { it.textOffset }
