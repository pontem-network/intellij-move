package org.move.lang.core.types.infer

import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvPat
import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.psi.MvTuplePat
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psi.ext.pat
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyTuple
import org.move.lang.core.types.ty.TyUnknown

fun collectBindings(pattern: MvPat, inferredTy: Ty, parentCtx: InferenceContext) {
    fun bind(pat: MvPat, ty: Ty) {
        when (pat) {
            is MvBindingPat -> {
                parentCtx.bindingTypes[pat] = ty
            }
            is MvTuplePat -> {
                if (ty is TyTuple && pat.patList.size == ty.types.size) {
                    pat.patList.zip(ty.types)
                        .forEach { (pat, ty) -> bind(pat, ty) }
                } else {
                    pat.patList.map { bind(it, TyUnknown) }
                }
            }
            is MvStructPat -> {
                if (ty is TyStruct && pat.fields.size == ty.fieldsTy().size) {
                    val fieldsTy = ty.fieldsTy()
                    for (field in pat.fields) {
                        val fieldTy = fieldsTy[field.referenceName] ?: TyUnknown
                        field.pat?.let { bind(it, fieldTy) }
                    }
                } else {
                    pat.fields.map {
                        it.pat?.let { pat -> bind(pat, TyUnknown) }
                    }
                }
            }
        }
    }
    bind(pattern, inferredTy)
}
