package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.jetbrains.rd.util.concurrentMapOf
import org.move.cli.MoveProject
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvCallable
import org.move.lang.core.psi.ext.isInline
import org.move.lang.core.psi.ext.receiverExpr
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.moveProject

val ACQUIRES_TYPE_CONTEXT: Key<CachedValue<AcquiresTypeContext>> = Key.create("ACQUIRES_TYPE_CONTEXT")

val MoveProject.acquiresContext: AcquiresTypeContext
    get() {
        val manager = CachedValuesManager.getManager(project)
        return manager.getCachedValue(this, ACQUIRES_TYPE_CONTEXT, {
            val context = AcquiresTypeContext()
            CachedValueProvider.Result.create(
                context,
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }, false)
    }


abstract class AcquireTypesOwnerVisitor: PsiRecursiveElementVisitor() {

    abstract fun visitAcquireTypesOwner(acqTypesOwner: MvAcquireTypesOwner)

    override fun visitElement(element: PsiElement) {
        if (element is MvAcquireTypesOwner) {
            visitAcquireTypesOwner(element)
        }
        super.visitElement(element)
    }
}

class AcquiresTypeContext {
    private val functionTypes: MutableMap<MvFunction, List<Ty>> = concurrentMapOf()
    private val callableTypes: MutableMap<MvCallable, List<Ty>> = concurrentMapOf()

    fun getFunctionTypes(function: MvFunction): List<Ty> {
        val inference = function.inference(false)
        return functionTypes.getOrPut(function) {
            if (function.isInline) {
                // collect inner callExpr types
                val allTypes = mutableListOf<Ty>()

                val visitor = object: AcquireTypesOwnerVisitor() {
                    override fun visitAcquireTypesOwner(acqTypesOwner: MvAcquireTypesOwner) {
                        val tys =
                            when (acqTypesOwner) {
                                is MvCallable -> getCallTypes(acqTypesOwner, inference)
                                is MvIndexExpr -> getIndexExprTypes(acqTypesOwner, inference)
                                else -> error("when is exhaustive")
                            }
                        allTypes.addAll(tys)
                    }
                }
                visitor.visitElement(function)

                allTypes
            } else {
                // parse from MvAcquiresType
                function.acquiresPathTypes.map { it.loweredType(false) }
            }
        }
    }

    fun getCallTypes(callable: MvCallable, inference: InferenceResult): List<Ty> {
        return callableTypes.getOrPut(callable) {
            val callTy = inference.getCallableType(callable) as? TyFunction ?: return emptyList()
            val callItem = callTy.item as? MvFunction ?: return emptyList()
            if (callItem.isInline) {
                val functionTypes = this.getFunctionTypes(callItem)
                val resolvedFunctionTypes = functionTypes
                    .map { it.substituteOrUnknown(callTy.substitution) }
                resolvedFunctionTypes
            } else {
                callTy.acquiresTypes
            }
        }
    }

    fun getIndexExprTypes(indexExpr: MvIndexExpr, inference: InferenceResult): List<Ty> {
        val receiverTy = inference.getExprType(indexExpr.receiverExpr)
        return if (receiverTy is TyAdt) {
            listOf(receiverTy)
        } else {
            emptyList()
        }
    }
}

fun MvFunction.getInnerAcquiresTypes(): List<Ty> {
    val typeContext = this.moveProject?.acquiresContext ?: return emptyList()
    return typeContext.getFunctionTypes(this)
}

fun MvCallExpr.getAcquiresTypes(inference: InferenceResult): List<Ty> {
    val typeContext = this.moveProject?.acquiresContext ?: return emptyList()
    return typeContext.getCallTypes(this, inference)
}
