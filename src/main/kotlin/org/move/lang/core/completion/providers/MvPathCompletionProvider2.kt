package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.utils.imports.ImportCandidate
import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.lang.core.MvPsiPattern.path
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.UNIMPORTED_ITEM_PRIORITY
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.MODULE
import org.move.lang.core.resolve.ref.TYPES_N_ENUMS
import org.move.lang.core.resolve2.PathKind
import org.move.lang.core.resolve2.pathKind
import org.move.lang.core.resolve2.ref.ResolutionContext
import org.move.lang.core.resolve2.ref.processPathResolveVariantsWithExpectedType
import org.move.lang.core.types.infer.inferExpectedTy
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import org.move.stdext.intersects

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
        val structAsType = TYPES_N_ENUMS.intersects(ns)

        val completionContext = MvCompletionContext(
            pathElement,
            msl,
            expectedTy,
            resolutionCtx = resolutionCtx,
            structAsType,
        )

        addPathVariants(
            pathElement, parameters, completionContext, ns, result,
        )
    }

    fun addPathVariants(
        pathElement: MvPath,
        parameters: CompletionParameters,
        completionContext: MvCompletionContext,
        ns: Set<Namespace>,
        result: CompletionResultSet,
    ) {
        val resolutionCtx = completionContext.resolutionCtx ?: error("always non-null in path completion")
        val processedNames = mutableSetOf<String>()
        collectCompletionVariants(result, completionContext) {
            var processor = it
            processor = applySharedCompletionFilters(ns, resolutionCtx, processor)
            processor = processor.wrapWithFilter { e ->
                // drop already visited items
                if (processedNames.contains(e.name)) return@wrapWithFilter false
                processedNames.add(e.name)
            }
            processor = filterCompletionVariantsByVisibility(pathElement, processor)

            val pathKind = pathElement.pathKind(true)
            processPathResolveVariantsWithExpectedType(
                resolutionCtx,
                pathKind,
                expectedType = completionContext.expectedTy,
                processor
            )
        }

        ProgressManager.checkCanceled()

        addCompletionsForOutOfScopeItems(
            parameters,
            pathElement,
            result,
            completionContext,
            ns,
            processedNames
        )
    }

    private fun addCompletionsForOutOfScopeItems(
        parameters: CompletionParameters,
        path: MvPath,
        result: CompletionResultSet,
        completionContext: MvCompletionContext,
        ns: Set<Namespace>,
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
                result.prefixMatcher,
                processedNames,
                importContext,
            )

        var candidatesCollector = createProcessor { e ->
            e as CandidateScopeEntry
            val lookupElement = e.element.createLookupElement(
                completionContext,
                priority = UNIMPORTED_ITEM_PRIORITY,
                insertHandler = ImportInsertHandler(parameters, e.candidate)
            )
            result.addElement(lookupElement)
        }
        candidatesCollector =
            applySharedCompletionFilters(ns, completionContext.resolutionCtx!!, candidatesCollector)

        candidatesCollector.processAll(
            candidates.map { CandidateScopeEntry(it.qualName.itemName, it.element, ns, it) }
        )
    }
}

data class CandidateScopeEntry(
    override val name: String,
    override val element: MvNamedElement,
    override val namespaces: Set<Namespace>,
    val candidate: ImportCandidate,
): ScopeEntry {
    override fun doCopyWithNs(namespaces: Set<Namespace>): ScopeEntry =
        this.copy(namespaces = namespaces)
}

fun applySharedCompletionFilters(
    ns: Set<Namespace>,
    resolutionCtx: ResolutionContext,
    processor0: RsResolveProcessor
): RsResolveProcessor {
    var processor = processor0
    processor = filterPathVariantsByUseGroupContext(resolutionCtx, processor)
//    if (MODULE in ns) {
//        processor = removeCurrentModuleItem(resolutionCtx, processor)
//    }
    return processor
}

fun filterCompletionVariantsByVisibility(
    context: MvElement,
    processor: RsResolveProcessor
): RsResolveProcessor {
    return processor.wrapWithFilter { e ->
        // drop invisible items
        if (!e.isVisibleFrom(context)) return@wrapWithFilter false

        true
    }
}

fun filterPathVariantsByUseGroupContext(
    resolutionCtx: ResolutionContext,
    processor: RsResolveProcessor
): RsResolveProcessor {
    val useGroup = resolutionCtx.useSpeck?.parent as? MvUseGroup
    val existingNames = useGroup?.names.orEmpty().toSet()
    return processor.wrapWithFilter { e ->
        // skip existing items, only non-empty for use groups
        if (e.name in existingNames) return@wrapWithFilter false

        // drop Self completion for non-UseGroup items
        if (useGroup == null && e.name == "Self") return@wrapWithFilter false

        true
    }
}

fun removeCurrentModuleItem(
    resolutionCtx: ResolutionContext,
    processor: RsResolveProcessor
): RsResolveProcessor {
    return processor.wrapWithFilter { e ->
        // filter out current module item, skips processing (return true)
        val element = e.element.getOriginalOrSelf()
        if (element is MvModule) {
            val containingModule = resolutionCtx.containingModule?.getOriginalOrSelf()
            if (containingModule != null) {
                return@wrapWithFilter !containingModule.equalsTo(element)
            }
        }
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
                val inference = (ancestor as MvElement).inference(msl) ?: return TyUnknown
                return inferExpectedTy(ancestor, inference)
            }
        }
    }
    return null
}
