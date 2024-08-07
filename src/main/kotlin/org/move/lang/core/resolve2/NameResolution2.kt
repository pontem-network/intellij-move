package org.move.lang.core.resolve2

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MODULES
import org.move.lang.core.resolve.ref.MvMandatoryReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.MODULE
import org.move.lang.core.resolve2.ref.ResolutionContext
import org.move.lang.core.types.Address
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleIndex

fun resolveBindingForFieldShorthand(
    element: MvMandatoryReferenceElement,
): List<MvNamedElement> {
    return collectResolveVariants(element.referenceName) {
        processNestedScopesUpwards(
            element,
            setOf(Namespace.NAME),
            ResolutionContext(element, isCompletion = false),
            it
        )
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
        // state between shadowing processors passed through prevScope
        processWithShadowingAndUpdateScope(prevScope, ns, processor) { shadowingProcessor ->
            processItemsInScope(
                scope, cameFrom, ns, ctx, shadowingProcessor
            )
        }
    }
}

fun processModulePathResolveVariants(
    ctx: ResolutionContext,
    address: Address,
    processor: RsResolveProcessor,
): Boolean {
    // if no project, cannot use the index
    val moveProject = ctx.moveProject
    if (moveProject == null) return false

    val project = ctx.element.project
    val searchScope = moveProject.searchScope()

    val addrProcessor = processor.wrapWithFilter { e ->
        val candidate = e.element as? MvModule ?: return@wrapWithFilter false
        val candidateAddress = candidate.address(moveProject)
        val sameValues = Address.equals(address, candidateAddress)

        if (ctx.isCompletion && sameValues) {
            // compare named addresses by name in case of the same values for the completion
            if (address is Named && candidateAddress is Named && address.name != candidateAddress.name)
                return@wrapWithFilter false
        }

        sameValues
    }

    val targetNames = addrProcessor.names
    if (targetNames == null) {
        // completion
        val moduleNames = MvModuleIndex.getAllModuleNames(project)
        moduleNames.forEach { moduleName ->
            val modules = MvModuleIndex.getModulesByName(project, moduleName, searchScope)
            for (module in modules) {
                if (addrProcessor.process(moduleName, MODULES, module)) return true
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
                stop = addrProcessor.process(targetModuleName, module, MODULES, visFilter)
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

//inline fun processWithShadowing(
//    prevScope: Map<String, Set<Namespace>>,
//    ns: Set<Namespace>,
//    processor: RsResolveProcessor,
//    f: (RsResolveProcessor) -> Boolean
//): Boolean {
//    val shadowingProcessor = processor.wrapWithShadowingProcessor(prevScope, ns)
//    return f(shadowingProcessor)
//}


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
        if (scope is MvModule) {
            // handle spec module {}
            if (handleModuleItemSpecsInItemsOwner(cameFrom, scope, handleScope)) return true
            // walk over all spec modules
            for (moduleSpec in scope.allModuleSpecs()) {
                val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
                if (handleScope(cameFrom, moduleSpecBlock)) return true
                if (handleModuleItemSpecsInItemsOwner(cameFrom, moduleSpecBlock, handleScope)) return true
            }
        }

        if (scope is MvModuleSpecBlock) {
            val module = scope.moduleSpec.moduleItem
            if (module != null) {
                cameFrom = scope
                scope = module
                continue
            }
        }

        if (stopAfter(scope)) break

        cameFrom = scope
        scope = scope.context as? MvElement
    }

    return false
}

private fun handleModuleItemSpecsInItemsOwner(
    cameFrom: MvElement,
    itemsOwner: MvItemsOwner,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean
): Boolean {
    val moduleItemSpecs = when (itemsOwner) {
        is MvModule -> itemsOwner.moduleItemSpecList
        is MvModuleSpecBlock -> itemsOwner.moduleItemSpecList
        else -> emptyList()
    }
    for (moduleItemSpec in moduleItemSpecs.filter { it != cameFrom }) {
        val itemSpecBlock = moduleItemSpec.itemSpecBlock ?: continue
        if (handleScope(cameFrom, itemSpecBlock)) return true
    }
    return false
}
