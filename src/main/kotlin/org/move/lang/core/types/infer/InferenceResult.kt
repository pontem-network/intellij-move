package org.move.lang.core.types.infer

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.declaredTy
import org.move.lang.core.psi.ext.isMslAvailable
import org.move.lang.core.psi.ext.parameterBindings
import org.move.lang.core.types.ty.TyUnknown

private val TYPE_INFERENCE_KEY: Key<CachedValue<InferenceContext>> = Key.create("TYPE_INFERENCE_KEY")

val MvElement.inference: InferenceContext get() {
    return this.containingFunction?.inference ?: InferenceContext(msl = this.isMslAvailable())
}

val MvFunction.inference: InferenceContext
    get() {
        return CachedValuesManager.getCachedValue(this, TYPE_INFERENCE_KEY) {
            val fctx = InferenceContext(msl = this.isMslAvailable())
            for (param in this.parameterBindings) {
                fctx.bindingTypes[param] = inferBindingTy(param)
            }
            for (stmt in this.codeBlock?.statementList.orEmpty()) {
                when (stmt) {
                    is MvExprStatement -> {
                        inferExprTy(stmt.expr, fctx)
                    }
                    is MvLetStatement -> {
                        val initializerTy = stmt.initializer?.expr?.let { inferExprTy(it, fctx) }
                        val patTy = stmt.declaredTy ?: initializerTy ?: TyUnknown
                        val pat = stmt.pat ?: continue
                        fctx.bindingTypes.putAll(collectBindings(pat, patTy))
                    }
                }
            }
            CachedValueProvider.Result(fctx, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }
