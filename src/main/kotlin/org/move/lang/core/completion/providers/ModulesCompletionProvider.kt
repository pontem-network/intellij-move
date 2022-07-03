package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.completion.IMPORTED_MODULE_PRIORITY
import org.move.lang.core.completion.createCompletionLookupElement
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.equalsTo
import org.move.lang.core.psi.ext.folderScope
import org.move.lang.core.psi.ext.itemScope
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.mslScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility

object ModulesCompletionProvider : MvCompletionProvider() {
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
        val itemVis =
            ItemVis(
                setOf(Namespace.MODULE),
                visibilities = Visibility.local(),
                msl = refElement.mslScope,
                itemScope = refElement.itemScope,
                folderScope = refElement.folderScope,
            )
        processItems(refElement, itemVis) {
            val lookup = it.element.createCompletionLookupElement(
                priority = IMPORTED_MODULE_PRIORITY
            )
            result.addElement(lookup)
            it.element.name?.let(processedNames::add)
            false
        }

        val path = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext =
            ImportContext.from(path, itemVis.replace(vs = setOf(Visibility.Public)))
        val containingMod = path.containingModule
        addCompletionsFromIndex(
            parameters,
            result,
            processedNames,
            importContext,
            itemFilter = { containingMod != null && !it.equalsTo(containingMod) }
        )
    }
}
