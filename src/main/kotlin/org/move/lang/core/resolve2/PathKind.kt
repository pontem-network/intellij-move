package org.move.lang.core.resolve2

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseGroup
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.ext.allowedNamespaces
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.isUseSpeck
import org.move.lang.core.psi.ext.useSpeck
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.types.Address
import org.move.lang.moveProject

sealed class PathKind {
    // aptos_std:: where aptos_std is a existing named address in a project
    data class NamedAddress(val address: Address.Named): PathKind()

    // 0x1::
    data class ValueAddress(val address: Address.Value): PathKind()

    // foo
    data class UnqualifiedPath(val ns: Set<Namespace>): PathKind()

    // any multi element path
    sealed class QualifiedPath(val path: MvPath, val qualifier: MvPath, val ns: Set<Namespace>): PathKind() {
        // `0x1:foo` or `aptos_framework::foo` (where aptos_framework is known named address)
        class Module(path: MvPath, qualifier: MvPath, ns: Set<Namespace>, val address: Address):
            QualifiedPath(path, qualifier, ns)

        // bar in foo::bar, where foo is not a named address
        class ModuleItem(path: MvPath, qualifier: MvPath, ns: Set<Namespace>):
            QualifiedPath(path, qualifier, ns)

        // bar in `0x1::foo::bar` or `aptos_std::foo::bar` (where aptos_std is known named address)
        class FQModuleItem(path: MvPath, qualifier: MvPath, ns: Set<Namespace>):
            QualifiedPath(path, qualifier, ns)

        // use 0x1::m::{item1};
        //               ^
        class UseGroupItem(path: MvPath, qualifier: MvPath, ns: Set<Namespace>):
            QualifiedPath(path, qualifier, ns)
    }
}

fun MvPath.pathKind(overwriteNs: Set<Namespace>? = null): PathKind {
    val ns = overwriteNs ?: this.allowedNamespaces()
    // [0x1::foo]::bar
    //     ^ qualifier
    val qualifier = this.path
    val moveProject = this.moveProject

    val useGroup = this.ancestorStrict<MvUseGroup>()
    if (useGroup != null) {
        // use 0x1::m::{item}
        //                ^
        val useSpeckQualifier = (useGroup.parent as MvUseSpeck).path
        return PathKind.QualifiedPath.UseGroupItem(this, useSpeckQualifier, ns)
    }

    if (qualifier == null) {
        // one-element path

        // if pathAddress exists, it means it has to be a value address
        val pathAddress = this.pathAddress
        if (pathAddress != null) {
            return PathKind.ValueAddress(Address.Value(pathAddress.text))
        }
        val referenceName = this.referenceName ?: error("if pathAddress is null, reference has to be non-null")

        // check whether it's a first element in use stmt, i.e. use [std]::module;
        //                                                           ^
        val useSpeck = this.useSpeck
        if (useSpeck != null && useSpeck.parent is MvUseStmt) {
            // if so, local path expr is a named address
            val namedAddress = moveProject?.getNamedAddressTestAware(referenceName)
            if (namedAddress != null) {
                return PathKind.NamedAddress(namedAddress)
            }
            // and it can be with null value if absent, still a named address
            return PathKind.NamedAddress(Address.Named(referenceName, null, moveProject))
        }

        // check whether it's inside use group, then it cannot be named address
//        if (this.useSpeck?.useGroup != null) {
//            return PathKind.UnqualifiedPath(ns)
//        }

        // outside use stmt context
        if (moveProject != null) {
            // try whether it's a named address
            val namedAddress = moveProject.getNamedAddressTestAware(referenceName)
            if (namedAddress != null) {
                return PathKind.NamedAddress(namedAddress)
            }
        }

        // if it's not, then it just an unqualified path
        return PathKind.UnqualifiedPath(ns)
    }

    val qualifierOfQualifier = qualifier.path

    // two-element paths
    if (qualifierOfQualifier == null) {
        val qualifierPathAddress = qualifier.pathAddress
        val qualifierItemName = qualifier.referenceName
        when {
            // 0x1::bar
            //       ^
            qualifierPathAddress != null -> {
                val address = Address.Value(qualifierPathAddress.text)
                return PathKind.QualifiedPath.Module(this, qualifier, ns, address)
            }
            // aptos_framework::bar
            //                  ^
            moveProject != null && qualifierItemName != null -> {
                val namedAddress = moveProject.getNamedAddressTestAware(qualifierItemName)
                if (namedAddress != null) {
                    // known named address, can be module path
                    return PathKind.QualifiedPath.Module(this, qualifier, ns, namedAddress)
                }
                if (this.isUseSpeck) {
                    // use std::main where std is the unknown named address
                    val address = Address.Named(qualifierItemName, null, moveProject)
                    return PathKind.QualifiedPath.Module(this, qualifier, ns, address)
                }
            }
        }
        // module::name
        return PathKind.QualifiedPath.ModuleItem(this, qualifier, ns)
    }

    // three-element path
    return PathKind.QualifiedPath.FQModuleItem(this, qualifier, ns)
}
