package org.move.lang.core.types.infer

import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.core.types.ty.hasTyInfer

sealed class ResolverFallback {
    abstract fun fallback(tyInfer: TyInfer): Ty

    object Unknown: ResolverFallback() {
        override fun fallback(tyInfer: TyInfer): Ty = TyUnknown
    }

    object Origin: ResolverFallback() {
        override fun fallback(tyInfer: TyInfer): Ty {
            return when (tyInfer) {
                // replace TyVar with the origin TyTypeParameter
                is TyInfer.TyVar -> tyInfer.origin ?: TyUnknown
                // NOTE: should never happen, as at this point they all either resolved or
                // fallen back to TyInteger.DEFAULT
                is TyInfer.IntVar -> TyUnknown
            }
        }
    }
}

class FullTypeResolver(val ctx: InferenceContext, val fallback: ResolverFallback): TypeFolder() {
    override fun fold(ty: Ty): Ty {
        if (!ty.hasTyInfer) return ty
        // try to resolve TyInfer shallow
        val resTy = if (ty is TyInfer) ctx.resolveTyInfer(ty) else ty
        if (resTy is TyUnknown) {
            return TyUnknown
        }
        if (resTy is TyInfer) {
            return fallback.fallback(resTy)
        }
        return resTy.deepFoldWith(this)
    }
}
