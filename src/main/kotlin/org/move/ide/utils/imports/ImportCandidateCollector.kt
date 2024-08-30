package org.move.ide.utils.imports

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.psi.MvQualNamedElement
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve2.createFilter
import org.move.lang.core.resolve2.namespace
import org.move.lang.core.resolve2.visInfo
import org.move.lang.index.MvNamedElementIndex

object ImportCandidateCollector {

    fun getImportCandidates(context: ImportContext, targetName: String): List<ImportCandidate> {
        val (path, ns, indexSearchScope) = context
        val project = path.project

        val candidates = mutableListOf<ImportCandidate>()
        val elementsFromIndex = MvNamedElementIndex.getElementsByName(project, targetName, indexSearchScope)
        for (elementFromIndex in elementsFromIndex) {
            if (elementFromIndex !is MvQualNamedElement) continue

            if (elementFromIndex.namespace !in ns) continue

            val visFilter = elementFromIndex.visInfo().createFilter()
            val visibilityStatus = visFilter.filter(path, ns)
            if (visibilityStatus != Visible) continue

            // double check in case of match
            if (elementFromIndex.name == targetName) {
                val itemQualName = elementFromIndex.qualName
                if (itemQualName != null) {
                    candidates.add(ImportCandidate(elementFromIndex, itemQualName))
                }
            }
        }

        return candidates
    }

    fun getCompletionCandidates(
        project: Project,
        prefixMatcher: PrefixMatcher,
        processedPathNames: Set<String>,
        importContext: ImportContext,
//        itemFilter: (PsiElement) -> Boolean = { true }
    ): List<ImportCandidate> {
        val keys = hashSetOf<String>().apply {
            val names = MvNamedElementIndex.getAllKeys(project)
            addAll(names)
            removeAll(processedPathNames)
        }

        return prefixMatcher.sortMatching(keys)
            .flatMap { targetName ->
                ProgressManager.checkCanceled()
                getImportCandidates(importContext, targetName).distinctBy { it.element }
            }
    }
}