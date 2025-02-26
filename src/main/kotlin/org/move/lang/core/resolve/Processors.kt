package org.move.lang.core.resolve

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.SmartList
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.completionPriority
import org.move.lang.core.psi.ext.MvMethodOrPath
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.lang.core.resolve.ref.RsPathResolveResult
import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.emptySubstitution

interface ScopeEntry {
    val name: String
    val element: MvNamedElement
    val namespaces: Set<Namespace>

    fun copyWithNs(namespaces: Set<Namespace>): ScopeEntry
}

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

fun <T: ScopeEntry> List<T>.filterByName(name: String): List<T> {
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
    e: ScopeEntry
) {
    val element = e.element
    val visibilityStatus = ctx.methodOrPath?.let { e.getVisibilityInContext(it) } ?: Visible
    val isVisible = visibilityStatus == Visible
    result += RsPathResolveResult(element, isVisible)
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
            createLookupElement(
                scopeEntry = entry,
                completionContext = context,
                priority = entry.element.completionPriority,
                subst = subst,
            )
        )
        return false
    }
}

data class SimpleScopeEntry(
    override val name: String,
    override val element: MvNamedElement,
    override val namespaces: Set<Namespace>,
): ScopeEntry {
    override fun copyWithNs(namespaces: Set<Namespace>): ScopeEntry = copy(namespaces = namespaces)
}

fun interface VisibilityFilter {
    fun filter(context: MvElement, ns: Set<Namespace>): VisibilityStatus
}

fun ScopeEntry.getVisibilityInContext(contextElement: MvElement): VisibilityStatus =
    when (this) {
        is ScopeEntryWithVisibility -> {
            this.element
                .visInfo(adjustScope = this.itemScope)
                .createFilter()
                .filter(contextElement, this.namespaces)
        }
        else -> Visible
    }


fun ScopeEntry.isVisibleInContext(context: MvElement): Boolean = getVisibilityInContext(context) == Visible

enum class VisibilityStatus {
    Visible,
    Invisible,
}

data class ScopeEntryWithVisibility(
    override val name: String,
    override val element: MvNamedElement,
    override val namespaces: Set<Namespace>,
    // when item is imported, import can have different item scope
    val itemScope: NamedItemScope,
): ScopeEntry {
    override fun copyWithNs(namespaces: Set<Namespace>): ScopeEntryWithVisibility = copy(namespaces = namespaces)
}




