package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.ADDRESS_IDENT
import org.move.lang.core.psi.MoveAddressRef
import org.move.lang.core.types.Address

//val MoveAddressRef.addressIdent: PsiElement? get() = this.findFirstChildByType(ADDRESS_IDENT)

fun MoveAddressRef.address(): Address? {
    val addressLit = addressIdent?.text ?: bech32AddressIdent?.text ?: return null
    return Address(addressLit)
}

fun MoveAddressRef.normalizedAddress(): Address? {
    val addressLit = addressIdent?.text ?: bech32AddressIdent?.text ?: return null
    return Address(addressLit).normalized()
}
