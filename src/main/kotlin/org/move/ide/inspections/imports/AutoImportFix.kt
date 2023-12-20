package org.move.ide.inspections.imports

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.ide.utils.imports.ImportCandidate
import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.ide.utils.imports.import
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.hasAncestor
import org.move.lang.core.psi.ext.importCandidateNamespaces
import org.move.lang.core.psi.ext.smartPointer
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.mslLetScope
import org.move.lang.core.resolve.processFileItems
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.openapiext.common.checkUnitTestMode
import org.move.openapiext.runWriteCommandAction

class AutoImportFix(element: MvReferenceElement): DiagnosticFix<MvReferenceElement>(element),
                                                  HighPriorityAction {

    private var isConsumed: Boolean = false

    override fun getFamilyName() = NAME
    override fun getText() = familyName

    override fun stillApplicable(
        project: Project,
        file: PsiFile,
        element: MvReferenceElement
    ): Boolean =
        !isConsumed

    override fun invoke(project: Project, file: PsiFile, element: MvReferenceElement) {
        if (element.reference == null) return
        if (element.hasAncestor<MvUseStmt>()) return

        val name = element.referenceName ?: return
        val candidates =
            ImportCandidateCollector.getImportCandidates(ImportContext.from(element), name)
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
        isConsumed = true
    }

    data class Context(val candidates: List<ImportCandidate>)

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
    val namespaces: Set<Namespace>,
    val visibilities: Set<Visibility>,
    val itemVis: ItemVis,
) {
    companion object {
        fun from(
            contextElement: MvReferenceElement,
            namespaces: Set<Namespace>,
            visibilities: Set<Visibility>,
            itemVis: ItemVis
        ): ImportContext {
            return ImportContext(contextElement, namespaces, visibilities, itemVis)
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
                mslLetScope = contextElement.mslLetScope,
                itemScopes = contextElement.itemScopes,
            )
            return ImportContext(contextElement, ns, vs, itemVis)
        }
    }
}

fun MoveFile.qualifiedItems(
    targetName: String,
    namespaces: Set<Namespace>,
    visibilities: Set<Visibility>,
    itemVis: ItemVis
): List<MvQualNamedElement> {
    checkUnitTestMode()
    val elements = mutableListOf<MvQualNamedElement>()
    processFileItems(this, namespaces, visibilities, itemVis) {
        if (it.element is MvQualNamedElement && it.name == targetName) {
            elements.add(it.element)
        }
        false
    }
    return elements
}
