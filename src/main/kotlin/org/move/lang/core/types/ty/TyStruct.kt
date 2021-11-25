package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MoveStructSignature
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.psi.ext.ability
import org.move.lang.core.psi.ext.fieldsMap
import org.move.lang.core.psi.ext.structDef
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.Ability
import org.move.lang.core.types.infer.foldTyTypeParameterWith

class TyStruct(
    val item: MoveStructSignature,
    val typeArguments: List<Ty> = emptyList()
) : Ty {
    val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

    override fun abilities(): Set<Ability> {
        return this.item.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun toString(): String = tyToString(this)

    fun fieldTy(name: String): Ty {
        val field = this.item.structDef?.fieldsMap?.get(name) ?: return TyUnknown
        return field.typeAnnotation
            ?.type
            ?.resolvedType()
            ?.foldTyTypeParameterWith { typeParam ->
                this.typeVars.find { it.origin?.parameter == typeParam.parameter }!!
            } ?: TyUnknown
    }
}
