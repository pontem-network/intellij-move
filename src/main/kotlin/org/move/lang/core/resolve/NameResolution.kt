package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.types.Address
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleIndex
import org.move.stdext.intersects

fun processFieldLookupResolveVariants(
    fieldLookup: MvMethodOrField,
    receiverItem: MvStructOrEnumItemElement,
    msl: Boolean,
    processor: RsResolveProcessor
): Boolean {
    if (!isFieldsAccessible(fieldLookup, receiverItem, msl)) return false
    return when (receiverItem) {
        is MvStruct -> processor.processAll(getFieldEntries(receiverItem))
        is MvEnum -> {
            val visitedFields = mutableSetOf<String>()
            for (variant in receiverItem.variants) {
                val visitedVariantFields = mutableSetOf<String>()
                for (fieldEntry in getFieldEntries(variant)) {
                    if (fieldEntry.name in visitedFields) continue

                    if (processor.process(fieldEntry)) return true
                    // collect all names for the variant
                    visitedVariantFields.add(fieldEntry.name)
                }
                // add variant fields to the global fields list to skip them in the next variants
                visitedFields.addAll(visitedVariantFields)
            }
            false
        }
        else -> error("unreachable")
    }
}

fun processStructLitFieldResolveVariants(
    litField: MvStructLitField,
    isCompletion: Boolean,
    processor: RsResolveProcessor,
): Boolean {
    val fieldsOwner = litField.parentStructLitExpr.path.reference?.resolveFollowingAliases() as? MvFieldsOwner
    if (fieldsOwner != null) {
        if (processor.processAll(getNamedFieldEntries(fieldsOwner))) return true
    }
    // if it's a shorthand, try to resolve to the underlying binding pat
    if (!isCompletion && litField.expr == null) {
        val ctx = ResolutionContext(litField, false)
        // search through available NAME items
        val availableBindings = getEntriesFromOuterScopes(litField, NAMES, ctx)
        // todo: what if constant or functions hits here?
        processor.processAll(availableBindings)
    }
    return false
}

fun processStructPatFieldResolveVariants(
    patFieldFull: MvPatFieldFull,
    processor: RsResolveProcessor
): Boolean {
    // used in completion
    val fieldsOwner =
        patFieldFull.patStruct.path.maybeFieldsOwner ?: return false
    return processor.processAll(getNamedFieldEntries(fieldsOwner))
}

fun processPatBindingResolveVariants(
    binding: MvPatBinding,
    isCompletion: Boolean,
    originalProcessor: RsResolveProcessor
): Boolean {
    // field pattern shorthand
    if (binding.parent is MvPatField) {
        val parentPat = binding.parent.parent as MvPatStruct
        val fieldsOwner = parentPat.path.maybeFieldsOwner
        // can be null if unresolved
        if (fieldsOwner != null) {
            if (originalProcessor.processAll(getNamedFieldEntries(fieldsOwner))) return true
            if (isCompletion) return false
        }
    }
    // copied as is from the intellij-rust, handles all items that can be matched in match arms
    val processor = originalProcessor.wrapWithFilter { entry ->
        val element = entry.element
        val isConstantLike = element.isConstantLike
        val isPathOrDestructable = when (element) {
            /*is MvModule, */is MvEnum, is MvEnumVariant, is MvStruct -> true
            else -> false
        }
        isConstantLike || (isCompletion && isPathOrDestructable)
    }
    val ns = if (isCompletion) (TYPES_N_ENUMS_N_MODULES + NAMES) else NAMES
    val ctx = ResolutionContext(binding, isCompletion)
    return processor.processAll(getEntriesFromOuterScopes(binding, ns, ctx))
}

fun resolveBindingForFieldShorthand(
    element: MvMandatoryReferenceElement,
): List<MvNamedElement> {
    return collectResolveVariants(element.referenceName) {
        val ctx = ResolutionContext(element, isCompletion = false)
        it.processAll(getEntriesFromOuterScopes(element, NAMES, ctx))
    }
}

fun getEntriesFromOuterScopes(
    scopeStart: MvElement,
    ns: Set<Namespace>,
    ctx: ResolutionContext,
): List<ScopeEntry> {
    val prevScope = hashMapOf<String, Set<Namespace>>()

    val collector = ScopeEntriesCollector()
    walkUpThroughScopes(scopeStart) { cameFrom, scope ->
        // state between shadowing processors passed through prevScope
        processWithShadowingBetweenScopes(prevScope, ns, collector) { shadowingProcessor ->
            val scopeEntries =
                getEntriesInScope(scope, cameFrom, ctx).filter { it.namespaces.intersects(ns) }
            shadowingProcessor.processAll(scopeEntries)
        }
    }
    return collector.result
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

    val addressMatcher = processor.wrapWithFilter { e ->
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

    val targetNames = addressMatcher.names
    // completion
    if (targetNames == null) {
        val moduleNames = MvModuleIndex.getAllModuleNames(project)
        moduleNames.forEach { moduleName ->
            val modules = MvModuleIndex.getModulesByName(project, moduleName, searchScope)
            if (addressMatcher.processAll(modules.mapNotNull { it.asEntry() })) return true
        }
        return false
    }

    for (targetModuleName in targetNames) {
        val modules = MvModuleIndex.getModulesByName(project, targetModuleName, searchScope)
        for (module in modules) {
            if (addressMatcher.processWithVisibility(targetModuleName, module, MODULES)) return true
        }
    }

    return false
}

inline fun processWithShadowingBetweenScopes(
    prevScope: MutableMap<String, Set<Namespace>>,
    ns: Set<Namespace>,
    processor: RsResolveProcessor,
    f: (RsResolveProcessor) -> Boolean
): Boolean {
    val currScope = mutableMapOf<String, Set<Namespace>>()
    val shadowingProcessor = processor.wrapWithShadowingProcessor(prevScope, currScope, ns)
    return try {
        f(shadowingProcessor)
    } finally {
        prevScope.putAll(currScope)
    }
}

fun walkUpThroughScopes(
    start: MvElement,
    stopAfter: (MvElement) -> Boolean = { false },
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

private fun getFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.fields.mapNotNull { it.asEntry() }
}

private fun processFieldDeclarations(fieldsOwner: MvFieldsOwner, processor: RsResolveProcessor): Boolean =
    fieldsOwner.fields.any { field ->
        val fieldEntry = field.asEntry() ?: return@any false
        processor.process(fieldEntry)
    }

private fun getNamedFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.namedFields.mapNotNull { it.asEntry() }
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
