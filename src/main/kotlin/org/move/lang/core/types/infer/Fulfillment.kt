package org.move.lang.core.types.infer

import com.intellij.openapi.progress.ProgressManager
import org.move.lang.core.types.ty.*

sealed class Obligation: TypeFoldable<Obligation> {
    data class Equate(val ty1: Ty, val ty2: Ty): Obligation() {

        override fun innerFoldWith(folder: TypeFolder): Equate =
            Equate(ty1.foldWith(folder), ty2.foldWith(folder))

        override fun visitWith(visitor: TypeVisitor): Boolean = innerVisitWith(visitor)

        override fun innerVisitWith(visitor: TypeVisitor): Boolean =
            ty1.visitWith(visitor) || ty2.visitWith(visitor)

        override fun toString(): String =
            "$ty1 == $ty2"
    }
}

data class PendingPredicateObligation(
    val obligation: Obligation,
    var stalledOn: List<Ty> = emptyList()
)

/**
 * [ObligationForest] is a mutable collection of obligations.
 * It's caller's responsibility to add new obligations via
 * [registerObligationAt] and to remove satisfied obligations
 * as a side effect of [processObligations].
 */
class ObligationForest {
    enum class NodeState {
        /** Obligations for which selection had not yet returned a non-ambiguous result */
        Pending,

        /** This obligation was selected successfully, but may or may not have subobligations */
        Success,

        /** This obligation was resolved to an error. Error nodes are removed from the vector by the compression step */
        Error,
    }

    data class ProcessObligationsResult(
        val hasErrors: Boolean,
        val stalled: Boolean
    )

    class Node(val obligation: PendingPredicateObligation) {
        var state: NodeState = NodeState.Pending
    }

    private val nodes: MutableList<Node> = mutableListOf()
    private val doneCache: MutableSet<Obligation> = HashSet()

    val pendingObligations: Sequence<PendingPredicateObligation> =
        nodes.asSequence().filter { it.state == NodeState.Pending }.map { it.obligation }

    fun registerObligationAt(obligation: PendingPredicateObligation) {
        if (doneCache.add(obligation.obligation))
            nodes.add(Node(obligation))
    }

    fun processObligations(
        processor: (PendingPredicateObligation) -> ProcessObligationResult,
        breakOnFirstError: Boolean = false
    ): ProcessObligationsResult {
        var hasErrors = false
        var stalled = true
        for (index in 0 until nodes.size) {
            ProgressManager.checkCanceled()
            val node = nodes[index]
            if (node.state != NodeState.Pending) continue

            when (val result = processor(node.obligation)) {
                is ProcessObligationResult.NoChanges -> Unit
                is ProcessObligationResult.Ok -> {
                    stalled = false
                    node.state = NodeState.Success
                    for (child in result.children) {
                        registerObligationAt(child)
                    }
                }
                is ProcessObligationResult.Err -> {
                    hasErrors = true
                    stalled = false
                    node.state = NodeState.Error
                    if (breakOnFirstError) return ProcessObligationsResult(hasErrors, stalled)
                }
            }
        }

        if (!stalled) {
            nodes.removeIf { it.state != NodeState.Pending }
        }

        return ProcessObligationsResult(hasErrors, stalled)
    }

    override fun toString(): String {
        return this.nodes.map { it.obligation.obligation }.joinToString(", ")
    }
}

class FulfillmentContext(val ctx: InferenceContext) {
    private val obligations: ObligationForest = ObligationForest()

    val pendingObligations: Sequence<PendingPredicateObligation> =
        obligations.pendingObligations

    fun registerObligation(obligation: Obligation) {
        obligations.registerObligationAt(
            PendingPredicateObligation(ctx.resolveTypeVarsIfPossible(obligation)),
        )
    }

    fun selectWherePossible() {
        @Suppress("ControlFlowWithEmptyBody")
        while (!obligations.processObligations(this::processObligation).stalled);
    }

    fun selectUntilError(): Boolean {
        do {
            val res = obligations.processObligations(this::processObligation, breakOnFirstError = true)
            if (res.hasErrors) return false
        } while (!res.stalled)

        return true
    }

    private fun processObligation(pendingObligation: PendingPredicateObligation): ProcessObligationResult {
        var (obligation, stalledOn) = pendingObligation
        if (stalledOn.isNotEmpty()) {
            val nothingChanged = stalledOn.all {
                val resolvedTy = ctx.shallowResolve(it)
                resolvedTy == it
            }
            if (nothingChanged) return ProcessObligationResult.NoChanges
            pendingObligation.stalledOn = emptyList()
        }

        obligation = ctx.resolveTypeVarsIfPossible(obligation) as Obligation.Equate

        ctx.combineTypes(obligation.ty1, obligation.ty2)
        return ProcessObligationResult.Ok()
    }
}

sealed class ProcessObligationResult {
    object Err : ProcessObligationResult()
    object NoChanges : ProcessObligationResult()
    data class Ok(val children: List<PendingPredicateObligation>) : ProcessObligationResult() {
        constructor(vararg children: PendingPredicateObligation) : this(listOf(*children))
    }
}
