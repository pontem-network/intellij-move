package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.useSpeck
import org.move.lang.core.resolve.resolveLocalItem
import org.move.stdext.wrapWithList

class MvModuleReferenceImpl(
    element: MvModuleRef,
): MvPolyVariantReferenceCached<MvModuleRef>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        if (element.isSelf) return element.containingModule.wrapWithList()

        check(element !is MvFQModuleRef) {
            "That element has different reference item"
        }

        val resolved = resolveLocalItem(element, setOf(Namespace.MODULE)).firstOrNull()
        if (resolved is MvUseAlias) {
            return resolved.wrapWithList()
        }
        val moduleRef = when {
            resolved is MvUseItem && resolved.text == "Self" -> resolved.useSpeck().fqModuleRef
            resolved is MvModuleUseSpeck -> resolved.fqModuleRef
            else -> return emptyList()
        }
        return moduleRef?.reference?.resolve().wrapWithList()
    }
}
