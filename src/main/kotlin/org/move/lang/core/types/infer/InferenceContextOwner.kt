package org.move.lang.core.types.infer

import com.intellij.psi.util.CachedValueProvider
import org.move.ide.formatter.impl.fileWithLocation
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModificationTrackerOwner
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.contextOrSelf
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.moveStructureModificationTracker
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.lang.toNioPathOrNull
import org.move.utils.cacheResult
import org.move.utils.recursionGuard

interface MvInferenceContextOwner: MvElement

fun inferTypesIn(element: MvInferenceContextOwner, msl: Boolean): InferenceResult {
    val inferenceCtx = InferenceContext(msl)
    return recursionGuard(element, { inferenceCtx.infer(element) }, memoize = false)
        ?: error("Cannot run nested type inference")
}

fun MvInferenceContextOwner.inference(msl: Boolean): InferenceResult {
    return if (msl) {
        synchronized(this) {
            ComputeInferenceInMsl(this).getResults()
        }
    } else {
        synchronized(this) {
            ComputeInference(this).getResults()
        }
    }
}

fun printPsiElementInfo(owner: MvElement) {
    val loc = owner.fileWithLocation
    if (loc != null) {
        val (file, loc) = loc
        if (owner is MvNamedElement) {
            println("> ${owner.elementType} with name `${owner.name}`")
        }
        println(file.toNioPathOrNull())
        println(loc)
    }
    println("object_id = ${System.identityHashCode(owner)}")
}

class ComputeInference(override val owner: MvInferenceContextOwner): PsiCachedValueProvider<InferenceResult> {
    override fun compute(): CachedValueProvider.Result<InferenceResult> {

        val inference = inferTypesIn(owner, false)

        val localModificationTracker =
            owner.contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
        val cacheDependencies: List<Any> =
            listOfNotNull(
                owner.project.moveStructureModificationTracker,
                localModificationTracker
            )
        return owner.cacheResult(inference, cacheDependencies)
    }
}

class ComputeInferenceInMsl(override val owner: MvInferenceContextOwner): PsiCachedValueProvider<InferenceResult> {
    override fun compute(): CachedValueProvider.Result<InferenceResult> {
        val inference = inferTypesIn(owner, true)
        val localModificationTracker =
            owner.contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
        val cacheDependencies: List<Any> =
            listOfNotNull(
                owner.project.moveStructureModificationTracker,
                localModificationTracker
            )
        return owner.cacheResult(inference, cacheDependencies)
    }
}
