package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.move.lang.core.psi.*
import org.move.lang.core.types.ty.*
import org.move.lang.utils.MoveDiagnostic

fun infer(fn: MoveFunctionDef): InferenceResult {
    val fctx = FunctionInferenceContext()

    val params = fn.functionSignature?.parameters.orEmpty()
    for (param in params) {
        val paramTy = param.typeAnnotation?.type?.let { inferMoveTypeTy(it) } ?: TyUnknown
        val paramName = param.name ?: continue
        fctx.bindings[paramName] = paramTy
    }
    val codeBlock = fn.codeBlock
    if (codeBlock != null) {
        fctx.inferExprsAndCollectBindings(codeBlock)

        fctx.exprTypes.replaceAll { _, ty -> fctx.deepResolveTyInferFromContext(ty) }
        fctx.bindings.replaceAll { _, ty -> fctx.deepResolveTyInferFromContext(ty) }
    }
//    fctx.diagnostics.replaceAll { _, diag -> fctx.deepResolveTyInferFromContext(diag) }
    return InferenceResult(fctx.bindings, fctx.exprTypes, fctx.diagnostics)
}

class FunctionInferenceContext {
    val bindings: MutableMap<String, Ty> = HashMap()
    val exprTypes: MutableMap<MoveExpr, Ty> = HashMap()
    val diagnostics: MutableList<MoveDiagnostic> = mutableListOf()

    val varUnificationTable = UnificationTable<TyInfer.TyVar, Ty>()

    val constraintSolver = ConstraintSolver(InferenceContext())

    fun inferExprsAndCollectBindings(codeBlock: MoveCodeBlock) {
        val walker = TypeInferenceWalker(this)
        for (stmt in codeBlock.statementList) {
            when (stmt) {
                is MoveLetStatement -> {
                    val explicitTy = stmt.typeAnnotation?.type?.let { inferMoveTypeTy(it) }
                    val expr = stmt.initializer?.expr

                    val coercedInferredTy =
                        if (expr != null) {
                            val inferredTy = walker.inferExprTy(expr)
                            val coercedTy =
                                when {
                                    explicitTy != null
                                            && walker.isCoerceableTypes(expr, inferredTy, explicitTy) -> explicitTy
                                    else -> inferredTy
                                }
                            coercedTy
                        } else {
                            TyInfer.TyVar()
                        }

//                    val inferredExprTy = stmt.initializer?.expr?.let { walker.inferExprTy(it) }

                    // TODO: check inferred type is coercable (assignable) to explicitly passed type
//                    val pat = stmt.pat
//                    if (pat != null) {
//                        bindings += collectBindings(
//                            pat,
//                            explicitTy ?: coercedInferredTy ?: TyUnknown
//                        )
//                    }
                }
                is MoveExprStatement -> walker.inferExprTy(stmt.expr)
            }
        }
        codeBlock.expr?.let { walker.inferExprTy(it) }
    }

    fun resolveTyInferFromContext(ty: Ty): Ty {
        if (ty !is TyInfer) return ty
        return when (ty) {
//            is TyInfer.IntVar -> intUnificationTable.findValue(ty)?.let(::TyInteger) ?: ty
            is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(this::resolveTyInferFromContext)
                ?: ty
        }
    }

    fun tryCoerceTypes(ty1: Ty, ty2: Ty): CoerceResult {
        val resolvedTy1 = resolveTyInferFromContext(ty1)
        val resolvedTy2 = resolveTyInferFromContext(ty2)
        return tryCoerceTypesResolved(resolvedTy1, resolvedTy2)
    }

    fun tryCoerceTypesResolved(ty1: Ty, ty2: Ty): CoerceResult {
        return when {
            ty1 is TyInfer.TyVar -> unifyTyVarCoerceOk(ty1, ty2)
            ty2 is TyInfer.TyVar -> unifyTyVarCoerceOk(ty2, ty1)
            else -> tryCoerceNoVars(ty1, ty2)
        }
    }

    private fun unifyTyVarCoerceOk(ty1: TyInfer.TyVar, ty2: Ty): CoerceResult {
        when (ty2) {
            is TyInfer.TyVar -> varUnificationTable.unifyVarVar(ty1, ty2)
            else -> {
                val ty1r = varUnificationTable.findRoot(ty1)
                // TODO: add isTy2ContainsTy1 check
                varUnificationTable.unifyVarValue(ty1r, ty2)
            }
        }
        return CoerceResult.Ok
    }

    fun tryCoerceNoVars(ty1: Ty, ty2: Ty): CoerceResult {
        return when {
            ty1 === ty2 -> CoerceResult.Ok
            ty1 is TyPrimitive && ty2 is TyPrimitive && ty1 == ty2 -> CoerceResult.Ok
            ty1 is TyTypeParameter && ty2 is TyTypeParameter && ty1 == ty2 -> CoerceResult.Ok
            else -> CoerceResult.Mismatch(ty1, ty2)
        }
    }

    fun <T : TypeFoldable<T>> foldResolvingTyInfersFromCurrentContext(ty: T): T {
        return ty.foldTyInferWith(this::resolveTyInferFromContext)
    }

    fun deepResolveTyInferFromContext(ty: Ty): Ty {
        fun resolveVar(ty: Ty): Ty {
            if (ty !is TyInfer) return ty
            return when (ty) {
//                is TyInfer.IntVar -> TyInteger(intUnificationTable.findValue(ty) ?: TyInteger.DEFAULT_KIND)
                is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(::resolveVar) ?: ty.origin
                ?: TyUnknown
            }
        }
        return ty.foldTyInferWith(::resolveVar)
    }

    fun unifyTyVarsResolved(ty1: Ty, ty2: Ty) {
        when {
            ty1 is TyInfer.TyVar -> unifyTyVar(ty1, ty2)
            ty2 is TyInfer.TyVar -> unifyTyVar(ty2, ty1)
        }
    }

    fun unifyTyVar(tyVar: TyInfer.TyVar, ty: Ty) {
        when (ty) {
            is TyInfer.TyVar -> varUnificationTable.unifyVarVar(tyVar, ty)
            else -> varUnificationTable.unifyVarValue(tyVar, ty)
        }
    }

}

class InferenceResult(
    val bindings: Map<String, Ty>,
    val exprTypes: Map<MoveExpr, Ty>,
    val diagnostics: List<MoveDiagnostic>
)

//val PsiElement.inference: InferenceResult?
//    get() = parentOfType<MoveFunctionDef>(true)?.let { infer(it) }


sealed class CoerceResult {
    object Ok : CoerceResult()
    class Mismatch(val ty1: Ty, ty2: Ty) : CoerceResult()

    val isOk: Boolean get() = this is Ok
}
