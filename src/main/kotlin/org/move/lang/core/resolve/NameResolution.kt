package org.move.lang.core.resolve

import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.types.Address
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleIndex

fun getFieldLookupResolveVariants(
    fieldLookup: MvMethodOrField,
    receiverItem: MvStructOrEnumItemElement,
    msl: Boolean
): List<ScopeEntry> {
    if (!msl) {
        // cannot access field if not in the same module as `receiverItem` definition
        val currentModule = fieldLookup.containingModule ?: return emptyList()
        if (receiverItem.definitionModule != currentModule) return emptyList()
    }
    return buildList {
        when (receiverItem) {
            is MvStruct -> {
                addAll(getFieldEntries(receiverItem))
            }
            is MvEnum -> {
                val visitedFields = mutableSetOf<String>()
                for (variant in receiverItem.variants) {
                    val visitedVariantFields = mutableSetOf<String>()
                    for (fieldEntry in getFieldEntries(variant)) {
                        if (fieldEntry.name in visitedFields) continue
                        add(fieldEntry)
                        visitedVariantFields.add(fieldEntry.name)
                    }
                    // add variant fields to the global fields list to skip them in the next variants
                    visitedFields.addAll(visitedVariantFields)
                }
                false
            }
        }
    }
}

fun getStructLitFieldResolveVariants(
    litField: MvStructLitField,
    isCompletion: Boolean
): List<ScopeEntry> {
    return buildList {
        val fieldsOwner = litField.parentStructLitExpr.path.maybeFieldsOwner
        if (fieldsOwner != null) {
            addAll(getNamedFieldEntries(fieldsOwner))
        }
        // if it's a shorthand, also try to resolve to the underlying binding pat
        if (!isCompletion && litField.expr == null) {
            val ctx = ResolutionContext(litField, false)
            // search through available NAME items
            val availableBindings = getEntriesFromWalkingScopes(litField, ctx).filterByNs(NAMES)
            addAll(availableBindings)
        }
    }
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

fun getFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.fields.mapNotNull { it.asEntry() }
}

fun getNamedFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.namedFields.mapNotNull { it.asEntry() }
}

fun getEntriesFromWalkingScopes(
    scopeStart: MvElement,
    ctx: ResolutionContext,
): List<ScopeEntry> {
    val collector = ScopeEntriesCollector()

    val processedScopes = hashMapOf<String, Set<Namespace>>()
    walkUpThroughScopes(scopeStart) { cameFrom, scope ->
        val entries = getEntriesInScope(scope, cameFrom, ctx)

        // state between shadowing processors passed through prevScope
        val currScope = mutableMapOf<String, Set<Namespace>>()
        for (entry in entries) {
            val processedNs = processedScopes[entry.name].orEmpty()
            val entryNs = entry.namespaces

            // remove namespaces which are encountered before (shadowed by previous entries with this name)
            val unprocessedNs = entryNs - processedNs
            if (unprocessedNs.isEmpty()) {
                // all ns for this entry were shadowed
                continue
            }
            val entryWithReducedNs = entry.copyWithNs(namespaces = unprocessedNs)
            collector.process(entryWithReducedNs)

            // save encountered namespaces to the currScope
            currScope[entry.name] = processedNs +  entryNs
        }

        // at the end put all entries from the current scope into the `visitedScopes`
        processedScopes.putAll(currScope)

        false
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
