package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvLetStmt
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.collectCompletionVariants
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve2.processItemDeclarations
import org.move.lang.core.withParent

object StructPatCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement()
                .withParent<MvBindingPat>()
                .withSuperParent(2, psiElement<MvLetStmt>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val bindingPat = parameters.position.parent as MvBindingPat
        val module = bindingPat.containingModule ?: return
        val completionCtx = CompletionContext(bindingPat, bindingPat.isMsl())

        val moduleBlock = module.moduleBlock
        if (moduleBlock != null) {
            collectCompletionVariants(result, completionCtx) {
                processItemDeclarations(moduleBlock, setOf(Namespace.TYPE), it)
            }
        }
    }
}
