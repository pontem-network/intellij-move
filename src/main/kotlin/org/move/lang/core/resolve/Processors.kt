package org.move.lang.core.resolve

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.SmartList
import org.move.ide.utils.imports.ImportCandidate
import org.move.lang.core.completion.CompletionItem
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createCompletionItem
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.completionPriority
import org.move.lang.core.psi.ext.MvMethodOrPath
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.lang.core.resolve.ref.RsPathResolveResult
import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.emptySubstitution

interface RsResolveProcessor {
    /**
     * Return `true` to stop further processing,
     * return `false` to continue search
     */
    fun process(entry: ScopeEntry): Boolean

    fun processAll(vararg entries: List<ScopeEntry>): Boolean {
        for (entriesList in entries) {
            if (entriesList.any { process(it) }) return true
        }
        return false
    }
}

fun createProcessor(processor: (ScopeEntry) -> Unit): RsResolveProcessor {
    return object: RsResolveProcessor {
        override fun process(entry: ScopeEntry): Boolean {
            processor(entry)
            return false
        }
    }
}

/**
 * Only items matching the predicate will be processed.
 */
fun RsResolveProcessor.wrapWithFilter(
    filter: (ScopeEntry) -> Boolean
): RsResolveProcessor {
    return FilteringProcessor(this, filter)
}

private class FilteringProcessor(
    private val originalProcessor: RsResolveProcessor,
    private val filter: (ScopeEntry) -> Boolean,
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        return if (filter(entry)) {
            originalProcessor.process(entry)
        } else {
            false
        }
    }

    override fun toString(): String = "FilteringProcessor($originalProcessor, filter = $filter)"
}

fun List<ScopeEntry>.filterByName(name: String): List<ScopeEntry> {
    return this.filter { it.name == name }
}

class ScopeEntriesCollector: RsResolveProcessor {
    val result = mutableListOf<ScopeEntry>()

    override fun process(entry: ScopeEntry): Boolean {
        result += entry
        return false
    }
}

/// checks for visibility of items
fun collectMethodOrPathResolveVariants(
    methodOrPath: MvMethodOrPath,
    ctx: ResolutionContext,
    f: (RsResolveProcessor) -> Unit
): List<RsPathResolveResult<MvElement>> {
    val referenceName = methodOrPath.referenceName ?: return emptyList()
    val processor = SinglePathResolveVariantsCollector(ctx, referenceName)
    f(processor)
    return processor.result
}

private class SinglePathResolveVariantsCollector(
    private val ctx: ResolutionContext,
    private val referenceName: String,
    val result: MutableList<RsPathResolveResult<MvElement>> = SmartList(),
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        if (entry.name == referenceName) {
            collectMethodOrPathScopeEntry(ctx, result, entry)
        }
        return false
    }
}

private fun collectMethodOrPathScopeEntry(
    ctx: ResolutionContext,
    result: MutableList<RsPathResolveResult<MvElement>>,
    scopeEntry: ScopeEntry
) {
    val element = scopeEntry.element
    val methodOrPath = ctx.methodOrPath
    if (methodOrPath != null) {
        result += RsPathResolveResult(element, isVisibleInContext(scopeEntry, methodOrPath))
    } else {
        result += RsPathResolveResult(element, true)
    }
}

fun List<ScopeEntry>.toCompletionItems(
    ctx: MvCompletionContext,
    applySubst: Substitution = emptySubstitution
): List<CompletionItem> {
    return this.map {
        createCompletionItem(
            scopeEntry = it,
            completionContext = ctx,
            priority = it.element.completionPriority,
            subst = applySubst,
        )
    }
}

fun collectCompletionVariants(
    result: CompletionResultSet,
    context: MvCompletionContext,
    f: (RsResolveProcessor) -> Unit
) {
    val processor = CompletionVariantsCollector(result, context)
    f(processor)
}

private class CompletionVariantsCollector(
    private val result: CompletionResultSet,
    private val context: MvCompletionContext,
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        result.addElement(
            createCompletionItem(
                scopeEntry = entry,
                completionContext = context,
                priority = entry.element.completionPriority,
            )
        )
        return false
    }
}

sealed class ScopeEntryKind {
    class Simple: ScopeEntryKind()
    class CustomItemScope(val itemScope: NamedItemScope): ScopeEntryKind()
    class Candidate(val candidate: ImportCandidate): ScopeEntryKind()
}

data class ScopeEntry(
    val name: String,
    val element: MvNamedElement,
    val namespaces: Set<Namespace>,
    val entryKind: ScopeEntryKind = ScopeEntryKind.Simple(),
) {
    fun copyWithNs(namespaces: Set<Namespace>): ScopeEntry = copy(namespaces = namespaces)
}


