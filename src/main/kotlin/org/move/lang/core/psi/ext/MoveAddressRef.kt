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

val MvAddressRef.useGroupLevel: Int get() {
    // sort to the end if not a named address
    if (this.namedAddress == null) return 3

    val name = this.namedAddress?.text.orEmpty()
    val packageAddrs = this.moveProject?.packageAddresses()?.keys.orEmpty()
    return when (name) {
        "Std" -> 0
        !in packageAddrs -> 1
        else -> 2
    }
}
//fun MvAddressRef.toNormalizedAddress(contextProject: MoveProject = this.moveProject!!): Address? =
//    this.toAddress(contextProject)?.normalized()
