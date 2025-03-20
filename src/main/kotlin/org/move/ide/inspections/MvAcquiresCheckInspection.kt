package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.move.ide.inspections.fixes.AddAcquiresFix
import org.move.ide.inspections.fixes.RemoveAcquiresFix
import org.move.ide.presentation.declaringModule
import org.move.ide.presentation.fullnameNoArgs
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvCallable
import org.move.lang.core.psi.ext.isInline
import org.move.lang.core.psi.ext.receiverExpr
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyUnknown

data class NamedTy(var ty: Ty) {

    val fullname: String get() = this._fullname.value
    private val _fullname: Lazy<String> = lazy { ty.fullnameNoArgs() }

    val declModule: MvModule? get() = this._declaringModule.value
    private val _declaringModule: Lazy<MvModule?> = lazy { ty.declaringModule() }
}

fun List<Ty>.asNamedTys(): List<NamedTy> = this.map { NamedTy(it) }

fun getFunctionAcquiresTypes(owner: MvFunction): List<NamedTy> {
    val ctx = AcquiresTypeContext()
    val tys = when {
        owner.isInline -> {
            // collect `acquires` from inner calls and index exprs
            val inference = owner.inference(false)
            val allTypes = mutableListOf<NamedTy>()
            visitInnerAcquireTypeOwners(owner) {
                val tys = ctx.getAcquiredTypes(it, inference)
                allTypes.addAll(tys)
            }
            allTypes
        }
        else -> {
            // parse from MvAcquiresType
            owner.acquiredTys.asNamedTys()
//            owner.acquiresPathTypes.map { it.loweredType(false) }.asNamedTys()
        }
    }
    return tys
}

class AcquiresTypeContext {
    fun getAcquiredTypes(
        element: MvElement,
        outerInference: InferenceResult
    ): List<NamedTy> {
        return when (element) {
            is MvCallExpr, is MvMethodCall -> {
                this.getAcquiredTypesInCall(element, outerInference)
            }
            is MvIndexExpr -> {
                this.getAcquiredTypesInIndexExpr(element, outerInference)
            }
            else -> {
                emptyList()
            }
        }

    }

    fun getAcquiredTypesInCall(callable: MvCallable, inference: InferenceResult): List<NamedTy> {
        val callTy = inference.getCallableType(callable) ?: return emptyList()
        val callItem = callTy.genericKind() ?: return emptyList()
        val functionItem = callItem.item as? MvFunction ?: return emptyList()
        return if (functionItem.isInline) {
            val inlineFunctionTys = getFunctionAcquiresTypes(functionItem)
            inlineFunctionTys
                .map {
                    NamedTy(it.ty.substituteOrUnknown(callItem.substitution))
                }
        } else {
            functionItem.acquiredTys
                .asNamedTys()
                .map { NamedTy(it.ty.substitute(callItem.substitution)) }
        }
    }

    private fun getAcquiredTypesInIndexExpr(indexExpr: MvIndexExpr, inference: InferenceResult): List<NamedTy> {
        val receiverTy = inference.getExprType(indexExpr.receiverExpr)
        return if (receiverTy is TyAdt) {
            listOf(receiverTy)
        } else {
            emptyList()
        }
            .asNamedTys()
    }
}

private class MvAcquiresCheckVisitor(val holder: ProblemsHolder): MvVisitor() {
    override fun visitFunction(outerFunction: MvFunction) {
        val outerModule = outerFunction.module ?: return

        val ctx = AcquiresTypeContext()
        // it's non-inline, so it extracts MvAcquiresType
        val outerAcquiredTys =
            outerFunction.acquiresPathTypes.map { it.loweredType(false) }.asNamedTys()

        val inference = outerFunction.inference(false)
        val isInline = outerFunction.isInline

        val innerAcquiredTypes = mutableListOf<NamedTy>()
        visitInnerAcquireTypeOwners(outerFunction) { element ->
            val elementAcquiredTys = ctx.getAcquiredTypes(element, inference)
            // do not check for inline functions
            if (!isInline) {
                checkMissingAcquiresTypes(
                    element,
                    elementAcquiredTys,
                    outerAcquiredTys,
                    outerFunction,
                    outerModule
                )
            }
            innerAcquiredTypes.addAll(elementAcquiredTys)
        }

        // cannot check for unused acquires if there isn't one
        if (outerAcquiredTys.isEmpty()) return

        val innerAcquiredTypeNames = innerAcquiredTypes.map { it.fullname }

        val unusedTypeIndices = mutableListOf<Int>()
        val visitedTypes = mutableSetOf<String>()
        for ((i, outerAcqTy) in outerAcquiredTys.withIndex()) {
            if (outerAcqTy.ty is TyUnknown) continue

            if (outerAcqTy.declModule != outerModule) {
                unusedTypeIndices.add(i)
                continue
            }

            // check for duplicates
            val tyFullName = outerAcqTy.fullname
            if (tyFullName in visitedTypes) {
                unusedTypeIndices.add(i)
                continue
            }
            visitedTypes.add(tyFullName)

            if (tyFullName !in innerAcquiredTypeNames) {
                unusedTypeIndices.add(i)
                continue
            }
        }
        if (unusedTypeIndices.size == outerFunction.acquiresPathTypes.size) {
            // register whole acquiresType
            val outerAcquires = outerFunction.acquiresType ?: return
            holder.registerUnusedAcquires(outerAcquires)
            return
        }
        for (idx in unusedTypeIndices) {
            holder.registerUnusedAcquires(outerFunction.acquiresPathTypes[idx])
        }
    }

    private fun checkMissingAcquiresTypes(
        element: MvAcquireTypesOwner,
        elementAcquiredTys: List<NamedTy>,
        outerAcquiredTys: List<NamedTy>,
        outerFunction: MvFunction,
        outerModule: MvModule,
    ) {
        val outerAcquiredTypeNames = outerAcquiredTys.map { it.fullname }.toSet()
        val missingTypeItems = buildList {
            for (elementAcqTy in elementAcquiredTys) {
                val elementTy = elementAcqTy.ty
                if (elementTy is TyAdt) {
                    // no acquireable in this module, not relevant
                    if (elementAcqTy.declModule != outerModule) continue

                    if (elementAcqTy.fullname !in outerAcquiredTypeNames) {
                        add(elementTy.item)
                    }
                }
            }
        }

        if (missingTypeItems.isNotEmpty()) {
            val name = outerFunction.name ?: return
            val missingTypeNames = missingTypeItems.mapNotNull { it.name }
            holder.registerProblem(
                element,
                "Function '$name' is not marked as 'acquires ${missingTypeNames.joinToString()}'",
                ProblemHighlightType.GENERIC_ERROR,
                AddAcquiresFix(outerFunction, missingTypeNames)
            )
        }
    }

    private fun ProblemsHolder.registerUnusedAcquires(ref: PsiElement) {
        this.registerProblem(
            ref,
            "Unused acquires clause",
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
            RemoveAcquiresFix(ref)
        )
    }
}

class MvAcquiresCheckInspection: MvLocalInspectionTool() {
    override val isSyntaxOnly: Boolean get() = true
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): MvVisitor {
        return MvAcquiresCheckVisitor(holder)
    }
}