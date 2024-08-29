package org.move.lang.core.types

import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.generics
import org.move.lang.core.psi.tyTypeParams
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TySchema
import org.move.lang.core.types.ty.TyTypeParameter

object MvPsiTypeImplUtil {
    fun declaredType(psi: MvTypeParameter): Ty = TyTypeParameter.named(psi)
    fun declaredType(psi: MvStruct): Ty = TyAdt.valueOf(psi)
    fun declaredType(psi: MvEnum): Ty = TyAdt.valueOf(psi)
    fun declaredType(psi: MvSchema): Ty = TySchema(psi, psi.tyTypeParams, psi.generics)
}