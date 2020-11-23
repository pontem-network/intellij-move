package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.parentImport
import org.move.lang.core.resolve.resolveItem

class MoveModuleReferenceImpl(
    element: MoveModuleRef,
) : MoveReferenceCached<MoveModuleRef>(element) {

    override fun resolveInner(): MoveNamedElement? {
        val resolved = resolveItem(element, Namespace.MODULE)
        if (resolved is MoveImportAlias) {
            return resolved
        }
        val qualModuleRef = when {
            resolved is MoveItemImport && resolved.text == "Self" -> resolved.parentImport().fullyQualifiedModuleRef
            resolved is MoveModuleImport -> resolved.fullyQualifiedModuleRef
            else -> return null
        }
        return qualModuleRef.reference.resolve()
    }
}