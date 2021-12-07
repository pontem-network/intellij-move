package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStructDef
import org.move.lang.core.psi.MvStructFieldDef
import org.move.lang.core.psi.MvStructFieldsDefBlock
import org.move.lang.core.types.infer.inferMvTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvStructFieldDef.fieldsDefBlock: MvStructFieldsDefBlock?
    get() =
        parent as? MvStructFieldsDefBlock

val MvStructFieldDef.structDef: MvStructDef?
    get() =
        fieldsDefBlock?.parent as? MvStructDef

val MvStructFieldDef.declaredTy: Ty
    get() = this.typeAnnotation?.type?.let { inferMvTypeTy(it) } ?: TyUnknown
