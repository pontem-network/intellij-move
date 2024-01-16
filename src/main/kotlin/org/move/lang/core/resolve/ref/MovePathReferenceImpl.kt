package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.isUpdateFieldArg2
import org.move.lang.core.psi.ext.itemUseSpeck
import org.move.lang.core.psi.ext.namespaces
import org.move.lang.core.resolve.*

class MvPathReferenceImpl(
    element: MvPath,
): MvPolyVariantReferenceCached<MvPath>(element), MvPathReference {

    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE

    override fun multiResolveInner(): List<MvNamedElement> {
        val pathNamespaces = element.namespaces()
        val vs = Visibility.buildSetOfVisibilities(element)
        val contextScopeInfo =
            ContextScopeInfo(
                refItemScopes = element.refItemScopes,
                letStmtScope = element.letStmtScope,
            )

        val refName = element.referenceName ?: return emptyList()
        val moduleRef = element.moduleRef
        // first, see whether it's a fully qualified path (ADDRESS::MODULE::NAME) and try to resolve those
        if (moduleRef is MvFQModuleRef) {
            // TODO: can be replaced with index call
            val module = moduleRef.reference?.resolve() as? MvModule ?: return emptyList()
            return resolveModuleItem(module, refName, pathNamespaces, vs, contextScopeInfo)
        }
        // second,
        // if it's MODULE::NAME -> resolve MODULE into corresponding FQModuleRef using imports
        if (moduleRef != null) {
            if (moduleRef.isSelf) {
                val containingModule = moduleRef.containingModule ?: return emptyList()
                return resolveModuleItem(
                    containingModule,
                    refName,
                    pathNamespaces,
                    setOf(Visibility.Internal),
                    contextScopeInfo
                )
            }
            val useSpeckFQModuleRef = resolveIntoFQModuleRefInUseSpeck(moduleRef) ?: return emptyList()
            val useSpeckModule =
                useSpeckFQModuleRef.reference?.resolve() as? MvModule ?: return emptyList()
            return resolveModuleItem(useSpeckModule, refName, pathNamespaces, vs, contextScopeInfo)
        } else {
            // if it's NAME
            // special case second argument of update_field function in specs
            if (element.isUpdateFieldArg2) return emptyList()

            // try local names
            val item = resolveLocalItem(element, pathNamespaces).firstOrNull() ?: return emptyList()
            // local name -> return
            return when (item) {
                // item import
                is MvUseItem -> {
                    // find corresponding FQModuleRef from imports and resolve
//                    // TODO: index call
                    val useSpeckModule =
                        item.itemUseSpeck.fqModuleRef.reference?.resolve() as? MvModule
                            ?: return emptyList()
                    return resolveModuleItem(useSpeckModule, refName, pathNamespaces, vs, contextScopeInfo)
                }
                // module import
                is MvModuleUseSpeck -> {
                    val module = item.fqModuleRef?.reference?.resolve() as? MvModule
                    return listOfNotNull(module)
                }
                // local item
                else -> listOf(item)
            }
        }
    }
}
