package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*

fun processModuleItems(
    module: MvModule,
    itemVis: ItemVis,
    processor: MatchingProcessor<MvNamedElement>,
): Boolean {
    for (namespace in itemVis.namespaces) {
        var found = when (namespace) {
            Namespace.NAME -> processor.matchAll(
                itemVis,
                if (itemVis.isMsl) module.consts() else emptyList()
            )
            Namespace.FUNCTION -> processor.matchAll(
                itemVis,
                itemVis.visibilities.flatMap { module.visibleFunctions(it) },
                if (itemVis.isMsl) module.specFunctions() else emptyList(),
            )
            Namespace.TYPE -> processor.matchAll(itemVis, module.structs())
            Namespace.SCHEMA -> processor.matchAll(itemVis, module.schemas())
            Namespace.ERROR_CONST -> processor.matchAll(itemVis, module.consts())
            else -> false
        }
        if (!found) {
            for (moduleSpec in module.allModuleSpecs()) {
                val moduleSpecBlock = moduleSpec.moduleSpecBlock ?: continue
                found = when (namespace) {
                    Namespace.NAME -> processor.matchAll(itemVis, moduleSpecBlock.specFunctionList)
                    Namespace.SCHEMA -> processor.matchAll(itemVis, moduleSpecBlock.schemaList)
                    else -> false
                }
                if (found) break
            }
        }
        if (found) return true
    }
    return false
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

class MvPathReferenceImpl(
    element: MvPath,
    val namespaces: Set<Namespace>,
) : MvReferenceCached<MvPath>(element), MvPathReference {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    override fun resolveInner(): List<MvNamedElement> {
        val vs = Visibility.buildSetOfVisibilities(element)
        val itemVis = ItemVis(
            namespaces,
            vs,
            element.mslScope,
            itemScope = element.itemScope,
        )

        val refName = element.referenceName ?: return emptyList()
        val moduleRef = element.moduleRef
        // first, see whether it's a fully qualified path (ADDRESS::MODULE::NAME) and try to resolve those
        if (moduleRef is MvFQModuleRef) {
            // TODO: can be replaced with index call
            val module = moduleRef.reference?.resolve() as? MvModule
                ?: return emptyList()
            return resolveModuleItem(module, refName, itemVis)
        }
        // second,
        // if it's MODULE::NAME -> resolve MODULE into corresponding FQModuleRef using imports
        if (moduleRef != null) {
            if (moduleRef.isSelf) {
                val containingModule = moduleRef.containingModule ?: return emptyList()
                return resolveModuleItem(
                    containingModule, refName, itemVis.copy(visibilities = setOf(Visibility.Internal))
                )
            }
            val fqModuleRef = resolveIntoFQModuleRef(moduleRef) ?: return emptyList()
            // TODO: can be replaced with index call
            val module = fqModuleRef.reference?.resolve() as? MvModule
                ?: return emptyList()
            return resolveModuleItem(module, refName, itemVis)
        } else {
            // if it's NAME
            // special case second argument of update_field function in specs
            if (element.isUpdateFieldArg2) return emptyList()

            // try local names
            val item = resolveLocalItem(element, namespaces).firstOrNull() ?: return emptyList()
            // local name -> return
            return when (item) {
                // item import
                is MvUseItem -> {
                    // find corresponding FQModuleRef from imports and resolve
                    val fqModRef = item.useSpeck().fqModuleRef
                    // TODO: index call
                    val module = fqModRef.reference?.resolve() as? MvModule
                        ?: return emptyList()
                    return resolveModuleItem(module, refName, itemVis)
                }
                // module import
                is MvModuleUseSpeck -> {
                    val module = item.fqModuleRef?.reference?.resolve() as? MvModule
                    return listOfNotNull(module)
                }
                // local item
                else -> listOf(item)
            }
        }
    }
}
