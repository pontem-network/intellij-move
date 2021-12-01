package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructFieldDef
import org.move.lang.core.psi.MoveStructFieldsDefBlock
import org.move.lang.core.types.infer.inferMoveTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MoveStructFieldDef.fieldsDefBlock: MoveStructFieldsDefBlock?
    get() =
        parent as? MoveStructFieldsDefBlock

val MoveStructFieldDef.structDef: MoveStructDef?
    get() =
        fieldsDefBlock?.parent as? MoveStructDef

val MoveStructFieldDef.declaredTy: Ty
    get() = this.typeAnnotation?.type?.let { inferMoveTypeTy(it) } ?: TyUnknown
