package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveAddressDef
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.types.Address

val MoveAddressDef.address: Address?
    get() =
        addressRef?.address()

fun MoveAddressDef.modules(): List<MoveModuleDef> =
    addressBlock?.childrenOfType<MoveModuleDef>().orEmpty()


abstract class MoveAddressDefMixin(node: ASTNode) : MoveElementImpl(node),
                                                    MoveAddressDef {

}