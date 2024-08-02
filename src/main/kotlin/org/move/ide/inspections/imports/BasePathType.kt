package org.move.ide.inspections.imports

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

// classifies foo of `foo::bar::baz`
sealed class BasePathType {
    data object Address: BasePathType()
    data class Module(val moduleName: String): BasePathType()
    data class Item(val itemName: String): BasePathType()
}

fun MvPath.basePathType(): BasePathType? {
    val rootPath = this.rootPath()
    val qualifier = rootPath.qualifier
    // foo
    if (qualifier == null) {
        return rootPath.referenceName?.let { BasePathType.Item(it) }
    }

    // 0x1::m
    if (qualifier.pathAddress != null) return BasePathType.Address

    val qualifierBase = qualifier.qualifier
    // aptos_framework::m::foo
    if (qualifierBase != null) {
        return BasePathType.Address
    }

    // todo: `aptos_framework::m`,
    //  first resolve aptos_framework into the NamedAddress, then return the BasePathType.Address

    // m::foo
    return qualifier.referenceName?.let { BasePathType.Module(it) }
}

// only Main/Test for now
val MvElement.usageScope: NamedItemScope
    get() {
        var parentElement = this.parent
        while (parentElement != null) {
//        if (parentElement is MslOnlyElement) return ItemScope.MAIN
            if (parentElement is MvDocAndAttributeOwner && parentElement.hasTestOnlyAttr) {
                return NamedItemScope.TEST
            }
            if (parentElement is MvDocAndAttributeOwner && parentElement.hasVerifyOnlyAttr) {
                return NamedItemScope.VERIFY
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
