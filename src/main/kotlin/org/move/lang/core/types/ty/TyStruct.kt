package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.tyAbilities
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.*

data class TyStruct(
    val item: MvStruct,
    val typeVars: List<TyInfer.TyVar>,
    val fieldTys: Map<String, Ty>,
    var typeArguments: List<Ty>
) : Ty {
    override fun abilities(): Set<Ability> = this.item.tyAbilities

    override fun innerFoldWith(folder: TypeFolder): Ty {
        folder.depth += 1
        return TyStruct(
            item,
            typeVars,
            fieldTys.mapValues { it.value.foldWith(folder) },
            typeArguments.map { it.foldWith(folder) }
        )
    }

    override fun toString(): String = tyToString(this)

    fun fieldTy(name: String): Ty {
        return this.fieldTys[name] ?: TyUnknown
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean {
        return fieldTys.any { it.value.visitWith(visitor) } || typeArguments.any { it.visitWith(visitor) }
    }

    // This method is rarely called (in comparison with folding), so we can implement it in a such inefficient way.
    override val typeParameterValues: Substitution
        get() {
            val typeSubst = item.typeParameters.withIndex().associate { (i, param) ->
                TyTypeParameter(param) to typeArguments.getOrElse(i) { TyUnknown }
            }
//            val regionSubst = item.lifetimeParameters.withIndex().associate { (i, param) ->
//                ReEarlyBound(param) to regionArguments.getOrElse(i) { ReUnknown }
//            }
//            val constSubst = item.constParameters.withIndex().associate { (i, param) ->
//                CtConstParameter(param) to constArguments.getOrElse(i) { CtUnknown }
//            }
            return Substitution(typeSubst)
        }
}
