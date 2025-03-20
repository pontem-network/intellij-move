package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvStructOrEnumItemElement
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.types.infer.*

data class TyAdt(
    override val item: MvStructOrEnumItemElement,
    override val substitution: Substitution,
    val typeArguments: List<Ty>,
): GenericTy(item, substitution, mergeFlags(typeArguments) or HAS_TY_ADT_MASK) {

    override fun abilities(): Set<Ability> = this.item.abilities

    override fun deepFoldWith(folder: TypeFolder): Ty {
        return TyAdt(
            item,
            substitution.foldWith(folder),
            typeArguments.map { it.foldWith(folder) }
        )
    }

    override fun toString(): String = tyToString(this)

    override fun deepVisitWith(visitor: TypeVisitor): Boolean {
        return typeArguments.any { it.visitWith(visitor) } || substitution.deepVisitWith(visitor)
    }

    // This method is rarely called (in comparison with folding), so we can implement it in a such inefficient way.
    override val typeParamsToTypeArgsSubst: Substitution
        get() {
            val typeParamMapping = item.typeParameters.withIndex().associate { (i, typeParam) ->
                val tyTypeParam = TyTypeParameter.named(typeParam)
                val typeArg = typeArguments.getOrElse(i) { TyUnknown }
                tyTypeParam to typeArg
            }
            return Substitution(typeParamMapping)
        }

    companion object {
        fun valueOf(struct: MvStructOrEnumItemElement): TyAdt {
            val typeParamsSubst = struct.typeParamsSubst
            return TyAdt(
                struct,
                typeParamsSubst,
                struct.tyTypeParams
            )
        }
    }
}

val TyAdt.enumItem: MvEnum? get() = this.item as? MvEnum
val TyAdt.structItem: MvStruct? get() = this.item as? MvStruct
