package org.move.lang.core.types.infer

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.util.CachedValue
import org.jetbrains.annotations.TestOnly
import org.move.cli.settings.isDebugModeEnabled
import org.move.ide.formatter.impl.location
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*
import org.move.lang.core.types.ty.TyReference.Companion.coerceMutability
import org.move.lang.toNioPathOrNull
import org.move.openapiext.document
import org.move.openapiext.getOffsetPosition
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.utils.cache
import org.move.utils.cacheManager
import org.move.utils.cacheResult
import org.move.utils.recursionGuard

interface MvInferenceContextOwner: MvElement

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
    // TODO: do we need to skip unification then, if we recreate the InferenceContext anyway?
    val inferenceCtx = InferenceContext(msl, skipUnification = true)
    return inferenceCtx.combineTypes(expectedTy, actualTy).isOk
}

typealias RelateResult = RsResult<Unit, CombineTypeError>

private inline fun RelateResult.and(rhs: () -> RelateResult): RelateResult = if (isOk) rhs() else this

typealias CoerceResult = RsResult<CoerceOk, CombineTypeError>

class CoerceOk()

fun RelateResult.into(): CoerceResult = map { CoerceOk() }

sealed class CombineTypeError {
    class TypeMismatch(val ty1: Ty, val ty2: Ty): CombineTypeError()
}

interface InferenceData {
    val patTypes: Map<MvPat, Ty>

    fun getPatTypeOrUnknown(pat: MvPat): Ty = patTypes[pat] ?: TyUnknown

    fun getPatType(pat: MvPat): Ty =
        patTypes[pat] ?: pat.project.inferenceErrorOrTyUnknown(pat)
}

data class InferenceResult(
    override val patTypes: Map<MvPat, Ty>,
    private val exprTypes: Map<MvExpr, Ty>,
    private val exprExpectedTypes: Map<MvExpr, Ty>,
    private val methodOrPathTypes: Map<MvMethodOrPath, Ty>,
    private val resolvedFields: Map<MvStructDotField, MvNamedElement?>,
    private val resolvedMethodCalls: Map<MvMethodCall, MvNamedElement?>,
    val callableTypes: Map<MvCallable, Ty>,
    val typeErrors: List<TypeError>
): InferenceData {
    fun getExprType(expr: MvExpr): Ty = exprTypes[expr] ?: expr.project.inferenceErrorOrTyUnknown(expr)

    @TestOnly
    fun hasExprType(expr: MvExpr): Boolean = expr in exprTypes

    /// Explicitly allow uninferred expr
    fun getExprTypeOrUnknown(expr: MvExpr): Ty = exprTypes[expr] ?: TyUnknown
    fun getExprTypeOrNull(expr: MvExpr): Ty? = exprTypes[expr]

    fun getExpectedType(expr: MvExpr): Ty = exprExpectedTypes[expr] ?: TyUnknown
    fun getCallableType(callable: MvCallable): Ty? = callableTypes[callable]
    fun getMethodOrPathType(methodOrPath: MvMethodOrPath): Ty? = methodOrPathTypes[methodOrPath]

    fun getResolvedField(field: MvStructDotField): MvNamedElement? = resolvedFields[field]
    fun getResolvedMethod(methodCall: MvMethodCall): MvNamedElement? = resolvedMethodCalls[methodCall]
}

fun inferTypesIn(element: MvInferenceContextOwner, msl: Boolean): InferenceResult {
    val inferenceCtx = InferenceContext(msl)
    return recursionGuard(element, { inferenceCtx.infer(element) }, memoize = false)
        ?: error("Cannot run nested type inference")
}

private val NEW_INFERENCE_KEY_NON_MSL: Key<CachedValue<InferenceResult>> =
    Key.create("NEW_INFERENCE_KEY_NON_MSL")
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

class InferenceContext(
    var msl: Boolean,
    private val skipUnification: Boolean = false
): InferenceData {

    override val patTypes = mutableMapOf<MvPat, Ty>()

    private val exprTypes = mutableMapOf<MvExpr, Ty>()
    private val exprExpectedTypes = mutableMapOf<MvExpr, Ty>()
    private val callableTypes = mutableMapOf<MvCallable, Ty>()

    //    private val pathTypes = mutableMapOf<MvPath, Ty>()
    private val methodOrPathTypes = mutableMapOf<MvMethodOrPath, Ty>()

    val resolvedFields = mutableMapOf<MvStructDotField, MvNamedElement?>()
    val resolvedMethodCalls = mutableMapOf<MvMethodCall, MvNamedElement?>()

    private val typeErrors = mutableListOf<TypeError>()

    val varUnificationTable = UnificationTable<TyInfer.TyVar, Ty>()
    val intUnificationTable = UnificationTable<TyInfer.IntVar, Ty>()

    fun startSnapshot(): Snapshot = CombinedSnapshot(
        intUnificationTable.startSnapshot(),
        varUnificationTable.startSnapshot(),
    )

    inline fun <T> freezeUnificationTable(action: () -> T): T {
        val snapshot = startSnapshot()
        try {
            return action()
        } finally {
            snapshot.rollback()
        }
    }

    fun infer(owner: MvInferenceContextOwner): InferenceResult {
        val returnTy = when (owner) {
            is MvFunctionLike -> owner.rawReturnType(msl)
            else -> TyUnknown
        }
        val inference = TypeInferenceWalker(this, owner.project, returnTy)

        inference.extractParameterBindings(owner)

        when (owner) {
            is MvFunctionLike -> owner.anyBlock?.let { inference.inferFnBody(it) }
            is MvItemSpec -> {
                owner.itemSpecBlock?.let { inference.inferSpec(it) }
            }
            is MvModuleItemSpec -> owner.itemSpecBlock?.let { inference.inferSpec(it) }
            is MvSchema -> owner.specBlock?.let { inference.inferSpec(it) }
        }

        fallbackUnresolvedTypeVarsIfPossible()

        exprTypes.replaceAll { _, ty -> fullyResolve(ty) }
        patTypes.replaceAll { _, ty -> fullyResolve(ty) }

        // for call expressions, we need to leave unresolved ty vars intact
        // to determine whether an explicit type annotation required
        callableTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }

        exprExpectedTypes.replaceAll { _, ty -> fullyResolveWithOrigins(ty) }
        typeErrors.replaceAll { err -> fullyResolveWithOrigins(err) }
//        pathTypes.replaceAll { _, ty -> fullyResolveWithOrigins(ty) }
        methodOrPathTypes.replaceAll { _, ty -> fullyResolveWithOrigins(ty) }

        return InferenceResult(
            patTypes,
            exprTypes,
            exprExpectedTypes,
            methodOrPathTypes,
            resolvedFields,
            resolvedMethodCalls,
            callableTypes,
            typeErrors
        )
    }

    private fun fallbackUnresolvedTypeVarsIfPossible() {
        val allTypes = exprTypes.values.asSequence() + patTypes.values.asSequence()
        for (ty in allTypes) {
            ty.visitInferTys { tyInfer ->
                val rty = shallowResolve(tyInfer)
                if (rty is TyInfer) {
                    fallbackIfPossible(rty)
                }
                false
            }
        }
    }

    private fun fallbackIfPossible(ty: TyInfer) {
        when (ty) {
            is TyInfer.IntVar -> intUnificationTable.unifyVarValue(ty, TyInteger.default())
            is TyInfer.TyVar -> Unit
        }
    }

    fun isTypeInferred(expr: MvExpr): Boolean {
        return exprTypes.containsKey(expr)
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

    fun writeCallableType(callable: MvCallable, ty: Ty) {
        this.callableTypes[callable] = ty
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: GenericTy> instantiateMethodOrPath(
        methodOrPath: MvMethodOrPath,
        genericItem: MvTypeParametersOwner
    ): Pair<T, Substitution>? {
        var itemTy =
            this.methodOrPathTypes.getOrPut(methodOrPath) {
                // can only be method or path, both are resolved to MvNamedElement
                val genericNamedItem = genericItem as MvNamedElement
                TyLowering.lowerPath(methodOrPath, genericNamedItem, msl) as? T ?: return null
            }

        val typeParameters = genericItem.tyInfers
        itemTy = itemTy.substitute(typeParameters) as? T ?: return null

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

    private fun <T: Ty> combineTypePairs(pairs: List<Pair<T, T>>): RelateResult {
        var canUnify: RelateResult = Ok(Unit)
        for ((ty1, ty2) in pairs) {
            canUnify = combineTypes(ty1, ty2).and { canUnify }
        }
        return canUnify
    }

    private fun combineTyVar(ty1: TyInfer.TyVar, ty2: Ty): RelateResult {
        // skip unification for isCompatible check to prevent bugs
        if (skipUnification) return Ok(Unit)
        when (ty2) {
            is TyInfer.TyVar -> varUnificationTable.unifyVarVar(ty1, ty2)
            else -> {
                val ty1r = varUnificationTable.findRoot(ty1)
                val isTy2ContainsTy1 = ty2.visitWith(object: TypeVisitor {
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
        if (skipUnification) return Ok(Unit)
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
            ty1msl is TyUnknown || ty2msl is TyUnknown -> {
                ty1msl.hasTyInfer && ty1msl.visitTyVarWith {
                    combineTyVar(it, TyUnknown); false
                }
                ty2msl.hasTyInfer && ty2msl.visitTyVarWith {
                    combineTyVar(it, TyUnknown); false
                }
                Ok(Unit)
            }

            ty1msl is TyTypeParameter && ty2msl is TyTypeParameter && ty1msl == ty2msl -> Ok(Unit)
            ty1msl is TyUnit && ty2msl is TyUnit -> Ok(Unit)
            ty1msl is TyInteger && ty2msl is TyInteger
                    && isCompatibleIntegers(ty1msl, ty2msl) -> Ok(Unit)
            ty1msl is TyPrimitive && ty2msl is TyPrimitive && ty1msl.name == ty2msl.name -> Ok(Unit)

            ty1msl is TyVector && ty2msl is TyVector -> combineTypes(ty1msl.item, ty2msl.item)
            ty1msl is TyRange && ty2msl is TyRange -> Ok(Unit)

            ty1msl is TyReference && ty2msl is TyReference
                    // inferredTy permissions should be a superset of expectedTy permissions
                    && coerceMutability(ty1msl, ty2msl) ->
                combineTypes(ty1msl.referenced, ty2msl.referenced)

            ty1msl is TyStruct && ty2msl is TyStruct
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
        return combineTypes(inferred, expected).into()
    }

    fun shallowResolve(ty: Ty): Ty {
        if (ty !is TyInfer) return ty

        return when (ty) {
            is TyInfer.IntVar -> intUnificationTable.findValue(ty) ?: ty
            is TyInfer.TyVar -> varUnificationTable.findValue(ty)?.let(this::shallowResolve) ?: ty
        }
    }

    fun <T: TypeFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        return ty.foldTyInferWith(this::shallowResolve)
    }

    fun <T: TypeFoldable<T>> fullyResolve(value: T): T = value.foldWith(fullTypeResolver)

    private inner class FullTypeResolver: TypeFolder() {
        override fun fold(ty: Ty): Ty {
            if (!ty.needsInfer) return ty
            val res = shallowResolve(ty)
            return if (res is TyInfer) TyUnknown else res.innerFoldWith(this)
        }
    }

    private val fullTypeResolver: FullTypeResolver = FullTypeResolver()

    /**
     * Similar to [fullyResolve], but replaces unresolved [TyInfer.TyVar] to its [TyInfer.TyVar.origin]
     * instead of [TyUnknown]
     */
    fun <T: TypeFoldable<T>> fullyResolveWithOrigins(value: T): T {
        return value.foldWith(fullTypeWithOriginsResolver)
    }

    private inner class FullTypeWithOriginsResolver: TypeFolder() {
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

    // Awful hack: check that inner expressions did not annotated as an error
    // to disallow annotation intersections. This should be done in a different way
    fun reportTypeError(typeError: TypeError) {
        val element = typeError.element
        if (!element.descendantHasTypeError(this.typeErrors)
            && typeError.element.containingFile.isPhysical
        ) {
            typeErrors.add(typeError)
        }
    }
}

fun PsiElement.descendantHasTypeError(existingTypeErrors: List<TypeError>): Boolean {
    return existingTypeErrors.any { typeError -> this.isAncestorOf(typeError.element) }
}

fun Project.inferenceErrorOrTyUnknown(inferredElement: MvElement): TyUnknown =
    when {
        // pragma statements are not supported for now
//        inferredElement.hasAncestorOrSelf<MvPragmaSpecStmt>() -> TyUnknown
        // error out if debug mode is enabled
        this.isDebugModeEnabled -> error(inferredElement.inferenceErrorMessage)
        else -> TyUnknown
    }

private val MvElement.inferenceErrorMessage: String
    get() {
        var text = "${this.elementType} `${this.text}` is never inferred"
        val file = this.containingFile
        if (file != null) {
            this.location?.let { (line, col) ->
                val virtualFile = file.originalFile.virtualFile
                if (virtualFile == null) {
                    // in-memory, print actual text
                    val textOffset = this.textOffset
                    val fileText = file.text
                    text += "\nFile: in-memory\n"
                    text += fileText.substring(0, textOffset)
                    text += "/*caret*/"
                    text += fileText.substring(textOffset + 1)
                } else {
                    text += "\nFile: ${virtualFile.toNioPathOrNull()} at ($line, $col)"
                }
            }
        }
        when (this) {
            is MvExpr -> {
                val stmt = this.ancestorStrict<MvStmt>()
                if (stmt != null) {
                    val psiString = DebugUtil.psiToString(stmt, true)
                    text += "\n"
                    text += psiString
                    // print next stmt too
                    val nextPsiContext = stmt.getNextNonCommentSibling() as? MvStmt
                    if (nextPsiContext != null) {
                        text += DebugUtil.psiToString(nextPsiContext, true)
                    }
                }
            }
        }
        return text
    }
