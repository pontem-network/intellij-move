package org.move.lang.core.psi.ext

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.types.Address
import org.move.lang.moveProject

fun MvAddressRef.toAddress(contextProject: MoveProject? = this.moveProject()): Address? {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        val refName = namedAddress.referenceName ?: return null
        return contextProject?.getAddresses()?.get(refName)?.let { Address(it) }
    }
    val addressLit = addressIdent?.text ?: bech32AddressIdent?.text ?: return null
    return Address(addressLit)
}

fun MvAddressRef.toNormalizedAddress(contextProject: MoveProject = this.moveProject()!!): Address? =
    this.toAddress(contextProject)?.normalized()
