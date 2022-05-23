package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.ide.inspections.imports.ImportContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.mslScope
import org.move.lang.core.resolve.processItems
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems

fun handleItemsWithShadowing(element: MvPath, itemVis: ItemVis, handle: (MvNamedElement) -> Unit) {
    val visited = mutableSetOf<String>()
    processItems(element, itemVis) {
        if (visited.contains(it.name)) return@processItems false
        visited.add(it.name)

        handle(it.element)
        false
    }
}

abstract class MvPathCompletionProvider : MvCompletionProvider() {

    abstract fun addCompletions(
        parameters: CompletionParameters,
        element: MvPath,
        result: CompletionResultSet
    )

    final override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val maybePath = parameters.position.parent
        val path = maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== path.referenceNameElement) return

        addCompletions(parameters, path, result)
    }
}

object NamesCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.path()
                .andNot(MvPsiPatterns.pathType())
                .andNot(MvPsiPatterns.schemaRef())

    override fun addCompletions(
        parameters: CompletionParameters,
        element: MvPath,
        result: CompletionResultSet
    ) {
        val moduleRef = element.moduleRef
        val itemVis = ItemVis(setOf(Namespace.NAME), msl = element.mslScope)

        if (moduleRef != null) {
            val module = moduleRef.reference?.resolve() as? MvModule
                ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(element)
            }
            processModuleItems(module, itemVis.replace(vs = vs)) {
                val lookup = it.element.createCompletionLookupElement()
                result.addElement(lookup)
                false
            }
            return
        }

        handleItemsWithShadowing(element, itemVis) {
            val lookupElement = it.createCompletionLookupElement()
            result.addElement(lookupElement)
        }
    }
}

object TypesCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPatterns.pathType()

    override fun addCompletions(
        parameters: CompletionParameters,
        element: MvPath,
        result: CompletionResultSet
    ) {
        val moduleRef = element.moduleRef
        val itemVis = ItemVis(setOf(Namespace.TYPE), msl = MslScope.NONE)

        if (moduleRef != null) {
            val module = moduleRef.reference?.resolve() as? MvModule
                ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(element)
            }
            processModuleItems(module, itemVis.replace(vs = vs)) {
                val lookup = it.element.createCompletionLookupElement()
                result.addElement(lookup)
                false
            }
            return
        }

        val processedNames = mutableSetOf<String>()
        handleItemsWithShadowing(element, itemVis) {
            val lookup = it.createCompletionLookupElement()
            result.addElement(lookup)
            it.name?.let { name -> processedNames.add(name) }
        }

//        val pathElement = parameters.originalPosition?.parent as? MvPath ?: return
//        val importContext =
//            ImportContext.from(pathElement, itemVis.replace(vs = setOf(Visibility.Public)))
//        addCompletionsFromIndex(
//            parameters,
//            result,
//            processedNames,
//            importContext
//        )
    }
}

object SchemasCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.schemaRef()

    override fun addCompletions(
        parameters: CompletionParameters,
        element: MvPath,
        result: CompletionResultSet
    ) {
        val moduleRef = element.moduleRef
        val itemVis = ItemVis(setOf(Namespace.SCHEMA), msl = MslScope.EXPR)

        if (moduleRef != null) {
            val module = moduleRef.reference?.resolve() as? MvModule
                ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(element)
            }
            processModuleItems(module, itemVis.replace(vs = vs)) {
                val lookup = it.element.createCompletionLookupElement()
                result.addElement(lookup)
                false
            }
            return
        }

        handleItemsWithShadowing(element, itemVis) {
            val lookup = it.createCompletionLookupElement()
            result.addElement(lookup)
        }
    }
}
