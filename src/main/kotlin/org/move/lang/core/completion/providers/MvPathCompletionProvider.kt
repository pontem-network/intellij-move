package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.UNIMPORTED_ITEM_PRIORITY
import org.move.lang.core.completion.createLookupElementWithContext
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.types.infer.inferExpectedTy
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

abstract class MvPathCompletionProvider: MvCompletionProvider() {

    abstract val namespace: Namespace

    open fun pathScopeInfo(pathElement: MvPath): ContextScopeInfo =
        ContextScopeInfo(
            letStmtScope = pathElement.letStmtScope,
            refItemScopes = pathElement.refItemScopes,
        )

    final override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val maybePath = parameters.position.parent
        val pathElement = maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== pathElement.referenceNameElement) return

        val moduleRef = pathElement.moduleRef
        val namespaces = setOf(this.namespace)
        val pathScopeInfo = pathScopeInfo(pathElement)
        val msl = pathElement.isMslScope
        val expectedTy = getExpectedTypeForEnclosingPathOrDotExpr(pathElement, msl)

        val ctx = CompletionContext(pathElement, namespaces, pathScopeInfo, expectedTy)

        if (moduleRef != null) {
            val module = moduleRef.reference?.resolveWithAliases() as? MvModule
                ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.publicVisibilitiesFor(pathElement)
            }
            processModuleItems(module, namespaces, vs, pathScopeInfo) {
                val lookup = it.element.createLookupElementWithContext(ctx)
                result.addElement(lookup)
                false
            }
            return
        }

        val processedNames = mutableSetOf<String>()
        processItems(pathElement, namespaces, pathScopeInfo) { (name, element) ->
            if (processedNames.contains(name)) {
                return@processItems false
            }
            processedNames.add(name)
            result.addElement(
                element.createLookupElementWithContext(ctx, priority = element.completionPriority)
            )
            false
        }

        // disable auto-import in module specs for now
        if (pathElement.containingModuleSpec != null) return

        val originalPathElement = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext =
            ImportContext.from(
                originalPathElement,
                namespaces,
                setOf(Visibility.Public),
                pathScopeInfo
            )
        val candidates = getImportCandidates(
            parameters,
            result,
            processedNames,
            importContext,
        )
        candidates.forEach { candidate ->
            val lookupElement = candidate.element.createLookupElementWithContext(
                ctx,
                priority = UNIMPORTED_ITEM_PRIORITY,
                insertHandler = ImportInsertHandler(parameters, candidate)
            )
            result.addElement(lookupElement)
        }
    }
}

object NamesCompletionProvider: MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.path()
                .andNot(MvPsiPatterns.pathType())
                .andNot(MvPsiPatterns.schemaLit())

    override val namespace: Namespace get() = Namespace.NAME

//    override fun itemVis(pathElement: MvPath): ItemVis {
//        return ItemVis(
//            setOf(Namespace.NAME),
//            Visibility.none(),
//            mslLetScope = pathElement.mslLetScope,
//            itemScopes = pathElement.itemScopes,
//        )
//    }
}

object FunctionsCompletionProvider: MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.path()
                .andNot(MvPsiPatterns.pathType())
                .andNot(MvPsiPatterns.schemaLit())

    override val namespace: Namespace get() = Namespace.FUNCTION

//    override fun itemVis(pathElement: MvPath): ItemVis {
//        return ItemVis(
//            setOf(Namespace.FUNCTION),
//            Visibility.none(),
//            mslLetScope = pathElement.mslLetScope,
//            itemScopes = pathElement.itemScopes,
//        )
//    }
}

object TypesCompletionProvider: MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPatterns.pathType()

    override val namespace: Namespace get() = Namespace.TYPE

//    override fun itemVis(pathElement: MvPath): ItemVis {
//        return ItemVis(
//            setOf(Namespace.TYPE),
//            Visibility.none(),
//            mslLetScope = pathElement.mslLetScope,
//            itemScopes = pathElement.itemScopes,
//        )
//    }
}

object SchemasCompletionProvider: MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            StandardPatterns.or(
                MvPsiPatterns.schemaLit(), MvPsiPatterns.pathInsideIncludeStmt()
            )

    override val namespace: Namespace get() = Namespace.SCHEMA

    override fun pathScopeInfo(pathElement: MvPath): ContextScopeInfo {
        return ContextScopeInfo(
            letStmtScope = LetStmtScope.EXPR_STMT,
            refItemScopes = pathElement.refItemScopes,
        )
    }
}

private fun getExpectedTypeForEnclosingPathOrDotExpr(element: MvReferenceElement, msl: Boolean): Ty? {
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
