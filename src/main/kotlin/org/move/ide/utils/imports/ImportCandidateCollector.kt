package org.move.ide.utils.imports

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.psi.MvQualNamedElement
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve.createFilter
import org.move.lang.core.resolve.moduleItemNamespace
import org.move.lang.core.resolve.visInfo
import org.move.lang.index.MvNamedElementIndex

object ImportCandidateCollector {

    fun getImportCandidates(context: ImportContext, targetName: String): List<ImportCandidate> {
        val (path, ns, indexSearchScope) = context
        val project = path.project

        val candidates = mutableListOf<ImportCandidate>()
        val elementsFromIndex = MvNamedElementIndex.getElementsByName(project, targetName, indexSearchScope)

        for ((i, elementFromIndex) in elementsFromIndex.withIndex()) {
            // check for cancellation sometimes
            if (i % 100 == 0) ProgressManager.checkCanceled()

            if (elementFromIndex !is MvQualNamedElement) continue
            if (elementFromIndex.moduleItemNamespace !in ns) continue

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
    ): List<ImportCandidate> {
        val keys = hashSetOf<String>().apply {
            val names = MvNamedElementIndex.getAllKeys(project)
            addAll(names)
            removeAll(processedPathNames)
        }
        val matchingKeys = prefixMatcher.sortMatching(keys)
        return matchingKeys.flatMap { targetName ->
            getImportCandidates(importContext, targetName).distinctBy { it.element }
        }
    }
}