package org.move.lang.core.resolve2

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.createProcessor
import org.move.lang.core.resolve.process
import org.move.lang.core.resolve.processAll
import org.move.lang.core.resolve.ref.Namespace
import java.util.*

val MvNamedElement.namespaces: Set<Namespace>
    get() = when (this) {
        is MvFunction -> EnumSet.of(Namespace.FUNCTION)
        is MvStruct -> EnumSet.of(Namespace.TYPE)
        is MvConst -> EnumSet.of(Namespace.NAME)
//        is MvConst -> EnumSet.of(Namespace.CONST)
        is MvSchema -> EnumSet.of(Namespace.SCHEMA)
        is MvModule -> EnumSet.of(Namespace.MODULE)
        else -> EnumSet.of(Namespace.NAME)
    }

val MvNamedElement.namespace
    get() = when (this) {
        is MvFunctionLike -> Namespace.FUNCTION
        is MvStruct -> Namespace.TYPE
        is MvConst -> Namespace.NAME
        is MvSchema -> Namespace.SCHEMA
        is MvModule -> Namespace.MODULE
        is MvGlobalVariableStmt -> Namespace.NAME
        else -> error("when should be exhaustive, $this is not covered")
    }

fun processItemDeclarations(
    itemsOwner: MvItemsOwner,
    ns: Set<Namespace>,
    processor: RsResolveProcessor
): Boolean {

    // 1. loop over all items in module (item is anything accessible with MODULE:: )
    // 2. for every item, use it's .visibility to create VisibilityFilter, even it's just a { false }
    val items = itemsOwner.itemElements +
            (itemsOwner as? MvModuleBlock)?.module?.innerSpecItems.orEmpty() +
            (itemsOwner as? MvModuleBlock)?.module?.let { getItemsFromModuleSpecs(it, ns) }.orEmpty()
    for (item in items) {
        val name = item.name ?: continue

        val namespace = item.namespace
        if (namespace !in ns) continue

        val visibilityFilter = item.visInfo().createFilter()
        if (processor.process(name, item, EnumSet.of(namespace), visibilityFilter)) return true
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
                Namespace.FUNCTION ->
                    processor.processAll(
                        moduleSpec.specFunctions(),
                        moduleSpec.specInlineFunctions(),
                    )
                Namespace.SCHEMA -> processor.processAll(moduleSpec.schemas())
                else -> false
            }
            if (matched) return true
        }
    }
    return false
}
