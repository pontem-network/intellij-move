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
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.childrenOfType
import org.move.lang.core.psi.ext.hasAncestor
import org.move.lang.core.psi.ext.names
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

    data class Context(val candidates: List<ImportCandidate>)

    fun invoke(project: Project) {
        val element = startElement as? MvReferenceElement ?: return
        if (element.reference == null) return
        if (element.hasAncestor<MvUseStmt>()) return

        val name = element.referenceName ?: return
        val candidates = getImportCandidates(element, name)
        if (candidates.isEmpty()) return

//        val moveProject = element.moveProject ?: return
//        val searchScope = moveProject.searchScope()
//        val files = MvNamedElementIndex
//            .findFilesByElementName(project, name, searchScope)
//            .toMutableList()
//        if (isUnitTestMode) {
//            // always add current file in tests
//            val currentFile = element.containingFile as? MvFile ?: return
//            files.add(0, currentFile)
//        }
//        val candidates = files
//            .flatMap { it.qualifiedItems() }
//            .filter { it.name == name }
//            .mapNotNull { el -> el.usePath?.let { ImportCandidate(el, it) } }
//        if (candidates.isEmpty()) return

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

        fun findApplicableContext(refElement: MvReferenceElement): Context? {
            if (refElement.reference == null) return null
            if (refElement.resolvable) return null
            if (refElement.ancestorStrict<MvUseStmt>() != null) return null

            // TODO: no auto-import if name in scope, but cannot be resolved

            val candidates = when (refElement) {
                is MvModuleRef -> {
                    val refName = refElement.referenceName ?: return null
                    getImportCandidates(refElement, refName)
                }
                is MvPath -> {
                    val refName = refElement.referenceName ?: return null
                    getImportCandidates(refElement, refName)
                }
                else -> return null
            }
            return Context(candidates.toList())
        }

        fun getImportCandidates(contextElement: MvReferenceElement, target: String): List<ImportCandidate> {
            val name = contextElement.referenceName ?: return emptyList()
            val moveProject = contextElement.moveProject ?: return emptyList()
            val searchScope = moveProject.searchScope()
            val files = MvNamedElementIndex
                .findFilesByElementName(contextElement.project, name, searchScope)
                .toMutableList()
            if (isUnitTestMode) {
                // always add current file in tests
                val currentFile = contextElement.containingFile as? MvFile ?: return emptyList()
                files.add(0, currentFile)
            }
            val candidates = files
                .flatMap { it.qualifiedItems() }
                .filter { it.name == target }
                .mapNotNull { el -> el.usePath?.let { ImportCandidate(el, it) } }
            return candidates
        }
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
    val newUseStmt = psiFactory.useStmt(usePath)
    if (this.tryGroupWithOtherUseItems(psiFactory, newUseStmt)) return

    val anchor = childrenOfType<MvUseStmt>().lastElement
    if (anchor != null) {
        addAfter(newUseStmt, anchor)
    } else {
        val firstItem = this.items().first()
        addBefore(newUseStmt, firstItem)
        addAfter(psiFactory.createNewline(), firstItem)
    }
}

private fun MvItemsOwner.tryGroupWithOtherUseItems(psiFactory: MvPsiFactory, newUseStmt: MvUseStmt): Boolean {
    val newUseSpeck = newUseStmt.itemUseSpeck ?: return false
    val newName = newUseSpeck.names().singleOrNull() ?: return false
    val newFqModule = newUseSpeck.fqModuleRef
    return useStmtList
        .mapNotNull { it.itemUseSpeck }
        .any { it.tryGroupWith(psiFactory, newFqModule, newName) }
}

private fun MvItemUseSpeck.tryGroupWith(
    psiFactory: MvPsiFactory,
    newFqModule: MvFQModuleRef,
    newName: String
): Boolean {
    if (!this.fqModuleRef.textMatches(newFqModule)) return false
    if (newName in this.names()) return true
    val speck = psiFactory.itemUseSpeck(newFqModule.text, this.names() + newName)
    this.replace(speck)
    return true
}

private val <T : MvElement> List<T>.lastElement: T? get() = maxByOrNull { it.textOffset }
