package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.lang.core.MvPsiPatterns
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isSelf
import org.move.lang.core.resolve.processNestedScopesUpwards
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.processModuleItems

abstract class ItemsCompletionProvider: MvCompletionProvider() {
    abstract val namespace: Namespace
    abstract fun fqSplitItem(parameters: CompletionParameters): Pair<MvModuleRef?, MvReferenceElement>

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val (moduleRef, element) = fqSplitItem(parameters)
//
        if (parameters.position !== element.referenceNameElement) return
        if (moduleRef != null) {
            val referredModule = moduleRef.reference?.resolve() as? MvModuleDef ?: return
            val vs = when {
                moduleRef.isSelf -> setOf(Visibility.Internal)
                else -> Visibility.buildSetOfVisibilities(element)
            }
            processModuleItems(referredModule, vs, setOf(namespace)) {
                if (it.element != null) {
                    val lookup = it.element.createLookupElement()
                    result.addElement(lookup)
                }
                false
            }
            return
        }

        val visited = mutableSetOf<String>()
        processNestedScopesUpwards(element, namespace) {
            if (it.element != null && !visited.contains(it.name)) {
                visited.add(it.name)
                val lookup = it.element.createLookupElement()
                result.addElement(lookup)
            }
            false
        }
    }
}

object NamesCompletionProvider: ItemsCompletionProvider() {
    override val elementPattern: ElementPattern<PsiElement>
        get() =
            MvPsiPatterns.pathIdent()
                .andNot(MvPsiPatterns.pathType())

    override val namespace: Namespace get() = Namespace.NAME

    override fun fqSplitItem(parameters: CompletionParameters): Pair<MvModuleRef?, MvReferenceElement> {
        val maybePathIdent = parameters.position.parent
        val maybePath = maybePathIdent.parent
        val path = maybePath as? MvPath ?: maybePath.parent as MvPath
        val moduleRef = path.pathIdent.moduleRef
        return Pair(moduleRef, path)
    }
}

object TypesCompletionProvider: ItemsCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPatterns.pathType()

    override val namespace: Namespace get() = Namespace.TYPE

    override fun fqSplitItem(parameters: CompletionParameters): Pair<MvModuleRef?, MvReferenceElement> {
        val maybePath = parameters.position.parent.parent
        val maybeQualPathType = maybePath.parent
        val refElement =
            maybeQualPathType as? MvPathType
                ?: maybeQualPathType.parent as MvPathType
        val moduleRef = refElement.path.pathIdent.moduleRef
        return Pair(moduleRef, refElement.path)
    }
}
