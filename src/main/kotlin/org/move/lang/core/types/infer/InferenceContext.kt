package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.rd.util.concurrentMapOf
import org.move.ide.presentation.expectedBindingFormText
import org.move.ide.presentation.name
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.itemSpecBlock
import org.move.lang.core.psi.ext.rightBrace
import org.move.lang.core.types.ty.*
import org.move.utils.cache
import org.move.utils.cacheManager
import java.lang.ref.SoftReference

interface MvInferenceContextOwner : MvElement {
    fun parameterBindings(): List<MvBindingPat>
}

private val INFERENCE_KEY_NON_MSL: Key<CachedValue<InferenceContext>> = Key.create("INFERENCE_KEY_NON_MSL")
private val INFERENCE_KEY_MSL: Key<CachedValue<InferenceContext>> = Key.create("INFERENCE_KEY_MSL")

fun MvElement.maybeInferenceContext(msl: Boolean): InferenceContext? {
    val inferenceOwner =
        PsiTreeUtil.getParentOfType(this, MvInferenceContextOwner::class.java, false)
            ?: return null
    return if (msl) {
        project.cacheManager.cache(inferenceOwner, INFERENCE_KEY_MSL) {
            inferenceOwner.createCachedResult(getOwnerInferenceContext(inferenceOwner, true))
        }
//        getProjectPsiDependentCache(inferenceOwner) {
//            getOwnerInferenceContext(it, true)
//        }
    } else {
        project.cacheManager.cache(inferenceOwner, INFERENCE_KEY_NON_MSL) {
            inferenceOwner.createCachedResult(getOwnerInferenceContext(inferenceOwner, false))
        }
//        getProjectPsiDependentCache(inferenceOwner) {
//            getOwnerInferenceContext(it, false)
//        }
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
    for (param in owner.parameterBindings()) {
        inferenceCtx.bindingTypes[param] = inferBindingPatTy(param, inferenceCtx, itemContext)
    }
    when (owner) {
        is MvFunctionLike -> {
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
            if (expectedTy != inferredTy) {
                Compat.TypeMismatch(expectedTy, inferredTy)
            } else {
                Compat.Yes
            }
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

enum class TypeErrorScope {
    MAIN, MODULE;
}

sealed class TypeError(open val element: PsiElement) {
    abstract fun message(): String

    companion object {
        fun isAllowedTypeError(error: TypeError, typeErrorScope: TypeErrorScope): Boolean {
            return when (typeErrorScope) {
                TypeErrorScope.MODULE -> error is CircularType
                TypeErrorScope.MAIN -> {
                    if (error is CircularType) return false
                    val element = error.element
                    if (
                        (error is UnsupportedBinaryOp || error is IncompatibleArgumentsToBinaryExpr)
                        && (element is MvElement && element.isMsl())
                    ) {
                        return false
                    }
                    true
                }
            }
        }
    }

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
                    "expected integer type, but found '${ty.text()}'"
        }
    }

    data class IncompatibleArgumentsToBinaryExpr(
        override val element: PsiElement,
        val leftTy: Ty,
        val rightTy: Ty,
        val op: String,
    ) : TypeError(element) {
        override fun message(): String {
            return "Incompatible arguments to '$op': " +
                    "'${leftTy.text()}' and '${rightTy.text()}'"
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

    data class CircularType(
        override val element: PsiElement,
        val structItem: MvStruct
    ) : TypeError(element) {
        override fun message(): String {
            return "Circular reference of type '${structItem.name}'"
        }
    }
}

class InferenceContext(val msl: Boolean, val itemContext: ItemContext) {
    var exprTypes = concurrentMapOf<MvExpr, Ty>()
    val patTypes = mutableMapOf<MvPat, Ty>()
    val typeTypes = mutableMapOf<MvType, Ty>()

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

    fun getBindingPatTy(pat: MvBindingPat): Ty {
        val existing = this.bindingTypes[pat]
        if (existing != null) {
            return existing
        } else {
            val ty = inferBindingPatTy(pat, this, this.itemContext)
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
