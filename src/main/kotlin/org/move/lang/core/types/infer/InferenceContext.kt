package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.ty
import org.move.lang.core.types.ty.*

private val TYPE_INFERENCE_KEY: Key<CachedValue<InferenceContext>> = Key.create("TYPE_INFERENCE_KEY")

fun MvElement.inferenceCtx(msl: Boolean): InferenceContext {
    return this.containingFunctionLike?.inferenceCtx(msl) ?: InferenceContext(msl)
}

fun MvFunctionLike.inferenceCtx(msl: Boolean): InferenceContext {
    val ctx = CachedValuesManager.getCachedValue(this, TYPE_INFERENCE_KEY) {
        val fctx = InferenceContext(msl)
        for (param in this.parameterBindings) {
            fctx.bindingTypes[param] = param.inferredTy(fctx)
        }
        when (this) {
            is MvFunction -> {
                for (stmt in this.codeBlock?.stmtList.orEmpty()) {
                    when (stmt) {
                        is MvExprStmt -> {
                            inferExprTy(stmt.expr, fctx)
                        }
                        is MvLetStmt -> {
                            val initializerTy = stmt.initializer?.expr?.let { inferExprTy(it, fctx) }
                            val patTy = stmt.declaredTy ?: initializerTy ?: TyUnknown
                            val pat = stmt.pat ?: continue
                            fctx.bindingTypes.putAll(collectBindings(pat, patTy))
                        }
                    }
                }
            }
        }
        CachedValueProvider.Result(fctx, PsiModificationTracker.MODIFICATION_COUNT)
    }
    ctx.msl = msl
    return ctx
}

fun instantiateItemTy(item: MvNameIdentifierOwner, msl: Boolean): Ty {
    return when (item) {
        is MvStruct -> TyStruct(item)
        is MvFunctionLike -> {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

            fun findTypeVar(parameter: MvTypeParameter): Ty {
                return typeVars.find { it.origin?.parameter == parameter }!!
            }

            val paramTypes = mutableListOf<Ty>()
            for (param in item.parameters) {
                val paramType = param.typeAnnotation?.type
                    ?.let { inferMvTypeTy(it, msl) }
                    ?.foldTyTypeParameterWith { findTypeVar(it.parameter) } ?: TyUnknown
                paramTypes.add(paramType)
            }
            val returnMvType = item.returnType?.type
            val retTy = if (returnMvType == null) {
                TyUnit
            } else {
                inferMvTypeTy(returnMvType, msl).foldTyTypeParameterWith { findTypeVar(it.parameter) }
            }
            val acqTys = item.acquiresPathTypes.map {
                val acqItem =
                    it.path.reference?.resolve() as? MvNameIdentifierOwner ?: return@map TyUnknown
                instantiateItemTy(acqItem, msl)
                    .foldTyTypeParameterWith { tp -> findTypeVar(tp.parameter) }
            }
            TyFunction(item, typeVars, paramTypes, retTy, acqTys)
        }
        is MvTypeParameter -> item.ty()
        else -> TyUnknown
    }
}

fun isCompatibleMutability(from: Mutability, to: Mutability): Boolean =
    from == to || from.isMut && !to.isMut

fun isCompatibleReferences(expectedTy: TyReference, inferredTy: TyReference): Boolean {
    return isCompatible(expectedTy.referenced, inferredTy.referenced)
}

fun isCompatibleStructs(expectedTy: TyStruct, inferredTy: TyStruct): Boolean {
    return expectedTy.item.fqName == inferredTy.item.fqName
            && expectedTy.typeArgs.size == inferredTy.typeArgs.size
            && expectedTy.typeArgs.zip(inferredTy.typeArgs).all { isCompatible(it.first, it.second) }
}

fun isCompatibleTuples(expectedTy: TyTuple, inferredTy: TyTuple): Boolean {
    return expectedTy.types.size == inferredTy.types.size
            && expectedTy.types.zip(inferredTy.types).all { isCompatible(it.first, it.second) }
}

fun isCompatibleIntegers(expectedTy: TyInteger, inferredTy: TyInteger): Boolean {
    return expectedTy.kind == TyInteger.DEFAULT_KIND
            || inferredTy.kind == TyInteger.DEFAULT_KIND
            || expectedTy.kind == inferredTy.kind
}

/// find common denominator for both types
fun combineTys(ty1: Ty, ty2: Ty): Ty {
    if (!isCompatible(ty1, ty2) && !isCompatible(ty2, ty1)) return TyUnknown
    return when {
        ty1 is TyReference && ty2 is TyReference
                && isCompatible(ty1.referenced, ty2.referenced) -> {
            val combinedMutability = if (ty1.mutability.isMut && ty2.mutability.isMut) {
                Mutability.MUTABLE
            } else {
                Mutability.IMMUTABLE
            }
            TyReference(ty1.referenced, combinedMutability, ty1.msl || ty2.msl)
        }
        else -> ty1
    }
}

fun isCompatible(rawExpectedTy: Ty, rawInferredTy: Ty): Boolean {
    val expectedTy = rawExpectedTy.mslTy()
    val inferredTy = rawInferredTy.mslTy()
    return when {
        expectedTy is TyUnknown || inferredTy is TyUnknown -> true
        expectedTy is TyInfer.TyVar || inferredTy is TyInfer.TyVar -> {
            // check abilities
            true
        }
        expectedTy is TyTypeParameter || inferredTy is TyTypeParameter -> {
            // check abilities
            true
        }
        expectedTy is TyUnit && inferredTy is TyUnit -> true
        expectedTy is TyInteger && inferredTy is TyInteger -> isCompatibleIntegers(expectedTy, inferredTy)
        expectedTy is TyPrimitive && inferredTy is TyPrimitive
                && expectedTy.name == inferredTy.name -> true
        expectedTy is TyVector && inferredTy is TyVector
                && isCompatible(expectedTy.item, inferredTy.item) -> true

        expectedTy is TyReference && inferredTy is TyReference
                && isCompatibleMutability(inferredTy.mutability, expectedTy.mutability) ->
            isCompatibleReferences(expectedTy, inferredTy)

        expectedTy is TyStruct && inferredTy is TyStruct -> isCompatibleStructs(expectedTy, inferredTy)
        expectedTy is TyTuple && inferredTy is TyTuple -> isCompatibleTuples(expectedTy, inferredTy)
        else -> false
    }
}

class InferenceContext(var msl: Boolean) {
    var exprTypes = mutableMapOf<MvExpr, Ty>()
    var callExprTypes = mutableMapOf<MvCallExpr, TyFunction>()
    var bindingTypes = mutableMapOf<MvBindingPat, Ty>()

    val unificationTable = UnificationTable<TyInfer.TyVar, Ty>()

    private val solver = ConstraintSolver(this)

    fun registerConstraint(constraint: EqualityConstraint) {
        solver.registerConstraint(constraint)
    }

    fun processConstraints(): Boolean {
        return solver.processConstraints()
    }

    fun cacheExprTy(expr: MvExpr, ty: Ty) {
        this.exprTypes[expr] = ty
    }

    fun cacheCallExprTy(expr: MvCallExpr, ty: TyFunction) {
        this.callExprTypes[expr] = ty
    }

    @Synchronized
    fun resolveTyVarsFromContext(ctx: InferenceContext) {
        this.exprTypes = ctx.resolveTyMap(this.exprTypes)
        this.bindingTypes = ctx.resolveTyMap(this.bindingTypes)
    }

    fun <K> resolveTyMap(map: Map<K, Ty>): MutableMap<K, Ty> {
        val resolvedTyMap = mutableMapOf<K, Ty>()
        for ((expr, ty) in map.entries) {
            resolvedTyMap[expr] = this.resolveTy(ty)
        }
        return resolvedTyMap
    }

    fun resolveTy(ty: Ty): Ty {
        return ty.foldTyInferWith(this::resolveTyInferFromContext)
    }

    fun resolveTyInferFromContext(ty: Ty): Ty {
        if (ty !is TyInfer) return ty
        return when (ty) {
            is TyInfer.TyVar -> unificationTable.findValue(ty)?.let(this::resolveTyInferFromContext) ?: ty
        }
    }


}
