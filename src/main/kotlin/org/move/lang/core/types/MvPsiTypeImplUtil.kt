package org.move.lang.core.types

import org.move.lang.core.psi.*
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TySchema
import org.move.lang.core.types.ty.TyTypeParameter

object MvPsiTypeImplUtil {
    fun declaredType(psi: MvTypeParameter): Ty = TyTypeParameter.named(psi)
    fun declaredType(psi: MvStruct): Ty = TyAdt.valueOf(psi)
    fun declaredType(psi: MvEnum): Ty = TyAdt.valueOf(psi)
    fun declaredType(psi: MvSchema): Ty = TySchema(psi, psi.typeParamsToTypeParamsSubst, psi.tyTypeParams)
}