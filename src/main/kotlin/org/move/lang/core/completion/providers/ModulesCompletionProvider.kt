package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.*
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.containingModuleSpec
import org.move.lang.core.psi.ext.equalsTo
import org.move.lang.core.psi.refItemScopes
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.letStmtScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility

object ModulesCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.path()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val maybePath = parameters.position.parent
        val refElement =
            maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== refElement.referenceNameElement) return
        if (refElement.moduleRef != null) return

        val processedNames = mutableSetOf<String>()
        val namespaces = setOf(Namespace.MODULE)
        val contextScopeInfo =
            ContextScopeInfo(
                letStmtScope = refElement.letStmtScope,
                refItemScopes = refElement.refItemScopes,
            )
        val completionCtx = CompletionContext(refElement, contextScopeInfo)
        processItems(refElement, namespaces, contextScopeInfo) { (name, element) ->
            result.addElement(
                element.createLookupElement(completionCtx, priority = IMPORTED_MODULE_PRIORITY)
            )
            processedNames.add(name)
            false
        }

        // disable auto-import in module specs for now
        if (refElement.containingModuleSpec != null) return

        val path = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext =
            ImportContext.from(
                path,
                namespaces,
                setOf(Visibility.Public),
                contextScopeInfo
            )
        val containingMod = path.containingModule
        val candidates = getImportCandidates(parameters, result, processedNames, importContext,
                                             itemFilter = {
                                                 containingMod != null && !it.equalsTo(
                                                     containingMod
                                                 )
                                             })
        candidates.forEach { candidate ->
            val lookupElement =
                candidate.element.createLookupElement(
                    completionCtx,
                    structAsType = Namespace.TYPE in importContext.namespaces,
                    priority = UNIMPORTED_ITEM_PRIORITY,
                    insertHandler = ImportInsertHandler(parameters, candidate)
                )
            result.addElement(lookupElement)
        }
    }
}
