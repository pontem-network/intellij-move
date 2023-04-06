package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.tyAbilities
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.infer.UnificationTable

data class TyStruct(
    val item: MvStruct,
    val typeVars: List<TyInfer.TyVar>,
    val fieldTys: Map<String, Ty>,
    var typeArgs: List<Ty>
) : Ty {
    override fun abilities(): Set<Ability> = this.item.tyAbilities

    override fun innerFoldWith(folder: TypeFolder): Ty {
        folder.depth += 1
        return TyStruct(
            item,
            typeVars,
            fieldTys.mapValues { it.value.foldWith(folder) },
            typeArgs.map { it.foldWith(folder) }
        )
    }

    override fun toString(): String = tyToString(this)

    fun fieldTy(name: String): Ty {
        return this.fieldTys[name] ?: TyUnknown
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean {
        return fieldTys.any { it.value.visitWith(visitor) } || typeArgs.any { it.visitWith(visitor) }
    }
}
