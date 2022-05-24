package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.mslScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems

abstract class MvPathCompletionProvider : MvCompletionProvider() {

    abstract fun itemVis(pathElement: MvPath): ItemVis

    final override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val maybePath = parameters.position.parent
        val pathElement = maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== pathElement.referenceNameElement) return

        val moduleRef = pathElement.moduleRef
        val itemVis = itemVis(pathElement)

        if (moduleRef != null) {
            val module = moduleRef.reference?.resolve() as? MvModule
                ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(pathElement)
            }
            processModuleItems(module, itemVis.replace(vs = vs)) {
                val lookup = it.element.createCompletionLookupElement(ns = itemVis.namespaces)
                result.addElement(lookup)
                false
            }
            return
        }

        val processedNames = mutableSetOf<String>()
        processItems(pathElement, itemVis) {
            if (processedNames.contains(it.name)) return@processItems false
            processedNames.add(it.name)

            val lookupElement = it.element.createCompletionLookupElement(ns = itemVis.namespaces)
            result.addElement(lookupElement)

            false
        }

        val originalPathElement = parameters.originalPosition?.parent as? MvPath ?: return
        val importContext =
            ImportContext.from(originalPathElement, itemVis.replace(vs = setOf(Visibility.Public)))
        addCompletionsFromIndex(
            parameters,
            result,
            processedNames,
            importContext
        )
    }
}

object NamesCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.path()
                .andNot(MvPsiPatterns.pathType())
                .andNot(MvPsiPatterns.schemaRef())

    override fun itemVis(pathElement: MvPath): ItemVis {
        return ItemVis(setOf(Namespace.NAME), msl = pathElement.mslScope)
    }
}

object TypesCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPatterns.pathType()

    override fun itemVis(pathElement: MvPath): ItemVis {
        return ItemVis(setOf(Namespace.TYPE), msl = MslScope.NONE)
    }
}

object SchemasCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.schemaRef()

    override fun itemVis(pathElement: MvPath): ItemVis {
        return ItemVis(setOf(Namespace.SCHEMA), msl = MslScope.EXPR)
    }
}
