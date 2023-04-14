package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSchemaFieldStmt
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

//fun MvSchemaFieldStmt.annotationTy(inferenceCtx: InferenceContext): Ty =
//    this.typeAnnotation.type
//        ?.let { inferenceCtx.getTypeTy(it) } ?: TyUnknown
