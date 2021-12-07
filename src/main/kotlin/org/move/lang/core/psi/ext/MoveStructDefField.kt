package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvStructFieldDef
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

abstract class MvStructFieldDefMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                        MvStructFieldDef {

    override fun getIcon(flags: Int): Icon = MvIcons.STRUCT_FIELD
}
