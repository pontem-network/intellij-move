package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvBlockFields
import org.move.lang.core.psi.MvNamedFieldDecl
import org.move.lang.core.psi.impl.MvMandatoryNameIdentifierOwnerImpl
import javax.swing.Icon

val MvNamedFieldDecl.blockFields: MvBlockFields? get() = parent as? MvBlockFields

val MvNamedFieldDecl.fieldOwner: MvFieldsOwner get() = blockFields?.parent as MvFieldsOwner

abstract class MvNamedFieldDeclMixin(node: ASTNode) : MvMandatoryNameIdentifierOwnerImpl(node),
                                                      MvNamedFieldDecl {

    override fun getIcon(flags: Int): Icon = MoveIcons.FIELD
}