package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvCallable
import org.move.lang.core.psi.ext.isInline
import org.move.lang.core.psi.ext.receiverExpr
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyFunction

interface MvAcquireTypesOwner: MvElement

fun visitInnerAcquireTypeOwners(element: MvElement, visit: (MvAcquireTypesOwner) -> Unit) {
    val recursiveVisitor = object: PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is MvAcquireTypesOwner) {
                visit(element)
            }
            super.visitElement(element)
        }
    }
    recursiveVisitor.visitElement(element)
}

class AcquiresTypeContext {
    fun getAcquiredTypes(
        element: MvElement,
        outerInference: InferenceResult
    ): List<Ty> {
        return when (element) {
            is MvCallExpr, is MvMethodCall -> this.getAcquiredTypesInCall(element, outerInference)
            is MvIndexExpr -> {
                this.getAcquiredTypesInIndexExpr(element, outerInference)
            }
            else -> {
                emptyList()
            }
        }

    }

    fun getTypesAcquiredInFun(function: MvFunction): List<Ty> {
        when {
            function.isInline -> {
                // collect `acquires` from inner calls and index exprs
                val inference = function.inference(false)
                val allTypes = mutableListOf<Ty>()
                visitInnerAcquireTypeOwners(function) {
                    val tys = this.getAcquiredTypes(it, inference)
                    allTypes.addAll(tys)
                }
                return allTypes
            }
            else -> {
                // parse from MvAcquiresType
                return function.acquiresPathTypes.map { it.loweredType(false) }
            }
        }
    }

    fun getAcquiredTypesInCall(callable: MvCallable, inference: InferenceResult): List<Ty> {
        val callTy = inference.getCallableType(callable) as? TyFunction ?: return emptyList()
        val callItem = callTy.item as? MvFunction ?: return emptyList()
        return if (callItem.isInline) {
            val functionTypes = this.getTypesAcquiredInFun(callItem)
            val resolvedFunctionTypes = functionTypes
                .map { it.substituteOrUnknown(callTy.substitution) }
            resolvedFunctionTypes
        } else {
            callTy.acquiresTypes
        }
    }

    fun getAcquiredTypesInIndexExpr(indexExpr: MvIndexExpr, inference: InferenceResult): List<Ty> {
        val receiverTy = inference.getExprType(indexExpr.receiverExpr)
        return if (receiverTy is TyAdt) {
            listOf(receiverTy)
        } else {
            emptyList()
        }
    }
}

fun MvFunction.getInnerAcquiresTypes(): List<Ty> {
    return AcquiresTypeContext().getTypesAcquiredInFun(this)
}

fun MvCallExpr.getAcquiresTypes(inference: InferenceResult): List<Ty> {
    return AcquiresTypeContext().getAcquiredTypesInCall(this, inference)
}
