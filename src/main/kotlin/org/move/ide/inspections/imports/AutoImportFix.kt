package org.move.ide.inspections.imports

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.lang.MoveFile
import org.move.lang.core.completion.DefaultInsertHandler
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.Visibility
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
        val refElement = startElement as? MvReferenceElement ?: return
        if (refElement.reference == null) return
        if (refElement.hasAncestor<MvUseStmt>()) return

        val name = refElement.referenceName ?: return
        val candidates = getImportCandidates(ImportContext.from(refElement), name)
        if (candidates.isEmpty()) return

        if (candidates.size == 1) {
            project.runWriteCommandAction {
                candidates.first().import(refElement)
            }
        } else {
            DataManager.getInstance().dataContextFromFocusAsync.onSuccess {
                chooseItemAndImport(project, it, candidates, refElement)
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
            if (refElement is MvPath && refElement.moduleRef != null) return null

            // TODO: no auto-import if name in scope, but cannot be resolved

            val refName = refElement.referenceName ?: return null
            val candidates = getImportCandidates(ImportContext.from(refElement), refName)
            return Context(candidates)
        }

        fun getImportCandidates(
            context: ImportContext,
            targetName: String,
            itemFilter: (MvQualifiedNamedElement) -> Boolean = { true }
        ): List<ImportCandidate> {
            val (contextElement, itemVis) = context
            val moveProject = contextElement.moveProject ?: return emptyList()
            val searchScope = moveProject.searchScope()
            val files = MoveElementsIndex
                .findFilesByElementName(contextElement.project, targetName, searchScope)
                .toMutableList()
            if (isUnitTestMode) {
                // always add current file in tests
                val currentFile = contextElement.containingFile as? MoveFile ?: return emptyList()
                files.add(0, currentFile)
            }
            return files
                .flatMap { it.qualifiedItems(targetName, itemVis) }
                .filter(itemFilter)
                .mapNotNull { el -> el.fqPath?.let { ImportCandidate(el, it) } }
        }
    }
}

@Suppress("DataClassPrivateConstructor")
data class ImportContext private constructor(
    val pathElement: MvReferenceElement,
    val itemVis: ItemVis,
) {
    companion object {
        fun from(contextElement: MvReferenceElement, itemVis: ItemVis): ImportContext {
            return ImportContext(contextElement, itemVis)
        }

        fun from(contextElement: MvReferenceElement): ImportContext {
            val ns = contextElement.namespaces()
            val vs = if (contextElement.containingScript != null) {
                setOf(Visibility.Public, Visibility.PublicScript)
            } else {
                setOf(Visibility.Public)
            }
            val itemVis = ItemVis(
                namespaces = ns,
                visibilities = vs,
                mslScope = contextElement.mslScope,
                itemScope = contextElement.itemScope,
                folderScope = contextElement.folderScope,
            )
            return ImportContext(contextElement, itemVis)
        }
    }
}

data class ImportCandidate(val element: MvQualifiedNamedElement, val fqPath: FqPath)

fun ImportCandidate.import(context: MvElement) {
    checkWriteAccessAllowed()
    val psiFactory = element.project.psiFactory
    val insertionScope = context.containingModule?.moduleBlock
        ?: context.containingScript?.scriptBlock
        ?: return
    val insertTestOnly = insertionScope.itemScope == ItemScope.MAIN
            && context.itemScope == ItemScope.TEST
    insertionScope.insertUseItem(psiFactory, fqPath, insertTestOnly)
}

private fun MvItemsOwner.insertUseItem(psiFactory: MvPsiFactory, usePath: FqPath, testOnly: Boolean) {
    val newUseStmt = psiFactory.useStmt(usePath.toString(), testOnly)
    if (this.tryGroupWithOtherUseItems(psiFactory, newUseStmt)) return

    val anchor = childrenOfType<MvUseStmt>().lastElement
    if (anchor != null) {
        addAfter(newUseStmt, anchor)
    } else {
        val firstItem = this.items().first()
        addBefore(newUseStmt, firstItem)
        addBefore(psiFactory.createNewline(), firstItem)
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

class ImportInsertHandler(
    val parameters: CompletionParameters,
    val candidate: ImportCandidate
) : DefaultInsertHandler() {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)
        context.commitDocument()
        val path = parameters.originalPosition?.parent as? MvPath ?: return
        candidate.import(path)
    }
}
