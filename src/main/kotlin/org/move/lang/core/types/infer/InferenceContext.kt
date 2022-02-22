package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.acquiresPathTypes
import org.move.lang.core.psi.ext.fqName
import org.move.lang.core.psi.ext.parameters
import org.move.lang.core.psi.ext.typeParameters
import org.move.lang.core.psi.mixins.ty
import org.move.lang.core.types.ty.*

fun instantiateItemTy(item: MvNameIdentifierOwner): Ty {
    return when (item) {
        is MvStruct -> TyStruct(item)
        is MvFunction -> {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

            fun findTypeVar(parameter: MvTypeParameter): Ty {
                return typeVars.find { it.origin?.parameter == parameter }!!
            }

            val paramTypes = mutableListOf<Ty>()
            for (param in item.parameters) {
                val paramType = param.typeAnnotation?.type
                    ?.let { inferMvTypeTy(it) }
                    ?.foldTyTypeParameterWith { findTypeVar(it.parameter) } ?: TyUnknown
                paramTypes.add(paramType)
            }
            val returnMvType = item.returnType?.type
            val retTy = if (returnMvType == null) {
                TyUnit
            } else {
                inferMvTypeTy(returnMvType).foldTyTypeParameterWith { findTypeVar(it.parameter) }
            }
            val acqTys = item.acquiresPathTypes.map {
                val acqItem =
                    it.path.reference?.resolve() as? MvNameIdentifierOwner ?: return@map TyUnknown
                instantiateItemTy(acqItem)
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
            TyReference(ty1.referenced, combinedMutability)
        }
        else -> ty1
    }
}

fun isCompatible(expectedTy: Ty, inferredTy: Ty): Boolean {
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

class InferenceContext(val msl: Boolean = false) {
    val exprTypes = mutableMapOf<MvExpr, Ty>()
    val callExprTypes = mutableMapOf<MvCallExpr, TyFunction>()
    val bindingTypes = mutableMapOf<MvBindingPat, Ty>()
    val unificationTable = UnificationTable<TyInfer.TyVar, Ty>()

    private val solver = ConstraintSolver(this)

    fun registerConstraint(constraint: Constraint) {
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
