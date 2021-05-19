package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isSpecElement
import org.move.lang.core.resolve.processNestedScopesUpwards
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.processPublicModuleItems

object NamesCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            PlatformPatterns.or(
                MovePsiPatterns.qualPathIdentifier()
                    .andNot(MovePsiPatterns.qualPathTypeIdentifier()),
                MovePsiPatterns.specIdentifier()
            )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val directParent = parameters.position.parent
        val refElement = directParent as? MoveReferenceElement ?: directParent.parent as MoveReferenceElement

        if (parameters.position !== refElement.referenceNameElement) return

        val namespace = namespaceOf(refElement)
        // if refElement is qual path with module ref present -> get names from the module and return
        if (refElement is MoveQualPathReferenceElement) {
            val moduleRef = refElement.qualPath.moduleRef
            if (moduleRef != null) {
                val module = moduleRef.reference?.resolve() as? MoveModuleDef ?: return
                processPublicModuleItems(module, setOf(namespace)) {
                    if (it.element != null) {
                        val lookup = it.element.createLookupElement(false)
                        result.addElement(lookup)
                    }
                    false
                }
                return
            }
        }

        val visited = mutableSetOf<String>()
        processNestedScopesUpwards(refElement, namespace) {
            if (it.element != null && !visited.contains(it.name)) {
                visited.add(it.name)
                val lookup = it.element.createLookupElement(refElement.isSpecElement())
                result.addElement(lookup)
            }
            false
        }
    }

    private fun namespaceOf(refElement: MoveReferenceElement) =
        when (refElement) {
            is MoveQualTypeReferenceElement -> Namespace.TYPE
            is MoveSchemaReferenceElement -> Namespace.SCHEMA
            else -> Namespace.NAME
        }
}
