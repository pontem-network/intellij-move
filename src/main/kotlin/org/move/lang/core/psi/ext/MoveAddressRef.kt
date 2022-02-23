package org.move.lang.core.psi.ext

import org.move.cli.MoveProject
import org.move.lang.core.psi.MvAddressRef
import org.move.lang.core.types.Address
import org.move.lang.moveProject

fun MvAddressRef.toAddress(moveProj: MoveProject? = this.moveProject): Address? {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        val refName = namedAddress.referenceName
        return moveProj?.getNamedAddress(refName)
    }
    val addressLitText = diemAddress?.text ?: bech32Address?.text ?: return null
    return Address.Value(addressLitText)
}

//fun MvAddressRef.toNormalizedAddress(contextProject: MoveProject = this.moveProject!!): Address? =
//    this.toAddress(contextProject)?.normalized()
