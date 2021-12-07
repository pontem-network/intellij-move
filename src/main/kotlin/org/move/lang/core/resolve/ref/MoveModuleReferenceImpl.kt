package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.moduleImport
import org.move.lang.core.resolve.resolveItem

class MvModuleReferenceImpl(
    element: MvModuleRef,
) : MvReferenceCached<MvModuleRef>(element) {

    override fun resolveInner(): MvNamedElement? {
        if (element.isSelf) return element.containingModule

        val resolved = resolveItem(element, Namespace.MODULE)
        if (resolved is MvImportAlias) {
            return resolved
        }
        val moduleRef = when {
            resolved is MvItemImport && resolved.text == "Self" -> resolved.moduleImport().fqModuleRef
            resolved is MvModuleImport -> resolved.fqModuleRef
            else -> return null
        }
        return moduleRef.reference?.resolve()
    }
}
