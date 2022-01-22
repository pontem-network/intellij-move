package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.moduleRef
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.mslScope
import org.move.lang.core.resolve.processItemsInScopesBottomUp
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems

fun handleItemsWithShadowing(element: MvPath, itemVis: ItemVis, handle: (MvNamedElement) -> Unit) {
    val visited = mutableSetOf<String>()
    processItemsInScopesBottomUp(element, itemVis) {
        if (visited.contains(it.name)) return@processItemsInScopesBottomUp false
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
        val maybePathIdent = parameters.position.parent
        val maybePath = maybePathIdent.parent
        val path = maybePath as? MvPath ?: maybePath.parent as MvPath

        if (parameters.position !== path.referenceNameElement) return

        addCompletions(parameters, path, result)
    }
}

object NamesCompletionProvider : MvPathCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.pathIdent()
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
            val module = moduleRef.reference?.resolve() as? MvModuleDef ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(element)
            }
            processModuleItems(module, itemVis.replace(vs = vs)) {
                val lookup = it.element.createLookupElement()
                result.addElement(lookup)
                false
            }
            return
        }

        handleItemsWithShadowing(element, itemVis) {
            val lookupElement = it.createLookupElement()
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
            val module = moduleRef.reference?.resolve() as? MvModuleDef ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(element)
            }
            processModuleItems(module, itemVis.replace(vs = vs)) {
                val lookup = it.element.createLookupElement()
                result.addElement(lookup)
                false
            }
            return
        }

        handleItemsWithShadowing(element, itemVis) {
            val lookup = it.createLookupElement()
            result.addElement(lookup)
        }
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
            val module = moduleRef.reference?.resolve() as? MvModuleDef ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(element)
            }
            processModuleItems(module, itemVis.replace(vs = vs)) {
                val lookup = it.element.createLookupElement()
                result.addElement(lookup)
                false
            }
            return
        }

        handleItemsWithShadowing(element, itemVis) {
            val lookup = it.createLookupElement()
            result.addElement(lookup)
        }
    }
}
