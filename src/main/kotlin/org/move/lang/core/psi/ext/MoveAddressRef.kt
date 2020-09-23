package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAddressRef
import org.move.lang.core.types.Address

fun MoveAddressRef.address(): Address? {
    val addressLit = addressLiteral?.text ?: bech32AddressLiteral?.text ?: return null
    return Address(addressLit)
}
