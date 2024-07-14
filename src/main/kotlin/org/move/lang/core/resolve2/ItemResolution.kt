package org.move.lang.core.resolve2

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.process
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.NAME
import org.move.lang.core.resolve.ref.Namespace.TYPE
import org.move.stdext.intersects
import java.util.*

val MvNamedElement.namespaces: Set<Namespace>
    get() = when (this) {
        is MvFunction -> EnumSet.of(Namespace.FUNCTION)
        is MvStruct -> EnumSet.of(Namespace.TYPE)
        is MvConst -> EnumSet.of(Namespace.CONST)
        is MvSchema -> EnumSet.of(Namespace.SCHEMA)
        is MvModule -> EnumSet.of(Namespace.MODULE)
        else -> EnumSet.of(Namespace.NAME)
    }

val MvNamedElement.namespace
    get() = when (this) {
        is MvFunction -> Namespace.FUNCTION
        is MvStruct -> Namespace.TYPE
        is MvConst -> Namespace.CONST
        is MvSchema -> Namespace.SCHEMA
        is MvModule -> Namespace.MODULE
        else -> error("when should be exhaustive")
    }

fun processItemDeclarations(
    itemsOwner: MvItemsOwner,
    ns: Set<Namespace>,
    processor: RsResolveProcessor
): Boolean {

    // todo:
    // 1. loop over all items in module (item is anything accessible with MODULE:: )
    // 2. for every item, use it's .visibility to create VisibilityFilter, even it's just a { false }

    for (item in itemsOwner.visibleItems) {
        val name = item.name ?: continue

        val namespaces = mutableSetOf(item.namespace)
//        if (namespaces.contains(TYPE) && ns.contains(NAME)) {
//            // struct lit / pat
//            namespaces.add(NAME)
//        }
        if (!namespaces.intersects(ns)) continue

        val itemVisibility = ItemVisibility(item, isTestOnly = item.hasTestOnlyAttr, vis = item.visibility2)
        val visibilityFilter = itemVisibility.createFilter()
        if (processor.process(name, item, namespaces, visibilityFilter)) return true
    }

    return false
}