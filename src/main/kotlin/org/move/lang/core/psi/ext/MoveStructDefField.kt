package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveStructFieldDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

abstract class MoveStructFieldDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                        MoveStructFieldDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT_FIELD

    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        return this.typeAnnotation?.type?.resolvedType(emptyMap()) ?: TyUnknown
    }
}
