package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.completion.createSelfLookup
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvUseItem
import org.move.lang.core.psi.MvUseItemGroup
import org.move.lang.core.psi.ext.isSelfModuleRef
import org.move.lang.core.psi.ext.itemUseSpeck
import org.move.lang.core.psi.ext.names
import org.move.lang.core.psi.refItemScopes
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.letStmtScope
import org.move.lang.core.resolve.processModuleItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.withParent

object ImportsCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() = PlatformPatterns
            .psiElement().withParent<MvUseItem>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val itemImport = parameters.position.parent as MvUseItem
        if (parameters.position !== itemImport.referenceNameElement) return

        val moduleRef = itemImport.itemUseSpeck.fqModuleRef
        val referredModule = moduleRef.reference?.resolve() as? MvModule
            ?: return

        val p = itemImport.parent
        if (p is MvUseItemGroup && "Self" !in p.names) {
            result.addElement(referredModule.createSelfLookup())
        }

        val vs = when {
            moduleRef.isSelfModuleRef -> setOf(Visibility.Internal)
            else -> Visibility.visibilityScopesForElement(itemImport)
        }
        val ns = setOf(Namespace.NAME, Namespace.TYPE, Namespace.FUNCTION)
        val contextScopeInfo =
            ContextScopeInfo(
                letStmtScope = itemImport.letStmtScope,
                refItemScopes = itemImport.refItemScopes,
            )

        val completionContext = CompletionContext(itemImport, contextScopeInfo)
        processModuleItems(referredModule, ns, vs, contextScopeInfo) {
            result.addElement(
                it.element.createLookupElement(
                    completionContext,
                    insertHandler = BasicInsertHandler(),
                    structAsType = true
                )
            )
            false
        }
    }
}
