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
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.psi.ext.itemScope
import org.move.lang.core.psi.ext.toAddress
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.mslScope
import org.move.lang.core.resolve.processFQModuleRef
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.withParent

object FQModuleCompletionProvider : MvCompletionProvider() {
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

        val itemVis = ItemVis(
            namespaces = setOf(Namespace.MODULE),
            visibilities = Visibility.none(),
            mslScope = fqModuleRef.mslScope,
            itemScope = fqModuleRef.itemScope,
        )
        val completionContext = CompletionContext(fqModuleRef, itemVis)
        val positionAddress = fqModuleRef.addressRef.toAddress()
        processFQModuleRef(fqModuleRef) {
            val module = it.element
            if (positionAddress == module.addressRef()?.toAddress()) {
                val lookup = module.createLookupElement(completionContext)
                result.addElement(lookup)
            }
            false
        }
    }
}
