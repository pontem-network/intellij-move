package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvStructOrEnumItemElement
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.types.infer.*

data class TyAdt(
    val adtItem: MvStructOrEnumItemElement,
    val substitution: Substitution,
    val typeArguments: List<Ty>,
): Ty(mergeFlags(substitution.valueTys) or mergeFlags(typeArguments) or HAS_TY_ADT_MASK) {

    override fun abilities(): Set<Ability> = this.adtItem.abilities

    override fun deepFoldWith(folder: TypeFolder): Ty {
        return TyAdt(
            adtItem,
            substitution.foldWith(folder),
            typeArguments.map { it.foldWith(folder) }
        )
    }

    override fun toString(): String = tyToString(this)

    override fun deepVisitWith(visitor: TypeVisitor): Boolean {
        return typeArguments.any { it.visitWith(visitor) } || substitution.deepVisitWith(visitor)
    }

//    // This method is rarely called (in comparison with folding), so we can implement it in a such inefficient way.
//    override val typeParamsToTypeArgsSubst: Substitution
//        get() {
//            return this.substitution
////            val typeParamMapping = adtItem.typeParameters.withIndex().associate { (i, typeParam) ->
////                val tyTypeParam = TyTypeParameter.named(typeParam)
////                val typeArg = typeArguments.getOrElse(i) { TyUnknown }
////                tyTypeParam to typeArg
////            }
////            return Substitution(typeParamMapping)
//        }

    companion object {
        fun valueOf(struct: MvStructOrEnumItemElement): TyAdt {
            val typeParamsSubst = struct.tyTypeParamsSubst
            return TyAdt(
                struct,
                typeParamsSubst,
                struct.tyTypeParams
            )
        }
    }
}

//val TyAdt.enumItem: MvEnum? get() = this.adtItem as? MvEnum
//val TyAdt.structItem: MvStruct? get() = this.adtItem as? MvStruct
