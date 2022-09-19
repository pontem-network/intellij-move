package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.rd.util.concurrentMapOf
import org.move.ide.presentation.expectedBindingFormText
import org.move.ide.presentation.name
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.ty
import org.move.lang.core.types.ty.*

interface MvInferenceContextOwner : MvElement {
    fun parameterBindings(): List<MvBindingPat>
}

fun MvInferenceContextOwner.inferenceCtx(msl: Boolean): InferenceContext {
    return if (msl) {
        getProjectPsiDependentCache(this) {
            getOwnerInferenceContext(it, true)
        }
    } else {
        getProjectPsiDependentCache(this) {
            getOwnerInferenceContext(it, false)
        }
    }
}

fun MvElement.ownerInferenceCtx(msl: Boolean = this.isMsl()): InferenceContext {
    val inferenceOwner =
        PsiTreeUtil.getParentOfType(this, MvInferenceContextOwner::class.java, false)
    return inferenceOwner?.inferenceCtx(msl) ?: InferenceContext(msl)
}

private fun getOwnerInferenceContext(owner: MvInferenceContextOwner, msl: Boolean): InferenceContext {
    val inferenceCtx = InferenceContext(msl)
    for (param in owner.parameterBindings()) {
        inferenceCtx.bindingTypes[param] = param.inferredTy(inferenceCtx)
    }
    when (owner) {
        is MvFunction -> {
            owner.codeBlock?.let {
                inferCodeBlockTy(it, inferenceCtx, owner.returnTypeTy(inferenceCtx))
            }
        }
        is MvItemSpec -> {
            owner.itemSpecBlock?.let { inferSpecBlockStmts(it, inferenceCtx) }
        }
    }
    return inferenceCtx
}

fun inferCodeBlockTy(block: MvCodeBlock, blockCtx: InferenceContext, expectedTy: Ty?): Ty {
    for (stmt in block.stmtList) {
        inferStmt(stmt, blockCtx)
        blockCtx.processConstraints()
        blockCtx.resolveTyVarsFromContext(blockCtx)
    }
    val tailExpr = block.expr
    if (tailExpr == null) {
        if (expectedTy != null && expectedTy !is TyUnit) {
            blockCtx.typeErrors.add(
                TypeError.TypeMismatch(
                    block.rightBrace ?: block,
                    expectedTy,
                    TyUnit
                )
            )
            return TyUnknown
        }
        return TyUnit
    } else {
        val tailExprTy = inferExprTy(tailExpr, blockCtx, expectedTy)
        blockCtx.processConstraints()
        blockCtx.resolveTyVarsFromContext(blockCtx)
        return tailExprTy
    }
}

fun inferSpecBlockStmts(block: MvItemSpecBlock, blockCtx: InferenceContext) {
    for (stmt in block.stmtList) {
        inferStmt(stmt, blockCtx)
        blockCtx.processConstraints()
        blockCtx.resolveTyVarsFromContext(blockCtx)
    }
}

fun inferStmt(stmt: MvStmt, blockCtx: InferenceContext) {
    when (stmt) {
        is MvExprStmt -> inferExprTy(stmt.expr, blockCtx)
        is MvSpecExprStmt -> inferExprTy(stmt.expr, blockCtx)
        is MvLetStmt -> {
            val explicitTy = stmt.typeAnnotation?.type?.let { inferTypeTy(it, blockCtx) }
            val initializerTy = stmt.initializer?.expr?.let { inferExprTy(it, blockCtx, explicitTy) }
            val pat = stmt.pat ?: return
            val patTy = inferPatTy(pat, blockCtx, explicitTy ?: initializerTy)
            collectBindings(pat, patTy, blockCtx)
        }
    }
}

fun instantiateItemTy(item: MvNameIdentifierOwner, inferenceCtx: InferenceContext): Ty {
    return when (item) {
        is MvStruct -> {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
            fun findTypeVar(parameter: MvTypeParameter): Ty {
                return typeVars.find { it.origin?.origin == parameter }!!
            }

            val fieldTys = mutableMapOf<String, Ty>()
            for (field in item.fields) {
                val fieldName = field.name ?: return TyUnknown
                val fieldTy = item
                    .fieldsMap[fieldName]
                    ?.declarationTypeTy(inferenceCtx)
                    ?.foldTyTypeParameterWith { findTypeVar(it.origin) }
                    ?: TyUnknown
                fieldTys[fieldName] = fieldTy
            }

            val typeArgs = item.typeParameters.map { findTypeVar(it) }
            TyStruct(item, typeVars, fieldTys, typeArgs)
        }

        is MvFunctionLike -> {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
            fun findTypeVar(parameter: MvTypeParameter): Ty {
                return typeVars.find { it.origin?.origin == parameter }!!
            }

            val paramTypes = mutableListOf<Ty>()
            for (param in item.parameters) {
                val paramType = param.typeAnnotation?.type
                    ?.let { inferTypeTy(it, inferenceCtx) }
                    ?.foldTyTypeParameterWith { findTypeVar(it.origin) } ?: TyUnknown
                paramTypes.add(paramType)
            }
            val returnMvType = item.returnType?.type
            val retTy = if (returnMvType == null) {
                TyUnit
            } else {
                inferTypeTy(returnMvType, inferenceCtx).foldTyTypeParameterWith { findTypeVar(it.origin) }
            }
            val acqTys = item.acquiresPathTypes.map {
                val acqItem =
                    it.path.reference?.resolve() as? MvNameIdentifierOwner ?: return@map TyUnknown
                instantiateItemTy(acqItem, inferenceCtx)
                    .foldTyTypeParameterWith { tp -> findTypeVar(tp.origin) }
            }
            val typeArgs = item.typeParameters.map { findTypeVar(it) }
            TyFunction(item, typeVars, paramTypes, retTy, acqTys, typeArgs)
        }

        is MvTypeParameter -> item.ty()
        else -> TyUnknown
    }
}

fun isCompatibleReferences(expectedTy: TyReference, inferredTy: TyReference, msl: Boolean): Compat {
    return checkTysCompatible(expectedTy.referenced, inferredTy.referenced, msl)
}

fun isCompatibleStructs(expectedTy: TyStruct, inferredTy: TyStruct, msl: Boolean): Compat {
    val isCompat = expectedTy.item.fqName == inferredTy.item.fqName
            && expectedTy.typeArgs.size == inferredTy.typeArgs.size
            && expectedTy.typeArgs.zip(inferredTy.typeArgs).all { isCompatible(it.first, it.second, msl) }
    return if (isCompat) {
        Compat.Yes
    } else {
        Compat.TypeMismatch(expectedTy, inferredTy)
    }
}

fun isCompatibleTuples(expectedTy: TyTuple, inferredTy: TyTuple, msl: Boolean): Compat {
    val isCompat = expectedTy.types.size == inferredTy.types.size
            && expectedTy.types.zip(inferredTy.types).all { isCompatible(it.first, it.second, msl) }
    return if (isCompat) Compat.Yes else Compat.TypeMismatch(expectedTy, inferredTy)
}

fun isCompatibleIntegers(expectedTy: TyInteger, inferredTy: TyInteger): Compat {
    val isCompat = expectedTy.kind == TyInteger.DEFAULT_KIND
            || inferredTy.kind == TyInteger.DEFAULT_KIND
            || expectedTy.kind == inferredTy.kind
    return if (isCompat) {
        Compat.Yes
    } else {
        Compat.TypeMismatch(expectedTy, inferredTy)
    }
}

/// find common denominator for both types
fun combineTys(ty1: Ty, ty2: Ty, msl: Boolean): Ty {
    if (!isCompatible(ty1, ty2, msl) && !isCompatible(ty2, ty1, msl)) return TyUnknown
    return when {
        ty1 is TyReference && ty2 is TyReference
                && isCompatible(ty1.referenced, ty2.referenced, msl) -> {
            val combined = ty1.permissions.intersect(ty2.permissions)
            TyReference(ty1.referenced, combined, ty1.msl || ty2.msl)
        }

        else -> ty1
    }
}

fun isCompatible(rawExpectedTy: Ty, rawInferredTy: Ty, msl: Boolean = true): Boolean {
    val compat = checkTysCompatible(rawExpectedTy, rawInferredTy, msl)
    return compat == Compat.Yes
}

fun checkTysCompatible(rawExpectedTy: Ty, rawInferredTy: Ty, msl: Boolean = true): Compat {
    val expectedTy = rawExpectedTy.mslTy()
    val inferredTy = rawInferredTy.mslTy()
    return when {
        expectedTy is TyNever || inferredTy is TyNever -> Compat.Yes
        expectedTy is TyUnknown || inferredTy is TyUnknown -> Compat.Yes
        expectedTy is TyInfer.TyVar && inferredTy !is TyInfer.TyVar -> {
            isCompatibleAbilities(expectedTy, inferredTy, msl)
        }
        /* expectedTy !is TyInfer.TyVar && */ inferredTy is TyInfer.TyVar -> {
            // todo: should always be false
            // todo: can it ever occur anyway?
            Compat.Yes
        }
        expectedTy is TyInfer.IntVar && (inferredTy is TyInfer.IntVar || inferredTy is TyInteger) -> {
            Compat.Yes
        }

        inferredTy is TyInfer.IntVar && expectedTy is TyInteger -> {
            Compat.Yes
        }

        expectedTy is TyTypeParameter || inferredTy is TyTypeParameter -> {
            // check abilities
            Compat.Yes
        }

        expectedTy is TyUnit && inferredTy is TyUnit -> Compat.Yes
        expectedTy is TyInteger && inferredTy is TyInteger -> isCompatibleIntegers(expectedTy, inferredTy)
        expectedTy is TyPrimitive && inferredTy is TyPrimitive
                && expectedTy.name == inferredTy.name -> Compat.Yes

        expectedTy is TyVector && inferredTy is TyVector
                && isCompatible(expectedTy.item, inferredTy.item, msl) -> Compat.Yes

        expectedTy is TyReference && inferredTy is TyReference
                // inferredTy permissions should be a superset of expectedTy permissions
                && (expectedTy.permissions - inferredTy.permissions).isEmpty() ->
            isCompatibleReferences(expectedTy, inferredTy, msl)

        expectedTy is TyStruct && inferredTy is TyStruct -> isCompatibleStructs(expectedTy, inferredTy, msl)
        expectedTy is TyTuple && inferredTy is TyTuple -> isCompatibleTuples(expectedTy, inferredTy, msl)
        else -> Compat.TypeMismatch(expectedTy, inferredTy)
    }
}

sealed class Compat {
    object Yes : Compat()
    data class AbilitiesMismatch(val abilities: Set<Ability>) : Compat()
    data class TypeMismatch(val expectedTy: Ty, val ty: Ty) : Compat()
}

fun isCompatibleAbilities(expectedTy: Ty, actualTy: Ty, msl: Boolean): Compat {
    // skip ability check for specs
    if (msl) return Compat.Yes
    val missingAbilities = expectedTy.abilities() - actualTy.abilities()
    if (missingAbilities.isNotEmpty()) {
        return Compat.AbilitiesMismatch(missingAbilities)
    } else {
        return Compat.Yes
    }
}

sealed class TypeError(open val element: PsiElement) {
    abstract fun message(): String

    data class TypeMismatch(
        override val element: PsiElement,
        val expectedTy: Ty,
        val actualTy: Ty
    ) : TypeError(element) {
        override fun message(): String {
            return when (element) {
                is MvReturnExpr -> "Invalid return type '${actualTy.name()}', expected '${expectedTy.name()}'"
                else -> "Incompatible type '${actualTy.name()}', expected '${expectedTy.name()}'"
            }
        }
    }

    data class AbilitiesMismatch(
        override val element: PsiElement,
        val elementTy: Ty,
        val missingAbilities: Set<Ability>
    ) : TypeError(element) {
        override fun message(): String {
            return "The type '${elementTy.text()}' " +
                    "does not have required ability '${missingAbilities.map { it.label() }.first()}'"
        }
    }

    data class UnsupportedBinaryOp(
        override val element: PsiElement,
        val ty: Ty,
        val op: String
    ) : TypeError(element) {
        override fun message(): String {
            return "Invalid argument to '$op': " +
                    "expected 'u8', 'u64', 'u128', but found '${ty.text()}'"
        }
    }

    data class InvalidUnpacking(
        override val element: PsiElement,
        val assignedTy: Ty,
    ) : TypeError(element) {
        override fun message(): String {
            return "Invalid unpacking. Expected ${assignedTy.expectedBindingFormText()}"
        }
    }
}

class InferenceContext(val msl: Boolean) {
    var exprTypes = concurrentMapOf<MvExpr, Ty>()
    val patTypes = mutableMapOf<MvPat, Ty>()
    var typeTypes = mutableMapOf<MvType, Ty>()

    var callExprTypes = mutableMapOf<MvCallExpr, TyFunction>()
    val bindingTypes = concurrentMapOf<MvBindingPat, Ty>()

    var typeErrors = mutableListOf<TypeError>()

    val unificationTable = UnificationTable<TyInfer.TyVar, Ty>()
    val intUnificationTable = UnificationTable<TyInfer.IntVar, Ty>()

    private val solver = ConstraintSolver(this)

    fun addConstraint(ty1: Ty, ty2: Ty) {
        solver.registerConstraint(EqualityConstraint(ty1, ty2))
    }

    fun processConstraints(): Boolean {
        return solver.processConstraints()
    }

    fun cacheExprTy(expr: MvExpr, ty: Ty) {
        this.exprTypes[expr] = ty
    }

    fun cachePatTy(pat: MvPat, ty: Ty) {
        this.patTypes[pat] = ty
    }

    fun cacheTypeTy(type: MvType, ty: Ty) {
        this.typeTypes[type] = ty
    }

    fun cacheCallExprTy(expr: MvCallExpr, ty: TyFunction) {
        this.callExprTypes[expr] = ty
    }

    fun resolveTyVarsFromContext(ctx: InferenceContext) {
        for ((expr, ty) in this.exprTypes.entries) {
            this.exprTypes[expr] = ctx.resolveTy(ty)
        }
        for ((binding, ty) in this.bindingTypes.entries) {
            this.bindingTypes[binding] = ctx.resolveTy(ty)
        }
    }

    fun resolveTy(ty: Ty): Ty {
        return ty.foldTyInferWith(this::resolveTyInfer)
    }

    fun resolveTyInfer(ty: Ty): Ty {
        if (ty !is TyInfer) return ty
        return when (ty) {
            is TyInfer.TyVar -> unificationTable.findValue(ty)?.let(this::resolveTyInfer) ?: ty
            is TyInfer.IntVar -> intUnificationTable.findValue(ty)?.let(this::resolveTyInfer) ?: ty
        }
    }

    fun childContext(): InferenceContext {
        val childContext = InferenceContext(this.msl)
        childContext.exprTypes = this.exprTypes
        childContext.typeTypes = this.typeTypes
        childContext.callExprTypes = this.callExprTypes
        childContext.typeErrors = this.typeErrors
        return childContext
    }
}
