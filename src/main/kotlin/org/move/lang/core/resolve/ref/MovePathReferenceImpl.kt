package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*

fun processModuleItems(
    module: MvModule,
    itemVis: ItemVis,
    processor: MatchingProcessor,
): Boolean {
    for (namespace in itemVis.namespaces) {
        val found = when (namespace) {
            Namespace.NAME -> processor.matchAll(
                itemVis.visibilities.flatMap { module.functions(it) },
                if (itemVis.isMsl) module.specFunctions() else emptyList(),
                if (itemVis.isMsl) module.constBindings() else emptyList()
            )
            Namespace.TYPE -> processor.matchAll(module.structs())
            Namespace.SCHEMA -> processor.matchAll(module.schemas())
            else -> false
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
    val namespace: Namespace,
) : MvReferenceCached<MvPath>(element), MvPathReference {

    override fun resolveInner(): List<MvNamedElement> {
        val ns = mutableSetOf(namespace)
        val vs = Visibility.buildSetOfVisibilities(element)
        val itemVis = ItemVis(ns, vs, element.mslScope)

        val refName = element.referenceName ?: return emptyList()
        val moduleRef = element.pathIdent.moduleRef
        // first, see whether it's a fully qualified path (ADDRESS::MODULE::NAME) and try to resolve those
        if (moduleRef is MvFQModuleRef) {
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
                    containingModule, refName, itemVis.replace(vs = setOf(Visibility.Internal))
                )
            }
            val fqModuleRef = resolveIntoFQModuleRef(moduleRef) ?: return emptyList()
            val module = fqModuleRef.reference?.resolve() as? MvModule
                ?: return emptyList()
            return resolveModuleItem(module, refName, itemVis)
        } else {
            // if it's NAME
            // special case second argument of update_field function in specs
            if (element.isUpdateFieldArg2) return emptyList()

            // try local names
            val item = resolveItem(element, namespace).firstOrNull() ?: return emptyList()
            // local name -> return
            if (item !is MvUseItem) return listOf(item)
            // find corresponding FQModuleRef from imports and resolve
            val fqModuleRef = item.moduleImport().fqModuleRef
            val module = fqModuleRef.reference?.resolve() as? MvModule
                ?: return emptyList()
            return resolveModuleItem(module, refName, itemVis)
        }
    }
}
