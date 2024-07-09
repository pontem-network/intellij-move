package org.move.ide.inspections.imports

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.hasTestAttr
import org.move.lang.core.psi.ext.hasTestOnlyAttr
import org.move.lang.core.psi.ext.moduleRef

// only Main/Test for now
val MvPath.pathUsageScope: NamedItemScope
    get() {
        var parentElement = this.parent
        while (parentElement != null) {
//        if (parentElement is MslOnlyElement) return ItemScope.MAIN
            if (parentElement is MvDocAndAttributeOwner && parentElement.hasTestOnlyAttr) {
                return NamedItemScope.TEST
            }
            if (parentElement is MvFunction && parentElement.hasTestAttr) {
                return NamedItemScope.TEST
            }
            parentElement = parentElement.parent
        }
        return NamedItemScope.MAIN
    }

// only Main/Test for now
val MvUseStmt.declaredItemScope: NamedItemScope
    get() {
        if (this.hasTestOnlyAttr) {
            return NamedItemScope.TEST
        }
        var parentElement = this.parent
        while (parentElement != null) {
//        if (parentElement is MslOnlyElement) return ItemScope.MAIN
            if (parentElement is MvDocAndAttributeOwner && parentElement.hasTestOnlyAttr) {
                return NamedItemScope.TEST
            }
            if (parentElement is MvFunction && parentElement.hasTestAttr) {
                return NamedItemScope.TEST
            }
            parentElement = parentElement.parent
        }
        return NamedItemScope.MAIN
    }

sealed class PathStart(open val usageScope: NamedItemScope) {
    data class Address(
        val addressRef: MvAddressRef,
        override val usageScope: NamedItemScope
    ): PathStart(usageScope)

    data class Module(
        val modName: String,
        val moduleRef: MvModuleRef,
        override val usageScope: NamedItemScope
    ): PathStart(usageScope)

    data class Item(
        val itemName: String,
        override val usageScope: NamedItemScope
    ): PathStart(usageScope)

    companion object {
        val MvPath.pathStart: PathStart?
            get() {
                val usageScope = this.pathUsageScope
                val pathModuleRef = this.moduleRef
                if (pathModuleRef != null) {
                    if (pathModuleRef is MvFQModuleRef) {
                        return Address(pathModuleRef.addressRef, usageScope)
                    } else {
                        val modName = pathModuleRef.referenceName ?: return null
                        return Module(modName, pathModuleRef, usageScope)
                    }
                } else {
                    val itemName = this.referenceName ?: return null
                    return Item(itemName, usageScope)
                }
            }
    }
}

