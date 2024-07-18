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
import org.move.lang.core.psi.ext.equalsTo
import org.move.lang.core.psi.ext.isMslScope
import org.move.lang.core.psi.ext.names
import org.move.lang.core.psi.ext.rootPath
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.LetStmtScope.EXPR_STMT
import org.move.lang.core.resolve.SimpleScopeEntry
import org.move.lang.core.resolve.createProcessor
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.*
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.wrapWithFilter
import org.move.lang.core.resolve2.pathKind
import org.move.lang.core.resolve2.ref.PathResolutionContext
import org.move.lang.core.resolve2.ref.processPathResolveVariants

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
        val contextScopeInfo =
            if (parentElement is MvSchemaLit)
                ContextScopeInfo(
                    refItemScopes = pathElement.itemScopes,
                    letStmtScope = EXPR_STMT
                ) else ContextScopeInfo.from(pathElement)
        val resolutionCtx = PathResolutionContext(pathElement, contextScopeInfo)
        val msl = pathElement.isMslScope
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(pathElement, msl)

        val completionContext = CompletionContext(
            pathElement,
            contextScopeInfo,
            expectedTy,
            resolutionCtx = resolutionCtx
        )

        // 0x1::m::{a, b, Self}
        //               //^
        if (resolutionCtx.isUseSpeck) {
            val useGroup = resolutionCtx.useSpeck?.parent as? MvUseGroup
            if (useGroup != null && "Self" !in useGroup.names) {
                result.addElement(createSelfLookup())
            }
        }

        val ns = buildSet {
            add(MODULE)
            when (parentElement) {
                is MvPathType -> add(TYPE)
                is MvSchemaLit -> add(SCHEMA)
                else -> {
                    add(NAME)
                    add(FUNCTION)
                }
            }
        }

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
//        val qualifier = pathElement.qualifier
        val structAsType = TYPE in ns
        val resolutionCtx = completionContext.resolutionCtx ?: error("always non-null in path completion")

        var completionCollector = createProcessor { e ->
            val element = e.element as? MvNamedElement ?: return@createProcessor
            val lookup =
                element.createLookupElement(
                    completionContext,
                    structAsType = structAsType,
                    priority = element.completionPriority
                )
            result.addElement(lookup)
        }

        val processedNames = mutableSetOf<String>()
        completionCollector = completionCollector.wrapWithFilter { e ->
            // custom filters
            for (completionFilter in completionFilters) {
                if (completionFilter.removeEntry(e, resolutionCtx)) return@wrapWithFilter false
            }

            if (processedNames.contains(e.name)) return@wrapWithFilter false
            processedNames.add(e.name)

            true
        }

        val pathKind = pathElement.pathKind(overwriteNs = ns)
//        val pathKind = classifyPath(pathElement, overwriteNs = ns)
        processPathResolveVariants(resolutionCtx, pathKind, completionCollector)

        // disable auto-import in module specs for now
        if (pathElement.containingModuleSpec != null) return

        val originalPathElement = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext =
            ImportContext.from(
                originalPathElement,
                ns,
                setOf(Visibility.Public),
                completionContext.contextScopeInfo
            )
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
                structAsType = structAsType,
                priority = UNIMPORTED_ITEM_PRIORITY,
                insertHandler = ImportInsertHandler(parameters, candidate)
            )
            result.addElement(lookupElement)
        }
    }

    private fun createSelfLookup() = LookupElementBuilder.create("Self").bold()
}
