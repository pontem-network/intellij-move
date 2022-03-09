package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructFieldDef
import org.move.lang.core.psi.MvStructFieldsDefBlock
import org.move.lang.core.types.infer.inferMvTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvStructFieldDef.fieldsDefBlock: MvStructFieldsDefBlock?
    get() =
        parent as? MvStructFieldsDefBlock

val MvStructFieldDef.struct: MvStruct
    get() =
        fieldsDefBlock?.parent as MvStruct

fun MvStructFieldDef.declaredTy(msl: Boolean): Ty =
    this.typeAnnotation?.type?.let { inferMvTypeTy(it, msl) } ?: TyUnknown
