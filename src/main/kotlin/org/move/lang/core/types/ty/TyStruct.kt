package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.ext.declaredTy
import org.move.lang.core.psi.ext.fieldsMap
import org.move.lang.core.psi.ext.tyAbilities
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.infer.foldTyTypeParameterWith

data class TyStruct(
    val item: MvStruct,
    val typeVars: List<TyInfer.TyVar>,
    val fieldTys: Map<String, Ty>,
    var typeArgs: List<Ty>
) : Ty {
    override fun abilities(): Set<Ability> = this.item.tyAbilities

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyStruct(
            item,
            typeVars,
            fieldTys.mapValues { folder(it.value) },
            typeArgs.map(folder)
        )
    }

    override fun toString(): String = tyToString(this)

    fun fieldsTy(): Map<String, Ty> {
        return this.item.fieldsMap.mapValues { (_, field) -> field.declaredTy(false) }
    }

    fun fieldTy(name: String, msl: Boolean): Ty {
        val field = this.item.fieldsMap[name] ?: return TyUnknown
        return field.declaredTy(msl)
            .foldTyTypeParameterWith { typeParam ->
                this.typeVars.find { it.origin?.parameter == typeParam.parameter }!!
            }
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean {
        return fieldTys.any { it.value.visitWith(visitor) } || typeArgs.any { it.visitWith(visitor) }
    }
}
