package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvLetStatement
import org.move.lang.core.psi.MvStruct_
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psiElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems
import org.move.lang.core.withParent

object StructPatCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() =
            PlatformPatterns.psiElement()
                .withParent<MvBindingPat>()
                .withSuperParent(2, psiElement<MvLetStatement>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val bindingPat = parameters.position.parent as MvBindingPat
        val module = bindingPat.containingModule ?: return

        processModuleItems(module, setOf(Visibility.Internal), setOf(Namespace.TYPE)) {
            if (it.element != null) {
                val lookup = it.element.createLookupElement()
                result.addElement(lookup)
            }
            false

        }
    }


}
