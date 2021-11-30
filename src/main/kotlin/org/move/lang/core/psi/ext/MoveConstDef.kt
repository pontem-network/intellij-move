package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveConstDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferMoveTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MoveConstDef.declaredTy: Ty
    get() = this.typeAnnotation?.type?.let { inferMoveTypeTy(it) } ?: TyUnknown

abstract class MoveConstDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                  MoveConstDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.CONST

    override fun resolvedType(): Ty {
        return this.typeAnnotation?.type?.inferTypeTy() ?: TyUnknown
    }
}
