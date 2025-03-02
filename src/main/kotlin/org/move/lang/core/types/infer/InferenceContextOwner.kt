package org.move.lang.core.types.infer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import okio.withLock
import org.move.ide.formatter.impl.fileWithLocation
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModificationTrackerOwner
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.contextOrSelf
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.moveStructureModificationTracker
import org.move.lang.core.resolve.PsiCachedValueProvider
import org.move.lang.core.resolve.getResults
import org.move.lang.toNioPathOrNull
import org.move.utils.cacheResult
import org.move.utils.recursionGuard
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

interface MvInferenceContextOwner: MvElement

fun inferTypesIn(element: MvInferenceContextOwner, msl: Boolean): InferenceResult {
    val inferenceCtx = InferenceContext(msl)
    return recursionGuard(element, { inferenceCtx.infer(element) }, memoize = false)
        ?: error("Cannot run nested type inference")
}

fun MvInferenceContextOwner.inference(msl: Boolean): InferenceResult {
    val lock = this.project.inferenceLocks.getLock(this, msl)
    return if (msl) {
        lock.withLock {
            ComputeInferenceInMsl(this).getResults()
        }
    } else {
        lock.withLock {
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

val Project.inferenceLocks: InferenceLocksService get() = this.service<InferenceLocksService>()

@Service(Service.Level.PROJECT)
class InferenceLocksService(val project: Project) {
    val locks = ConcurrentHashMap<Int, ReentrantLock>()
    val locksMsl = ConcurrentHashMap<Int, ReentrantLock>()

    fun getLock(element: MvElement, msl: Boolean): ReentrantLock {
        val locks = if (msl) this.locksMsl else this.locks
        return locks.getOrPut(System.identityHashCode(element)) { ReentrantLock() }
    }
}

class ComputeInference(override val owner: MvInferenceContextOwner): PsiCachedValueProvider<InferenceResult> {
    override fun compute(): CachedValueProvider.Result<InferenceResult> {

        val inference = inferTypesIn(owner, false)
//
//        val inference = lock.withLock {
//            printOwnerInfo(owner)
//            inferTypesIn(owner, false)
//        }
//        var ownerLock = owner.getUserData(INFERENCE_LOCK)
//        if (ownerLock == null) {
//            ownerLock = ReentrantLock()
//            owner.putUserData(INFERENCE_LOCK, ownerLock)
//        }
//
//        val inference = try {
//            ownerLock.lock()
//
//            printInferenceContextOwnerInfo(owner)
//            inferTypesIn(owner, false)
//
//        } finally {
//            ownerLock.unlock()
//        }

//        printInferenceContextOwnerInfo(owner)
//        val inference = inferTypesIn(owner, false)

//        val inference = synchronized(owner) {
//            printOwnerInfo(owner)
//            inferTypesIn(owner, false)
//        }

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
