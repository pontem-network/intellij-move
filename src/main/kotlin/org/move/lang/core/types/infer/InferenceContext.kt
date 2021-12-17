package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.fqName
import org.move.lang.core.psi.ext.parameters
import org.move.lang.core.psi.ext.typeParameters
import org.move.lang.core.types.ty.*

fun instantiateItemTy(item: MvNameIdentifierOwner): Ty {
    return when (item) {
        is MvStruct_ -> TyStruct(item)
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
            val retType: Ty
            if (returnMvType == null) {
                retType = TyUnit
            } else {
                retType =
                    inferMvTypeTy(returnMvType).foldTyTypeParameterWith { findTypeVar(it.parameter) }
            }
            TyFunction(item, typeVars, paramTypes, retType)
        }
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
            && expectedTy.typeArguments.size == inferredTy.typeArguments.size
            && expectedTy.typeArguments.zip(inferredTy.typeArguments)
        .all { isCompatible(it.first, it.second) }
}

fun isCompatibleIntegers(expectedTy: TyInteger, inferredTy: TyInteger): Boolean {
    return expectedTy.kind == TyInteger.DEFAULT_KIND
            || inferredTy.kind == TyInteger.DEFAULT_KIND
            || expectedTy.kind == inferredTy.kind
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
        expectedTy is TyInteger && inferredTy is TyInteger -> isCompatibleIntegers(expectedTy, inferredTy)
        expectedTy is TyPrimitive && inferredTy is TyPrimitive
                && expectedTy.name == inferredTy.name -> true
        expectedTy is TyVector && inferredTy is TyVector
                && isCompatible(expectedTy.item, inferredTy.item) -> true

        expectedTy is TyReference && inferredTy is TyReference
                && isCompatibleMutability(inferredTy.mutability, expectedTy.mutability) ->
            isCompatibleReferences(expectedTy, inferredTy)

        expectedTy is TyStruct && inferredTy is TyStruct -> isCompatibleStructs(expectedTy, inferredTy)
        else -> false
    }
}

class InferenceContext {
    val exprTypes = mutableMapOf<MvExpr, Ty>()
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
