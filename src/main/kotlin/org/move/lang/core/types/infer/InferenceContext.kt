package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.DebugUtil
import org.jetbrains.annotations.TestOnly
import org.move.cli.settings.isDebugModeEnabled
import org.move.ide.formatter.impl.location
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.RsPathResolveResult
import org.move.lang.core.types.ty.*
import org.move.lang.core.types.ty.TyReference.Companion.coerceMutability
import org.move.lang.toNioPathOrNull
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok

fun isCompatibleIntegers(expectedTy: TyInteger, inferredTy: TyInteger): Boolean {
    return expectedTy.kind == TyInteger.DEFAULT_KIND
            || inferredTy.kind == TyInteger.DEFAULT_KIND
            || expectedTy.kind == inferredTy.kind
}

fun compatAbilities(expectedTy: Ty, actualTy: Ty, msl: Boolean): Boolean {
    if (msl) return true
    if (expectedTy.hasTyAdt
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

    fun getPatType(pat: MvPat): Ty = patTypes[pat] ?: inferenceErrorOrFallback(pat, TyUnknown)

    fun getPatFieldType(patField: MvPatField): Ty

    fun getResolvedLitField(litField: MvStructLitField): List<MvNamedElement>

    fun getBindingType(binding: MvPatBinding): Ty =
        when (val parent = binding.parent) {
            is MvPatField -> getPatFieldType(parent)
            else -> getPatType(binding)
        }
}

data class InferenceResult(
    override val patTypes: Map<MvPat, Ty>,

    val patFieldTypes: Map<MvPatField, Ty>,

    private val exprTypes: Map<MvExpr, Ty>,
    private val exprExpectedTypes: Map<MvExpr, Ty>,

    private val resolvedPaths: Map<MvPath, List<RsPathResolveResult>>,
    private val resolvedFields: Map<MvFieldLookup, MvNamedElement?>,
    private val resolvedMethodCalls: Map<MvMethodCall, MvNamedElement?>,
    private val resolvedBindings: Map<MvPatBinding, MvNamedElement?>,
    private val resolvedLitFields: Map<MvStructLitField, List<MvNamedElement>>,

    val callableTypes: Map<MvCallable, Ty>,
    val lambdaExprTypes: Map<MvLambdaExpr, Ty>,
    val typeErrors: List<TypeError>
): InferenceData {
    fun getExprType(expr: MvExpr): Ty = exprTypes[expr] ?: inferenceErrorOrFallback(expr, TyUnknown)

    @TestOnly
    fun hasExprType(expr: MvExpr): Boolean = expr in exprTypes

    /// Explicitly allow uninferred expr
    fun getExprTypeOrUnknown(expr: MvExpr): Ty = exprTypes[expr] ?: TyUnknown
    fun getExprTypeOrNull(expr: MvExpr): Ty? = exprTypes[expr]

    fun getExpectedType(expr: MvExpr): Ty = exprExpectedTypes[expr] ?: TyUnknown
    fun getCallableType(callable: MvCallable): Ty? = callableTypes[callable]

    fun getResolvedPath(path: MvPath): List<RsPathResolveResult>? =
        resolvedPaths[path] ?: inferenceErrorOrFallback(path, null)

    fun getResolvedField(field: MvFieldLookup): MvNamedElement? = resolvedFields[field]
    fun getResolvedMethod(methodCall: MvMethodCall): MvNamedElement? = resolvedMethodCalls[methodCall]
    fun getResolvedPatBinding(binding: MvPatBinding): MvNamedElement? = resolvedBindings[binding]

    override fun getResolvedLitField(litField: MvStructLitField): List<MvNamedElement> =
        resolvedLitFields[litField].orEmpty()

    override fun getPatFieldType(patField: MvPatField): Ty =
        patFieldTypes[patField] ?: inferenceErrorOrFallback(patField, TyUnknown)
}

fun MvElement.inferenceContextOwner(): MvInferenceContextOwner? = this.ancestorOrSelf()

fun MvElement.inference(msl: Boolean): InferenceResult? {
    val contextOwner = inferenceContextOwner() ?: return null
    return contextOwner.inference(msl)
}

class InferenceContext(
    var msl: Boolean,
    private val skipUnification: Boolean = false
): InferenceData {

    override val patTypes = mutableMapOf<MvPat, Ty>()
    private val patFieldTypes = mutableMapOf<MvPatField, Ty>()

    private val exprTypes = mutableMapOf<MvExpr, Ty>()
    private val exprExpectedTypes = mutableMapOf<MvExpr, Ty>()
    private val callableTypes = mutableMapOf<MvCallable, Ty>()

    val lambdaExprTypes = mutableMapOf<MvLambdaExpr, Ty>()
    val lambdaExprs = mutableListOf<MvLambdaExpr>()

    val resolvedPaths = mutableMapOf<MvPath, List<RsPathResolveResult>>()
    val resolvedFields = mutableMapOf<MvFieldLookup, MvNamedElement?>()
    val resolvedMethodCalls = mutableMapOf<MvMethodCall, MvNamedElement?>()
    val resolvedBindings = mutableMapOf<MvPatBinding, MvNamedElement?>()

    val resolvedLitFields: MutableMap<MvStructLitField, List<MvNamedElement>> = hashMapOf()

    private val typeErrors = mutableListOf<TypeError>()

    val varUnificationTable = UnificationTable<TyInfer.TyVar, Ty>()
    val intUnificationTable = UnificationTable<TyInfer.IntVar, Ty>()

    fun startSnapshot(): Snapshot = CombinedSnapshot(
        intUnificationTable.startSnapshot(),
        varUnificationTable.startSnapshot(),
    )

    inline fun <T> freezeUnification(action: () -> T): T {
        val snapshot = startSnapshot()
        try {
            return action()
        } finally {
            snapshot.rollback()
        }
    }

    fun infer(owner: MvInferenceContextOwner): InferenceResult {
        val returnTy = when (owner) {
            is MvFunctionLike -> owner.returnTypeTy(msl)
            else -> TyUnknown
        }
        val inference = TypeInferenceWalker(this, owner.project, returnTy)

        inference.extractParameterBindings(owner)

        if (owner is MvDocAndAttributeOwner) {
            for (attr in owner.attrList) {
                for (attrItem in attr.attrItemList) {
                    inference.inferAttrItem(attrItem)
                }
            }
        }

        when (owner) {
            is MvFunctionLike -> owner.anyBlock?.let { inference.inferFnBody(it) }
            is MvItemSpec -> {
                owner.itemSpecBlock?.let { inference.inferSpecBlock(it) }
            }
            is MvModuleItemSpec -> owner.itemSpecBlock?.let { inference.inferSpecBlock(it) }
            is MvSchema -> owner.specBlock?.let { inference.inferSpecBlock(it) }
        }

        //  1. collect lambda expr bodies while inferring the context
        //  2. for every lambda expr body:
        //     1. infer lambda expr body, adding items to outer inference result
        //     2. resolve all vars again in the InferenceContext
        //  3. resolve vars replacing unresolved vars with tyunknown

        while (lambdaExprs.isNotEmpty()) {
            val lambdaExpr = lambdaExprs.removeFirst()

            resolveAllTypeVarsIfPossible()

            val retTy = (lambdaExprTypes[lambdaExpr] as? TyLambda)?.returnType
            lambdaExpr.expr?.let {
                if (retTy != null) {
                    inference.inferExprTypeCoercableTo(it, retTy)
                } else {
                    inference.inferExprType(it)
                }
            }
        }

        fallbackUnresolvedTypeVarsIfPossible()

        exprTypes.replaceAll { _, ty -> fullyResolveTypeVars(ty) }
        patTypes.replaceAll { _, ty -> fullyResolveTypeVars(ty) }
        patFieldTypes.replaceAll { _, ty -> fullyResolveTypeVars(ty) }

        // for call expressions, we need to leave unresolved ty vars intact
        // to determine whether an explicit type annotation required
        callableTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }
        exprExpectedTypes.replaceAll { _, ty -> fullyResolveTypeVarsWithOrigins(ty) }
        lambdaExprTypes.replaceAll { _, ty -> fullyResolveTypeVarsWithOrigins(ty) }

        typeErrors.replaceAll { err -> fullyResolveTypeVarsWithOrigins(err) }

        return InferenceResult(
            patTypes,
            patFieldTypes,
            exprTypes,
            exprExpectedTypes,
            resolvedPaths,
            resolvedFields,
            resolvedMethodCalls,
            resolvedBindings,
            resolvedLitFields,
            callableTypes,
            lambdaExprTypes,
            typeErrors
        )
    }

    private fun resolveAllTypeVarsIfPossible() {
        patTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }
        exprTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }
        patFieldTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }
        callableTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }
        exprExpectedTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }
        lambdaExprTypes.replaceAll { _, ty -> resolveTypeVarsIfPossible(ty) }
    }

    private fun fallbackUnresolvedTypeVarsIfPossible() {
        val allTypes = exprTypes.values.asSequence() + patTypes.values.asSequence()
        for (ty in allTypes) {
            ty.visitInferTys { tyInfer ->
                val rty = resolveTyInfer(tyInfer)
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

    fun writePath(path: MvPath, resolved: List<RsPathResolveResult>) {
        resolvedPaths[path] = resolved
    }

    fun writeFieldPatTy(psi: MvPatField, ty: Ty) {
        patFieldTypes[psi] = ty
    }

    fun writeExprExpectedTy(expr: MvExpr, ty: Ty) {
        this.exprExpectedTypes[expr] = ty
    }

    fun writeCallableType(callable: MvCallable, ty: Ty) {
        this.callableTypes[callable] = ty
    }

    override fun getPatFieldType(patField: MvPatField): Ty =
        patFieldTypes[patField] ?: inferenceErrorOrFallback(patField, TyUnknown)

    override fun getResolvedLitField(litField: MvStructLitField): List<MvNamedElement> =
        resolvedLitFields[litField].orEmpty()

    fun getExprType(expr: MvExpr): Ty {
        return exprTypes[expr] ?: inferenceErrorOrFallback(expr, TyUnknown)
    }

    fun combineTypes(ty1: Ty, ty2: Ty): RelateResult {
        val resolvedTy1 = resolveIfTyInfer(ty1)
        val resolvedTy2 = resolveIfTyInfer(ty2)
        return combineTypesResolved(resolvedTy1, resolvedTy2)
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

    private fun combineIntVar(ty1: TyInfer.IntVar, ty2: Ty): RelateResult {
        // skip unification for isCompatible check to prevent bugs
        if (skipUnification) return Ok(Unit)
        when (ty2) {
            is TyInfer.IntVar -> intUnificationTable.unifyVarVar(ty1, ty2)
            is TyInteger -> intUnificationTable.unifyVarValue(ty1, ty2)
            is TyUnknown -> {
                // do nothing, unknown should no influence IntVar
            }
            else -> return Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
        return Ok(Unit)
    }

    fun combineTypesNoVars(ty1: Ty, ty2: Ty): RelateResult {
        return when {
            ty1 === ty2 -> Ok(Unit)
            ty1 is TyNever || ty2 is TyNever -> Ok(Unit)

            // assign TyUnknown to all TyVars if other type is unknown
            ty1 is TyUnknown -> {
                ty2.visitTyVarWith { combineTyVar(it, TyUnknown); false }
                Ok(Unit)
            }
            ty2 is TyUnknown -> {
                ty1.visitTyVarWith { combineTyVar(it, TyUnknown); false }
                Ok(Unit)
            }

            ty1 is TyTypeParameter && ty2 is TyTypeParameter && ty1 == ty2 -> Ok(Unit)
            ty1 is TyUnit && ty2 is TyUnit -> Ok(Unit)
            ty1 is TyInteger && ty2 is TyInteger
                    && isCompatibleIntegers(ty1, ty2) -> Ok(Unit)
            ty1 is TyPrimitive && ty2 is TyPrimitive && ty1.name == ty2.name -> Ok(Unit)

            ty1 is TyVector && ty2 is TyVector -> combineTypes(ty1.item, ty2.item)
            ty1 is TyRange && ty2 is TyRange -> Ok(Unit)

            ty1 is TyReference && ty2 is TyReference
                    // inferredTy permissions should be a superset of expectedTy permissions
                    && coerceMutability(ty1, ty2) ->
                combineTypes(ty1.referenced, ty2.referenced)

            ty1 is TyAdt && ty2 is TyAdt
                    && ty1.item == ty2.item ->
                combineTypePairs(ty1.typeArguments.zip(ty2.typeArguments))

            ty1 is TyTuple && ty2 is TyTuple
                    && ty1.types.size == ty2.types.size ->
                combineTypePairs(ty1.types.zip(ty2.types))

            ty1 is TyLambda && ty2 is TyLambda -> combineTyLambdas(ty1, ty2)

            else -> Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
    }

    fun combineTyLambdas(ty1: TyLambda, ty2: TyLambda): RelateResult {
        // todo: error if lambdas has different number of parameters
        for ((ty1param, ty2param) in ty1.paramTypes.zip(ty2.paramTypes)) {
            val res = combineTypes(ty1param, ty2param)
            if (res.isErr) {
                return res
            }
        }
        // todo: resolve variables
        return combineTypes(ty1.returnType, ty2.returnType)
    }

    fun tryCoerce(inferred: Ty, expected: Ty): CoerceResult {
        if (inferred === expected) {
            return Ok(CoerceOk())
        }
        return combineTypes(inferred, expected).into()
    }

    fun resolveIfTyInfer(ty: Ty) = if (ty is TyInfer) resolveTyInfer(ty) else ty

    fun resolveTyInfer(tyInfer: TyInfer): Ty {
        return when (tyInfer) {
            is TyInfer.IntVar -> intUnificationTable.findValue(tyInfer) ?: tyInfer
            is TyInfer.TyVar -> varUnificationTable.findValue(tyInfer)?.let(this::resolveIfTyInfer) ?: tyInfer
        }
    }

    fun <T: TypeFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        return if (ty.hasTyInfer) ty.deepFoldTyInferWith(this::resolveTyInfer) else ty
    }

    /// every TyVar unresolved at the end of this function converted into TyUnknown
    fun <T: TypeFoldable<T>> fullyResolveTypeVars(value: T): T = value.foldWith(fullTypeResolver)

    private inner class FullTypeResolver: TypeFolder() {
        override fun fold(ty: Ty): Ty {
            if (!ty.hasTyInfer) return ty
            // try to resolve TyInfer shallow
            val resTy = if (ty is TyInfer) resolveTyInfer(ty) else ty

            // if still unresolved, return unknown type
            if (resTy is TyInfer) return TyUnknown

            return resTy.innerFoldWith(this)
        }
    }

    private val fullTypeResolver: FullTypeResolver = FullTypeResolver()

    /**
     * Similar to [fullyResolveTypeVars], but replaces unresolved [TyInfer.TyVar] to its [TyInfer.TyVar.origin]
     * instead of [TyUnknown]
     */
    fun <T: TypeFoldable<T>> fullyResolveTypeVarsWithOrigins(value: T): T {
        return value.foldWith(fullTypeWithOriginsResolver)
    }

    private val fullTypeWithOriginsResolver: FullTypeWithOriginsResolver = FullTypeWithOriginsResolver()

    private inner class FullTypeWithOriginsResolver: TypeFolder() {
        override fun fold(ty: Ty): Ty {
            if (!ty.hasTyInfer) return ty
            val resTy = if (ty is TyInfer) resolveTyInfer(ty) else ty
            return when (resTy) {
                // if it's TyUnknown, check whether the original ty has an origin, use that
                is TyUnknown -> (ty as? TyInfer.TyVar)?.origin ?: TyUnknown
                // replace TyVar with the origin TyTypeParameter
                is TyInfer.TyVar -> resTy.origin ?: TyUnknown
                // replace integer with TyUnknown, todo: why?
                is TyInfer.IntVar -> TyUnknown
                else -> resTy.innerFoldWith(this)
            }
        }
    }

    // Awful hack: check that inner expressions did not annotated as an error
    // to disallow annotation intersections. This should be done in a different way
    fun reportTypeError(typeError: TypeError) {
        typeErrors.add(typeError)
    }
}

fun PsiElement.descendantHasTypeError(existingTypeErrors: List<TypeError>): Boolean {
    return existingTypeErrors.any { typeError -> this.isAncestorOf(typeError.element) }
}

fun <T> inferenceErrorOrFallback(inferredElement: MvElement, fallback: T): T =
    when {
        // pragma statements are not supported for now
//        inferredElement.hasAncestorOrSelf<MvPragmaSpecStmt>() -> TyUnknown
        // error out if debug mode is enabled
        isDebugModeEnabled() -> throw InferenceError(inferredElement.inferenceErrorMessage)
        else -> fallback
    }

class InferenceError(message: String, var context: PsiErrorContext? = null): IllegalStateException(message) {
    override fun toString(): String {
        var message = super.toString()
        val context = context
        if (context != null) {
            message += ", \ncontext: \n$context"
        }
        return message
    }
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
