package org.move.ide.inspections.imports

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.move.ide.inspections.DiagnosticFix
import org.move.ide.utils.imports.ImportCandidate
import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.ide.utils.imports.import
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.moveProject
import org.move.openapiext.runWriteCommandAction

class AutoImportFix(element: MvPath): DiagnosticFix<MvPath>(element),
                                      HighPriorityAction {

    private var isConsumed: Boolean = false

    override fun getFamilyName() = NAME
    override fun getText() = familyName

    override fun stillApplicable(
        project: Project,
        file: PsiFile,
        element: MvPath
    ): Boolean =
        !isConsumed

    override fun invoke(project: Project, file: PsiFile, element: MvPath) {
        if (element.reference == null) return
        if (element.hasAncestor<MvUseStmt>()) return

        val name = element.referenceName ?: return
        val importContext = ImportContext.from(element, false) ?: return
        val candidates =
            ImportCandidateCollector.getImportCandidates(importContext, name)
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

        fun findApplicableContext(path: MvPath): Context? {
            if (path.ancestorStrict<MvUseSpeck>() != null) return null
            if (path.qualifier != null) return null

            val pathReference = path.reference ?: return null
            val resolvedVariants = pathReference.multiResolve()
            when {
                // resolved correctly
                resolvedVariants.size == 1 -> return null
                // multiple variants, cannot import
                resolvedVariants.size > 1 -> return null
            }

            val referenceName = path.referenceName ?: return null
            val importContext = ImportContext.from(path, false) ?: return null
            val candidates =
                ImportCandidateCollector.getImportCandidates(importContext, referenceName)
            return Context(candidates)
        }
    }
}

@Suppress("DataClassPrivateConstructor")
data class ImportContext private constructor(
    val pathElement: MvPath,
    val ns: Set<Namespace>,
    val indexSearchScope: GlobalSearchScope,
) {
    companion object {
        fun from(
            path: MvPath,
            isCompletion: Boolean,
            ns: Set<Namespace> = path.allowedNamespaces(isCompletion),
        ): ImportContext? {
            val searchScope = path.moveProject?.searchScope() ?: return null
            return ImportContext(path, ns, searchScope)
        }
    }
}