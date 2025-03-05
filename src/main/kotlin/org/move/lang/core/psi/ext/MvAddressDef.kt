package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import org.move.ide.presentation.locationString
import org.move.lang.core.psi.MvAddressDef
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvModule
import org.move.lang.core.types.refAddress

fun MvAddressDef.modules(): List<MvModule> =
    addressBlock?.childrenOfType<MvModule>().orEmpty()


abstract class MvAddressDefMixin(node: ASTNode): MvElementImpl(node),
                                                 MvAddressDef {

    override fun getPresentation(): ItemPresentation? {
        val addressText = this.addressRef?.refAddress()?.identifierText() ?: ""
        return PresentationData(
            addressText,
            this.locationString(tryRelative = true),
            null,
            null
        )
    }

}
