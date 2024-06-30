package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.jetbrains.rd.util.concurrentMapOf
import org.move.cli.MoveProject
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvIndexExpr
import org.move.lang.core.psi.acquiresPathTypes
import org.move.lang.core.psi.ext.MvCallable
import org.move.lang.core.psi.ext.isInline
import org.move.lang.core.psi.ext.receiverExpr
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyStruct
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

class AcquiresTypeContext {
    private val functionTypes: MutableMap<MvFunction, List<Ty>> = concurrentMapOf()
    private val callableTypes: MutableMap<MvCallable, List<Ty>> = concurrentMapOf()

    fun getFunctionTypes(function: MvFunction): List<Ty> {
        val inference = function.inference(false)
        return functionTypes.getOrPut(function) {
            if (function.isInline) {
                // collect inner callExpr types
                val allTypes = mutableListOf<Ty>()
                for (innerCallExpr in inference.callableTypes.keys) {
                    val types = getCallTypes(innerCallExpr, inference)
                    allTypes.addAll(types)
                }
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
        return if (receiverTy is TyStruct) {
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
