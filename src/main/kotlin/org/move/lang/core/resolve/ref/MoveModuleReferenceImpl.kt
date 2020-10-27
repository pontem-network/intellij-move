package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveModuleRef
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.resolve.resolveItem
import org.move.lang.core.resolve.resolveModuleRef

class MoveModuleReferenceImpl(
    element: MoveModuleRef,
) : MoveReferenceBase<MoveModuleRef>(element) {

    override fun resolve(): MoveNamedElement? {
        return resolveModuleRef(element)
//        return resolveItem(element, Namespace.MODULE)
    }

//    override fun resolveInner(): List<MoveNamedElement> {
//        return resolveItem(element, Namespace.MODULE)?.let { listOf(it) }.orEmpty()
//        val resolved = resolveModuleRef(element)
//        return if (resolved !== null) listOf(resolved) else emptyList()
//        val moduleRef = element
//        return when (moduleRef) {
//            is MoveFullyQualifiedModuleRef -> resolveExternalModule(moduleRef)
//            else -> resolveUnqualifiedModuleRef(moduleRef)
//        }
//        return when (container) {
//            is MoveModuleImport -> resolveExternalModule(container.fullyQualifiedModuleRef)
//            else -> {
//                if (moduleRef is MoveFullyQualifiedModuleRef) {
//                    resolveExternalModule(moduleRef)
//                } else {
//                    resolveUnqualifiedModuleRef(moduleRef)
//                }
//            }
//        }
//    }
}