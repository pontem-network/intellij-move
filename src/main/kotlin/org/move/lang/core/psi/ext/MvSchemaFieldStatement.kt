package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSchemaFieldStmt
import org.move.lang.core.types.infer.ItemContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvSchemaFieldStmt.annotationTy(itemContext: ItemContext): Ty =
    this.typeAnnotation.type
        ?.let { itemContext.getTypeTy(it) } ?: TyUnknown
