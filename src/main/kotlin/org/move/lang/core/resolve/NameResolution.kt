package org.move.lang.core.resolve

import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.ext.getModuleSpecsFromIndex
import org.move.lang.core.psi.ext.itemSpecBlock
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.filterByName
import org.move.lang.core.types.Address
import org.move.lang.index.MvModuleFileIndex
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.utils.psiCacheResult

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
            // search through available NAME items
            val availableBindings = getEntriesFromWalkingScopes(litField, NAMES)
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

        val ns = if (isCompletion) TYPES_N_ENUMS_N_ENUM_VARIANTS_N_MODULES else ENUM_VARIANTS

        val bindingEntries = getEntriesFromWalkingScopes(binding, ns)

        for (bindingEntry in bindingEntries) {
            val element = bindingEntry.element() ?: continue

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
    val entries = getEntriesFromWalkingScopes(element, NAMES)
    return entries.filterByName(element.referenceName)
}

fun getFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.fields.asEntries()
}

fun getNamedFieldEntries(fieldsOwner: MvFieldsOwner): List<ScopeEntry> {
    return fieldsOwner.namedFields.asEntries()
}

fun getEntriesFromWalkingScopes(scopeStart: MvElement, ns: NsSet): List<ScopeEntry> {
    val entries = buildList {
        val resolveScopes = getResolveScopes(scopeStart)

        val visitedScopes = hashMapOf<String, NsSet>()
        for ((scope, cameFrom) in resolveScopes) {
            val entries = getEntriesInScope(scope, cameFrom, ns)
            if (entries.isEmpty()) continue

            // state between shadowing processors passed through prevScope
            val currScope = mutableMapOf<String, NsSet>()
            for (entry in entries) {
                val entryNs = entry.ns

                // filter entries by expected ns
                if (!ns.contains(entryNs)) {
                    continue
                }

                val visitedNs = visitedScopes.getOrDefault(entry.name, NONE)
                if (visitedNs.contains(entryNs)) {
                    // all ns for this entry were shadowed
                    continue
                }
                add(entry)
                // save encountered namespaces to the currScope
                currScope[entry.name] = NsSet.of(entryNs, *visitedNs.toTypedArray())
            }
            // at the end put all entries from the current scope into the `visitedScopes`
            visitedScopes.putAll(currScope)
        }
    }
    return entries
}

fun getModulesAsEntries(ctx: ResolutionContext, address: Address): List<ScopeEntry> {
    // no Aptos project, cannot resolve modules
    val moveProject = ctx.moveProject ?: return emptyList()
    val searchScope = moveProject.searchScope()

    if (ctx.isCompletion) {
        val allModules = MvModuleFileIndex.getAllModulesForCompletion(moveProject, searchScope, address)
        return allModules.asEntries()
    }

    val targetModuleName = ctx.path.referenceName ?: return emptyList()
    val modules =
        MvModuleFileIndex.getModulesForId(moveProject, address, targetModuleName).asEntries()
    return modules
}

data class ResolveScope(val scope: MvElement, val prevScope: MvElement)

fun getResolveScopes(start: MvElement): List<ResolveScope> {
    var cameFrom = start
    var scope = start.context as MvElement?

    return buildList {
        while (scope != null) {
            add(ResolveScope(scope, cameFrom))

            // walk all items in original module block
            if (scope is MvModule) {
                // handle `spec module {}` in the current module
                for (moduleSpecBlock in getModuleItemSpecs(scope, cameFrom)) {
                    add(ResolveScope(moduleSpecBlock, cameFrom))
                }

                // walk over all spec modules
                for (moduleSpec in scope.getModuleSpecsFromIndex()) {
                    val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
                    add(ResolveScope(moduleSpecBlock, cameFrom))
                    // handle `spec module {}` in all spec blocks
                    for (childModuleSpecBlock in getModuleItemSpecs(moduleSpecBlock, cameFrom)) {
                        add(ResolveScope(childModuleSpecBlock, cameFrom))
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
    }
}

private fun getModuleItemSpecs(itemsOwner: MvItemsOwner, cameFrom: MvElement): List<MvSpecCodeBlock> {
    val moduleItemSpecs = ModuleItemSpecs(itemsOwner).getResults()
    return moduleItemSpecs.filter { it != cameFrom }.mapNotNull { it.itemSpecBlock }
}

class ModuleItemSpecs(override val owner: MvItemsOwner): PsiCachedValueProvider<List<MvModuleItemSpec>> {
    override fun compute(): CachedValueProvider.Result<List<MvModuleItemSpec>> {
        val moduleItemSpecs = when (owner) {
            is MvModule -> owner.moduleItemSpecList
            is MvModuleSpecBlock -> owner.moduleItemSpecList
            else -> emptyList()
        }
        return owner.psiCacheResult(moduleItemSpecs)
    }
}
