package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.MatchingProcessor
import org.move.lang.core.resolve.resolveItem
import org.move.lang.core.resolve.resolveModuleRefIntoQual

class MoveQualPathReferenceImpl<T : MoveQualPathReferenceElement>(
    qualPathRefElement: T,
    private val namespace: Namespace,
) : MoveReferenceBase<T>(qualPathRefElement) {

    override fun resolve(): MoveNamedElement? {
        val moduleRef = element.qualPath.moduleRef
        val refName = element.referenceName ?: return null

        val qualModuleRef =
            if (moduleRef != null) {
                if (moduleRef.isSelf) {
                    val containingModule = moduleRef.containingModule ?: return null
                    val vs = setOf(Visibility.Internal())
                    val ns = setOf(namespace)
                    return resolveModuleItem(containingModule, refName, vs, ns)
                }
                resolveModuleRefIntoQual(moduleRef) ?: return null
            } else {
                val resolved = resolveItem(element, namespace)
                if (resolved !is MoveItemImport) {
                    return resolved
                }
                resolved.parentImport().fullyQualifiedModuleRef
            }

        val vs = Visibility.buildSetOfVisibilities(element)
        val module = (qualModuleRef.reference?.resolve() as? MoveModuleDef) ?: return null

        return resolveModuleItem(module, refName, vs, setOf(namespace))
    }
}

fun processModuleItems(
    module: MoveModuleDef,
    visibilities: Set<Visibility>,
    namespaces: Set<Namespace>,
    processor: MatchingProcessor,
): Boolean {
    for (namespace in namespaces) {
        val found = when (namespace) {
            Namespace.NAME -> processor.matchAll(
                listOf(
                    visibilities.flatMap { module.functionSignatures(it) },
                    module.structSignatures(),
                    module.consts(),
                ).flatten()
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
    module: MoveModuleDef,
    refName: String,
    vs: Set<Visibility>,
    ns: Set<Namespace>,
): MoveNamedElement? {
    var resolved: MoveNamedElement? = null
    processModuleItems(module, vs, ns) {
        if (it.name == refName && it.element != null) {
            resolved = it.element
            return@processModuleItems true
        }
        return@processModuleItems false
    }
    return resolved
}
