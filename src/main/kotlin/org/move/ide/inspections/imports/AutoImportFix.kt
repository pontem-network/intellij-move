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
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.letStmtScope
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
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

        fun findApplicableContext(path: MvPath): Context? {
            if (path.reference == null) return null
            if (path.resolvable) return null
            if (path.ancestorStrict<MvUseSpeck>() != null) return null
            if (path.qualifier != null) return null

            // TODO: no auto-import if name in scope, but cannot be resolved

            val refName = path.referenceName ?: return null
            val importContext = ImportContext.from(path)
            val candidates =
                ImportCandidateCollector.getImportCandidates(importContext, refName)
            return Context(candidates)
        }
    }
}

@Suppress("DataClassPrivateConstructor")
data class ImportContext private constructor(
    val pathElement: MvPath,
    val ns: Set<Namespace>,
//    val visibilities: Set<Visibility>,
//    val contextScopeInfo: ContextScopeInfo,
) {
    companion object {
        fun from(
            pathElement: MvPath,
            ns: Set<Namespace>,
//            visibilities: Set<Visibility>,
//            contextScopeInfo: ContextScopeInfo
        ): ImportContext {
            return ImportContext(pathElement, ns)
//            return ImportContext(pathElement, ns, visibilities, contextScopeInfo)
        }

        fun from(path: MvPath): ImportContext {
            val ns = path.importCandidateNamespaces()
//            val vs =
//                if (path.containingScript != null) {
//                    setOf(Visibility.Public, Visibility.PublicScript)
//                } else {
//                    val module = path.containingModule
//                    if (module != null) {
//                        setOf(Visibility.Public, Visibility.PublicFriend(module.asSmartPointer()))
//                    } else {
//                        setOf(Visibility.Public)
//                    }
//                }
//            val contextScopeInfo = ContextScopeInfo(
//                letStmtScope = path.letStmtScope,
//                refItemScopes = path.refItemScopes,
//            )
            return ImportContext(path, ns)
//            return ImportContext(path, ns, vs, contextScopeInfo)
        }
    }
}

//fun MoveFile.qualifiedItems(
//    targetName: String,
//    namespaces: Set<Namespace>,
//    visibilities: Set<Visibility>,
//    itemVis: ItemVis
//): List<MvQualNamedElement> {
//    checkUnitTestMode()
//    val elements = mutableListOf<MvQualNamedElement>()
//    processFileItems(this, namespaces, visibilities, itemVis) {
//        if (it.element is MvQualNamedElement && it.name == targetName) {
//            elements.add(it.element)
//        }
//        false
//    }
//    return elements
//}
