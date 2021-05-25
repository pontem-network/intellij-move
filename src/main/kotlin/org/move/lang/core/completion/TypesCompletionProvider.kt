package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.MoveQualPathType
import org.move.lang.core.psi.ext.isSpecElement
import org.move.lang.core.resolve.processNestedScopesUpwards
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.processPublicModuleItems

object TypesCompletionProvider : MoveCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MovePsiPatterns.qualPathTypeIdentifier()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val directParent = parameters.position.parent
        val refElement =
            directParent as? MoveQualPathType ?: directParent.parent as MoveQualPathType

        if (parameters.position !== refElement.referenceNameElement) return

        val moduleRef = refElement.qualPath.moduleRef
        if (moduleRef != null) {
            val module = moduleRef.reference?.resolve() as? MoveModuleDef ?: return
            processPublicModuleItems(module, setOf(Namespace.TYPE)) {
                if (it.element != null) {
                    val lookup = it.element.createLookupElement(false)
                    result.addElement(lookup)
                }
                false
            }
            return
        }

//        val module = resolveModule(refElement)
//        if (module != null) {
//            processPublicModuleItems(module, setOf(Namespace.TYPE)) {
//                if (it.element != null) {
//                    val lookup = it.element.createLookupElement(false)
//                    result.addElement(lookup)
//                }
//                false
//            }
//            return
//        }

        processNestedScopesUpwards(refElement, Namespace.TYPE) {
            if (it.element != null) {
                val lookup = it.element.createLookupElement(refElement.isSpecElement())
                result.addElement(lookup)
            }
            false
        }
    }
}
