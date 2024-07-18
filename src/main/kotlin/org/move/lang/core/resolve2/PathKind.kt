package org.move.lang.core.resolve2

import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseGroup
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.ext.allowedNamespaces
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.isUseSpeck
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
    val qualifierPath = this.path
    val moveProject = this.moveProject

    val useGroup = this.ancestorStrict<MvUseGroup>()
    if (useGroup != null) {
        // use 0x1::m::{item}
        //                ^
        val qualifier = (useGroup.parent as MvUseSpeck).path
        return PathKind.QualifiedPath.UseGroupItem(this, qualifier, ns)
    }

    if (qualifierPath == null) {
        val parentPath = this.parent as? MvPath
        if (parentPath != null) {
            // can be an address
            val pathAddress = this.pathAddress
            val referenceName = this.referenceName
            when {
                pathAddress != null -> return PathKind.ValueAddress(Address.Value(pathAddress.text))
                moveProject != null && referenceName != null -> {
                    // try for named address
                    val namedAddress = moveProject.getNamedAddressTestAware(referenceName)
                    if (namedAddress != null) {
                        return PathKind.NamedAddress(namedAddress)
                    }
                    // check whether it's a first item in use speck
                    val speckParent = parentPath.parent as? MvUseSpeck
                    if (speckParent != null && speckParent.parent is MvUseStmt) {
                        // always a named address, even if unknown
                        return PathKind.NamedAddress(Address.Named(referenceName, null, moveProject))
                    }
                }
            }
        }
        return PathKind.UnqualifiedPath(ns)
    }

    val qualifierOfQualifier = qualifierPath.path

    // two-element paths
    if (qualifierOfQualifier == null) {
        val qualifierPathAddress = qualifierPath.pathAddress
        val qualifierItemName = qualifierPath.referenceName
        when {
            // 0x1::bar
            //       ^
            qualifierPathAddress != null -> {
                val address = Address.Value(qualifierPathAddress.text)
                return PathKind.QualifiedPath.Module(this, qualifierPath, ns, address)
            }
            // aptos_framework::bar
            //                  ^
            moveProject != null && qualifierItemName != null -> {
                val namedAddress = moveProject.getNamedAddressTestAware(qualifierItemName)
                if (namedAddress != null) {
                    // known named address, can be module path
                    return PathKind.QualifiedPath.Module(this, qualifierPath, ns, namedAddress)
                }
                if (this.isUseSpeck) {
                    // use std::main where std is the unknown named address
                    val address = Address.Named(qualifierItemName, null, moveProject)
                    return PathKind.QualifiedPath.Module(this, qualifierPath, ns, address)
                }
            }
        }
        // module::name
        return PathKind.QualifiedPath.ModuleItem(this, qualifierPath, ns)
    }

    // three-element path
    return PathKind.QualifiedPath.FQModuleItem(this, qualifierPath, ns)
}