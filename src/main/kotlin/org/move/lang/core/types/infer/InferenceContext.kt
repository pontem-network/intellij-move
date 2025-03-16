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
import org.move.lang.core.types.ty.TyReference.Companion.isCompatibleMut
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

typealias CombineResult = RsResult<Unit, CombineTypeError>

private inline fun CombineResult.and(rhs: () -> CombineResult): CombineResult = if (isOk) rhs() else this

typealias CoerceResult = RsResult<CoerceOk, CombineTypeError>

class CoerceOk()

fun CombineResult.into(): CoerceResult = map { CoerceOk() }

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

    private val varUniTable = UnificationTable<TyInfer.TyVar>()
    private val intVarUniTable = UnificationTable<TyInfer.IntVar>()

    fun <T> freezeUnification(action: () -> T): T {
        val tableSnapshots =
            listOf(varUniTable, intVarUniTable).map { it.startSnapshot() }
        try {
            return action()
        } finally {
            tableSnapshots.forEach { it.rollback() }
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

        fallbackUnresolvedIntVars(exprTypes.values + patTypes.values)

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

    private fun fallbackUnresolvedIntVars(tys: Collection<Ty>) {
        for (ty in tys) {
            ty.deepVisitTyInfers { tyInfer ->
                val intVar = resolveTyInfer(tyInfer)
                if (intVar is TyInfer.IntVar) {
                    intVarUniTable.unifyVarValue(intVar, TyInteger.default())
                }
                false
            }
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

    fun combineTypes(ty1: Ty, ty2: Ty): CombineResult {
        val leftTy =
            (if (ty1 is TyInfer) this.resolveTyInfer(ty1) else ty1)
                .refineForSpecs(this.msl)
        val rightTy =
            (if (ty2 is TyInfer) this.resolveTyInfer(ty2) else ty2)
                .refineForSpecs(this.msl)
        return combineResolvedTypes(leftTy, rightTy)
    }

    private fun combineResolvedTypes(leftTy: Ty, rightTy: Ty): CombineResult {
        return when {
            leftTy is TyInfer.TyVar -> combineTyVar(leftTy, rightTy)
            rightTy is TyInfer.TyVar -> combineTyVar(rightTy, leftTy)
            else -> when {
                leftTy is TyInfer.IntVar -> combineIntVar(leftTy, rightTy)
                rightTy is TyInfer.IntVar -> combineIntVar(rightTy, leftTy)
                else -> combineTypesNoTyInfers(leftTy, rightTy)
            }
        }
    }

    private fun <T: Ty> combineTypePairs(pairs: List<Pair<T, T>>): CombineResult {
        var canUnify: CombineResult = Ok(Unit)
        for ((ty1, ty2) in pairs) {
            canUnify = combineTypes(ty1, ty2).and { canUnify }
        }
        return canUnify
    }

    private fun combineTyVar(tyVar: TyInfer.TyVar, ty: Ty): CombineResult {
        // skip unification for isCompatible check to prevent bugs
        if (skipUnification) return Ok(Unit)
        when (ty) {
            is TyInfer.TyVar -> varUniTable.unifyVarVar(tyVar, ty)
            else -> {
                val rootTyVar = varUniTable.resolveToRootTyVar(tyVar)
                if (ty.containsTyVar(rootTyVar)) {
                    // "E0308 cyclic type of infinite size"
                    varUniTable.unifyVarValue(rootTyVar, TyUnknown)
                    return Ok(Unit)
                }
                varUniTable.unifyVarValue(rootTyVar, ty)
            }
        }
        return Ok(Unit)
    }

    private fun Ty.containsTyVar(tyVar: TyInfer.TyVar): Boolean {
        return this.deepVisitTyInfers { innerTyVar ->
            innerTyVar is TyInfer.TyVar
                    && varUniTable.resolveToRootTyVar(innerTyVar) == tyVar
        }
    }

    private fun combineIntVar(ty1: TyInfer.IntVar, ty2: Ty): CombineResult {
        // skip unification for isCompatible check to prevent bugs
        if (skipUnification) return Ok(Unit)
        when (ty2) {
            is TyInfer.IntVar -> intVarUniTable.unifyVarVar(ty1, ty2)
            is TyInteger -> intVarUniTable.unifyVarValue(ty1, ty2)
            is TyUnknown -> {
                // do nothing, unknown should no influence IntVar
            }
            else -> return Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
        return Ok(Unit)
    }

    fun combineTypesNoTyInfers(ty1: Ty, ty2: Ty): CombineResult {
        return when {
            ty1 === ty2 -> Ok(Unit)
            ty1 is TyNever || ty2 is TyNever -> Ok(Unit)

            // assign TyUnknown to all TyVars if other type is unknown
            ty1 is TyUnknown || ty2 is TyUnknown -> {
                listOfNotNull(ty1, ty2).forEach {
                    it.deepVisitTyInfers { tyInfer ->
                        if (tyInfer is TyInfer.TyVar) {
                            combineTyVar(tyInfer, TyUnknown)
                        }
                        false
                    }
                }
                Ok(Unit)
            }
            ty1 is TyUnit && ty2 is TyUnit -> Ok(Unit)

            ty1 is TyTypeParameter && ty2 is TyTypeParameter && ty1 == ty2 -> Ok(Unit)
            ty1 is TyPrimitive && ty2 is TyPrimitive && ty1.name == ty2.name -> Ok(Unit)

            ty1 is TyInteger && ty2 is TyInteger -> combineTyIntegers(ty1, ty2)
            ty1 is TyVector && ty2 is TyVector -> combineTypes(ty1.item, ty2.item)
            ty1 is TyRange && ty2 is TyRange -> Ok(Unit)

            ty1 is TyReference && ty2 is TyReference -> combineTyRefs(ty1, ty2)
            ty1 is TyLambda && ty2 is TyLambda -> combineTyLambdas(ty1, ty2)

            ty1 is TyAdt && ty2 is TyAdt -> combineTyAdts(ty1, ty2)
            ty1 is TyTuple && ty2 is TyTuple -> combineTyTuples(ty1, ty2)

            else -> Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
    }

    private fun combineTyIntegers(ty1: TyInteger, ty2: TyInteger): CombineResult {
        if (!isCompatibleIntegers(ty1, ty2)) {
            return Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
        return Ok(Unit)
    }

    private fun combineTyRefs(ty1: TyReference, ty2: TyReference): CombineResult {
        // inferredTy permissions should be a superset of expectedTy permissions
        if (!isCompatibleMut(ty1, ty2)) {
//        if (!coerceMutability(ty1, ty2)) {
            return Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
        return combineTypes(ty1.referenced, ty2.referenced)
    }

    private fun combineTyAdts(ty1: TyAdt, ty2: TyAdt): CombineResult {
        if (ty1.item != ty2.item) {
            return Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
        return combineTypePairs(ty1.typeArguments.zip(ty2.typeArguments))
    }

    private fun combineTyTuples(ty1: TyTuple, ty2: TyTuple): CombineResult {
        if (ty1.types.size != ty2.types.size) {
            return Err(CombineTypeError.TypeMismatch(ty1, ty2))
        }
        return combineTypePairs(ty1.types.zip(ty2.types))
    }

    private fun combineTyLambdas(ty1: TyLambda, ty2: TyLambda): CombineResult {
        // todo: error if lambdas has different number of parameters
        for ((ty1param, ty2param) in ty1.paramTypes.zip(ty2.paramTypes)) {
            val combineRes = combineTypes(ty1param, ty2param)
            if (combineRes.isErr) {
                return combineRes
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

    // can return last `?T` if no value type found.
    fun resolveTyInfer(tyInfer: TyInfer): Ty {
        return when (tyInfer) {
            is TyInfer.IntVar -> intVarUniTable.resolveTyInfer(tyInfer) ?: tyInfer
            is TyInfer.TyVar -> {
                // check if variable is unified, or return untouched
                val varValueTy = varUniTable.resolveTyInfer(tyInfer) ?: return tyInfer
                if (varValueTy is TyInfer.IntVar) {
                    // value is always non-null Ty (not TyVar).
                    // It can only be TyInfer.TyVar, in this case we resolve it as integer.
                    intVarUniTable.resolveTyInfer(varValueTy) ?: varValueTy
                } else {
                    varValueTy
                }
            }
        }
    }

    fun <T: TypeFoldable<T>> resolveTypeVarsIfPossible(ty: T): T {
        return if (ty.hasTyInfer) {
            ty.deepFoldTyInferWith { this.resolveTyInfer(it) }
        } else {
            ty
        }
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

            return resTy.deepFoldWith(this)
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
                else -> resTy.deepFoldWith(this)
            }
        }
    }

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
