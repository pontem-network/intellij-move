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

fun processWithShadowingAcrossScopes(
    prevScope: MutableMap<String, Set<Namespace>>,
    processor: RsResolveProcessor,
    f: (RsResolveProcessor) -> Boolean
): Boolean {
    val currScope = mutableMapOf<String, Set<Namespace>>()
    val shadowingProcessor = ShadowingProcessor(processor, prevScope, currScope)
    val stop = f(shadowingProcessor)
    prevScope.putAll(currScope)
    return stop
}

private class ShadowingProcessor(
    private val originalProcessor: RsResolveProcessor,
    private val prevScope: Map<String, Set<Namespace>>,
    private val currScope: MutableMap<String, Set<Namespace>>,
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        val visitedNs = prevScope[entry.name]
        val entryNs = entry.namespaces
        // drop entries from namespaces that were encountered before
        val entryWithRestNs =
            if (visitedNs != null) {
                val restNs = entryNs.minus(visitedNs)
                if (restNs.isEmpty()) {
                    return false
                }
                entry.copyWithNs(restNs)
            } else {
                entry
            }
        // save encountered namespaces to the currScope
        currScope[entry.name] = visitedNs?.let { it + entryNs } ?: entryNs
        return originalProcessor.process(entryWithRestNs)
    }

    override fun toString(): String = "ShadowingProcessor($originalProcessor)"
}

fun collectResolveVariants(referenceName: String?, f: (RsResolveProcessor) -> Unit): List<MvNamedElement> {
    if (referenceName == null) return emptyList()
    val processor = ResolveVariantsCollector(referenceName)
    f(processor)
    return processor.result
}

private class ResolveVariantsCollector(
    private val referenceName: String,
    val result: MutableList<MvNamedElement> = SmartList(),
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        if (entry.name == referenceName) {
            val element = entry.element
            result += element
        }
        return false
    }
}

fun List<ScopeEntry>.filterByName(name: String): List<ScopeEntry> {
    return this.filter { it.name == name }
}

fun collectResolveVariantsAsScopeEntries(
    referenceName: String?,
    f: (RsResolveProcessor) -> Unit
): List<ScopeEntry> {
    if (referenceName == null) return emptyList()
    val processor = ResolveVariantsAsScopeEntriesCollector(referenceName)
    f(processor)
    return processor.result
}

private class ResolveVariantsAsScopeEntriesCollector(
    private val referenceName: String,
    val result: MutableList<ScopeEntry> = mutableListOf(),
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        if (entry.name == referenceName) {
            result += entry
        }
        return false
    }
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

fun resolveSingle(referenceName: String?, f: (RsResolveProcessor) -> Unit): MvNamedElement? =
    resolveSingleEntry(referenceName, f)?.element

fun resolveSingleEntry(referenceName: String?, f: (RsResolveProcessor) -> Unit): ScopeEntry? {
    if (referenceName == null) return null
    val processor = ResolveSingleScopeEntryCollector(referenceName)
    f(processor)
    return processor.result.singleOrNull()
}

private class ResolveSingleScopeEntryCollector(
    private val referenceName: String,
    val result: MutableList<ScopeEntry> = SmartList(),
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        if (entry.name == referenceName) {
            result += entry
        }
        return result.isNotEmpty()
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
    subst: Substitution = emptySubstitution,
    f: (RsResolveProcessor) -> Unit
) {
    val processor = CompletionVariantsCollector(result, subst, context)
    f(processor)
}

private class CompletionVariantsCollector(
    private val result: CompletionResultSet,
    private val subst: Substitution,
    private val context: MvCompletionContext,
): RsResolveProcessor {
    override fun process(entry: ScopeEntry): Boolean {
        result.addElement(
            createCompletionItem(
                scopeEntry = entry,
                completionContext = context,
                priority = entry.element.completionPriority,
                subst = subst,
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


