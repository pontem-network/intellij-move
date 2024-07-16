package org.move.lang.core.resolve2.ref

import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvUseSpeck
import org.move.lang.core.psi.ext.allowedNamespaces
import org.move.lang.core.psi.ext.qualifier
import org.move.lang.core.psi.ext.rootPath
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve2.ref.RsPathResolveKind.*
import org.move.lang.core.types.Address
import org.move.lang.moveProject

sealed class RsPathResolveKind {
    /** A path consist of a single identifier, e.g. `foo` */
    data class UnqualifiedPath(val ns: Set<Namespace>): RsPathResolveKind()

    /** `bar` in `foo::bar` or `use foo::{bar}` */
    class QualifiedPath(
        val path: MvPath,
        val qualifier: MvPath,
        val ns: Set<Namespace>,
    ): RsPathResolveKind()

    /** bar in `0x1::bar` */
    class ModulePath(
        val path: MvPath,
        val address: Address,
    ): RsPathResolveKind()

    /** aptos_framework in `use aptos_framework::bar`*/
    data object NamedAddressPath: RsPathResolveKind()
}

fun classifyPath(path: MvPath): RsPathResolveKind {
    val qualifier = path.qualifier
    val ns = path.allowedNamespaces()
//        if (qualifier == null) {
//            return UnqualifiedPath(ns)
//        }
    val isUseSpeck = path.rootPath().parent is MvUseSpeck
    if (qualifier == null) {
        // left-most path
        if (isUseSpeck) {
            // use aptos_framework::
            //     //^
            return NamedAddressPath
        }
        return UnqualifiedPath(ns)
    }

    val qualifierPath = qualifier.path
    val pathAddress = qualifier.pathAddress
    val qualifierName = qualifier.referenceName

    return when {
        qualifierPath == null && pathAddress != null -> ModulePath(path, Address.Value(pathAddress.text))
        qualifierPath == null && isUseSpeck && qualifierName != null ->
            ModulePath(path, Address.Named(qualifierName, null, qualifier.moveProject))
//            qualifier.parent is MvUseSpeck && qualifierName != null ->
//                AddressPath(path, Address.Named(qualifierName, null, qualifier.moveProject))
        else -> QualifiedPath(path, qualifier, ns)
    }
}
