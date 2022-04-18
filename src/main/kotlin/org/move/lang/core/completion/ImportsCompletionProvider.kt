package org.move.lang.core.completion

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.MvItemUse
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvMultiItemUse
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.moduleImport
import org.move.lang.core.psi.ext.names
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems
import org.move.lang.core.withParent

object ImportsCompletionProvider : MvCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() = PlatformPatterns
            .psiElement().withParent<MvItemUse>()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val itemImport = parameters.position.parent as MvItemUse
        val moduleRef = itemImport.moduleImport().fqModuleRef

        if (parameters.position !== itemImport.referenceNameElement) return
        val referredModule = moduleRef.reference?.resolve() as? MvModuleDef ?: return

        val p = itemImport.parent
        if (p is MvMultiItemUse && "Self" !in p.names) {
            result.addElement(referredModule.createSelfLookup())
        }

        val vs = when {
            moduleRef.isSelf -> setOf(Visibility.Internal)
            else -> Visibility.buildSetOfVisibilities(itemImport)
        }
        val ns = setOf(Namespace.NAME, Namespace.TYPE)
        val itemVis = ItemVis(ns, vs)
        processModuleItems(referredModule, itemVis) {
            val lookup = it.element.createCompletionLookupElement(BasicInsertHandler())
            result.addElement(lookup)
            false
        }
    }
}
