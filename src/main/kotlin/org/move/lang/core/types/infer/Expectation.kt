package org.move.lang.core.types.infer

import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

/**
 * When type-checking an expression, we propagate downward
 * whatever type hint we are able in the form of an `org.rust.lang.core.types.infer.Expectation`
 *
 * Follows https://github.com/rust-lang/rust/blob/master/compiler/rustc_typeck/src/check/expectation.rs#L11
 */
sealed class Expectation {
    /** We know nothing about what type this expression should have */
    object NoExpectation : Expectation()

    /** This expression should have the type given (or some subtype) */
    class ExpectType(val ty: Ty) : Expectation()

    fun ty(ctx: InferenceContext): Ty? {
        return when (this) {
            is ExpectType -> ctx.resolveTypeVarsIfPossible(ty)
            else -> null
        }
    }

    companion object {
        fun fromType(ty: Ty?): Expectation {
            return if (ty == null || ty is TyUnknown) {
                NoExpectation
            } else {
                ExpectType(ty)
            }
        }
    }
}
