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
        val qualModuleRef =
            if (moduleRef == null) {
                val resolved = resolveItem(element, namespace)
                if (resolved !is MoveItemImport) {
                    return resolved
                }
                resolved.parentImport().fullyQualifiedModuleRef
            } else {
                resolveModuleRefIntoQual(moduleRef) ?: return null
            }
        return resolveQualifiedPath(qualModuleRef, element.referenceName, setOf(namespace))
    }
}

fun processPublicModuleItems(
    module: MoveModuleDef,
    ns: Set<Namespace>,
    processor: MatchingProcessor,
): Boolean {
    for (namespace in ns) {
        val found = when (namespace) {
            Namespace.NAME -> processor.matchAll(
                listOf(
                    module.publicFnSignatures(),
                    module.structSignatures(),
                    module.consts(),
                ).flatten()
            )
            Namespace.TYPE -> processor.matchAll(module.structSignatures())
            Namespace.SCHEMA -> processor.matchAll(module.schemas())
            else -> false
        }
        if (found) return true
    }
    return false
}

fun resolveQualifiedPath(
    qualModuleRef: MoveFullyQualifiedModuleRef,
    refName: String,
    ns: Set<Namespace>,
): MoveNamedElement? {
    val module = (qualModuleRef.reference.resolve() as? MoveModuleDef) ?: return null
    var resolved: MoveNamedElement? = null
    processPublicModuleItems(module, ns) {
        if (it.name == refName && it.element != null) {
            resolved = it.element
            return@processPublicModuleItems true
        }
        return@processPublicModuleItems false
    }
    return resolved
}
