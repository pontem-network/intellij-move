package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.move.lang.core.psi.*
import org.move.lang.core.types.ty.*


//fun FunctionInferenceContext.inferLiteralExprTy(literalExpr: MoveLiteralExpr): Ty {
//    return when {
//        literalExpr.boolLiteral != null -> TyBool
//        literalExpr.addressLiteral != null
//                || literalExpr.bech32AddressLiteral != null
//                || literalExpr.polkadotAddressLiteral != null -> TyAddress
//        literalExpr.integerLiteral != null || literalExpr.hexIntegerLiteral != null -> {
//            val literal = (literalExpr.integerLiteral ?: literalExpr.hexIntegerLiteral)!!
//            return TyInteger.fromSuffixedLiteral(literal) ?: TyInteger(TyInteger.DEFAULT_KIND)
//        }
//        literalExpr.byteStringLiteral != null -> TyByteString
//        else -> TyUnknown
//    }
//}

//fun FunctionInferenceContext.inferCastExprTy(castExpr: MoveCastExpr): Ty {
//    return inferMoveTypeTy(castExpr.type)
//}
//
//fun FunctionInferenceContext.inferExprType(expr: MoveExpr): Ty {
//    return when (expr) {
//        is MoveLiteralExpr -> this.inferLiteralExprTy(expr)
//        is MoveCastExpr -> this.inferCastExprTy(expr)
//        else -> TyUnknown
//    }
//}

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
        for (stmt in codeBlock.statementList) {
            fctx.extractBindingsFromStatement(stmt)
        }
        fctx.exprTypes.replaceAll { _, ty -> fctx.deepResolveTyInferFromContext(ty) }
        fctx.bindings.replaceAll { _, ty -> fctx.deepResolveTyInferFromContext(ty) }
    }
    return InferenceResult(fctx.bindings, fctx.exprTypes, emptyList())
}

class FunctionInferenceContext {
    val bindings: MutableMap<String, Ty> = HashMap()
    val exprTypes: MutableMap<MoveExpr, Ty> = HashMap()

    val constraintSolver = ConstraintSolver(this)

    val varUnificationTable = UnificationTable<TyInfer.TyVar, Ty>()

    fun extractBindingsFromStatement(stmt: MoveStatement) {
        val fctx = TypeInferenceWalker(this)
        when (stmt) {
            is MoveLetStatement -> {
                val explicitTy = stmt.typeAnnotation?.type?.let { inferMoveTypeTy(it) }
                val inferredExprTy = stmt.initializer?.expr?.let { fctx.inferExprTy(it) }
                // TODO: check inferred type is coercable (assignable) to explicitly passed type
                val pat = stmt.pat
                if (pat != null) {
                    bindings += collectBindings(
                        pat,
                        explicitTy ?: inferredExprTy ?: TyUnknown
                    )
                }
            }
            is MoveExprStatement -> fctx.inferExprTy(stmt.expr)
        }
    }

    fun resolveTyInferFromContext(ty: Ty): Ty {
        if (ty !is TyInfer) return ty
        return when (ty) {
//            is TyInfer.IntVar -> intUnificationTable.findValue(ty)?.let(::TyInteger) ?: ty
            is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(this::resolveTyInferFromContext)
                ?: ty
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
    val diagnostics: List<String>
)

val PsiElement.inference: InferenceResult?
    get() = parentOfType<MoveFunctionDef>()?.let { infer(it) }


sealed class CoerceResult {
    object Ok: CoerceResult()
    class Mismatch(val ty1: Ty, ty2: Ty): CoerceResult()

    val isOk: Boolean get() = this is Ok
}
