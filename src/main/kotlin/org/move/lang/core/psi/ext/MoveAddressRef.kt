package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAddressRef
import org.move.lang.core.types.Address

fun MoveAddressRef.address(): Address? {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        return namedAddress.addressValue?.let { Address(it) }
    }
    val addressLit = addressIdent?.text ?: bech32AddressIdent?.text ?: return null
    return Address(addressLit)
}

fun MoveAddressRef.normalizedAddress(): Address? = this.address()?.normalized()
