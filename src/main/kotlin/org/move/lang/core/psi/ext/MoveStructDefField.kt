package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvStructField
import org.move.lang.core.psi.impl.MvMandatoryNameIdentifierOwnerImpl
import org.move.lang.core.psi.type
import javax.swing.Icon

abstract class MvStructFieldMixin(node: ASTNode) : MvMandatoryNameIdentifierOwnerImpl(node),
                                                   MvStructField {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT_FIELD

    override fun getPresentation(): ItemPresentation {
        val fieldType = this.typeAnnotation?.text ?: ""
        return PresentationData(
            "${this.name}${fieldType}",
            null,
            MoveIcons.STRUCT_FIELD,
            null
        )
    }
}
