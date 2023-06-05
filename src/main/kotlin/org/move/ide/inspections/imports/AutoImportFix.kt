package org.move.ide.inspections.imports

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.ide.utils.imports.ImportCandidate
import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.ide.utils.imports.import
import org.move.lang.MoveFile
import org.move.lang.core.completion.DefaultInsertHandler
import org.move.lang.core.completion.providers.import
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Visibility
import org.move.openapiext.common.checkUnitTestMode
import org.move.openapiext.runWriteCommandAction

class AutoImportFix(element: PsiElement) : LocalQuickFixOnPsiElement(element), HighPriorityAction {
    private var isConsumed: Boolean = false

    override fun generatePreview(
        project: Project,
        previewDescriptor: ProblemDescriptor
    ): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

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
        val candidates =
            ImportCandidateCollector.getImportCandidates(ImportContext.from(refElement), name)
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
        isConsumed = true
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
            val importContext = ImportContext.from(refElement)
            val candidates =
                ImportCandidateCollector.getImportCandidates(importContext, refName)
            return Context(candidates)
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
            val ns = contextElement.importCandidateNamespaces()
            val vs = if (contextElement.containingScript != null) {
                setOf(Visibility.Public, Visibility.PublicScript)
            } else {
                val module = contextElement.containingModule
                if (module != null) {
                    setOf(Visibility.Public, Visibility.PublicFriend(module.smartPointer()))
                } else {
                    setOf(Visibility.Public)
                }
            }
            val itemVis = ItemVis(
                namespaces = ns,
                visibilities = vs,
                mslLetScope = contextElement.mslLetScope,
                itemScope = contextElement.itemScope,
            )
            return ImportContext(contextElement, itemVis)
        }
    }
}

fun MoveFile.qualifiedItems(targetName: String, itemVis: ItemVis): List<MvQualNamedElement> {
    checkUnitTestMode()
    val elements = mutableListOf<MvQualNamedElement>()
    processFileItems(this, itemVis) {
        if (it.element is MvQualNamedElement && it.name == targetName) {
            elements.add(it.element)
        }
        false
    }
    return elements
}
