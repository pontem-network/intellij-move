package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun MvFunctionParameter.declarationTypeTy(inferenceCtx: InferenceContext): Ty =
    this.typeAnnotation?.type
        ?.let { inferTypeTy(it, inferenceCtx) } ?: TyUnknown

val MvFunctionParameter.functionLike get() = this.parent.parent as? MvFunctionLike
