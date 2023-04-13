package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.tyAbilities
import org.move.lang.core.psi.generics
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.infer.mergeFlags

data class TyStruct2(
    val item: MvStruct,
    val typeArguments: List<Ty>,
) : Ty(mergeFlags(typeArguments)) {

    override fun abilities(): Set<Ability> = this.item.tyAbilities

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyStruct2(
            item,
            typeArguments.map { it.foldWith(folder) }
        )
    }

    override fun toString(): String = tyToString(this)

    // This method is rarely called (in comparison with folding), so we can implement it in a such inefficient way.
    override val typeParameterValues: Substitution
        get() {
            val typeSubst = item.typeParameters.withIndex().associate { (i, param) ->
                TyTypeParameter(param) to typeArguments.getOrElse(i) { TyUnknown }
            }
            return Substitution(typeSubst)
        }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean {
        return typeArguments.any { it.visitWith(visitor) }
    }

    companion object {
        fun valueOf(item: MvStruct): TyStruct2 = TyStruct2(item, item.generics)
    }
}
