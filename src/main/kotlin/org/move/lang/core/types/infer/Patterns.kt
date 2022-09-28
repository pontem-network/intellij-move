package org.move.lang.core.types.infer

import org.move.ide.presentation.fullname
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

fun inferPatTy(pat: MvPat, parentCtx: InferenceContext, expectedTy: Ty? = null): Ty {
    val existingTy = parentCtx.patTypes[pat]
    if (existingTy != null) {
        return existingTy
    }
    val patTy = when (pat) {
        is MvStructPat -> inferStructPatTy(pat, parentCtx, expectedTy)
        is MvTuplePat -> {
            val tupleTy = TyTuple(pat.patList.map { TyUnknown })
            if (expectedTy != null) {
                if (expectedTy is TyTuple && isCompatible(expectedTy, tupleTy)) {
                    val itemTys = pat.patList.mapIndexed { i, itemPat ->
                        inferPatTy(
                            itemPat,
                            parentCtx,
                            expectedTy.types[i]
                        )
                    }
                    return TyTuple(itemTys)
                } else {
                    parentCtx.typeErrors.add(TypeError.InvalidUnpacking(pat, expectedTy))
                }
            }
            TyUnknown
        }
        is MvBindingPat -> {
            val ty = expectedTy ?: TyUnknown
            parentCtx.bindingTypes[pat] = ty
            ty
        }
        else -> TyUnknown
    }
    parentCtx.cachePatTy(pat, patTy)
    return patTy
}

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
                val patTy = inferPatTy(pat, parentCtx, ty)
                when (patTy) {
                    is TyStruct -> {
                        when {
                            ty is TyUnknown -> pat.fields.map {
                                it.pat?.let { pat -> bind(pat, TyUnknown) }
                            }
                            ty is TyStruct && pat.fields.size == ty.fieldTys.size -> {
                                for (field in pat.fields) {
                                    val fieldTy = ty.fieldTys[field.referenceName] ?: TyUnknown
                                    field.pat?.let { bind(it, fieldTy) }
                                }
                            }
                        }
                    }
                    is TyUnknown -> {
                        pat.fields.map {
                            it.pat?.let { pat -> bind(pat, TyUnknown) }
                        }
                    }
                    else -> error("unreachable with type ${patTy.fullname()}")
                }
                if (ty is TyStruct && pat.fields.size == ty.fieldTys.size) {
                    for (field in pat.fields) {
                        val fieldTy = ty.fieldTys[field.referenceName] ?: TyUnknown
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
