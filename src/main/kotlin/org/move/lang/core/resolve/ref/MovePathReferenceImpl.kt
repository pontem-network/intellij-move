package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
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
                visibilities.flatMap { module.functions(it) }
            )
            Namespace.TYPE -> processor.matchAll(module.structs())
            Namespace.SCHEMA -> processor.matchAll(module.schemas())
            Namespace.SPEC_FUNC_NAME -> processor.matchAll(module.specFunctions())
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
): List<MvNamedElement> {
    val resolved = mutableListOf<MvNamedElement>()
    processModuleItems(module, vs, ns) {
        if (it.name == refName && it.element != null) {
            resolved.add(it.element)
            return@processModuleItems true
        }
        return@processModuleItems false
    }
    return resolved
}

class MvPathReferenceImpl(
    element: MvPath,
    private val namespace: Namespace,
) : MvReferenceCached<MvPath>(element), MvPathReference {

    override fun resolveInner(): List<MvNamedElement> {
        val vs = Visibility.buildSetOfVisibilities(element)
        val ns = mutableSetOf(namespace)
        // hack to add spec functions to the processable items in case of spec blocks
        // TODO: ns, vs and msl should be made into NameScope object and passed around
        if (ns.contains(Namespace.NAME) && element.isMsl) {
            ns.add(Namespace.SPEC_FUNC_NAME)
        }
        val refName = element.referenceName ?: return emptyList()

        val moduleRef = element.pathIdent.moduleRef
        // first, see whether it's a fully qualified path (ADDRESS::MODULE::NAME) and try to resolve those
        if (moduleRef is MvFQModuleRef) {
            val module = moduleRef.reference?.resolve() as? MvModuleDef ?: return emptyList()
            return resolveModuleItem(module, refName, vs, ns)
        }
        // second,
        // if it's MODULE::NAME -> resolve MODULE into corresponding FQModuleRef using imports
        if (moduleRef != null) {
            if (moduleRef.isSelf) {
                val containingModule = moduleRef.containingModule ?: return emptyList()
                return resolveModuleItem(
                    containingModule, refName, setOf(Visibility.Internal), ns
                )
            }
            val fqModuleRef = resolveIntoFQModuleRef(moduleRef) ?: return emptyList()
            val module = fqModuleRef.reference?.resolve() as? MvModuleDef ?: return emptyList()
            return resolveModuleItem(module, refName, vs, ns)
        } else {
            // if it's NAME
            // try local names
            val item = resolveItem(element, namespace).firstOrNull() ?: return emptyList()
            // local name -> return
            if (item !is MvItemImport) return listOf(item)
            // find corresponding FQModuleRef from imports and resolve
            val fqModuleRef = item.moduleImport().fqModuleRef
            val module = fqModuleRef.reference?.resolve() as? MvModuleDef ?: return emptyList()
            return resolveModuleItem(module, refName, vs, ns)
        }
    }

    override fun advancedResolve(): BoundElement<MvNamedElement>? {
        return resolve()?.let { BoundElement(it) }
    }

//    override fun handleElementRename(newElementName: String): PsiElement? {
//        val newElement = super.handleElementRename(newElementName)
//        return newElement
//    }
}
