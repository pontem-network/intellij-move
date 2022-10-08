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

val MvAddressRef.normalizedText: String get() = this.text.lowercase()

val MvAddressRef.useGroupLevel: Int
    get() {
        // sort to the end if not a named address
        if (this.namedAddress == null) return 4

        val name = this.namedAddress?.text.orEmpty().lowercase()
        val currentPackageAddresses =
            this.moveProject?.currentPackageAddresses()?.keys.orEmpty().map { it.lowercase() }
        return when (name) {
            "std" -> 0
            "aptos_std", "aptos_framework" -> 1
            !in currentPackageAddresses -> 2
            else -> 3
        }
    }
