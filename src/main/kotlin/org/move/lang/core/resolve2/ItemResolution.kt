package org.move.lang.core.resolve2

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.FUNCTIONS
import org.move.lang.core.resolve.ref.NAMES
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.*
import org.move.lang.core.resolve.ref.TYPES
import org.move.lang.core.types.infer.deepFoldTyTypeParameterWith
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyReference
import org.move.lang.moveProject
import java.util.*

val MvNamedElement.namespace
    get() = when (this) {
        is MvFunctionLike -> FUNCTION
        is MvStruct -> Namespace.TYPE
        is MvEnum -> Namespace.ENUM
        is MvConst -> Namespace.NAME
        is MvSchema -> Namespace.SCHEMA
        is MvModule -> Namespace.MODULE
        is MvGlobalVariableStmt -> Namespace.NAME
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
        .processAllItems(setOf(FUNCTION), itemModule.allNonTestFunctions())
}

fun processEnumVariantDeclarations(
    enum: MvEnum,
    ns: Set<Namespace>,
    processor: RsResolveProcessor
): Boolean {
    for (namespace in ns) {
        val stop = when (namespace) {
            NAME -> processor.processAll(NAMES, enum.variants)
            TYPE -> processor.processAll(TYPES, enum.variants)
            FUNCTION -> processor.processAll(FUNCTIONS, enum.tupleVariants)
            else -> continue
        }
        if (stop) return true
    }
    return false
}

fun processItemDeclarations(
    itemsOwner: MvItemsOwner,
    ns: Set<Namespace>,
    processor: RsResolveProcessor
): Boolean {

    // 1. loop over all items in module (item is anything accessible with MODULE:: )
    // 2. for every item, use it's .visibility to create VisibilityFilter, even it's just a { false }
    val items = itemsOwner.itemElements +
            (itemsOwner as? MvModule)?.innerSpecItems.orEmpty() +
            (itemsOwner as? MvModule)?.let { getItemsFromModuleSpecs(it, ns) }.orEmpty()
    for (item in items) {
        val name = item.name ?: continue

        val namespace = item.namespace
        if (namespace !in ns) continue

        if (processor.process(name, item, setOf(namespace))) return true
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
        val thisNs = setOf(namespace)
        for (moduleSpec in module.allModuleSpecs()) {
            val matched = when (namespace) {
                FUNCTION ->
                    processor.processAll(
                        thisNs,
                        moduleSpec.specFunctions(),
                        moduleSpec.specInlineFunctions(),
                    )
                Namespace.SCHEMA -> processor.processAll(thisNs, moduleSpec.schemas())
                else -> false
            }
            if (matched) return true
        }
    }
    return false
}
