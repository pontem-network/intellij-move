package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.MatchingProcessor
import org.move.lang.core.resolve.resolveExternalModule
import org.move.lang.core.resolve.resolveItem
import org.move.lang.core.resolve.resolveModuleRef

class MoveQualPathReferenceImpl<T : MoveQualPathReferenceElement>(
    qualPathRefElement: T,
    private val namespace: Namespace,
) : MoveReferenceBase<T>(qualPathRefElement) {

    override fun resolve(): MoveNamedElement? {
        var moduleRef = element.qualPath.moduleRef
        if (moduleRef == null) {
            val resolved = resolveItem(element, namespace)
            if (resolved !is MoveItemImport) {
                return resolved
            }
            moduleRef = resolved.parentImport().fullyQualifiedModuleRef
        }
//        val moduleRef = element.moduleRef
//            ?: return resolveItem(element, namespace)
        return resolveQualifiedPath(moduleRef, element.referenceName, setOf(namespace))
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
//                module.itemImports(),
                    module.publicFunctions(),
                    module.publicNativeFunctions(),
                    module.structs(),
                    module.nativeStructs(),
                    module.consts(),
                ).flatten()
            )
            Namespace.TYPE -> processor.matchAll(
                listOf(
                    module.itemImportsWithoutAliases(),
                    module.structs(),
                    module.nativeStructs(),
                ).flatten()
            )
            Namespace.SCHEMA -> processor.matchAll(module.schemas())
            else -> false
        }
        if (found) return true
    }
    return false
}

//fun resolvePathReference(refNameElement: Reference, namespace: Namespace): MoveNamedElement? {
//    val moduleRef = qualPath.moduleRef ?: return resolveItem(qualPath, namespace)
//
//    var resolved: MoveNamedElement = resolveModuleRef(moduleRef) ?: return null
//    // resolved could be either external module or local module alias
//    // in case of alias, we need to get to the actual module import, and resolve from there
//    if (resolved is MoveImportAlias) {
//        val parentImport = resolved.parent as MoveModuleImport
//        resolved = resolveExternalModule(parentImport.fullyQualifiedModuleRef) ?: return null
//    }
//
//    val module = resolved as MoveModuleDef
//    var resolvedItem: MoveNamedElement? = null
//    processPublicModuleItems(module, namespace) {
//        if (it.name == qualPath.referenceName && it.element != null) {
//            resolvedItem = it.element
//            return@processPublicModuleItems true
//        }
//        return@processPublicModuleItems false
//    }
//    return resolvedItem
//}


fun resolveQualifiedPath(moduleRef: MoveModuleRef, refName: String, ns: Set<Namespace>): MoveNamedElement? {
    var resolved: MoveNamedElement = resolveModuleRef(moduleRef) ?: return null
    // resolved could be either external module or local module alias
    // in case of alias, we need to get to the actual module import, and resolve from there
    if (resolved is MoveImportAlias) {
        val parentImport = resolved.parent as MoveModuleImport
        resolved = resolveExternalModule(parentImport.fullyQualifiedModuleRef) ?: return null
    }

    val module = resolved as MoveModuleDef
    var resolvedItem: MoveNamedElement? = null
    processPublicModuleItems(module, ns) {
        if (it.name == refName && it.element != null) {
            resolvedItem = it.element
            return@processPublicModuleItems true
        }
        return@processPublicModuleItems false
    }
    return resolvedItem
}


//    val pathAddress = qualPath.address ?: qualPath.containingAddress
//    val pathModuleName = qualPath.moduleName ?: qualPath.containingModule?.name
//    val pathName = qualPath.referenceName
//
//    var resolved: MoveNamedElement? = null
//    walkUpThroughScopes(
//        qualPath,
//        stopAfter = { it is MoveAddressBlock || it.parent is MoveFile }
//    ) { cameFrom, scope ->
//        processLexicalDeclarations(scope, cameFrom, namespace) {
//            run {
//                if (it.element == null || pathName != it.name) return@run false
//
//                if (pathAddress == it.element.containingAddress
//                    && pathModuleName == it.element.containingModule?.name
//                ) {
//                    resolved = it.element
//                    return@run true
//                }
//
//                return@run false
//            }
//        }
//    }
//    return resolved
//}