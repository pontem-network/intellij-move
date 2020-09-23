package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAddressDef
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.types.Address

val MoveAddressDef.address: Address?
    get() =
        addressRef?.address()

fun MoveAddressDef.modules(): List<MoveModuleDef> =
    addressBlock?.childrenOfType<MoveModuleDef>().orEmpty()