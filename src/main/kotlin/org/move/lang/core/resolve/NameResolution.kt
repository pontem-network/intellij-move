package org.move.lang.core.resolve

import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.types.Address
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleIndex

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
        val availableBindings = getEntriesFromWalkingScopes(litField, ctx)
            .filterByNs(NAMES)
        // todo: what if constant or functions hits here?
        processor.processAll(availableBindings)
    }
    return false
}

fun getStructPatFieldResolveVariants(patFieldFull: MvPatFieldFull): List<ScopeEntry> {
    // used in completion
    val fieldsOwner =
        patFieldFull.patStruct.path.maybeFieldsOwner ?: return emptyList()
    return getNamedFieldEntries(fieldsOwner)
}

fun getPatBindingsResolveVariants(
    binding: MvPatBinding,
    isCompletion: Boolean,
): List<ScopeEntry> {
    return buildList {
        // field pattern shorthand
        if (binding.parent is MvPatField) {
            val parentPat = binding.parent.parent as MvPatStruct
            val fieldsOwner = parentPat.path.maybeFieldsOwner
            // can be null if unresolved
            if (fieldsOwner != null) {
                addAll(getNamedFieldEntries(fieldsOwner))
                if (isCompletion) {
                    return@buildList
                }
            }
        }

        val ctx = ResolutionContext(binding, isCompletion)
        val ns = if (isCompletion) (TYPES_N_ENUMS_N_ENUM_VARIANTS + MODULES) else ENUM_VARIANTS

        val allScopesEntries = getEntriesFromWalkingScopes(binding, ctx)
        val bindingEntries = allScopesEntries.filterByNs(ns)

        for (bindingEntry in bindingEntries) {
            val element = bindingEntry.element

            // copied as is from the intellij-rust, handles all items that can be matched in match arms
            val isConstantLike = element.isConstantLike
            val isPathOrDestructable = when (element) {
                is MvEnum, is MvEnumVariant, is MvStruct -> true
                else -> false
            }
            if (isConstantLike || (isCompletion && isPathOrDestructable)) {
                add(bindingEntry)
            }
        }
    }
}

fun resolveBindingForFieldShorthand(element: MvMandatoryReferenceElement): List<ScopeEntry> {
    val ctx = ResolutionContext(element, isCompletion = false)
    val entries = getEntriesFromWalkingScopes(element, ctx)
        .filterByNs(NAMES)
    return entries.filterByName(element.referenceName)
}

private fun getFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.fields.mapNotNull { it.asEntry() }
}

private fun getNamedFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.namedFields.mapNotNull { it.asEntry() }
}

fun getEntriesFromWalkingScopes(
    scopeStart: MvElement,
    ctx: ResolutionContext,
): List<ScopeEntry> {
    val collector = ScopeEntriesCollector()

    val prevScope = hashMapOf<String, Set<Namespace>>()
    walkUpThroughScopes(scopeStart) { cameFrom, scope ->
        // state between shadowing processors passed through prevScope
        processWithShadowingAcrossScopes(prevScope, collector) { shadowingProcessor ->
            shadowingProcessor.processAll(getEntriesInScope(scope, cameFrom, ctx))
        }
    }

    return collector.result
}

fun ScopeEntry.matchesByAddress(moveProject: MoveProject, address: Address, isCompletion: Boolean): Boolean {
    val module = this.element as? MvModule ?: return false
    val moduleAddress = module.address(moveProject)
    val sameValues = Address.equals(moduleAddress, address)

    if (sameValues && isCompletion) {
        // compare named addresses by name in case of the same values for the completion
        if (address is Named && moduleAddress is Named && address.name != moduleAddress.name)
            return false
    }

    return sameValues
}

fun List<ScopeEntry>.filterByAddress(
    moveProject: MoveProject,
    address: Address,
    isCompletion: Boolean
): List<ScopeEntry> {
    // if no Aptos project, then cannot match by address
    return this.filter { it.matchesByAddress(moveProject, address, isCompletion) }
}

fun getModulesAsEntries(ctx: ResolutionContext, address: Address): List<ScopeEntry> {
    // no Aptos project, cannot resolve modules
    val moveProject = ctx.moveProject ?: return emptyList()
    return buildList {
        val project = moveProject.project
        val searchScope = moveProject.searchScope()

        if (ctx.isCompletion) {
            // todo: somehow get all modules from the index (or pre-filter with address)?
            val allModules = MvModuleIndex.getAllModuleNames(project).flatMap {
                MvModuleIndex.getModulesByName(project, it, searchScope).mapNotNull { it.asEntry() }
            }
                .filterByAddress(moveProject, address, isCompletion = true)
            addAll(allModules)
            return@buildList
        }

        val targetModuleName = ctx.path?.referenceName ?: return@buildList

        val moduleEntries = MvModuleIndex.getModulesByName(project, targetModuleName, searchScope)
            .map {
                ScopeEntry(
                    targetModuleName,
                    it,
                    MODULES,
                )
            }
        addAll(moduleEntries.filterByAddress(moveProject, address, isCompletion = false))
    }
}

fun walkUpThroughScopes(
    start: MvElement,
    handleScope: (cameFrom: MvElement, scope: MvElement) -> Boolean,
): Boolean {
    var cameFrom = start
    var scope = start.context as MvElement?
    while (scope != null) {
        if (handleScope(cameFrom, scope)) return true

        // walk all items in original module block
        if (scope is MvModule) {
            // handle `spec module {}` in the current module
            for (moduleSpecBlock in getModuleItemSpecsInItemsOwner(cameFrom, scope)) {
                if (handleScope(cameFrom, moduleSpecBlock)) return true
            }

            // walk over all spec modules
            for (moduleSpec in scope.allModuleSpecs()) {
                val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue

                if (handleScope(cameFrom, moduleSpecBlock)) return true
                // handle `spec module {}` in all spec blocks
                for (childModuleSpecBlock in getModuleItemSpecsInItemsOwner(cameFrom, moduleSpecBlock)) {
                    if (handleScope(cameFrom, childModuleSpecBlock)) return true
                }
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

        cameFrom = scope
        scope = scope.context as? MvElement
    }

    return false
}

private fun getModuleItemSpecsInItemsOwner(
    cameFrom: MvElement,
    itemsOwner: MvItemsOwner,
): List<MvSpecCodeBlock> {
    val moduleItemSpecs = when (itemsOwner) {
        is MvModule -> itemsOwner.moduleItemSpecList
        is MvModuleSpecBlock -> itemsOwner.moduleItemSpecList
        else -> emptyList()
    }
    return moduleItemSpecs.filter { it != cameFrom }.mapNotNull { it.itemSpecBlock }
}
