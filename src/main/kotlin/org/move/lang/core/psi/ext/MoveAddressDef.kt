package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import org.move.lang.containingMoveProject
import org.move.lang.core.psi.MoveAddressDef
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.types.Address

//val MoveAddressDef.address: Address?
//    get() =
//        addressRef?.toAddress()

//val MoveAddressDef.normalizedAddress: Address?
//    get() =
//        addressRef?.toNormalizedAddress()

fun MoveAddressDef.modules(): List<MoveModuleDef> =
    addressBlock?.childrenOfType<MoveModuleDef>().orEmpty()


abstract class MoveAddressDefMixin(node: ASTNode) : MoveElementImpl(node),
                                                    MoveAddressDef {
    override fun getPresentation(): ItemPresentation? {
//        val moveProject = this.containingFile.containingMoveProject() ?: return null
        val addressText = this.addressRef?.toAddress()?.text ?: ""
        return PresentationData(addressText, null, null, null);
    }

}
