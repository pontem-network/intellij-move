package org.move.lang.core.resolve

import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility

fun processModuleInnerItems(
    module: MvModule,
    namespaces: Set<Namespace>,
    visibilities: Set<Visibility>,
    contextScopeInfo: ContextScopeInfo,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in namespaces) {
        val found = when (namespace) {
            Namespace.NAME -> {
                processor.matchAll(
                    contextScopeInfo,
                    if (contextScopeInfo.isMslScope) module.consts() else emptyList(),
                    if (contextScopeInfo.isMslScope) module.structs() else emptyList(),
                    if (contextScopeInfo.isMslScope)
                        module.allModuleSpecs()
                            .map {
                                it.moduleItemSpecs()
                                    .flatMap { spec -> spec.itemSpecBlock?.globalVariables().orEmpty() }
                            }
                            .flatten()
                    else emptyList()
                )
            }
            Namespace.FUNCTION -> {
                val functions = visibilities.flatMap { module.functionsVisibleInScope(it) }
                val specFunctions =
                    if (contextScopeInfo.isMslScope) module.specFunctions() else emptyList()
                val specInlineFunctions =
                    if (contextScopeInfo.isMslScope) module.specInlineFunctions() else emptyList()
                processor.matchAll(
                    contextScopeInfo,
                    functions, specFunctions, specInlineFunctions
                )
            }
            Namespace.TYPE -> processor.matchAll(contextScopeInfo, module.structs())
            Namespace.SCHEMA -> processor.matchAll(contextScopeInfo, module.schemas())
            Namespace.CONST -> processor.matchAll(contextScopeInfo, module.consts())
            else -> continue
        }
        if (found) return true
    }
    return false
}

fun processModuleSpecItems(
    module: MvModule,
    namespaces: Set<Namespace>,
    contextScopeInfo: ContextScopeInfo,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in namespaces) {
        for (moduleSpec in module.allModuleSpecs()) {
            val matched = when (namespace) {
                Namespace.FUNCTION ->
                    processor.matchAll(
                        contextScopeInfo,
                        moduleSpec.specFunctions(),
                        moduleSpec.specInlineFunctions()
                    )
                Namespace.SCHEMA -> processor.matchAll(contextScopeInfo, moduleSpec.schemas())
                else -> false
            }
            if (matched) return true
        }
    }
    return false
}

fun processModuleItems(
    module: MvModule,
    namespaces: Set<Namespace>,
    visibilities: Set<Visibility>,
    contextScopeInfo: ContextScopeInfo,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    return processModuleInnerItems(module, namespaces, visibilities, contextScopeInfo, processor)
            ||
            contextScopeInfo.isMslScope && processModuleSpecItems(module, namespaces, contextScopeInfo, processor)
}

//fun resolveModuleItem(
//    module: MvModule,
//    name: String,
//    namespaces: Set<Namespace>,
//    visibilities: Set<Visibility>,
//    contextScopeInfo: ContextScopeInfo,
//): List<MvNamedElement> {
//    val resolved = mutableListOf<MvNamedElement>()
//    processModuleItems(module, namespaces, visibilities, contextScopeInfo) {
//        if (it.name == name) {
//            resolved.add(it.element)
//            return@processModuleItems true
//        }
//        return@processModuleItems false
//    }
//    return resolved
//}
