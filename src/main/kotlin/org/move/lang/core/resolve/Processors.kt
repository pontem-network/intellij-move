package org.move.lang.core.resolve

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.SmartList
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.NamedItemScope
import org.move.lang.core.psi.NamedItemScope.MAIN
import org.move.lang.core.psi.completionPriority
import org.move.lang.core.psi.ext.MvMethodOrPath
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.lang.core.resolve.ref.RsPathResolveResult
import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.emptySubstitution
import org.move.stdext.intersects

interface ScopeEntry {
    val name: String
    val element: MvNamedElement
    val namespaces: Set<Namespace>

    fun copyWithNs(namespaces: Set<Namespace>): ScopeEntry
}

interface RsResolveProcessorBase<in T: ScopeEntry> {
    /**
     * Return `true` to stop further processing,
     * return `false` to continue search
     */
    fun process(entry: T): Boolean

    fun processAll(vararg entries: List<T>): Boolean {
        for (entriesList in entries) {
            if (entriesList.any { process(it) }) return true
        }
        return false
    }

    /**
     * Indicates that processor is interested only in [ScopeEntry]s with specified [names].
     * Improves performance for Resolve2.
     * `null` in completion
     */
    val names: Set<String>?

    fun acceptsName(name: String): Boolean {
        return true
//        val names = names
//        return names == null || name in names
    }
}

typealias RsResolveProcessor = RsResolveProcessorBase<ScopeEntry>

fun createProcessor(processor: (ScopeEntry) -> Unit): RsResolveProcessor {
    return object: RsResolveProcessorBase<ScopeEntry> {
        override fun process(entry: ScopeEntry): Boolean {
            processor(entry)
            return false
        }

        override val names: Set<String>? get() = null
    }
}

/**
 * Only items matching the predicate will be processed.
 */
fun <T: ScopeEntry> RsResolveProcessorBase<T>.wrapWithFilter(
    filter: (T) -> Boolean
): RsResolveProcessorBase<T> {
    return FilteringProcessor(this, filter)
}

private class FilteringProcessor<in T: ScopeEntry>(
    private val originalProcessor: RsResolveProcessorBase<T>,
    private val filter: (T) -> Boolean,
): RsResolveProcessorBase<T> {
    override val names: Set<String>? = originalProcessor.names
    override fun process(entry: T): Boolean {
        return if (filter(entry)) {
            originalProcessor.process(entry)
        } else {
            false
        }
    }

    override fun toString(): String = "FilteringProcessor($originalProcessor, filter = $filter)"
}

fun <T: ScopeEntry> RsResolveProcessorBase<T>.wrapWithShadowingProcessor(
    prevScope: Map<String, Set<Namespace>>,
    currScope: MutableMap<String, Set<Namespace>>,
    ns: Set<Namespace>,
): RsResolveProcessorBase<T> {
    return ShadowingProcessor(this, prevScope, currScope, ns)
}

private class ShadowingProcessor<in T: ScopeEntry>(
    private val originalProcessor: RsResolveProcessorBase<T>,
    private val prevScope: Map<String, Set<Namespace>>,
    private val currScope: MutableMap<String, Set<Namespace>>,
    private val ns: Set<Namespace>,
): RsResolveProcessorBase<T> {
    override val names: Set<String>? = originalProcessor.names
    override fun process(entry: T): Boolean {
        if (!originalProcessor.acceptsName(entry.name) || entry.name == "_") {
            return originalProcessor.process(entry)
        }
        val prevNs = prevScope[entry.name]
        val newNs = entry.namespaces
        // drop entries from namespaces that were encountered before
        val entryWithIntersectedNs =
            if (prevNs != null) {
                val restNs = newNs.minus(prevNs)
                if (ns.intersects(restNs)) {
                    @Suppress("UNCHECKED_CAST")
                    entry.copyWithNs(restNs) as T
                } else {
                    return false
                }
            } else {
                entry
            }
        // save encountered namespaces to the currScope
        currScope[entry.name] = prevNs?.let { it + newNs } ?: newNs
        return originalProcessor.process(entryWithIntersectedNs)
    }

    override fun toString(): String = "ShadowingProcessor($originalProcessor, ns = $ns)"
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
): RsResolveProcessorBase<ScopeEntry> {
    override val names: Set<String> = setOf(referenceName)

    override fun process(entry: ScopeEntry): Boolean {
        if (entry.name == referenceName) {
            val element = entry.element
            result += element
        }
        return false
    }
}

fun <T: ScopeEntry> collectResolveVariantsAsScopeEntries(
    referenceName: String?,
    f: (RsResolveProcessorBase<T>) -> Unit
): List<T> {
    if (referenceName == null) return emptyList()
    val processor = ResolveVariantsAsScopeEntriesCollector<T>(referenceName)
    f(processor)
    return processor.result
}

private class ResolveVariantsAsScopeEntriesCollector<T: ScopeEntry>(
    private val referenceName: String,
    val result: MutableList<T> = mutableListOf(),
): RsResolveProcessorBase<T> {
    override val names: Set<String> = setOf(referenceName)

    override fun process(entry: T): Boolean {
        if (entry.name == referenceName) {
            result += entry
        }
        return false
    }
}

class ScopeEntriesCollector: RsResolveProcessor {
    val result = mutableListOf<ScopeEntry>()

    override val names get() = null

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
): RsResolveProcessorBase<ScopeEntry> {
    override val names: Set<String> = setOf(referenceName)

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
    val visibilityStatus = ctx.methodOrPath?.let { e.getVisibilityStatusFrom(it) } ?: Visible
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
): RsResolveProcessorBase<ScopeEntry> {
    override val names: Set<String> = setOf(referenceName)

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
): RsResolveProcessorBase<ScopeEntry> {
    override val names: Set<String>? get() = null

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

fun ScopeEntry.getVisibilityStatusFrom(contextElement: MvElement): VisibilityStatus =
    if (this is ScopeEntryWithVisibility) {
        val visFilter = this.element
            .visInfo(adjustScope = this.adjustedItemScope)
            .createFilter()
        visFilter.filter(contextElement, this.namespaces)
    } else {
        Visible
    }


fun ScopeEntry.isVisibleFrom(context: MvElement): Boolean = getVisibilityStatusFrom(context) == Visible

enum class VisibilityStatus {
    Visible,
    Invisible,
}

data class ScopeEntryWithVisibility(
    override val name: String,
    override val element: MvNamedElement,
    override val namespaces: Set<Namespace>,
    // when item is imported, import can have different item scope
    val adjustedItemScope: NamedItemScope,
): ScopeEntry {
    override fun copyWithNs(namespaces: Set<Namespace>): ScopeEntryWithVisibility = copy(namespaces = namespaces)
}

fun RsResolveProcessor.processWithVisibility(
    name: String,
    e: MvNamedElement,
    ns: Set<Namespace>,
    adjustedItemScope: NamedItemScope = MAIN,
): Boolean = process(ScopeEntryWithVisibility(name, e, ns, adjustedItemScope))

fun RsResolveProcessor.processNamedElement(
    name: String,
    namespaces: Set<Namespace>,
    e: MvNamedElement,
): Boolean =
    process(SimpleScopeEntry(name, e, namespaces))

fun RsResolveProcessor.processAllNamedElements(
    ns: Set<Namespace>,
    vararg lists: Iterable<MvNamedElement>,
): Boolean {
    return sequenceOf(*lists).flatten()
        .any {
            val name = it.name ?: return@any false
            processNamedElement(name, ns, it)
        }
}




