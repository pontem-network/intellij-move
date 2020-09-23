package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveItemImport
import org.move.lang.core.psi.MoveNamedElement

//class MoveItemImportReferenceImpl(element: MoveItemImport) : MoveReferenceBase<MoveItemImport>(element) {
//    override fun resolveVerbose(): ResolveEngine.ResolveResult {
//        val candidates = resolveItemImport(element)
//        return ResolveEngine.ResolveResult.buildFrom(candidates)
//
//    }
//}
//
//fun resolveItemImport(item: MoveItemImport): List<MoveNamedElement> {
//    val candidates = mutableListOf<MoveNamedElement>()
//
////    val address = moduleRef.addressRef?.address()
////    val moduleName = item.moduleRef.referenceName
////
////    walkUpThroughScopes(
////        item,
////        stopAfter = { it.parent is MoveFile }
////    ) { cameFrom, scope ->
////        processLexicalDeclarations(scope, cameFrom, Namespace.MODULE) {
////            run {
////                if (it.element == null || moduleName != it.name) return@run false
////
////                val element = it.element
////                if (element is MoveModuleImport
////                    && (address == null
////                            || element.moduleRef.addressRef?.address() == address)
////                ) {
////                    candidates += element
////                    return@run true
////                }
////
////                return@run false
////            }
////        }
////    }
//    return candidates
//}