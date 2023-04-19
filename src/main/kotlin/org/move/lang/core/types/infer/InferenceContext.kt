package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.jetbrains.rd.util.concurrentMapOf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*
import org.move.lang.core.types.ty.TyReference.Companion.coerceMutability
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.cacheResult
import org.move.utils.recursionGuard

interface MvInferenceContextOwner : MvElement

fun isCompatibleIntegers(expectedTy: TyInteger, inferredTy: TyInteger): Boolean {
    return expectedTy.kind == TyInteger.DEFAULT_KIND
            || inferredTy.kind == TyInteger.DEFAULT_KIND
            || expectedTy.kind == inferredTy.kind
}

fun compatAbilities(expectedTy: Ty, actualTy: Ty, msl: Boolean): Boolean {
    if (msl) return true
    if (expectedTy.hasTyStruct
        || expectedTy.hasTyInfer
        || expectedTy.hasTyTypeParameters
    ) {
        return (expectedTy.abilities() - actualTy.abilities()).isEmpty()
    }
    return true
}

fun isCompatible(expectedTy: Ty, actualTy: Ty, msl: Boolean): Boolean {
    val inferenceCtx = InferenceContext(msl, unify = false)
    return inferenceCtx.combineTypes(expectedTy, actualTy).isOk
}

typealias RelateResult = RsResult<Unit, CombineTypeError>

private inline fun RelateResult.and(rhs: () -> RelateResult): RelateResult = if (isOk) rhs() else this

typealias CoerceResult = RsResult<CoerceOk, CombineTypeError>

data class CoerceOk(
    val obligations: List<Obligation> = emptyList()
)

fun RelateResult.into(): CoerceResult = map { CoerceOk() }

sealed class CombineTypeError {
    class TypeMismatch(val ty1: Ty, val ty2: Ty) : CombineTypeError()
}

data class InferenceResult(
    private val exprTypes: Map<MvExpr, Ty>,
    private val patTypes: Map<MvPat, Ty>,
    private val exprExpectedTypes: Map<MvExpr, Ty>,
    private val acquiredTypes: Map<MvCallExpr, List<Ty>>,
    private val callExprTypes: Map<MvCallExpr, Ty>,
    private val pathTypes: Map<MvPath, GenericTy>,
    val typeErrors: List<TypeError>
) {
    fun getExprType(expr: MvExpr): Ty = exprTypes[expr] ?: error("Expr `${expr.text}` is never inferred")
    fun getPatType(pat: MvPat): Ty = patTypes[pat] ?: error("Pat `${pat.text}` is never inferred")
    fun getExpectedType(expr: MvExpr): Ty = exprExpectedTypes[expr] ?: TyUnknown
    fun getAcquiredTypes(expr: MvCallExpr, outerSubst: Substitution? = null): List<Ty> {
        val acquiresTypes = acquiredTypes[expr]?.takeIf { !it.contains(TyUnknown) } ?: emptyList()
        return if (outerSubst != null) {
            acquiresTypes.map { it.substituteOrUnknown(outerSubst) }
        } else {
            acquiresTypes
        }
    }

    fun getCallExprType(expr: MvCallExpr): Ty? = callExprTypes[expr]
    fun getResolvedPathType(path: MvPath): GenericTy? = pathTypes[path]

//    fun getResolvedPath(path: MvPath): List<ResolvedPath> = resolvedPaths[path] ?: emptyList()

//    companion object {
//        @JvmStatic
//        val EMPTY: InferenceResult = InferenceResult(
//            emptyMap(),
//            emptyMap(),
////            emptyMap(),
////            emptyMap(),
//            emptyList(),
//        )
//    }
}

fun inferTypesIn(element: MvInferenceContextOwner, msl: Boolean): InferenceResult {
    val inferenceCtx = InferenceContext(msl, unify = true)
    return recursionGuard(element, { inferenceCtx.infer(element) }, memoize = false)
        ?: error("Can not run nested type inference")
}

private val NEW_INFERENCE_KEY_NON_MSL: Key<CachedValue<InferenceResult>> = Key.create("NEW_INFERENCE_KEY_NON_MSL")
private val NEW_INFERENCE_KEY_MSL: Key<CachedValue<InferenceResult>> = Key.create("NEW_INFERENCE_KEY_MSL")

fun MvInferenceContextOwner.inference(msl: Boolean): InferenceResult {
    return if (msl) {
        project.cacheManager.cache(this, NEW_INFERENCE_KEY_MSL) {
            val localModificationTracker =
                this.contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
            val cacheDependencies: List<Any> =
                listOfNotNull(
                    this.project.moveStructureModificationTracker,
                    localModificationTracker
                )
            this.cacheResult(inferTypesIn(this, true), cacheDependencies)
        }
    } else {
        project.cacheManager.cache(this, NEW_INFERENCE_KEY_NON_MSL) {
            val localModificationTracker =
                this.contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
            val cacheDependencies: List<Any> =
                listOfNotNull(
                    this.project.moveStructureModificationTracker,
                    localModificationTracker
                )
            this.cacheResult(inferTypesIn(this, false), cacheDependencies)
        }
    }
}


fun MvElement.inference(msl: Boolean): InferenceResult? {
    val contextOwner = this.ancestorOrSelf<MvInferenceContextOwner>() ?: return null
    return contextOwner.inference(msl)
}

sealed class ResolvedPath(
    val element: MvElement,
    var subst: Substitution = emptySubstitution,
    val isVisible: Boolean = true
) {
//    class AssocItem(
//        override val element: RsAbstractable,
//        val source: TraitImplSource
//    ) : ResolvedPath()

    companion object {
//        fun from(entry: ScopeEntry, context: RsElement): ResolvedPath {
//            return if (entry is AssocItemScopeEntry) {
//                AssocItem(entry.element, entry.source)
//            } else {
//                val isVisible = entry.isVisibleFrom(context.containingMod)
//                Item(entry.element, isVisible)
//            }
//        }

//        fun from(entry: AssocItemScopeEntry): ResolvedPath =
//            AssocItem(entry.element, entry.source)
    }
}

class InferenceContext(var msl: Boolean, private val unify: Boolean) {
    val exprTypes = concurrentMapOf<MvExpr, Ty>()
    val patTypes = mutableMapOf<MvPat, Ty>()

    private val exprExpectedTypes = mutableMapOf<MvExpr, Ty>()
    private val acquiredTypes = mutableMapOf<MvCallExpr, List<Ty>>()
    private val callExprTypes = mutableMapOf<MvCallExpr, Ty>()
    private val pathTypes = mutableMapOf<MvPath, GenericTy>()

    var typeErrors = mutableListOf<TypeError>()

    val varUnificationTable = UnificationTable<TyInfer.TyVar, Ty>()
    val intUnificationTable = UnificationTable<TyInfer.IntVar, Ty>()

    val fulfill = FulfillmentContext(this)

    fun startSnapshot(): Snapshot = CombinedSnapshot(
        intUnificationTable.startSnapshot(),
        varUnificationTable.startSnapshot(),
    )

    inline fun <T> probe(action: () -> T): T {
        val snapshot = startSnapshot()
        try {
            return action()
        } finally {
            snapshot.rollback()
        }
    }

    inline fun <T : Any> commitIfNotNull(action: () -> T?): T? {
        val snapshot = startSnapshot()
        val result = action()
        if (result == null) snapshot.rollback() else snapshot.commit()
        return result
    }

    fun infer(owner: MvInferenceContextOwner): InferenceResult {
        val returnTy = when (owner) {
            is MvFunctionLike -> owner.rawReturnType(msl)
            else -> TyUnknown
        }
        val inference = TypeInferenceWalker(this, returnTy)

        inference.extractParameterBindings(owner)

        when (owner) {
            is MvFunctionLike -> owner.codeBlock?.let { inference.inferFnBody(it) }
            is MvItemSpec -> {
                owner.itemSpecBlock?.let { inference.inferSpec(it) }
            }
            is MvModuleItemSpec -> owner.itemSpecBlock?.let { inference.inferSpec(it) }
            is MvSchema -> owner.specBlock?.let { inference.inferSpec(it) }
        }

        fulfill.selectWherePossible()

//        fallbackUnresolvedTypeVarsIfPossible()
//
//        fulfill.selectWherePossible()

        exprTypes.replaceAll { _, ty -> fullyResolve(ty) }
        patTypes.replaceAll { _, ty -> fullyResolve(ty) }

        exprExpectedTypes.replaceAll { _, ty -> fullyResolveWithOrigins(ty) }
        acquiredTypes.replaceAll { _, tys -> tys.map { fullyResolve(it) } }
        callExprTypes.replaceAll { _, ty -> fullyResolve(ty) }
        pathTypes.replaceAll { _, ty -> fullyResolve(ty) as GenericTy }
        typeErrors.replaceAll { err -> fullyResolve(err) }

        return InferenceResult(
            exprTypes,
            patTypes,
            exprExpectedTypes,
            acquiredTypes,
            callExprTypes,
            pathTypes,
            typeErrors
        )
    }


//    private fun fallbackUnresolvedTypeVarsIfPossible() {
//        val allTypes = exprTypes.values.asSequence() + patTypes.values.asSequence()
//        for (ty in allTypes) {
//            ty.visitInferTys { tyInfer ->
//                val rty = shallowResolve(tyInfer)
//                if (rty is TyInfer) {
//                    fallbackIfPossible(rty)
//                }
//                false
//            }
//        }
//    }

//    private fun fallbackIfPossible(ty: TyInfer) {
//        when (ty) {
//            is TyInfer.IntVar -> intUnificationTable.unifyVarValue(ty, TyInteger.default())
//            is TyInfer.TyVar -> Unit
//        }
//    }

    fun isTypeInferred(expr: MvExpr): Boolean {
        return exprTypes.containsKey(expr)
    }

    fun registerEquateObligation(ty1: Ty, ty2: Ty) {
        fulfill.registerObligation(Obligation.Equate(ty1, ty2))
    }

    fun processConstraints(): Boolean {
        return fulfill.selectUntilError()
    }

    fun writeExprTy(expr: MvExpr, ty: Ty) {
        this.exprTypes[expr] = ty
    }

    fun writePatTy(pat: MvPat, ty: Ty) {
        this.patTypes[pat] = ty
    }

    fun writeExprExpectedTy(expr: MvExpr, ty: Ty) {
        this.exprExpectedTypes[expr] = ty
    }

    fun writeAcquiredTypes(callExpr: MvCallExpr, tys: List<Ty>) {
        this.acquiredTypes[callExpr] = tys
    }

    fun writeCallExprType(callExpr: MvCallExpr, ty: Ty) {
        this.callExprTypes[callExpr] = ty
    }

    fun writePath(path: MvPath, ty: GenericTy) {
        pathTypes[path] = ty
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : GenericTy> instantiatePath(
        path: MvPath,
        genericItem: MvTypeParametersOwner
    ): Pair<T, Substitution> {
        var itemTy = TyLowering.lowerPath(path, msl)
        writePath(path, itemTy as T)

        val typeParameters = genericItem.tyInfers
        itemTy = itemTy.substitute(typeParameters) as T

        unifySubst(typeParameters, itemTy.substitution)
        return Pair(itemTy, typeParameters)
    }

    fun unifySubst(subst1: Substitution, subst2: Substitution) {
        subst1.typeSubst.forEach { (k, v1) ->
            subst2[k]?.let { v2 ->
                if (k != v1 && v1 !is TyTypeParameter && v1 !is TyUnknown) {
                    combineTypes(v2, v1)
                }
            }
        }
    }

    fun combineTypes(ty1: Ty, ty2: Ty): RelateResult {
        return combineTypesResolved(shallowResolve(ty1), shallowResolve(ty2))
    }

    @Suppress("NAME_SHADOWING")
    private fun combineTypesResolved(ty1: Ty, ty2: Ty): RelateResult {
        val ty1 = ty1.mslScopeRefined(msl)
        val ty2 = ty2.mslScopeRefined(msl)
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
        // skip unification for isCompatible check to prevent bugs
        if (!unify) return Ok(Unit)
        when (ty2) {
            is TyInfer.TyVar -> varUnificationTable.unifyVarVar(ty1, ty2)
            else -> {
                val ty1r = varUnificationTable.findRoot(ty1)
                val isTy2ContainsTy1 = ty2.visitWith(object : TypeVisitor {
                    override fun invoke(ty: Ty): Boolean = when {
                        ty is TyInfer.TyVar && varUnificationTable.findRoot(ty) == ty1r -> true
                        ty.hasTyInfer -> ty.innerVisitWith(this)
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
        // skip unification for isCompatible check to prevent bugs
        if (!unify) return Ok(Unit)
        when (ty1) {
            is TyInfer.IntVar -> when (ty2) {
                is TyInfer.IntVar -> intUnificationTable.unifyVarVar(ty1, ty2)
                is TyInteger, is TyUnknown -> intUnificationTable.unifyVarValue(ty1, ty2)
                else -> return Err(CombineTypeError.TypeMismatch(ty1, ty2))
            }
            is TyInfer.TyVar -> error("unreachable")
        }
        return Ok(Unit)
    }

    fun combineTypesNoVars(ty1: Ty, ty2: Ty): RelateResult {
        val ty1msl = ty1
        val ty2msl = ty2
        return when {
            ty1 === ty2 -> Ok(Unit)
            ty1msl is TyNever || ty2msl is TyNever -> Ok(Unit)
            ty1msl is TyUnknown || ty2msl is TyUnknown -> Ok(Unit)

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
            ty1msl is TyInteger && ty2msl is TyInteger
                    && isCompatibleIntegers(ty1msl, ty2msl) -> Ok(Unit)
            ty1msl is TyPrimitive && ty2msl is TyPrimitive && ty1msl.name == ty2msl.name -> Ok(Unit)

            ty1msl is TyVector && ty2msl is TyVector -> combineTypes(ty1msl.item, ty2msl.item)

            ty1msl is TyReference && ty2msl is TyReference
                    // inferredTy permissions should be a superset of expectedTy permissions
                    && coerceMutability(ty1msl, ty2msl) ->
                combineTypes(ty1msl.referenced, ty2msl.referenced)

            ty1msl is TyStruct2 && ty2msl is TyStruct2
                    && ty1msl.item == ty2msl.item ->
                combineTypePairs(ty1msl.typeArguments.zip(ty2msl.typeArguments))

            ty1msl is TyTuple && ty2msl is TyTuple
                    && ty1msl.types.size == ty2msl.types.size ->
                combineTypePairs(ty1msl.types.zip(ty2msl.types))

            else -> Err(CombineTypeError.TypeMismatch(ty1msl, ty2msl))
        }
    }

    fun tryCoerce(inferred: Ty, expected: Ty): CoerceResult {
        if (inferred === expected) {
            return Ok(CoerceOk())
        }
//        if (inferred is TyInfer.TyVar) {
//            return combineTypes(inferred, expected).into()
//        }
        return combineTypes(inferred, expected).into()
//        return when {
//            // Coerce references
////            inferred is TyReference && expected is TyReference
////                    && coerceMutability(inferred, expected) -> {
////                tryCoerce(inferred.referenced, expected.referenced)
////            }
//            else -> combineTypes(inferred, expected).into()
//        }
    }

//    /**
//     * Reborrows `&mut A` to `&mut B` and `&(mut) A` to `&B`.
//     * To match `A` with `B`, autoderef will be performed
//     */
//    private fun coerceReference(inferred: TyReference, expected: TyReference): CoerceResult {
//        val autoderef = lookup.coercionSequence(inferred)
//        for (derefTy in autoderef.drop(1)) {
//            // TODO proper handling of lifetimes
//            val derefTyRef = TyReference(derefTy, expected.mutability, expected.region)
//            if (combineTypesIfOk(derefTyRef, expected)) {
//                // Deref `&a` to `a` and then reborrow as `&a`. No-op. See rustc's `coerce_borrowed_pointer`
//                val isTrivialReborrow = autoderef.stepCount() == 1
//                        && inferred.mutability == expected.mutability
//                        && !expected.mutability.isMut
//
//                if (!isTrivialReborrow) {
//                    val adjustments = autoderef.steps().toAdjustments(items) +
//                            listOf(Adjustment.BorrowReference(derefTyRef))
//                    return Ok(CoerceOk(adjustments))
//                }
//                return Ok(CoerceOk())
//            }
//        }
//
//        return Err(TypeMismatch(inferred, expected))
//    }

//    private fun coerceMutability(from: Mutability, to: Mutability): Boolean =
//        from == to || from.isMut && !to.isMut

    fun shallowResolve(ty: Ty): Ty {
        if (ty !is TyInfer) return ty

        return when (ty) {
            is TyInfer.IntVar -> intUnificationTable.findValue(ty) ?: ty
            is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(this::shallowResolve) ?: ty
        }
    }

    fun <T : TypeFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        return ty.foldTyInferWith(this::shallowResolve)
    }

    private fun <T : TypeFoldable<T>> fullyResolve(value: T): T {
        fun go(ty: Ty): Ty {
            if (ty !is TyInfer) return ty

            return when (ty) {
                is TyInfer.IntVar -> intUnificationTable.findValue(ty) ?: TyInteger.default()
                is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(::go) ?: ty.origin ?: TyUnknown
            }
        }
        return value.foldTyInferWith(::go)
    }

    /**
     * Similar to [fullyResolve], but replaces unresolved [TyInfer.TyVar] to its [TyInfer.TyVar.origin]
     * instead of [TyUnknown]
     */
    fun <T : TypeFoldable<T>> fullyResolveWithOrigins(value: T): T {
        return value.foldWith(fullTypeWithOriginsResolver)
    }

    private inner class FullTypeWithOriginsResolver : TypeFolder() {
        override fun fold(ty: Ty): Ty {
            if (!ty.hasTyInfer) return ty
            return when (val res = shallowResolve(ty)) {
                is TyUnknown -> (ty as? TyInfer.TyVar)?.origin ?: TyUnknown
                is TyInfer.TyVar -> res.origin ?: TyUnknown
                is TyInfer -> TyUnknown
                else -> res.innerFoldWith(this)
            }
        }
    }

    private val fullTypeWithOriginsResolver: FullTypeWithOriginsResolver = FullTypeWithOriginsResolver()

    fun reportTypeError(typeError: TypeError) {
        val element = typeError.element
        if (typeErrors.all { !element.isAncestorOf(it.element) }) {
            addTypeError(typeError)
        }
    }

    fun addTypeError(typeError: TypeError) {
        if (typeError.element.containingFile.isPhysical) {
            typeErrors.add(typeError)
        }
    }

//    fun resolveTyVarsFromContext(ctx: InferenceContext) {
//        for ((expr, ty) in this.exprTypes.entries) {
//            this.exprTypes[expr] = ctx.resolveTy(ty)
//        }
//        for ((binding, ty) in this.bindingTypes.entries) {
//            this.bindingTypes[binding] = ctx.resolveTy(ty)
//        }
//    }

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

    fun getPatType(pat: MvPat): Ty = patTypes[pat] ?: error("No pat in context")

//    fun getBindingPatTy(pat: MvBindingPat): Ty {
//        val existing = this.bindingTypes[pat]
//        if (existing != null) {
//            return existing
//        } else {
//            val ty = inferBindingPatTy(pat, this)
//            bindingTypes[pat] = ty
//            return ty
//        }
//    }

//    fun childContext(): InferenceContext {
//        val childContext = InferenceContext(this.msl, itemContext)
//        childContext.exprTypes = this.exprTypes
////        childContext.callExprTypes = this.callExprTypes
////        childContext.typeTypes = this.typeTypes
//        childContext.typeErrors = this.typeErrors
//        return childContext
//    }

//    companion object {
//        fun default(msl: Boolean, element: MvElement): InferenceContext =
//            InferenceContext(msl, element.itemContext(msl))
//    }
}
