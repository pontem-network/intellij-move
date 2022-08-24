package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.ide.inspections.imports.ImportInsertHandler
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.UNIMPORTED_ITEM_PRIORITY
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.functionInferenceCtx
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

abstract class MvPathCompletionProvider : MvCompletionProvider() {

    abstract fun itemVis(pathElement: MvPath): ItemVis

    final override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val maybePath = parameters.position.parent
        val pathElement = maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== pathElement.referenceNameElement) return

        val moduleRef = pathElement.moduleRef
        val itemVis = itemVis(pathElement)
        val inferenceCtx = pathElement.functionInferenceCtx()
        val expectedTy =
            getExpectedTypeForEnclosingPathOrDotExpr(pathElement, inferenceCtx)
        val ctx = CompletionContext(pathElement, itemVis, expectedTy)

        if (moduleRef != null) {
            val module = moduleRef.reference?.resolve() as? MvModule
                ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(pathElement)
            }
            processModuleItems(module, itemVis.copy(visibilities = vs)) {
                val lookup = it.element.createLookupElement(ctx)
                result.addElement(lookup)
                false
            }
            return
        }

        val processedNames = mutableSetOf<String>()
        processItems(pathElement, itemVis) {
            if (processedNames.contains(it.name)) return@processItems false
            processedNames.add(it.name)

            val lookupElement = it.element.createLookupElement(
                ctx,
                priority = it.element.completionPriority
            )
            result.addElement(lookupElement)

            false
        }

        // disable auto-import in module specs for now
        if (pathElement.containingModuleSpec != null) return

        val originalPathElement = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext =
            ImportContext.from(originalPathElement, itemVis.copy(visibilities = setOf(Visibility.Public)))
        val candidates = getImportCandidates(
            parameters,
            result,
            processedNames,
            importContext
        )
        candidates.forEach { candidate ->
            val lookupElement = candidate.element.createLookupElement(
                ctx,
                priority = UNIMPORTED_ITEM_PRIORITY,
                insertHandler = ImportInsertHandler(parameters, candidate)
            )
            result.addElement(lookupElement)
        }
    }
}

object NamesCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.path()
                .andNot(MvPsiPatterns.pathType())
                .andNot(MvPsiPatterns.schemaRef())

    override fun itemVis(pathElement: MvPath): ItemVis {
        return ItemVis(
            setOf(Namespace.NAME),
            Visibility.none(),
            mslScope = pathElement.mslScope,
            itemScope = pathElement.itemScope,
        )
    }
}

object TypesCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPatterns.pathType()

    override fun itemVis(pathElement: MvPath): ItemVis {
        return ItemVis(
            setOf(Namespace.TYPE),
            Visibility.none(),
            mslScope = pathElement.mslScope,
            itemScope = pathElement.itemScope,
        )
    }
}

object SchemasCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.schemaRef()

    override fun itemVis(pathElement: MvPath): ItemVis {
        return ItemVis(
            setOf(Namespace.SCHEMA),
            Visibility.none(),
            mslScope = MslScope.EXPR,
            itemScope = pathElement.itemScope,
        )
    }
}

private fun getExpectedTypeForEnclosingPathOrDotExpr(element: MvReferenceElement, ctx: InferenceContext): Ty {
    for (ancestor in element.ancestors) {
        if (element.endOffset < ancestor.endOffset) continue
        if (element.endOffset > ancestor.endOffset) break
        when (ancestor) {
            is MvRefExpr -> return ancestor.expectedTy(ctx)
            is MvDotExpr -> return ancestor.expectedTy(ctx)
        }
    }
    return TyUnknown
}
