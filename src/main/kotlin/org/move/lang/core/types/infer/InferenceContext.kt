package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.rd.util.concurrentMapOf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.contextOrSelf
import org.move.lang.core.psi.ext.funcItem
import org.move.lang.core.psi.ext.itemSpecBlock
import org.move.lang.core.psi.ext.rightBrace
import org.move.lang.core.types.ty.*
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.cacheResult

interface MvInferenceContextOwner : MvElement

private val INFERENCE_KEY_NON_MSL: Key<CachedValue<InferenceContext>> = Key.create("INFERENCE_KEY_NON_MSL")
private val INFERENCE_KEY_MSL: Key<CachedValue<InferenceContext>> = Key.create("INFERENCE_KEY_MSL")

fun MvElement.maybeInferenceContext(msl: Boolean): InferenceContext? {
    val inferenceOwner =
        PsiTreeUtil.getParentOfType(this, MvInferenceContextOwner::class.java, false)
            ?: return null
    return if (msl) {
        project.cacheManager.cache(inferenceOwner, INFERENCE_KEY_MSL) {
            val localModificationTracker =
                inferenceOwner.contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
            val cacheDependencies: List<Any> =
                listOfNotNull(
                    inferenceOwner.project.moveStructureModificationTracker,
                    localModificationTracker
                )
            inferenceOwner.cacheResult(
                getOwnerInferenceContext(inferenceOwner, true), cacheDependencies
            )
        }
    } else {
        project.cacheManager.cache(inferenceOwner, INFERENCE_KEY_NON_MSL) {
            val localModificationTracker =
                inferenceOwner.contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
            val cacheDependencies: List<Any> =
                listOfNotNull(
                    inferenceOwner.project.moveStructureModificationTracker,
                    localModificationTracker
                )
            inferenceOwner.cacheResult(
                getOwnerInferenceContext(inferenceOwner, false), cacheDependencies
            )
        }
    }
}

fun MvElement.inferenceContext(msl: Boolean): InferenceContext {
    val ctx = this.maybeInferenceContext(msl)
    // NOTE: use default case just for the safety, should never happen in the correct code
    return ctx ?: InferenceContext(msl, project.itemContext(msl))
}

private fun getOwnerInferenceContext(owner: MvInferenceContextOwner, msl: Boolean): InferenceContext {
    val itemContext = owner.itemContextOwner?.itemContext(msl) ?: owner.project.itemContext(msl)
    val inferenceCtx = InferenceContext(msl, itemContext)

    val params = when (owner) {
        is MvFunction -> owner.parameters
        is MvItemSpec -> owner.funcItem?.parameters.orEmpty()
        else -> emptyList()
    }
    for (param in params) {
        val binding = param.bindingPat
        inferenceCtx.bindingTypes[binding] = inferBindingPatTy(binding, inferenceCtx)
    }
    when (owner) {
        is MvFunction -> {
            owner.codeBlock?.let {
                val retTy = (itemContext.getItemTy(owner) as? TyFunction)?.retType
                inferCodeBlockTy(it, inferenceCtx, retTy)
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
            val explicitTy = stmt.typeAnnotation?.type?.let { blockCtx.getTypeTy(it) }
            val initializerTy = stmt.initializer?.expr?.let { inferExprTy(it, blockCtx, explicitTy) }
            val pat = stmt.pat ?: return
            val patTy = inferPatTy(pat, blockCtx, explicitTy ?: initializerTy)
            collectBindings(pat, patTy, blockCtx)
        }
    }
}

fun isCompatibleReferences(expectedTy: TyReference, inferredTy: TyReference, msl: Boolean): Compat {
    return checkTysCompatible(expectedTy.referenced, inferredTy.referenced, msl)
}

fun isCompatibleStructs(expectedTy: TyStruct, inferredTy: TyStruct, msl: Boolean): Compat {
    val isCompat = expectedTy.item.qualName == inferredTy.item.qualName
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

fun isCompatibleIntegers(expectedTy: TyInteger, inferredTy: TyInteger): Boolean {
    val isCompat = expectedTy.kind == TyInteger.DEFAULT_KIND
            || inferredTy.kind == TyInteger.DEFAULT_KIND
            || expectedTy.kind == inferredTy.kind
    return isCompat
//    return if (isCompat) {
//        Compat.Yes
//    } else {
//        Compat.TypeMismatch(expectedTy, inferredTy)
//    }
}

/// find common denominator for both types
fun combineTys(ty1: Ty, ty2: Ty, msl: Boolean): Ty {
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

fun checkTysCompatible(rawExpectedTy: Ty, rawInferredTy: Ty, msl: Boolean): Compat {
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
            if (expectedTy != inferredTy) {
                Compat.TypeMismatch(expectedTy, inferredTy)
            } else {
                Compat.Yes
            }
        }

        expectedTy is TyUnit && inferredTy is TyUnit -> Compat.Yes
        expectedTy is TyInteger && inferredTy is TyInteger -> {
            val compat = isCompatibleIntegers(expectedTy, inferredTy)
            if (!compat) {
                Compat.TypeMismatch(expectedTy, inferredTy)
            } else {
                Compat.Yes
            }
        }
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

typealias RelateResult = RsResult<Unit, CombineTypeError>

private inline fun RelateResult.and(rhs: () -> RelateResult): RelateResult = if (isOk) rhs() else this

sealed class CombineTypeError {
    class TypeMismatch(val ty1: Ty, val ty2: Ty) : CombineTypeError()
    class AbilitiesMismatch(val abilities: Set<Ability>) : CombineTypeError()
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

class InferenceContext(val msl: Boolean, val itemContext: ItemContext) {
    var exprTypes = concurrentMapOf<MvExpr, Ty>()
    val patTypes = mutableMapOf<MvPat, Ty>()
    val typeTypes = mutableMapOf<MvType, Ty>()

    var callExprTypes = mutableMapOf<MvCallExpr, TyFunction>()
    val bindingTypes = concurrentMapOf<MvBindingPat, Ty>()

    var typeErrors = mutableListOf<TypeError>()

    val varUnificationTable = UnificationTable<TyInfer.TyVar, Ty>()
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

    fun cacheCallExprTy(expr: MvCallExpr, ty: TyFunction) {
        this.callExprTypes[expr] = ty
    }

    fun getTypeTy(type: MvType): Ty {
        val existing = this.typeTypes[type]
        if (existing != null) {
            return existing
        } else {
            val ty = inferItemTypeTy(type, itemContext)
            this.typeTypes[type] = ty
            return ty
        }
    }

    fun combineTypes(ty1: Ty, ty2: Ty): RelateResult {
        return combineTypesResolved(shallowResolve(ty1), shallowResolve(ty2))
    }

    private fun combineTypesResolved(ty1: Ty, ty2: Ty): RelateResult {
        return when {
            ty1 is TyInfer.TyVar -> combineTyVar(ty1, ty2)
            ty2 is TyInfer.TyVar -> combineTyVar(ty2, ty1)
            else -> when {
                ty1 is TyInfer.IntVar -> combineIntVar(ty1, ty2)
                ty2 is TyInfer.IntVar -> combineIntVar(ty2, ty1)
                else -> combineTypesNoVars(ty1, ty2)
            }
        }
    }

    private fun <T : Ty> combineTypePairs(pairs: List<Pair<T, T>>): RelateResult {
        var canUnify: RelateResult = Ok(Unit)
        for ((ty1, ty2) in pairs) {
            canUnify = combineTypes(ty1, ty2).and { canUnify }
        }
        return canUnify
    }

    private fun combineTyVar(ty1: TyInfer.TyVar, ty2: Ty): RelateResult {
        val compat = isCompatibleAbilities(ty1, ty2, this.msl)
        if (compat is Compat.AbilitiesMismatch) {
            return Err(CombineTypeError.AbilitiesMismatch(compat.abilities))
        }
        when (ty2) {
            is TyInfer.TyVar -> {
                varUnificationTable.unifyVarVar(ty1, ty2)
            }
            else -> {
                val ty1r = varUnificationTable.findRoot(ty1)
                val isTy2ContainsTy1 = ty2.visitWith(object : TypeVisitor {
                    override fun invoke(ty: Ty): Boolean = when {
                        ty is TyInfer.TyVar && varUnificationTable.findRoot(ty) == ty1r -> true
                        ty.visitWith { it is TyInfer } -> ty.innerVisitWith(this)
                        else -> false
                    }
                })
                if (isTy2ContainsTy1) {
                    // "E0308 cyclic type of infinite size"
                    varUnificationTable.unifyVarValue(ty1r, TyUnknown)
                } else {
                    varUnificationTable.unifyVarValue(ty1r, ty2)
                }
            }
        }
        return Ok(Unit)
    }

    private fun combineIntVar(ty1: TyInfer, ty2: Ty): RelateResult {
        when (ty1) {
            is TyInfer.IntVar -> when (ty2) {
                is TyInfer.IntVar -> intUnificationTable.unifyVarVar(ty1, ty2)
                is TyInteger -> intUnificationTable.unifyVarValue(ty1, ty2)
                else -> return Err(CombineTypeError.TypeMismatch(ty1, ty2))
            }
            is TyInfer.TyVar -> error("unreachable")
        }
        return Ok(Unit)
    }

    fun combineTypesNoVars(ty1: Ty, ty2: Ty): RelateResult {
        val ty1msl = ty1.mslTy()
        val ty2msl = ty2.mslTy()
        return when {
            ty1 === ty2 -> Ok(Unit)
            ty1msl is TyNever || ty2msl is TyNever -> Ok(Unit)
            ty1msl is TyUnknown || ty2msl is TyUnknown -> Ok(Unit)
//            ty1msl is TyInfer.TyVar && ty2msl !is TyInfer.TyVar -> {
//                isCompatibleAbilities(ty1msl, ty2msl, msl)
//            }
//            /* expectedTy !is TyInfer.TyVar && */ ty2msl is TyInfer.TyVar -> {
//                // todo: should always be false
//                // todo: can it ever occur anyway?
//                Compat.Yes
//            }
//            ty1msl is TyInfer.IntVar && (ty2msl is TyInfer.IntVar || ty2msl is TyInteger) -> {
//                Compat.Yes
//            }

//            ty2msl is TyInfer.IntVar && ty1msl is TyInteger -> {
//                Compat.Yes
//            }

//            ty1msl is TyTypeParameter || ty2msl is TyTypeParameter -> {
//                // check abilities
//                if (ty1msl != ty2msl) {
//                    Compat.TypeMismatch(ty1msl, ty2msl)
//                } else {
//                    Compat.Yes
//                }
//            }
            ty1msl is TyTypeParameter && ty2msl is TyTypeParameter && ty1msl == ty2msl -> Ok(Unit)
            ty1msl is TyUnit && ty2msl is TyUnit -> Ok(Unit)
            ty1msl is TyInteger && ty2msl is TyInteger -> {
                val compat = isCompatibleIntegers(ty1msl, ty2msl)
                if (compat) {
                    Ok(Unit)
                } else {
                    Err(CombineTypeError.TypeMismatch(ty1msl, ty2msl))
                }
            }
            ty1msl is TyPrimitive && ty2msl is TyPrimitive && ty1msl.name == ty2msl.name -> Ok(Unit)

//            ty1msl is TyVector && ty2msl is TyVector
//                    && isCompatible(ty1msl.item, ty2msl.item, msl) -> Compat.Yes
            ty1msl is TyVector && ty2msl is TyVector -> combineTypes(ty1msl.item, ty2msl.item)

            ty1msl is TyReference && ty2msl is TyReference
                    // inferredTy permissions should be a superset of expectedTy permissions
                    && (ty1msl.permissions - ty2msl.permissions).isEmpty() ->
                combineTypes(ty1msl.referenced, ty2msl.referenced)

            ty1msl is TyStruct && ty2msl is TyStruct && ty1msl.item == ty2msl.item ->
                combineTypePairs(ty1msl.typeArgs.zip(ty2msl.typeArgs))

            ty1msl is TyTuple && ty2msl is TyTuple && ty1msl.types.size == ty2msl.types.size ->
                combineTypePairs(ty1msl.types.zip(ty2msl.types))

            else -> Err(CombineTypeError.TypeMismatch(ty1msl, ty2msl))
        }
    }

    private fun shallowResolve(ty: Ty): Ty {
        if (ty !is TyInfer) return ty

        return when (ty) {
            is TyInfer.IntVar -> intUnificationTable.findValue(ty) ?: ty
            is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(this::shallowResolve) ?: ty
        }
    }

    private fun <T : TypeFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        return ty.foldTyInferWith(this::shallowResolve)
    }

    private fun fullyResolve(ty: Ty): Ty {
        fun go(ty: Ty): Ty {
            if (ty !is TyInfer) return ty

            return when (ty) {
                is TyInfer.IntVar -> intUnificationTable.findValue(ty) ?: TyInteger.default()
                is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(::go) ?: ty.origin ?: TyUnknown
            }
        }
        return ty.foldTyInferWith(::go)
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
            is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(this::resolveTyInfer) ?: ty
            is TyInfer.IntVar -> intUnificationTable.findValue(ty)?.let(this::resolveTyInfer) ?: ty
        }
    }

    fun getBindingPatTy(pat: MvBindingPat): Ty {
        val existing = this.bindingTypes[pat]
        if (existing != null) {
            return existing
        } else {
            val ty = inferBindingPatTy(pat, this)
            bindingTypes[pat] = ty
            return ty
        }
    }

    fun childContext(): InferenceContext {
        val childContext = InferenceContext(this.msl, itemContext)
        childContext.exprTypes = this.exprTypes
        childContext.callExprTypes = this.callExprTypes
//        childContext.typeTypes = this.typeTypes
        childContext.typeErrors = this.typeErrors
        return childContext
    }

    companion object {
        fun default(msl: Boolean, element: MvElement): InferenceContext =
            InferenceContext(msl, element.itemContext(msl))
    }
}
