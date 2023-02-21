package org.move.lang.core.types.infer

import org.move.ide.presentation.fullname
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.annotationTy
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psi.ext.owner
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
                if (isCompatible(expectedTy, tupleTy)) {
                    if (expectedTy is TyTuple) {
                        val itemTys = pat.patList.mapIndexed { i, itemPat ->
                            inferPatTy(
                                itemPat,
                                parentCtx,
                                expectedTy.types[i]
                            )
                        }
                        return TyTuple(itemTys)
                    }
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

fun inferBindingPatTy(bindingPat: MvBindingPat, parentCtx: InferenceContext, itemContext: ItemContext): Ty {
//    val existingTy = parentCtx.bindingTypes[this]
//    if (existingTy != null) {
//        return existingTy
//    }
    val owner = bindingPat.owner
    return when (owner) {
        is MvFunctionParameter -> {
            owner.typeAnnotation
                ?.type
                ?.let { parentCtx.getTypeTy(it) }
                ?: TyUnknown
        }
        is MvLetStmt -> {
            val pat = owner.pat ?: return TyUnknown
            val explicitType = owner.typeAnnotation?.type
            if (explicitType != null) {
                val explicitTy = parentCtx.getTypeTy(explicitType)
                collectBindings(pat, explicitTy, parentCtx)
                return parentCtx.bindingTypes[bindingPat] ?: TyUnknown
            }
            val inferredTy = owner.initializer?.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
            collectBindings(pat, inferredTy, parentCtx)
            return parentCtx.bindingTypes[bindingPat] ?: TyUnknown
        }
        is MvSchemaFieldStmt -> owner.annotationTy(parentCtx)
        else -> TyUnknown
    }
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
