package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.psi.ext.isSpecElement
import org.move.lang.core.resolve.processNestedScopesUpwards
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems

object NamesCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MovePsiPatterns.pathIdent()
                .andNot(MovePsiPatterns.pathType())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val maybePathIdent = parameters.position.parent
        val maybePath = maybePathIdent.parent
        val path = maybePath as? MovePath ?: maybePath.parent as MovePath

        if (parameters.position !== path.referenceNameElement) return

        val namespace = namespaceOf(path)
        // if refElement is path with module ref present -> get names from the module and return
        val moduleRef = path.pathIdent.moduleRef
        if (moduleRef != null) {
            val referredModule = moduleRef.reference?.resolve() as? MoveModuleDef ?: return
            val ns = setOf(namespace)
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal())
                else -> Visibility.buildSetOfVisibilities(path)
            }
            processModuleItems(referredModule, vs, ns) {
                if (it.element != null) {
                    val lookup = it.element.createLookupElement(false)
                    result.addElement(lookup)
                }
                false
            }
            return
        }

        val visited = mutableSetOf<String>()
        processNestedScopesUpwards(path, namespace) {
            if (it.element != null && !visited.contains(it.name)) {
                visited.add(it.name)
                val lookup = it.element.createLookupElement(path.isSpecElement())
                result.addElement(lookup)
            }
            false
        }
    }

    private fun namespaceOf(refElement: MoveReferenceElement) =
        when (refElement) {
            is MoveSchemaReferenceElement -> Namespace.SCHEMA
            else -> Namespace.NAME
        }
}
