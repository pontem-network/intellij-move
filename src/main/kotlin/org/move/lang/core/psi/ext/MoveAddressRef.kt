package org.move.lang.core.psi.ext

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.types.Address
import org.move.lang.moveProject

fun MvAddressRef.toAddress(contextProject: MoveProject? = this.moveProject): Address? {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        val refName = namedAddress.referenceName
        return contextProject?.getAddressValue(refName)?.let { Address(it) }
    }
    val addressLitText = diemAddress?.text ?: bech32Address?.text ?: return null
    return Address(addressLitText)
}

fun MvAddressRef.toNormalizedAddress(contextProject: MoveProject = this.moveProject!!): Address? =
    this.toAddress(contextProject)?.normalized()
