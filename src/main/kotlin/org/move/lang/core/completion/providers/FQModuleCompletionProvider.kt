package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.MvFQModuleRef
import org.move.lang.core.psi.refItemScopes
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.resolve.letStmtScope
import org.move.lang.core.resolve.processFQModuleRef
import org.move.lang.core.types.Address
import org.move.lang.core.types.address
import org.move.lang.core.withParent
import org.move.lang.moveProject

object FQModuleCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement()
                .withParent<MvFQModuleRef>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val directParent = parameters.position.parent
        val fqModuleRef =
            directParent as? MvFQModuleRef
                ?: directParent.parent as MvFQModuleRef
        if (parameters.position !== fqModuleRef.referenceNameElement) return

        val contextScopeInfo = ContextScopeInfo(
            letStmtScope = fqModuleRef.letStmtScope,
            refItemScopes = fqModuleRef.refItemScopes,
        )
        val completionContext = CompletionContext(fqModuleRef, contextScopeInfo)

        val moveProj = fqModuleRef.moveProject
        val positionAddress = fqModuleRef.addressRef.address(moveProj)

        processFQModuleRef(fqModuleRef) {
            val module = it.element
            val moduleAddress = module.address(moveProj)
            if (Address.eq(positionAddress, moduleAddress)) {
                val lookup = module.createLookupElement(completionContext)
                result.addElement(lookup)
            }
            false
        }
    }
}
