package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvItemImport
import org.move.lang.core.psi.MvNamedElement

//class MvItemImportReferenceImpl(element: MvItemImport) : MvReferenceBase<MvItemImport>(element) {
//    override fun resolveVerbose(): ResolveEngine.ResolveResult {
//        val candidates = resolveItemImport(element)
//        return ResolveEngine.ResolveResult.buildFrom(candidates)
//
//    }
//}
//
//fun resolveItemImport(item: MvItemImport): List<MvNamedElement> {
//    val candidates = mutableListOf<MvNamedElement>()
//
////    val address = moduleRef.addressRef?.address()
////    val moduleName = item.moduleRef.referenceName
////
////    walkUpThroughScopes(
////        item,
////        stopAfter = { it.parent is MvFile }
////    ) { cameFrom, scope ->
////        processLexicalDeclarations(scope, cameFrom, Namespace.MODULE) {
////            run {
////                if (it.element == null || moduleName != it.name) return@run false
////
////                val element = it.element
////                if (element is MvModuleImport
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
