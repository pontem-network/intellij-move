package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.lang.core.MvPsiPatterns.path
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.UNIMPORTED_ITEM_PRIORITY
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.completion.getOriginalOrSelf
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.*
import org.move.lang.core.resolve2.PathKind
import org.move.lang.core.resolve2.pathKind
import org.move.lang.core.resolve2.ref.ResolutionContext
import org.move.lang.core.resolve2.ref.processPathResolveVariants
import org.move.lang.core.types.infer.inferExpectedTy
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

fun interface CompletionFilter {
    fun removeEntry(entry: ScopeEntry, ctx: ResolutionContext): Boolean
}

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

        val parentElement = pathElement.rootPath().parent
        val resolutionCtx = ResolutionContext(pathElement)
        val msl = pathElement.isMslScope
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(pathElement, msl)

        // 0x1::m::{a, b, Self}
        //               //^
        if (resolutionCtx.isUseSpeck) {
            val useGroup = resolutionCtx.useSpeck?.parent as? MvUseGroup
            if (useGroup != null && "Self" !in useGroup.names) {
                result.addElement(createSelfLookup())
            }
        }

        val ns = buildSet {
            val pathKind = pathElement.pathKind()
            if (resolutionCtx.isUseSpeck) {
                when (pathKind) {
                    is PathKind.QualifiedPath.Module -> add(MODULE)
                    is PathKind.QualifiedPath -> addAll(Namespace.items())
                    else -> {}
                }
            } else {
                if (pathKind is PathKind.UnqualifiedPath) {
                    add(MODULE)
                }
                when (parentElement) {
                    is MvPathType -> add(TYPE)
                    is MvSchemaLit -> add(SCHEMA)
                    else -> {
                        add(NAME)
                        add(FUNCTION)
                    }
                }
            }
        }
        val structAsType = TYPE in ns

        val completionContext = CompletionContext(
            pathElement,
            msl,
            expectedTy,
            resolutionCtx = resolutionCtx,
            structAsType,
        )
        addVariants(
            pathElement, parameters, completionContext, ns, result,
            listOf(
                CompletionFilter { e, ctx ->
                    // filter out current module, skips processing if true
                    val element = e.element.getOriginalOrSelf()
                    if (element is MvModule) {
                        val containingModule = ctx.containingModule?.getOriginalOrSelf()
                        if (containingModule != null) {
                            return@CompletionFilter containingModule.equalsTo(element)
                        }
                    }
                    false
                })
        )
    }

    fun addVariants(
        pathElement: MvPath,
        parameters: CompletionParameters,
        completionContext: CompletionContext,
        ns: Set<Namespace>,
        result: CompletionResultSet,
        completionFilters: List<CompletionFilter> = emptyList()
    ) {
        val resolutionCtx = completionContext.resolutionCtx ?: error("always non-null in path completion")
        val processedNames = mutableSetOf<String>()
        collectCompletionVariants(result, completionContext) {
            val processor = it
                .wrapWithFilter { e ->
                    // custom completion filters
                    for (completionFilter in completionFilters) {
                        if (completionFilter.removeEntry(e, resolutionCtx)) return@wrapWithFilter false
                    }

                    // drop already visited items
                    if (processedNames.contains(e.name)) return@wrapWithFilter false
                    processedNames.add(e.name)

                    // drop invisible items
                    if (!e.isVisibleFrom(pathElement)) return@wrapWithFilter false

                    true
                }
            val pathKind = pathElement.pathKind(overwriteNs = ns)
            processPathResolveVariants(resolutionCtx, pathKind, processor)
        }

        // disable auto-import in module specs for now
        if (pathElement.containingModuleSpec != null) return

        // no out-of-scope completions for use specks
        if (pathElement.isUseSpeck) return

        val originalPathElement = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext = ImportContext.from(originalPathElement, ns)
        val candidates =
            ImportCandidateCollector.getCompletionCandidates(
                parameters,
                result.prefixMatcher,
                processedNames,
                importContext,
            )
        candidates.forEach { candidate ->
            val entry = SimpleScopeEntry(candidate.qualName.itemName, candidate.element, ns)
            for (completionFilter in completionFilters) {
                if (completionFilter.removeEntry(entry, resolutionCtx)) return@forEach
            }
            val lookupElement = candidate.element.createLookupElement(
                completionContext,
                priority = UNIMPORTED_ITEM_PRIORITY,
                insertHandler = ImportInsertHandler(parameters, candidate)
            )
            result.addElement(lookupElement)
        }
    }

    private fun createSelfLookup() = LookupElementBuilder.create("Self").bold()
}

fun getExpectedTypeForEnclosingPathOrDotExpr(element: MvReferenceElement, msl: Boolean): Ty? {
    for (ancestor in element.ancestors) {
        if (element.endOffset < ancestor.endOffset) continue
        if (element.endOffset > ancestor.endOffset) break
        when (ancestor) {
            is MvPathType,
            is MvRefExpr,
            is MvDotExpr -> {
                val inference = (ancestor as MvElement).inference(msl) ?: return TyUnknown
                return inferExpectedTy(ancestor, inference)
            }
        }
    }
    return null
}
