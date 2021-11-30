package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.moduleImport
import org.move.lang.core.resolve.resolveItem

class MoveModuleReferenceImpl(
    element: MoveModuleRef,
) : MoveReferenceCached<MoveModuleRef>(element) {

    override fun resolveInner(): MoveNamedElement? {
        if (element.isSelf) return element.containingModule

        val resolved = resolveItem(element, Namespace.MODULE)
        if (resolved is MoveImportAlias) {
            return resolved
        }
        val moduleRef = when {
            resolved is MoveItemImport && resolved.text == "Self" -> resolved.moduleImport().fqModuleRef
            resolved is MoveModuleImport -> resolved.fqModuleRef
            else -> return null
        }
        return moduleRef.reference?.resolve()
    }
}
