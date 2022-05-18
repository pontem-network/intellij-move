package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.AutoImportFix
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.inspections.imports.MvNamedElementIndex
import org.move.ide.inspections.imports.import
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvPath
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.mslScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility

object ModulesCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.pathIdent()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val maybePathIdent = parameters.position.parent
        val maybePath = maybePathIdent.parent
        val refElement =
            maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== refElement.referenceNameElement) return
        if (refElement.pathIdent.moduleRef != null) return

        val processedNames = mutableSetOf<String>()
        val itemVis = ItemVis(setOf(Namespace.MODULE), emptySet(), refElement.mslScope)
        processItems(refElement, itemVis) {
            val lookup = it.element.createCompletionLookupElement()
            result.addElement(lookup)
            it.element.name?.let(processedNames::add)
            false
        }
        addCompletionsFromIndex(parameters, result, processedNames)
    }

    private fun addCompletionsFromIndex(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        processedPathNames: Set<String>
    ) {
        val path = parameters.originalPosition?.parent?.parent as? MvPath ?: return
        val importContext =
            ImportContext.from(path, ItemVis(setOf(Namespace.MODULE), setOf(Visibility.Public)))

        val project = parameters.position.project
        val keys = hashSetOf<String>().apply {
            val names = MvNamedElementIndex.getAllKeys(project)
            addAll(names)
            removeAll(processedPathNames)
        }
        for (elementName in result.prefixMatcher.sortMatching(keys)) {
            val candidates = AutoImportFix.getImportCandidates(importContext, elementName)
            candidates
                .distinctBy { it.element }
                .map { candidate ->
                    val element = candidate.element
                    element.createLookupElement().withInsertHandler { _, _ ->
                        candidate.import(path)
                    }
                }
                .forEach(result::addElement)
        }
    }
}
