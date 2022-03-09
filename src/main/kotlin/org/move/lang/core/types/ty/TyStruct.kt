package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.foldTyTypeParameterWith

data class TyStruct(
    val item: MvStruct,
    val typeArgs: List<Ty> = emptyList()
) : Ty {
    val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

    override fun abilities(): Set<Ability> {
        return this.item.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun innerFoldWith(folder: TypeFolder): Ty = TyStruct(item, typeArgs.map(folder))

    override fun toString(): String = tyToString(this)

    fun fieldsTy(): Map<String, Ty> {
        return this.item.fieldsMap.mapValues { (_, field) -> field.declaredTy(false) }
    }

    fun fieldTy(name: String): Ty {
        val field = this.item.fieldsMap[name] ?: return TyUnknown
        return field.declaredTy(false)
            .foldTyTypeParameterWith { typeParam ->
                this.typeVars.find { it.origin?.parameter == typeParam.parameter }!!
            }
    }
}
