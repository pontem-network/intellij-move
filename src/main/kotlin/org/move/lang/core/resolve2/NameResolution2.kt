package org.move.lang.core.resolve2

import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MvMandatoryReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.MODULE
import org.move.lang.core.resolve2.ref.ResolutionContext
import org.move.lang.core.types.Address
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleIndex

fun resolveBindingForFieldShorthand(
    element: MvMandatoryReferenceElement,
): List<MvNamedElement> {
    return collectResolveVariants(element.referenceName) {
        processNestedScopesUpwards(element, setOf(Namespace.NAME), ResolutionContext(element), it)
    }
}

fun processNestedScopesUpwards(
    scopeStart: MvElement,
    ns: Set<Namespace>,
    ctx: ResolutionContext,
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

    val addrProcessor = processor.wrapWithFilter { e ->
        val candidate = e.element as? MvModule ?: return@wrapWithFilter false
        val candidateAddress = candidate.address(moveProject)
        address == candidateAddress
    }

    val targetNames = addrProcessor.names
    if (targetNames == null) {
        // completion
        val moduleNames = MvModuleIndex.getAllModuleNames(project)
        moduleNames.forEach { moduleName ->
            val modules = MvModuleIndex.getModulesByName(project, moduleName, searchScope)
            for (module in modules) {
                if (addrProcessor.process(moduleName, module)) return true
            }
        }
        return false
    }

    var stop = false
    for (targetModuleName in targetNames) {
        MvModuleIndex
            .processModulesByName(project, targetModuleName, searchScope) {
                val module = it
                val visFilter = module.visInfo().createFilter()
                stop = addrProcessor.process(targetModuleName, module, setOf(MODULE), visFilter)
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


fun walkUpThroughScopes(
    start: MvElement,
    stopAfter: (MvElement) -> Boolean,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean,
): Boolean {
    var cameFrom = start
    var scope = start.context as MvElement?
    while (scope != null) {
        if (handleScope(cameFrom, scope)) return true

        // walk all items in original module block
        if (scope is MvModuleBlock) {
            // handle spec module {}
            if (handleModuleItemSpecsInBlock(cameFrom, scope, handleScope)) return true
            // walk over all spec modules
            for (moduleSpec in scope.module.allModuleSpecs()) {
                val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
                if (handleScope(cameFrom, moduleSpecBlock)) return true
                if (handleModuleItemSpecsInBlock(cameFrom, moduleSpecBlock, handleScope)) return true
            }
        }

        if (scope is MvModuleSpecBlock) {
            val moduleBlock = scope.moduleSpec.moduleItem?.moduleBlock
            if (moduleBlock != null) {
                cameFrom = scope
                scope = moduleBlock
                continue
            }
        }

        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.context as? MvElement
    }

    return false
}

private fun handleModuleItemSpecsInBlock(
    cameFrom: MvElement,
    block: MvElement,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean
): Boolean {
    val moduleItemSpecs = when (block) {
        is MvModuleBlock -> block.moduleItemSpecList
        is MvModuleSpecBlock -> block.moduleItemSpecList
        else -> emptyList()
    }
    for (moduleItemSpec in moduleItemSpecs.filter { it != cameFrom }) {
        val itemSpecBlock = moduleItemSpec.itemSpecBlock ?: continue
        if (handleScope(cameFrom, itemSpecBlock)) return true
    }
    return false
}
