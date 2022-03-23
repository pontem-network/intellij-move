package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.MvStructField
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

abstract class MvStructFieldMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                   MvStructField {

    override fun getIcon(flags: Int): Icon = MvIcons.STRUCT_FIELD
}
