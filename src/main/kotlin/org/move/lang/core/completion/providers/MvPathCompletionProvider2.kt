package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.progress.ProgressManager
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.lang.core.MvPsiPattern.path
import org.move.lang.core.completion.Completions
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.UNIMPORTED_ITEM_PRIORITY
import org.move.lang.core.completion.createCompletionItem
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.PathKind
import org.move.lang.core.resolve.isVisibleInContext
import org.move.lang.core.resolve.pathKind
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntry
import org.move.lang.core.types.infer.inferExpectedTy
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

object MvPathCompletionProvider2: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> get() = path()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val maybePath = parameters.position.parent
        val pathElement = maybePath as? MvPath ?: maybePath.parent as MvPath
        if (parameters.position !== pathElement.referenceNameElement) return

        val resolutionCtx = ResolutionContext(pathElement, isCompletion = true)
        val msl = pathElement.isMslScope
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(pathElement, msl)

        val pathKind = pathElement.pathKind(true)
        val ns = pathKind.ns
        val structAsType = ns.contains(Ns.TYPE)

        val completionContext = MvCompletionContext(
            pathElement,
            msl,
            expectedTy,
            resolutionCtx = resolutionCtx,
            structAsType,
        )
        val completions = Completions(completionContext, result)

        addPathVariants(pathElement, parameters, ns, completions)
    }

    fun addPathVariants(
        pathElement: MvPath,
        parameters: CompletionParameters,
        ns: NsSet,
        completions: Completions,
    ) {
        val resolutionCtx = completions.ctx.resolutionCtx ?: error("always non-null in path completion")
        val pathKind = pathElement.pathKind(true)

        var entries = getPathResolveVariantsWithExpectedType(
            resolutionCtx,
            pathKind,
            expectedType = completions.ctx.expectedTy
        )
        ProgressManager.checkCanceled()

        val useGroup = resolutionCtx.useSpeck?.parent as? MvUseGroup
        if (useGroup != null) {
            val existingNames = useGroup.names.toSet()
            entries = entries.filter { it.name !in existingNames }
        }
        if (useGroup == null) {
            entries = entries.filter { it.name != "Self" }
        }

        // todo: should it really be deduplicated?
        val uniqueEntries = entries.deduplicate()

        val visibleEntries = uniqueEntries
            .dropInvisibleEntries(contextElement = pathElement)
        val visitedNames = visibleEntries.map { it.name }

        completions.addEntries(visibleEntries)
        ProgressManager.checkCanceled()

        addCompletionsForOutOfScopeItems(
            parameters,
            pathElement,
            completions,
            ns,
            visitedNames.toMutableSet()
        )
    }

    private fun addCompletionsForOutOfScopeItems(
        parameters: CompletionParameters,
        path: MvPath,
        completions: Completions,
        ns: NsSet,
        processedNames: MutableSet<String>,
    ) {
        // disable auto-import in module specs for now
        if (path.containingModuleSpec != null) return

        // no out-of-scope completions for use specks
        if (path.isUseSpeck) return

        // no import candidates for qualified paths
        if (path.pathKind(true) is PathKind.QualifiedPath) return

        val originalPathElement = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext = ImportContext.from(originalPathElement, true, ns) ?: return
        val candidates =
            ImportCandidateCollector.getCompletionCandidates(
                path.project,
                completions.result.prefixMatcher,
                processedNames,
                importContext,
            )
        for (candidate in candidates) {
            val scopeEntry = candidate.element.asEntry() ?: continue
            val completionItem = createCompletionItem(
                scopeEntry,
                completions.ctx,
                priority = UNIMPORTED_ITEM_PRIORITY,
                insertHandler = ImportInsertHandler(parameters, candidate)
            )
            if (completionItem != null) {
                completions.addCompletionItem(completionItem)
            }
        }
    }
}

fun List<ScopeEntry>.dropInvisibleEntries(contextElement: MvElement): List<ScopeEntry> {
    return this.filter {
        isVisibleInContext(it, contextElement)
    }
}

fun List<ScopeEntry>.deduplicate(): List<ScopeEntry> {
    val visitedNames = mutableSetOf<String>()
    return this.filter {
        if (visitedNames.contains(it.name)) return@filter false
        visitedNames.add(it.name)
        true
    }
}

fun getExpectedTypeForEnclosingPathOrDotExpr(element: MvReferenceElement, msl: Boolean): Ty? {
    for (ancestor in element.ancestors) {
        if (element.endOffset < ancestor.endOffset) continue
        if (element.endOffset > ancestor.endOffset) break
        when (ancestor) {
            is MvPathType,
            is MvPathExpr,
            is MvDotExpr -> {
                val inference = ancestor.inference(msl) ?: return TyUnknown
                return inferExpectedTy(ancestor, inference)
            }
        }
    }
    return null
}
