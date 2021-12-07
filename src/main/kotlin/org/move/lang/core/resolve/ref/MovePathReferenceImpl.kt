package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.functionSignatures
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.moduleImport
import org.move.lang.core.psi.ext.structSignatures
import org.move.lang.core.resolve.MatchingProcessor
import org.move.lang.core.resolve.resolveIntoFQModuleRef
import org.move.lang.core.resolve.resolveItem
import org.move.lang.core.types.BoundElement

fun processModuleItems(
    module: MvModuleDef,
    visibilities: Set<Visibility>,
    namespaces: Set<Namespace>,
    processor: MatchingProcessor,
): Boolean {
    for (namespace in namespaces) {
        val found = when (namespace) {
            Namespace.NAME -> processor.matchAll(
                visibilities.flatMap { module.functionSignatures(it) }
//                listOf(
//                    visibilities.flatMap { module.functionSignatures(it) },
////                    module.structSignatures(),
////                    module.consts(),
//                ).flatten()
            )
            Namespace.TYPE -> processor.matchAll(module.structSignatures())
//            Namespace.SCHEMA -> processor.matchAll(module.schemas())
            else -> false
        }
        if (found) return true
    }
    return false
}

fun resolveModuleItem(
    module: MvModuleDef,
    refName: String,
    vs: Set<Visibility>,
    ns: Set<Namespace>,
): MvNamedElement? {
    var resolved: MvNamedElement? = null
    processModuleItems(module, vs, ns) {
        if (it.name == refName && it.element != null) {
            resolved = it.element
            return@processModuleItems true
        }
        return@processModuleItems false
    }
    return resolved
}

class MvPathReferenceImpl(
    element: MvPath,
    private val namespace: Namespace,
) : MvReferenceBase<MvPath>(element), MvPathReference {

    override fun resolve(): MvNamedElement? {
        val vs = Visibility.buildSetOfVisibilities(element)
        val ns = setOf(namespace)
        val refName = element.referenceName ?: return null

        val moduleRef = element.pathIdent.moduleRef
        // first, see whether it's a fully qualified path (ADDRESS::MODULE::NAME) and try to resolve those
        if (moduleRef is MvFQModuleRef) {
            val module = moduleRef.reference?.resolve() ?: return null
            return resolveModuleItem(module, refName, vs, ns)
        }
        // second,
        // if it's MODULE::NAME -> resolve MODULE into corresponding FQModuleRef using imports
        if (moduleRef != null) {
            if (moduleRef.isSelf) {
                val containingModule = moduleRef.containingModule ?: return null
                return resolveModuleItem(
                    containingModule, refName, setOf(Visibility.Internal()), ns
                )
            }
            val fqModuleRef = resolveIntoFQModuleRef(moduleRef) ?: return null
            val module = fqModuleRef.reference?.resolve() ?: return null
            return resolveModuleItem(module, refName, vs, ns)
        } else {
            // if it's NAME
            // try local names
            val item = resolveItem(element, namespace)
            // local name -> return
            if (item !is MvItemImport) return item
            // find corresponding FQModuleRef from imports and resolve
            val fqModuleRef = item.moduleImport().fqModuleRef
            val module = fqModuleRef.reference?.resolve() ?: return null
            return resolveModuleItem(module, refName, vs, ns)
        }
    }

    override fun advancedResolve(): BoundElement<MvNamedElement>? {
        return resolve()?.let { BoundElement(it) }
    }
}
