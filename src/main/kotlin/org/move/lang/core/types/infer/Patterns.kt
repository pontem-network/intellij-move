package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.declaredTy
import org.move.lang.core.psi.ext.inferTypeTy
import org.move.lang.core.psi.ext.owner
import org.move.lang.core.psi.ext.providedFields
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyTuple
import org.move.lang.core.types.ty.TyUnknown

fun collectBindings(pattern: MovePat, type: Ty): Map<MoveBindingPat, Ty> {
    val bindings = mutableMapOf<MoveBindingPat, Ty>()
    fun bind(pat: MovePat, ty: Ty) {
        when (pat) {
            is MoveBindingPat -> bindings += pat to ty
            is MoveTuplePat -> {
                if (ty is TyTuple && pat.patList.size == ty.types.size) {
                    pat.patList.zip(ty.types)
                        .forEach { (pat, ty) -> bind(pat, ty) }
                } else {
                    pat.patList.map { bind(it, TyUnknown) }
                }
            }
        }
    }
    bind(pattern, type)
    return bindings
}

fun inferBindingTy(bindingPat: MoveBindingPat): Ty {
    val owner = bindingPat.owner
    return when (owner) {
        is MoveFunctionParameter -> owner.declaredTy
        is MoveConstDef -> owner.declaredTy
        is MoveLetStatement -> {
            val pat = owner.pat ?: return TyUnknown
            val explicitType = owner.typeAnnotation?.type
            if (explicitType != null) {
                val explicitTy = inferMoveTypeTy(explicitType)
                return collectBindings(pat, explicitTy)[bindingPat] ?: TyUnknown
            }

            val inference = InferenceContext()
            val inferredTy = owner.initializer?.expr?.let { inferExprTy(it, inference) } ?: TyUnknown
            return collectBindings(pat, inferredTy)[bindingPat] ?: TyUnknown
        }
        else -> TyUnknown
    }
}
