package org.move.lang.core.resolve

import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace

fun processModuleInnerItems(
    module: MvModule,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in itemVis.namespaces) {
        val found = when (namespace) {
            Namespace.NAME -> {
                processor.matchAll(
                    itemVis,
                    if (itemVis.isMsl) module.consts() else emptyList(),
                    if (itemVis.isMsl) module.structs() else emptyList()
                )
            }
            Namespace.FUNCTION -> {
                val functions = itemVis.visibilities.flatMap { module.visibleFunctions(it) }
                val specFunctions =
                    if (itemVis.isMsl) module.specFunctions() else emptyList()
                val specInlineFunctions =
                    if (itemVis.isMsl) module.specInlineFunctions() else emptyList()
                processor.matchAll(
                    itemVis,
                    functions, specFunctions, specInlineFunctions
                )
            }
            Namespace.TYPE -> processor.matchAll(itemVis, module.structs())
            Namespace.SCHEMA -> processor.matchAll(itemVis, module.schemas())
            Namespace.ERROR_CONST -> processor.matchAll(itemVis, module.consts())
            else -> continue
        }
        if (found) return true
    }
    return false
}

fun processModuleSpecItems(
    module: MvModule,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in itemVis.namespaces) {
        for (moduleSpec in module.allModuleSpecs()) {
            val matched = when (namespace) {
                Namespace.FUNCTION ->
                    processor.matchAll(
                        itemVis,
                        moduleSpec.specFunctions(),
                        moduleSpec.specInlineFunctions()
                    )
                Namespace.SCHEMA -> processor.matchAll(itemVis, moduleSpec.schemas())
                else -> false
            }
            if (matched) return true
        }
    }
    return false
}

fun processModuleItems(
    module: MvModule,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    return processModuleInnerItems(module, itemVis, processor)
            || itemVis.isMsl && processModuleSpecItems(module, itemVis, processor)
}

fun resolveModuleItem(
    module: MvModule,
    name: String,
    itemVis: ItemVis,
): List<MvNamedElement> {
    val resolved = mutableListOf<MvNamedElement>()
    processModuleItems(module, itemVis) {
        if (it.name == name) {
            resolved.add(it.element)
            return@processModuleItems true
        }
        return@processModuleItems false
    }
    return resolved
}
