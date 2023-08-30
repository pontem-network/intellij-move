package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import org.move.lang.core.psi.MvAddressDef
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvModule
import org.move.lang.core.types.address
import org.move.lang.moveProject

fun MvAddressDef.modules(): List<MvModule> =
    addressBlock?.childrenOfType<MvModule>().orEmpty()


abstract class MvAddressDefMixin(node: ASTNode) : MvElementImpl(node),
                                                  MvAddressDef {
    override fun getPresentation(): ItemPresentation? {
        val addressText = this.addressRef?.address(this.moveProject)?.text() ?: ""
        return PresentationData(addressText, null, null, null)
    }

}
