package org.move.lang.core.resolve

import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseGroup
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.types.Address
import org.move.lang.moveProject

sealed class PathKind {

    abstract val ns: NsSet

    // `use aptos_std::` where aptos_std is a existing named address in a project
    data class NamedAddress(val address: Address.Named): PathKind() {
        override val ns: NsSet get() = NONE
    }

    data class NamedAddressOrUnqualifiedPath(
        val address: Address.Named,
        override val ns: NsSet
    ): PathKind()

    // 0x1::
    data class ValueAddress(val address: Address.Value): PathKind() {
        override val ns: NsSet get() = NONE
    }

    // foo
    data class UnqualifiedPath(override val ns: NsSet): PathKind()

    // any multi element path
    sealed class QualifiedPath(
        val path: MvPath,
        val qualifier: MvPath,
        override val ns: NsSet
    ): PathKind() {
        // `0x1:foo`
        class Module(path: MvPath, qualifier: MvPath, ns: NsSet, val address: Address):
            QualifiedPath(path, qualifier, ns)

        // `aptos_framework::foo` (where aptos_framework is known named address, but it can still be a module)
        class ModuleOrItem(path: MvPath, qualifier: MvPath, ns: NsSet, val address: Address):
            QualifiedPath(path, qualifier, ns)

        // bar in foo::bar, where foo is not a named address
        class ModuleItemOrEnumVariant(path: MvPath, qualifier: MvPath, ns: NsSet): QualifiedPath(path, qualifier, ns)

        // bar in `0x1::foo::bar` or `aptos_std::foo::bar` (where aptos_std is known named address)
        class FQModuleItem(path: MvPath, qualifier: MvPath, ns: NsSet): QualifiedPath(path, qualifier, ns) {
            fun baseAddress(): Address? {
                val base = this.path.basePath()
                val pathKind = base.pathKind(isCompletion = false)
                return when (pathKind) {
                    is NamedAddress -> pathKind.address
                    is ValueAddress -> pathKind.address
                    else -> null
                }
            }
        }

        // use 0x1::m::{item1};
        //               ^
        class UseGroupItem(path: MvPath, qualifier: MvPath, ns: NsSet):
            QualifiedPath(path, qualifier, ns)
    }
}

fun MvPath.pathKind(isCompletion: Boolean = false): PathKind {
    // [0x1::foo]::bar
    //     ^ qualifier
    val qualifier = this.path
    val moveProject = this.moveProject

    val useGroup = this.ancestorStrict<MvUseGroup>()
    if (useGroup != null) {
        // use 0x1::m::{item}
        //                ^
        val useSpeckQualifier = (useGroup.parent as MvUseSpeck).path
        return PathKind.QualifiedPath.UseGroupItem(
            this,
            useSpeckQualifier,
            // MODULES for `Self`
            IMPORTABLE_NS_N_MODULES
        )
    }

    // one-element path
    if (qualifier == null) {
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
            return PathKind.NamedAddress(Address.Named(referenceName))
        }

        // outside use stmt context
        if (moveProject != null) {
            // check whether there's a '::' after it, then try for a named address
            if (this.isColonColonNext) {
                val namedAddress = moveProject.getNamedAddress(referenceName)
                if (namedAddress != null) {
                    return PathKind.NamedAddressOrUnqualifiedPath(namedAddress, MODULES)
                }
            }
        }

        // if it's not, then it just an unqualified path
        val ns = this.allowedNamespaces(isCompletion)
        return PathKind.UnqualifiedPath(ns)
    }

    val qualifierOfQualifier = qualifier.path
    val ns = this.allowedNamespaces(isCompletion)

    // two-element paths
    if (qualifierOfQualifier == null) {
        val qualifierPathAddress = qualifier.pathAddress
        val qualifierReferenceName = qualifier.referenceName
        when {
            // 0x1::bar
            //       ^
            qualifierPathAddress != null -> {
                val address = Address.Value(qualifierPathAddress.text)
                return PathKind.QualifiedPath.Module(this, qualifier, MODULES, address)
            }
            // aptos_framework::bar
            //                  ^
            moveProject != null && qualifierReferenceName != null -> {
                val namedAddress = moveProject.getNamedAddress(qualifierReferenceName)
                // `use std::main`
                //            ^
                if (this.isUseSpeck) {
                    val address =
                        namedAddress ?: Address.Named(qualifierReferenceName)
                    return PathKind.QualifiedPath.Module(this, qualifier, MODULES, address)
                }
                if (namedAddress != null) {
                    // known named address, can be module path, or module item path too
                    return PathKind.QualifiedPath.ModuleOrItem(this, qualifier, ns.add(MODULES), namedAddress)
                }
            }
        }
        // module::name
        //         ^
        return PathKind.QualifiedPath.ModuleItemOrEnumVariant(this, qualifier, ns)
    }

    // three or four element path
    if (this.isUseSpeck) {
        // MODULES are for `use 0x1::m::Self;`
        return PathKind.QualifiedPath.FQModuleItem(this, qualifier, ns.add(MODULES))
    }

    return PathKind.QualifiedPath.FQModuleItem(this, qualifier, ns)
}
