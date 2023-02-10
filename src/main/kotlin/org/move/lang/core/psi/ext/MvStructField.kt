package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructBlock
import org.move.lang.core.psi.MvStructField
import org.move.lang.core.types.infer.ItemContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvStructField.fieldsDefBlock: MvStructBlock?
    get() =
        parent as? MvStructBlock

val MvStructField.struct: MvStruct
    get() =
        fieldsDefBlock?.parent as MvStruct

fun MvStructField.fieldAnnotationTy(itemContext: ItemContext): Ty =
    this.typeAnnotation
        ?.type
        ?.let { itemContext.getTypeTy(it) } ?: TyUnknown
