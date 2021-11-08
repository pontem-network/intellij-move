package org.move.lang.core.psi.ext

import org.move.lang.containingMoveProject
import org.move.lang.core.psi.MoveAddressRef
import org.move.lang.core.types.Address

fun MoveAddressRef.address(): Address? {
    val namedAddress = this.namedAddress
    if (namedAddress != null) {
        val moveProject = namedAddress.containingFile.containingMoveProject() ?: return null
        val refName = namedAddress.referenceName ?: return null
        return moveProject.getAddressValue(refName)?.let { Address(it) }
//        return namedAddress.addressValue?.let { Address(it) }
    }
    val addressLit = addressIdent?.text ?: bech32AddressIdent?.text ?: return null
    return Address(addressLit)
}

fun MoveAddressRef.normalizedAddress(): Address? = this.address()?.normalized()
