package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.tyAbilities
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.*

data class TyStruct(
    override val item: MvStruct,
    override val substitution: Substitution,
    val typeArguments: List<Ty>,
) : GenericTy(item, substitution, mergeFlags(typeArguments) or HAS_TY_STRUCT_MASK) {

    override fun abilities(): Set<Ability> = this.item.tyAbilities

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyStruct(
            item,
            substitution.foldValues(folder),
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
        return typeArguments.any { it.visitWith(visitor) } || substitution.visitValues(visitor)
    }
}
