package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyTuple
import org.move.lang.core.types.ty.TyUnknown

fun collectBindings(pattern: MvPat, type: Ty): Map<MvBindingPat, Ty> {
    val bindings = mutableMapOf<MvBindingPat, Ty>()
    fun bind(pat: MvPat, ty: Ty) {
        when (pat) {
            is MvBindingPat -> bindings += pat to ty
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
    bind(pattern, type)
    return bindings
}

//fun inferBindingTy(bindingPat: MvBindingPat, msl: Boolean): Ty {
//    val owner = bindingPat.owner
//    return when (owner) {
//        is MvFunctionParameter -> owner.declaredTy(msl)
//        is MvConstDef -> owner.declaredTy(msl)
//        is MvLetStmt -> {
//            val pat = owner.pat ?: return TyUnknown
//            val explicitType = owner.typeAnnotation?.type
//            if (explicitType != null) {
//                val explicitTy = inferMvTypeTy(explicitType, msl)
//                return collectBindings(pat, explicitTy)[bindingPat] ?: TyUnknown
//            }
//
//            val inference = InferenceContext(msl = bindingPat.isMsl())
//            val inferredTy = owner.initializer?.expr?.let { inferExprTy(it, inference) } ?: TyUnknown
//            return collectBindings(pat, inferredTy)[bindingPat] ?: TyUnknown
//        }
//        else -> TyUnknown
//    }
//}
