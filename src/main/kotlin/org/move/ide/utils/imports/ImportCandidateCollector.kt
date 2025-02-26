package org.move.ide.utils.imports

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.psi.MvQualNamedElement
import org.move.lang.core.resolve.asEntries
import org.move.lang.core.resolve.isVisibleInContext
import org.move.lang.core.resolve.ref.filterByNs
import org.move.lang.index.MvNamedElementIndex

object ImportCandidateCollector {

    fun getImportCandidates(context: ImportContext, targetName: String): List<ImportCandidate> {
        val (path, ns, indexSearchScope) = context
        val project = path.project

        val candidates = mutableListOf<ImportCandidate>()
        val elementsFromIndex = MvNamedElementIndex.getElementsByName(project, targetName, indexSearchScope)

        val importableEntries = elementsFromIndex.toList().asEntries().filterByNs(ns)
        for ((i, scopeEntry) in importableEntries.withIndex()) {
            // check for cancellation sometimes
            if (i % 50 == 0) ProgressManager.checkCanceled()

            val element = scopeEntry.element
            if (element !is MvQualNamedElement) continue

            if (!isVisibleInContext(scopeEntry, path)) continue

            // double check in case of match
            if (scopeEntry.name == targetName) {
                val itemQualName = element.qualName
                if (itemQualName != null) {
                    candidates.add(ImportCandidate(element, itemQualName))
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