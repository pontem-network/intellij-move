package org.move.lang.core.resolve2

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.MODULE
import org.move.lang.core.resolve2.ref.PathResolutionContext
import org.move.lang.core.types.Address
import org.move.lang.core.types.address
import org.move.lang.index.MvNamedElementIndex
import java.util.EnumSet

fun processNestedScopesUpwards(
    scopeStart: MvElement,
    ns: Set<Namespace>,
    ctx: PathResolutionContext,
    processor: RsResolveProcessor
): Boolean {
    val prevScope = hashMapOf<String, Set<Namespace>>()
    return walkUpThroughScopes(
        scopeStart,
        stopAfter = { it is MvModule }
    ) { cameFrom, scope ->
        processWithShadowingAndUpdateScope(prevScope, ns, processor) { shadowingProcessor ->
            processItemsInScope(
                scope, cameFrom, ns, ctx, shadowingProcessor
            )
        }
    }
}

fun processModulePathResolveVariants(
    element: MvElement,
    moveProject: MoveProject?,
    address: Address,
    processor: RsResolveProcessor,
): Boolean {
    // if no project, cannot use the index
    if (moveProject == null) return false

    val project = element.project
    val searchScope = moveProject.searchScope()

    val addressProcessor = processor.wrapWithFilter { e ->
        val candidate = e.element as? MvModule ?: return@wrapWithFilter false
        val candidateAddress = candidate.address(moveProject)
        address == candidateAddress
    }

    var stop = false
    for (targetModuleName in processor.names.orEmpty()) {
        MvNamedElementIndex
            .processElementsByName(project, targetModuleName, searchScope) {
                val module = it
                val visFilter = module.visInfo().createFilter()
                stop = addressProcessor.process(targetModuleName, module, setOf(MODULE), visFilter)
                // true to continue processing, if .process does not find anything, it returns false
                !stop
            }
        if (stop) return true
    }

    return false
}

inline fun processWithShadowingAndUpdateScope(
    prevScope: MutableMap<String, Set<Namespace>>,
    ns: Set<Namespace>,
    processor: RsResolveProcessor,
    f: (RsResolveProcessor) -> Boolean
): Boolean {
    val currScope = mutableMapOf<String, Set<Namespace>>()
    val shadowingProcessor = processor.wrapWithShadowingProcessorAndUpdateScope(prevScope, currScope, ns)
    return try {
        f(shadowingProcessor)
    } finally {
        prevScope.putAll(currScope)
    }
}

inline fun processWithShadowing(
    prevScope: Map<String, Set<Namespace>>,
    ns: Set<Namespace>,
    processor: RsResolveProcessor,
    f: (RsResolveProcessor) -> Boolean
): Boolean {
    val shadowingProcessor = processor.wrapWithShadowingProcessor(prevScope, ns)
    return f(shadowingProcessor)
}