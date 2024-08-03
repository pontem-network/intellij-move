package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvBlockFields
import org.move.lang.core.psi.MvNamedFieldDecl
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.impl.MvMandatoryNameIdentifierOwnerImpl
import javax.swing.Icon

val MvNamedFieldDecl.fieldsDefBlock: MvBlockFields?
    get() =
        parent as? MvBlockFields

val MvNamedFieldDecl.structItem: MvStruct
    get() =
        fieldsDefBlock?.parent as MvStruct

abstract class MvNamedFieldDeclMixin(node: ASTNode) : MvMandatoryNameIdentifierOwnerImpl(node),
                                                      MvNamedFieldDecl {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT_FIELD

    override fun getPresentation(): ItemPresentation {
        val fieldType = this.typeAnnotation?.text ?: ""
        return PresentationData(
            "${this.name}${fieldType}",
            this.locationString(true),
            MoveIcons.STRUCT_FIELD,
            null
        )
    }
}