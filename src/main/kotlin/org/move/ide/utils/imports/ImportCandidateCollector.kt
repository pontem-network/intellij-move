package org.move.ide.utils.imports

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.resolve.isVisibleInContext
import org.move.lang.index.MvNamedItemFilesIndex

object ImportCandidateCollector {

    fun getImportCandidates(context: ImportContext, targetNames: List<String>): List<ImportCandidate> {
        val (path, ns, searchScope) = context
        val project = path.project

        val candidates = mutableListOf<ImportCandidate>()
        val importableEntries =
            MvNamedItemFilesIndex.getEntriesFor(project, searchScope, targetNames, ns)
        for ((i, scopeEntry) in importableEntries.withIndex()) {
            // check for cancellation sometimes
            if (i % 50 == 0) ProgressManager.checkCanceled()

            if (!isVisibleInContext(scopeEntry, path)) continue

            // double check in case of match
            if (targetNames.any { it == scopeEntry.name }) {
                val fqName = scopeEntry.elementFQName()
                if (fqName != null) {
                    candidates.add(ImportCandidate(scopeEntry.element(), fqName))
                }
            }
        }

        return candidates
    }

    fun getCompletionCandidates(
        project: Project,
        prefixMatcher: PrefixMatcher,
        processedNames: Set<String>,
        importContext: ImportContext,
    ): List<ImportCandidate> {
        val namesFromIndex = MvNamedItemFilesIndex.getAllItemNames(project, importContext.ns)
        val keys = namesFromIndex - processedNames
        val matchingKeys = prefixMatcher.sortMatching(keys).toList()
        return getImportCandidates(importContext, matchingKeys)
            .distinctBy { it.element }
    }
}