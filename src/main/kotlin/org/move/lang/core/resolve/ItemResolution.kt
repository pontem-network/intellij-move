package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.MODULES
import org.move.lang.core.resolve.ref.NAMES
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.*
import org.move.lang.core.resolve.ref.SCHEMAS
import org.move.lang.core.resolve.ref.TYPES
import org.move.lang.core.resolve.ref.TYPES_N_ENUMS_N_NAMES
import org.move.lang.core.resolve.ref.TYPES_N_NAMES
import org.move.lang.core.types.infer.deepFoldTyTypeParameterWith
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyReference
import org.move.lang.moveProject

val MvNamedElement.moduleItemNamespace
    get() = when (this) {
        is MvFunctionLike -> NAME
        is MvStruct -> TYPE
        is MvEnum -> ENUM
        is MvConst -> NAME
        is MvSchema -> SCHEMA
        is MvModule -> MODULE
        is MvGlobalVariableStmt -> NAME
        else -> error("when should be exhaustive, $this is not covered")
    }

val MvNamedElement.itemNs
    get() = when (this) {
        is MvFunctionLike -> NAMES
        is MvStruct -> TYPES_N_NAMES
        is MvEnum -> TYPES_N_ENUMS_N_NAMES
        is MvConst -> NAMES
        is MvModule -> MODULES
        is MvSchema -> SCHEMAS
        is MvGlobalVariableStmt -> NAMES
        else -> error("when should be exhaustive, $this is not covered")
    }

fun processMethodResolveVariants(
    methodOrField: MvMethodOrField,
    receiverTy: Ty,
    msl: Boolean,
    processor: RsResolveProcessor
): Boolean {
    val moveProject = methodOrField.moveProject ?: return false
    val itemModule = receiverTy.itemModule(moveProject) ?: return false
    return processor
        .wrapWithFilter { e ->
            val function = e.element as? MvFunction ?: return@wrapWithFilter false
            val selfTy = function.selfParamTy(msl) ?: return@wrapWithFilter false
            // need to use TyVar here, loweredType() erases them
            val selfTyWithTyVars =
                selfTy.deepFoldTyTypeParameterWith { tp -> TyInfer.TyVar(tp) }
            TyReference.isCompatibleWithAutoborrow(receiverTy, selfTyWithTyVars, msl)
        }
        .processAll(itemModule.allNonTestFunctions().mapNotNull { it.asEntry() })
}

fun processEnumVariantDeclarations(
    enum: MvEnum,
    ns: Set<Namespace>,
    processor: RsResolveProcessor
): Boolean {
    for (namespace in ns) {
        val stop = when (namespace) {
            NAME -> processor.processAll(enum.variants.mapNotNull { it.asEntry()?.copyWithNs(NAMES) })
            TYPE -> processor.processAll(enum.variants.mapNotNull { it.asEntry()?.copyWithNs(TYPES) })
            else -> continue
        }
        if (stop) return true
    }
    return false
}

fun processItemDeclarations(
    itemsOwner: MvModule,
    ns: Set<Namespace>,
    processor: RsResolveProcessor
): Boolean {
    // 1. loop over all items in module (item is anything accessible with MODULE:: )
    // 2. for every item, use it's .visibility to create VisibilityFilter, even it's just a { false }
    val items = itemsOwner.itemElements +
            (itemsOwner as? MvModule)?.innerSpecItems.orEmpty() +
            (itemsOwner as? MvModule)?.let { getItemsFromModuleSpecs(it, ns) }.orEmpty()
    for (itemElement in items) {
        val name = itemElement.name ?: continue

        val itemNamespace = itemElement.moduleItemNamespace
        if (itemNamespace !in ns) continue

        if (processor.process(ScopeEntryWithVisibility(
                name,
                itemElement,
                setOf(itemNamespace),
                adjustedItemScope = NamedItemScope.MAIN
            ))) return true
    }

    return false
}

fun getItemsFromModuleSpecs(module: MvModule, ns: Set<Namespace>): List<MvItemElement> {
    val c = mutableListOf<MvItemElement>()
    processItemsFromModuleSpecs(module, ns, createProcessor { c.add(it.element as MvItemElement) })
    return c
}

fun processItemsFromModuleSpecs(
    module: MvModule,
    namespaces: Set<Namespace>,
    processor: RsResolveProcessor,
): Boolean {
    for (namespace in namespaces) {
        for (moduleSpec in module.allModuleSpecs()) {
            val matched = when (namespace) {
                NAME ->
                    processor.processAllNamedElements(
                        NAMES,
                        moduleSpec.specFunctions(),
                        moduleSpec.specInlineFunctions(),
                    )
                SCHEMA -> processor.processAllNamedElements(SCHEMAS, moduleSpec.schemas())
                else -> false
            }
            if (matched) return true
        }
    }
    return false
}
