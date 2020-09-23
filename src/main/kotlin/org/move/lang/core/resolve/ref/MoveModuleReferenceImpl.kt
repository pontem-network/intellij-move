package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveModuleRef
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.resolve.resolveModuleRef

class MoveModuleReferenceImpl(
    element: MoveModuleRef,
) : MoveReferenceBase<MoveModuleRef>(element) {

    override fun resolve(): MoveNamedElement? {
        return resolveModuleRef(element)
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
    }
}