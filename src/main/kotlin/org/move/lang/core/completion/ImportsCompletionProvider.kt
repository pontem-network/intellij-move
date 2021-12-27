package org.move.lang.core.completion

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MvItemImport
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.moduleImport
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems
import org.move.lang.core.withParent

object ImportsCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() = PlatformPatterns
            .psiElement().withParent<MvItemImport>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val itemImport = parameters.position.parent as MvItemImport
        val moduleRef = itemImport.moduleImport().fqModuleRef
        val namespaces = setOf(Namespace.NAME, Namespace.TYPE)

        if (parameters.position !== itemImport.referenceNameElement) return
        val referredModule = moduleRef.reference?.resolve() as? MvModuleDef ?: return
        val vs = when {
            moduleRef.isSelf -> setOf(Visibility.Internal)
            else -> Visibility.buildSetOfVisibilities(itemImport)
        }
        processModuleItems(referredModule, vs, namespaces) {
            if (it.element != null) {
                val lookup = it.element.createLookupElement(BasicInsertHandler())
                result.addElement(lookup)
            }
            false
        }
    }
}
