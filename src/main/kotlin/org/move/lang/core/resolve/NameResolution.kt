package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.ALL_NAMESPACES
import org.move.lang.core.resolve.ref.MODULES
import org.move.lang.core.resolve.ref.MvMandatoryReferenceElement
import org.move.lang.core.resolve.ref.NAMES
import org.move.lang.core.resolve.ref.NONE
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.lang.core.resolve.ref.TYPES_N_ENUMS_N_MODULES
import org.move.lang.core.types.Address
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.address
import org.move.lang.index.MvModuleIndex
import org.move.stdext.intersects

fun processFieldLookupResolveVariants(
    fieldLookup: MvMethodOrField,
    receiverItem: MvStructOrEnumItemElement,
    msl: Boolean,
    originalProcessor: RsResolveProcessor
): Boolean {
    if (!isFieldsAccessible(fieldLookup, receiverItem, msl)) return false

    val processor = originalProcessor
        .wrapWithMapper { it: ScopeEntry ->
            it.copyWithNs(ALL_NAMESPACES)
        }

    return when (receiverItem) {
        is MvStruct -> processFieldDeclarations(receiverItem, processor)
        is MvEnum -> {
            val visitedFields = mutableSetOf<String>()
            for (variant in receiverItem.variants) {
                val visitedVariantFields = mutableSetOf<String>()
                for (field in variant.fields) {
                    val fieldEntry = field.asEntry() ?: continue
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
    if (fieldsOwner != null && processNamedFieldDeclarations(fieldsOwner, processor)) return true
    // if it's a shorthand, try to resolve to the underlying binding pat
    if (!isCompletion && litField.expr == null) {
        val ctx = ResolutionContext(litField, false)
        // return is ignored as struct lit field cannot be marked as resolved through binding pat
        processNestedScopesUpwards(litField, NAMES, ctx, processor)
    }
    return false
}

fun processStructPatFieldResolveVariants(
    patFieldFull: MvPatFieldFull,
    processor: RsResolveProcessor
): Boolean {
    // used in completion
    val fieldsOwner = patFieldFull.patStruct.path.maybeFieldsOwner ?: return false
    return processNamedFieldDeclarations(fieldsOwner, processor)
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
            if (processNamedFieldDeclarations(fieldsOwner, originalProcessor)) return true
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
//        if (originalProcessor.acceptsName(entry.name)) {
//        } else {
//            false
//        }
    }
    val ns = if (isCompletion) (TYPES_N_ENUMS_N_MODULES + NAMES) else NAMES
    val ctx = ResolutionContext(binding, isCompletion)
    return processNestedScopesUpwards(binding, ns, ctx, processor)
}

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
            val filterNs = shadowingProcessor.wrapWithFilter { scopeEntry ->
                val entryNamespaces = scopeEntry.namespaces
                // entry is available in any of the `ns`
                entryNamespaces.intersects(ns)
            }
            processItemsInScope(
                scope, cameFrom, ns, ctx, filterNs
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

inline fun processWithShadowingAndUpdateScope(
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

private fun processFieldDeclarations(fieldsOwner: MvFieldsOwner, processor: RsResolveProcessor): Boolean =
    fieldsOwner.fields.any { field ->
        val fieldEntry = field.asEntry() ?: return@any false
        processor.process(fieldEntry)
    }

private fun processNamedFieldDeclarations(fieldsOwner: MvFieldsOwner, processor: RsResolveProcessor): Boolean =
    fieldsOwner.namedFields.any { field ->
        val fieldEntry = field.asEntry() ?: return@any false
        processor.process(fieldEntry)
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
