package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.psi.MvUseStmt

val MvUseStmt.addressRef: MvAddressRef?
    get() {
        val moduleUseSpeck = this.moduleUseSpeck
        if (moduleUseSpeck != null) {
            val fqModuleRef = moduleUseSpeck.fqModuleRef
            if (fqModuleRef != null) {
                return fqModuleRef.addressRef
            } else {
                return moduleUseSpeck.addressRef
            }
        }
        val itemUseSpeck = this.itemUseSpeck
        if (itemUseSpeck != null) {
            return itemUseSpeck.fqModuleRef.addressRef
        }
        return null
    }

val MvUseStmt.useSpeckText: String
    get() {
        val moduleUseSpeck = this.moduleUseSpeck
        if (moduleUseSpeck != null) {
            return moduleUseSpeck.text
        }
        val itemUseSpeck = this.itemUseSpeck
        if (itemUseSpeck != null) {
            return itemUseSpeck.text
        }
        return ""
    }
